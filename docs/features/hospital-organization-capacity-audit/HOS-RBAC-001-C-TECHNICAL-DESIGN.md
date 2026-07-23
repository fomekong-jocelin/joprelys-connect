# HOS-RBAC-001-C — Conception technique de séparation des tâches d’hospitalisation

## 0. État d’intégration

Cette conception est **implémentée et fusionnée dans `main`**.

- PR canonique : **#107** ;
- commit fusionné : `4df94f43ee5943a55ae60bac22794b1a8ff746a4` ;
- workflow PR final : **#1011 vert** ;
- workflow `main` après fusion : **#1012 vert**.

Le code fusionné a été relu dans `RbacCatalog`, `HospitalizationController`, les tests backend d’autorisation et le composant Angular hospitalisation. HOS-RBAC-001-D / #121 poursuit ce chantier en supprimant définitivement la permission legacy `HOSPITALIZATION_MANAGE` au lieu de la conserver pour rétro-compatibilité.

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

La conservation temporaire de `HOSPITALIZATION_MANAGE` prévue initialement dans #107 est **supersédée par HOS-RBAC-001-D**. La permission est retirée du catalogue Java et supprimée du référentiel persistant par V86. Aucun alias ni fallback n'est conservé.

## 4. Matrice des rôles système

| Rôle | Admit | Note | Consent | Care | Med administer | Consumable |
|---|---:|---:|---:|---:|---:|---:|
| `ADMIN_CLINIQUE` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| `MEDECIN` | ✓ | ✓ | ✓ | ✓ | — | — |
| `INFIRMIER` | — | ✓ | ✓ | ✓ | ✓ | ✓ |
| `RESPONSABLE_HOSPITALISATION` | ✓ | — | — | — | — | — |
| `AGENT_HYGIENE` | — | — | — | — | — | — |
| `TECHNICIEN_MAINTENANCE` | — | — | — | — | — | — |

Cette matrice est un **défaut système**, pas une règle réglementaire universelle. Les établissements peuvent utiliser des rôles personnalisés, mais ceux-ci doivent être composés uniquement de permissions explicites existantes. Les futures habilitations professionnelles et délégations datées devront encore restreindre les droits effectifs.

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

`PatientHospitalizationComponent` possède une décision par onglet :

- séjour absent : admission selon `HOSPITALIZATION_ADMIT` ;
- Notes : `HOSPITALIZATION_NOTE_WRITE` ;
- Consentements : `HOSPITALIZATION_CONSENT_RECORD` ;
- Soins : `HOSPITALIZATION_CARE_WRITE` ;
- Médicaments : `HOSPITALIZATION_MEDICATION_ADMINISTER` ;
- Consommables : `HOSPITALIZATION_CONSUMABLE_RECORD` ;
- Bloc/CRO : `CLINICAL_WRITE`.

La continuité urgence → hospitalisation masque le formulaire d’admission et affiche un message explicite si `HOSPITALIZATION_ADMIT` manque. Le service backend reste la source de vérité en cas d’appel direct.

HOS-RBAC-001-D ajoute un cas de non-régression frontend : une authority stale `HOSPITALIZATION_MANAGE` présente seule dans un ancien contexte ne doit autoriser aucune écriture.

## 7. Tests de sécurité

### Backend

`HospitalizationControllerAuthorizationTest` vérifie par réflexion l’autorité exacte de chaque méthode sensible. Une régression vers une permission générique fait échouer le test.

`RbacCatalogHospitalizationClinicalPermissionTest` teste les droits **positifs et négatifs** des rôles système, notamment :

- médecin sans administration médicamenteuse/consommables par défaut ;
- infirmier sans admission ;
- responsable hospitalisation sans écritures cliniques ;
- rôles hygiène/maintenance sans droits cliniques ;
- administrateur clinique avec toutes les permissions établissement ;
- absence complète de `HOSPITALIZATION_MANAGE` du catalogue à partir de HOS-RBAC-001-D.

### Frontend

Le test hospitalisation vérifie :

- admission autorisée avec `HOSPITALIZATION_ADMIT` ;
- mapping de chaque onglet vers sa permission dédiée ;
- absence de fallback legacy ;
- refus fail-closed lorsqu'un contexte stale ne possède que `HOSPITALIZATION_MANAGE`.

## 8. Suppression du reliquat legacy

Le modèle cible ne comporte plus `HOSPITALIZATION_MANAGE`.

HOS-RBAC-001-D / #121 applique cette convergence :

1. retrait de la permission de `RbacCatalog` ;
2. Flyway V86 supprime la ligne persistée ;
3. la FK `role_permissions.permission_code -> permissions.code ON DELETE CASCADE` supprime les associations de rôles personnalisés correspondantes ;
4. aucun mapping automatique n'attribue les nouvelles permissions ;
5. le rôle personnalisé lui-même est conservé ;
6. toute permission hospitalière nécessaire est attribuée explicitement ensuite.

Cette stratégie est volontairement **fail closed** et évite de transformer une dette de rétro-compatibilité en dette de sécurité.

## 9. Menaces et limites résiduelles

HOS-RBAC-001-C/D traite la **granularité de l’action**, mais pas encore le contexte d’autorisation.

Restent ouverts :

- ABAC par établissement/unité ;
- relation de soin patient-professionnel ;
- affectation active du professionnel ;
- habilitations professionnelles et spécialités ;
- délégations temporaires ;
- validation à quatre yeux de certaines actions ;
- correction append-only des actes ;
- clearance administrative et financière de sortie.

Par conséquent GAP-016 reste `PARTIAL` et GAP-017 reste `OPEN` tant que ces contrôles et validations externes ne sont pas terminés.

## 10. Rollback

Après V86, réintroduire `HOSPITALIZATION_MANAGE` n’est pas un rollback acceptable. Le chantier est forward-only pendant la phase de développement : en cas d'anomalie, la correction doit porter sur la permission dédiée ou la matrice du rôle concerné.

## 11. Validations externes requises

- validation RSSI/DPO de la matrice ;
- validation direction médicale des responsabilités médecin/infirmier ;
- validation responsable hospitalisation de l’admission administrative ;
- recette multi-profils avec comptes représentatifs.

## 12. Règle de non-régression

Les prochains travaux doivent partir du code présent dans `main`. Ils ne doivent ni recréer les six permissions, ni dupliquer les annotations `@PreAuthorize`, ni restaurer `HOSPITALIZATION_MANAGE` sous forme de permission, alias ou fallback. Toute évolution supplémentaire doit faire l’objet d’un ticket distinct et cibler exclusivement un risque encore ouvert.
