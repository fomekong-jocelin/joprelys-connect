# HOS-RBAC-001-C — Conception technique

## But

Remplacer le droit monolithique `HOSPITALIZATION_MANAGE` sur les écritures d’hospitalisation par des permissions de capacité métier étroite, testables et attribuables indépendamment.

## Surface API protégée

| Endpoint | Permission |
|---|---|
| `POST /api/hospitalizations` | `HOSPITALIZATION_ADMIT` |
| `POST /api/hospitalizations/{id}/notes` | `HOSPITALIZATION_NOTE_WRITE` |
| `POST /api/hospitalizations/{id}/consents` | `HOSPITALIZATION_CONSENT_MANAGE` |
| `POST /api/hospitalizations/{id}/daily-cares` | `HOSPITALIZATION_CARE_WRITE` |
| `POST /api/hospitalizations/{id}/medication-administrations` | `HOSPITALIZATION_MEDICATION_ADMINISTER` |
| `POST /api/hospitalizations/{id}/patient-consumptions` | `HOSPITALIZATION_CONSUMABLE_WRITE` |

Les lectures restent sous `HOSPITALIZATION_READ`. Les permissions déjà séparées de transfert, sortie médicale, départ physique et circuits de lit ne changent pas.

## Source de vérité RBAC

Les permissions sont déclarées dans `RbacCatalog`. `RbacBootstrap` appelle `RbacStore.seedCatalog()` au démarrage :

- les nouvelles permissions sont créées ou mises à jour ;
- les rôles système sont resynchronisés ;
- les affectations de rôles utilisateurs existantes sont conservées ;
- les rôles personnalisés ne sont pas modifiés automatiquement.

Ce dernier point est intentionnel : convertir automatiquement `HOSPITALIZATION_MANAGE` en six droits recréerait un privilège global.

## Matrice de responsabilités

### Médecin

Admission, note, consentement, soin, transfert et décision médicale de sortie. Pas d’administration médicamenteuse ni de consommation par défaut.

### Infirmier

Note, soin, administration médicamenteuse, consommables et transfert. Pas d’admission, consentement clinique, décision de sortie ni départ physique.

### Responsable hospitalisation

Admission administrative, transfert, départ physique et gestion opérationnelle des lits. Aucun droit clinique d’écriture.

### Administrateur clinique

Conserve toutes les permissions non-plateforme via le mécanisme `without(all, ...)` existant.

## Invariants

1. Une autorisation de prescription ne vaut jamais autorisation d’administration.
2. Un rôle de coordination des lits ne vaut jamais autorisation d’écriture clinique.
3. Une permission historique ne doit pas servir de fallback à une permission dédiée.
4. Les refus d’autorisation sont réalisés avant l’entrée dans le service applicatif par `@PreAuthorize`.
5. La séparation actuelle reste RBAC tenant-wide ; la restriction par unité/équipe/relationship nécessite un futur ABAC.

## Tests

- réflexion sur les annotations `@PreAuthorize` des six endpoints ;
- présence des six permissions dans le catalogue ;
- tests positifs/négatifs des rôles `MEDECIN`, `INFIRMIER`, `RESPONSABLE_HOSPITALISATION` ;
- non-régression des rôles hygiène/maintenance ;
- suite Maven complète via CI.

## Risques résiduels

- les notes ne distinguent pas encore note médicale et transmission infirmière ;
- l’admission ne sépare pas ordre médical et exécution administrative ;
- le consentement combine encore acte clinique et preuve documentaire ;
- les permissions ne sont pas limitées par unité de soins ;
- les rôles personnalisés doivent être revus explicitement avant production ;
- le frontend doit être progressivement aligné pour masquer les commandes que l’API refusera désormais.
