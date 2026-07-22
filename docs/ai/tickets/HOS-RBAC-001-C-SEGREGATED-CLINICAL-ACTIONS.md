# HOS-RBAC-001-C — Séparer les actions cliniques d’hospitalisation

## Statut

`IMPLEMENTATION EN COURS / QA CI EN COURS / VALIDATIONS EXTERNES EN ATTENTE`

## Contexte

Après les incréments #100 à #104, les opérations de capacité, transfert, décision médicale de sortie, départ physique, nettoyage et maintenance disposent de permissions dédiées. Les écritures courantes du séjour restaient toutefois regroupées sous `HOSPITALIZATION_MANAGE` :

- admission ;
- note d’évolution ;
- consentement ;
- soin journalier ;
- administration médicamenteuse ;
- consommable patient.

Ce regroupement permettait à un rôle autorisé pour une seule activité d’accéder techniquement à toutes les autres. Il empêchait également une interface fidèle au principe du moindre privilège.

## Objectif

Remplacer l’autorisation historique par des permissions correspondant exactement aux commandes existantes, sans introduire un faux droit de prescription et sans modifier les payloads HTTP.

## Permissions ajoutées

| Permission | Commande couverte |
|---|---|
| `HOSPITALIZATION_ADMIT` | créer un séjour actif et affecter un lit |
| `HOSPITALIZATION_NOTE_WRITE` | ajouter une note ou transmission au séjour |
| `HOSPITALIZATION_CONSENT_RECORD` | enregistrer un consentement et son document |
| `HOSPITALIZATION_CARE_RECORD` | consigner un soin effectivement réalisé |
| `HOSPITALIZATION_MEDICATION_ADMINISTER` | tracer une administration médicamenteuse réalisée |
| `HOSPITALIZATION_CONSUMABLE_RECORD` | tracer les consommables réellement utilisés |

`HOSPITALIZATION_MANAGE` reste dans le catalogue uniquement pour permettre l’inventaire et la migration des rôles personnalisés. Aucun endpoint hospitalier ne l’accepte comme fallback.

## Décision médicament

L’endpoint actuel est :

```text
POST /api/hospitalizations/{id}/medication-administrations
```

Il ne crée aucune prescription. Le nom de permission reprend donc l’intention réelle `ADMINISTER`. Un médecin ne reçoit pas automatiquement cette permission et un infirmier ne reçoit pas de droit de prescription par implication.

## Matrice de rôles système

| Rôle | Admission | Notes | Consentement | Soins | Administration médicament | Consommables |
|---|---:|---:|---:|---:|---:|---:|
| `MEDECIN` | oui | oui | oui | non | non | non |
| `INFIRMIER` | non | oui | non | oui | oui | oui |
| `RESPONSABLE_HOSPITALISATION` | oui | non | non | non | non | non |
| `AGENT_HYGIENE` | non | non | non | non | non | non |
| `TECHNICIEN_MAINTENANCE` | non | non | non | non | non | non |
| `ADMIN_CLINIQUE` | oui | oui | oui | oui | oui | oui |

Le médecin conserve le droit de décision médicale de sortie. L’infirmier conserve le transfert, mais ne peut ni admettre ni enregistrer un consentement. Le responsable hospitalisation peut réaliser l’admission opérationnelle et gérer le parcours de lit sans écrire les actes cliniques.

## Endpoints modifiés

| Endpoint | Permission |
|---|---|
| `POST /api/hospitalizations` | `HOSPITALIZATION_ADMIT` |
| `POST /api/hospitalizations/{id}/notes` | `HOSPITALIZATION_NOTE_WRITE` |
| `POST /api/hospitalizations/{id}/consents` | `HOSPITALIZATION_CONSENT_RECORD` |
| `POST /api/hospitalizations/{id}/daily-cares` | `HOSPITALIZATION_CARE_RECORD` |
| `POST /api/hospitalizations/{id}/medication-administrations` | `HOSPITALIZATION_MEDICATION_ADMINISTER` |
| `POST /api/hospitalizations/{id}/patient-consumptions` | `HOSPITALIZATION_CONSUMABLE_RECORD` |

Les endpoints de lecture restent sous `HOSPITALIZATION_READ`. Les comptes rendus opératoires restent sous `CLINICAL_WRITE`, car leur contrat était déjà séparé.

## Interface Angular

Le composant historique utilisait un booléen unique `canModify`. Le contrat de template est conservé pour limiter le risque de régression, mais la valeur est désormais résolue selon la zone active :

- aucun séjour actif : admission ;
- onglet notes : notes ;
- onglet consentements : consentement ;
- onglet soins : soin ;
- onglet médicaments : administration ;
- onglet consommables : consommable ;
- onglet CRO : `CLINICAL_WRITE`.

Les méthodes sensibles du composant parent revérifient également la permission avant d’envoyer la commande. Le backend reste la source d’autorisation.

## Compatibilité et déploiement

- aucune URL supprimée ou renommée ;
- aucun payload modifié ;
- aucun schéma de base de données modifié ;
- les rôles système sont resynchronisés par le bootstrap RBAC ;
- les rôles personnalisés basés sur `HOSPITALIZATION_MANAGE` doivent être remappés explicitement ;
- les utilisateurs doivent renouveler leur JWT après synchronisation.

Aucun fallback `hasAnyAuthority(nouveau, HOSPITALIZATION_MANAGE)` n’est ajouté : il annulerait la séparation de tâches.

## Tests ajoutés ou renforcés

### Backend

- présence des six permissions dans le catalogue ;
- matrice positive et négative médecin/infirmier/responsable hospitalisation ;
- absence des écritures cliniques sur les rôles hygiène et maintenance ;
- annotation exacte de chaque endpoint ;
- test d’architecture interdisant `HOSPITALIZATION_MANAGE` sur les endpoints hospitaliers.

### Frontend

- résolution de la permission d’admission lorsqu’aucun séjour n’est actif ;
- résolution par onglet pour les six zones d’écriture ;
- maintien de `CLINICAL_WRITE` pour le CRO ;
- absence de toute consultation de `HOSPITALIZATION_MANAGE` ;
- profil notes seul incapable d’afficher l’administration médicamenteuse.

## Risques résiduels

- les notes restent génériques : le modèle ne distingue pas encore note médicale, transmission infirmière et ordre clinique ;
- l’admission combine encore décision clinique, création du séjour et affectation du lit ;
- les soins et consommables ne vérifient pas encore une affectation d’unité ou une relation de soin ;
- l’administration médicamenteuse n’est pas encore contrainte par une prescription active ;
- aucune délégation temporaire ni habilitation clinique structurée ;
- les rôles personnalisés exigent une revue manuelle avant production.

## Validations externes requises

- validation médicale de la matrice médecin/infirmier ;
- validation cadre infirmier des soins, médicaments et consommables ;
- validation juridique/DPO du circuit de consentement ;
- validation responsable hospitalisation de l’admission opérationnelle ;
- recette avec comptes représentatifs et rôles personnalisés ;
- inventaire des consommateurs de `HOSPITALIZATION_MANAGE` avant retrait futur du catalogue.

## Critères de sortie

- [x] six permissions dédiées créées ;
- [x] rôles système remappés ;
- [x] endpoints protégés sans fallback legacy ;
- [x] interface alignée par zone ;
- [x] tests négatifs ajoutés ;
- [ ] CI backend et frontend verte sur le head final ;
- [ ] validation métier et RSSI ;
- [ ] recette multi-profils ;
- [ ] remappage des rôles personnalisés avant production.
