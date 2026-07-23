# HOS-RBAC-001-C — Conception technique de séparation des tâches d’hospitalisation

## 0. État d’intégration

Cette conception est **implémentée et fusionnée dans `main`**.

- PR canonique : **#107** ;
- commit fusionné : `4df94f43ee5943a55ae60bac22794b1a8ff746a4` ;
- workflow PR final : **#1011 vert** ;
- workflow `main` après fusion : **#1012 vert** ;
- branche `main` actuelle auditée : `078c3dc5f913f615910fad9f061085bc7acdcfec`.

Le code fusionné a été relu dans `RbacCatalog`, `HospitalizationController`, les tests backend d’autorisation et le composant Angular hospitalisation. La documentation doit donc traiter HOS-RBAC-001-C comme **livré techniquement**, sans recréer son code. Restent uniquement les validations externes, l’inventaire des rôles personnalisés, la recette multi-profils et la préparation du déploiement.

## 1. Décision

Les écritures d’un séjour hospitalier ne doivent plus être autorisées par une permission générique couvrant plusieurs responsabilités métier.

HOS-RBAC-001-C remplace l’usage opérationnel de `HOSPITALIZATION_MANAGE` par des permissions orientées **intention métier**. Cette décision applique le principe du moindre privilège et prépare l’évolution vers un contrôle d’accès combinant RBAC, contexte d’unité, relation de soin, affectation et habilitation professionnelle.

Aucune URL ni structure de payload HTTP n’est modifiée dans cet incrément.

## 2. Problème de sécurité traité

Avant HOS-RBAC-001-C, trois rôles système (`MEDECIN`, `INFIRMIER`, `RESPONSABLE_HOSPITALISATION`) recevaient `HOSPITALIZATION_MANAGE`. La même autorité permettait alors de :

1. créer un séjour ;
2. écrire une note ;
3. tracer un consentement ;
4. tracer un soin ;
5. tracer une administration médicamenteuse ;
6. enregistrer un consommable patient.

Ce regroupement créait plusieurs risques :

- escalade fonctionnelle implicite ;
- impossibilité d’appliquer une séparation médecin/infirmier/gestionnaire ;
- impossibilité d’auditer la raison exacte pour laquelle un rôle possédait un droit ;
- dérive sémantique entre prescription et administration d’un médicament ;
- dérive entre consommation patient et gestion du stock ;
- interface web exposant des actions non pertinentes au profil.

## 3. Nouveau catalogue d’autorisations

| Permission | Intention | N’autorise pas |
|---|---|---|
| `HOSPITALIZATION_ADMIT` | créer le séjour et affecter le premier lit | écrire les actes du séjour |
| `HOSPITALIZATION_NOTE_WRITE` | ajouter une observation/transmission | modifier les autres actes cliniques |
| `HOSPITALIZATION_CONSENT_RECORD` | tracer le consentement, témoin et document associé | décider à la place du patient ou donner l’information médicale |
| `HOSPITALIZATION_CARE_WRITE` | consigner un soin/acte courant | prescrire ou administrer automatiquement un médicament |
| `HOSPITALIZATION_MEDICATION_ADMINISTER` | tracer l’administration effective d’un médicament | prescrire, dispenser ou gérer le stock pharmacie |
| `HOSPITALIZATION_CONSUMABLE_RECORD` | tracer un consommable réellement utilisé | créer ou déplacer le stock |

`HOSPITALIZATION_MANAGE` reste temporairement déclaré au catalogue afin de permettre une migration contrôlée des rôles personnalisés. Il n’est plus distribué aux trois rôles système concernés et ne protège plus les six endpoints de cet incrément.

## 4. Matrice des rôles système

| Rôle | Admit | Note | Consent | Care | Med administer | Consumable |
|---|---:|---:|---:|---:|---:|---:|
| `ADMIN_CLINIQUE` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| `MEDECIN` | ✓ | ✓ | ✓ | ✓ | — | — |
| `INFIRMIER` | — | ✓ | ✓ | ✓ | ✓ | ✓ |
| `RESPONSABLE_HOSPITALISATION` | ✓ | — | — | — | — | — |
| `AGENT_HYGIENE` | — | — | — | — | — | — |
| `TECHNICIEN_MAINTENANCE` | — | — | — | — | — | — |

Cette matrice est un **défaut système**, pas une règle réglementaire universelle. Les établissements pourront utiliser des rôles personnalisés. Les futures habilitations professionnelles et délégations datées devront encore restreindre les droits effectifs.

## 5. Mapping des endpoints

| Endpoint | Autorité |
|---|---|
| `POST /api/hospitalizations` | `HOSPITALIZATION_ADMIT` |
| `POST /api/hospitalizations/{id}/notes` | `HOSPITALIZATION_NOTE_WRITE` |
| `POST /api/hospitalizations/{id}/consents` | `HOSPITALIZATION_CONSENT_RECORD` |
| `POST /api/hospitalizations/{id}/daily-cares` | `HOSPITALIZATION_CARE_WRITE` |
| `POST /api/hospitalizations/{id}/medication-administrations` | `HOSPITALIZATION_MEDICATION_ADMINISTER` |
| `POST /api/hospitalizations/{id}/patient-consumptions` | `HOSPITALIZATION_CONSUMABLE_RECORD` |

Les commandes déjà séparées restent inchangées : transfert, décision médicale de sortie, départ physique, capacité, nettoyage et maintenance.

Les comptes-rendus opératoires restent sous `CLINICAL_WRITE` dans cet incrément. Leur futur découpage doit être traité avec le chantier bloc opératoire afin d’éviter une permission artificielle sans modèle d’équipe, de programme opératoire ni d’habilitations.

## 6. Frontend

Le frontend ne sert jamais de barrière de sécurité primaire. Il reflète néanmoins les autorités afin de ne pas exposer une action qui sera refusée par le backend.

`PatientHospitalizationComponent` possède désormais une décision par onglet :

- séjour absent : admission selon `HOSPITALIZATION_ADMIT` ;
- Notes : `HOSPITALIZATION_NOTE_WRITE` ;
- Consentements : `HOSPITALIZATION_CONSENT_RECORD` ;
- Soins : `HOSPITALIZATION_CARE_WRITE` ;
- Médicaments : `HOSPITALIZATION_MEDICATION_ADMINISTER` ;
- Consommables : `HOSPITALIZATION_CONSUMABLE_RECORD` ;
- Bloc/CRO : `CLINICAL_WRITE`.

La continuité urgence → hospitalisation masque le formulaire d’admission et affiche un message explicite si `HOSPITALIZATION_ADMIT` manque. Le service backend reste la source de vérité en cas d’appel direct.

## 7. Tests de sécurité

### Backend

`HospitalizationControllerAuthorizationTest` vérifie par réflexion l’autorité exacte de chaque méthode sensible. Une régression vers `HOSPITALIZATION_MANAGE` fait échouer le test.

`RbacCatalogHospitalizationClinicalPermissionTest` teste les droits **positifs et négatifs** des rôles système, notamment :

- médecin sans administration médicamenteuse/consommables par défaut ;
- infirmier sans admission ;
- responsable hospitalisation sans écritures cliniques ;
- rôles hygiène/maintenance sans droits cliniques ;
- administrateur clinique avec toutes les permissions établissement.

Les tests RBAC existants vérifient également que `HOSPITALIZATION_MANAGE` n’est plus distribué au médecin, à l’infirmier ni au responsable hospitalisation.

### Frontend

Le test de continuité urgence couvre :

- admission autorisée avec `HOSPITALIZATION_ADMIT` ;
- absence d’appel API et message utilisateur lorsque la permission manque.

La suite Angular et le build production constituent la non-régression globale des templates et dépendances.

## 8. Migration des rôles personnalisés

Le changement est rétrocompatible sur le contrat HTTP mais **pas sémantiquement transparent pour les rôles personnalisés**.

Avant déploiement production :

1. inventorier les rôles personnalisés contenant `HOSPITALIZATION_MANAGE` ;
2. déterminer les intentions réellement nécessaires pour chacun ;
3. attribuer uniquement les nouvelles permissions requises ;
4. resynchroniser le catalogue et les rôles système ;
5. renouveler les JWT/sessions afin de recalculer les authorities ;
6. tester au moins un compte représentatif par profil ;
7. ne retirer la permission legacy du catalogue qu’après preuve qu’aucun rôle/intégrateur ne l’utilise encore.

Il ne faut pas mapper automatiquement `HOSPITALIZATION_MANAGE` vers les six permissions : cela recréerait l’escalade que cette story cherche précisément à supprimer.

## 9. Menaces et limites résiduelles

HOS-RBAC-001-C traite la **granularité de l’action**, mais pas encore le contexte d’autorisation.

Restent ouverts :

- ABAC par établissement/unité ;
- relation de soin patient-professionnel ;
- affectation active du professionnel ;
- habilitations professionnelles et spécialités ;
- délégations temporaires ;
- validation à quatre yeux de certaines actions ;
- correction append-only des actes ;
- clearance administrative et financière de sortie.

Par conséquent GAP-016 reste `PARTIAL` et GAP-017 reste `OPEN`.

## 10. Rollback

Le rollback applicatif peut rétablir l’ancienne version du code, mais il ne faut pas supprimer les nouvelles permissions des référentiels tant que des rôles personnalisés les utilisent.

En cas de problème de déploiement, privilégier :

1. correction de la matrice d’un rôle ciblé ;
2. renouvellement de son jeton ;
3. rollback applicatif si nécessaire.

Réattribuer globalement `HOSPITALIZATION_MANAGE` aux profils n’est pas un rollback sûr : cela réintroduit volontairement une élévation de privilège.

## 11. Validations externes requises

- validation RSSI/DPO de la matrice ;
- validation direction médicale des responsabilités médecin/infirmier ;
- validation responsable hospitalisation de l’admission administrative ;
- revue des rôles personnalisés ;
- recette multi-profils avec comptes représentatifs.

## 12. Règle de non-régression

Les prochains travaux doivent partir du code présent dans `main`. Ils ne doivent ni recréer les six permissions, ni dupliquer les annotations `@PreAuthorize`, ni restaurer `HOSPITALIZATION_MANAGE` comme permission de secours. Toute évolution supplémentaire doit faire l’objet d’un ticket distinct et cibler exclusivement un risque encore ouvert.