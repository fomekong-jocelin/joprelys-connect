# HOS-RBAC-001-C — Conception technique

## Décision

Les écritures d’un séjour hospitalier sont autorisées par capacité métier et non par un droit générique. Les contrôleurs restent responsables uniquement de la frontière HTTP et de la vérification déclarative Spring Security ; les règles métier demeurent dans les services applicatifs existants.

## Autorisations des commandes

| Commande HTTP | Permission |
|---|---|
| `POST /api/hospitalizations` | `HOSPITALIZATION_ADMIT` |
| `POST /api/hospitalizations/{id}/notes` | `HOSPITALIZATION_NOTE_WRITE` |
| `POST /api/hospitalizations/{id}/consents` | `HOSPITALIZATION_CONSENT_MANAGE` |
| `POST /api/hospitalizations/{id}/daily-cares` | `HOSPITALIZATION_CARE_WRITE` |
| `POST /api/hospitalizations/{id}/medication-administrations` | `HOSPITALIZATION_MEDICATION_ADMINISTER` |
| `POST /api/hospitalizations/{id}/patient-consumptions` | `HOSPITALIZATION_CONSUMABLE_MANAGE` |

Les lectures restent protégées par `HOSPITALIZATION_READ`. Le transfert, la décision médicale de sortie et le départ physique conservent leurs permissions spécialisées. Les comptes rendus opératoires restent sous `CLINICAL_WRITE`.

## Prescription et administration

`medication-administrations` représente un acte réalisé et horodaté. Il ne crée ni prescription, ni posologie planifiée, ni ordre médicamenteux. Le droit `PHARMACY_PRESCRIPTION_READ` ne permet donc pas l’administration. Une future prescription hospitalière devra introduire :

- une entité/commande de prescription ;
- une permission médicale distincte ;
- une relation explicite entre prescription et administration ;
- les règles de modification, arrêt, substitution et traçabilité.

## Catalogue et migration

`HospitalizationPermissionCatalog` centralise les six codes et la matrice minimale des rôles. `HospitalizationRbacBootstrap`, exécuté après le bootstrap RBAC historique :

1. crée ou met à jour les six permissions de façon idempotente ;
2. retire `HOSPITALIZATION_MANAGE` des rôles système migrés ;
3. attribue les permissions spécialisées selon la matrice ;
4. laisse la permission historique disponible temporairement pour l’analyse des rôles personnalisés.

Cette compatibilité transitoire évite de supprimer silencieusement un code encore référencé par un rôle personnalisé. Aucun endpoint concerné ne reconnaît toutefois ce droit historique après la migration.

## Frontend

Chaque composant d’écriture consulte directement la permission correspondant à son action et bloque également la méthode de soumission. Le composant parent applique :

- `HOSPITALIZATION_ADMIT` à l’admission ;
- `HOSPITALIZATION_CONSENT_MANAGE` aux consentements ;
- `CLINICAL_WRITE` au compte rendu opératoire ;
- une permission spécialisée par onglet pour les autres panneaux.

L’interface ne constitue jamais la barrière de sécurité principale : le backend reste autoritaire.

## Tests

- réflexion sur les annotations `@PreAuthorize` des six endpoints ;
- matrice de rôles et absence de droit implicite de prescription ;
- tests composants positifs et négatifs ;
- test spécifique : `PHARMACY_PRESCRIPTION_READ` seul ne permet pas d’administrer ;
- Maven strict, tests Angular et build production via CI.

## Déploiement

1. sauvegarder la matrice des rôles personnalisés ;
2. déployer le backend et laisser les bootstraps resynchroniser le catalogue ;
3. renouveler les JWT ;
4. déployer le frontend aligné ;
5. exécuter la recette multi-profils ;
6. migrer explicitement les rôles personnalisés ;
7. planifier la suppression définitive de `HOSPITALIZATION_MANAGE` lorsqu’aucune affectation ne le référence.

## Risques résiduels

- pas d’ABAC par unité, affectation ou relation de soin ;
- pas de délégation temporelle ;
- prescription hospitalière non modélisée ;
- responsable de séjour non validé par habilitation et affectation ;
- clearance administrative toujours séparée dans HOS-DIS-001-B.
