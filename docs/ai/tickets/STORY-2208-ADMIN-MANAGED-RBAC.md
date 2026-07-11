# STORY-2208 — RBAC administrable par établissement

## Statut
QA

## Priorité
P0 — Sécurité et cohérence des postes métier

## Problème

Le modèle historique stockait un rôle sous forme de chaîne libre dans `users.role`. Les rôles pouvaient être séparés par des virgules, les contrôles d'accès étaient dispersés et l'administrateur clinique ne pouvait pas définir qui avait le droit de réaliser une action.

Les rôles `DAF`, `CAISSIER` et `SECRETAIRE_COMPTABLE` étaient déjà utilisés dans les parcours financiers, mais ils n'étaient pas garantis par un catalogue central et n'étaient pas administrables.

## Objectif

Mettre en place un RBAC tenanté permettant à l'administrateur clinique de gérer les rôles, les permissions et les affectations utilisateurs sans modifier le code.

## Décision d'architecture — comptes plateforme

- `ADMIN_CLINIQUE` est obligatoirement rattaché à un établissement et ne peut administrer que celui-ci.
- `ADMIN_JOPRELYS` et `SUPER_ADMIN` sont des comptes plateforme et ne sont pas obligatoirement rattachés à un établissement.
- un administrateur plateforme sélectionne explicitement l'établissement dont il souhaite administrer le RBAC ;
- le périmètre sélectionné est transmis au backend et validé à chaque lecture ou mutation ;
- les rôles et permissions plateforme ne sont jamais proposés dans la matrice d'un établissement ;
- aucun changement cross-tenant n'est autorisé.

## Rôles système initiaux

- SUPER_ADMIN
- ADMIN_JOPRELYS
- ADMIN_CLINIQUE
- DAF
- SECRETAIRE_COMPTABLE
- CAISSIER
- AGENT_ACCUEIL
- MEDECIN
- INFIRMIER
- BIOLOGISTE
- PHARMACIEN
- GESTIONNAIRE_STOCK
- RESPONSABLE_HOSPITALISATION

## Modèle cible

- `permissions` : catalogue global et stable des capacités applicatives ;
- `roles` : rôles système ou personnalisés, tenantés pour les rôles personnalisés ;
- `role_permissions` : permissions associées à un rôle ;
- `user_roles` : affectation multi-rôles ;
- `rbac_audit_log` : journal des changements de rôles, permissions et affectations.

Le champ historique `users.role` reste temporairement disponible pour migrer les comptes existants et préserver la compatibilité. Il ne doit plus être la source de vérité après la migration.

## Règles de sécurité

1. Seuls `SUPER_ADMIN` et les utilisateurs disposant de `RBAC_MANAGE` peuvent modifier le RBAC.
2. Un administrateur clinique ne peut gérer que son établissement.
3. Un administrateur plateforme doit sélectionner un établissement avant toute opération RBAC tenantée.
4. Un utilisateur ne peut pas modifier ses propres rôles ou permissions.
5. Le dernier administrateur actif d'un établissement ne peut pas être désactivé ni privé de `RBAC_MANAGE`.
6. Les rôles système ne peuvent pas être supprimés.
7. Les rôles plateforme ne peuvent pas être attribués depuis l'administration d'une clinique.
8. Les permissions plateforme ne peuvent pas être ajoutées à un rôle clinique personnalisé.
9. Les changements de permissions sont résolus côté serveur à chaque requête.
10. Le backend reste l'autorité : masquer un bouton dans Angular ne suffit jamais.
11. Toutes les mutations sont auditées.

## Permissions prioritaires

### Administration
- USER_READ
- USER_MANAGE
- RBAC_READ
- RBAC_MANAGE

### Facturation
- BILLING_INVOICE_READ
- BILLING_INVOICE_WRITE
- BILLING_INVOICE_VALIDATE
- BILLING_INVOICE_CANCEL
- BILLING_DISCOUNT_APPLY
- BILLING_CREDIT_NOTE_CREATE

### Caisse
- CASH_QUEUE_READ
- CASH_PAYMENT_COLLECT
- CASH_SESSION_OPEN
- CASH_SESSION_CLOSE
- CASH_MOVEMENT_WRITE
- CASH_HISTORY_READ
- CASH_CLOSEOUT_REPORT_DOWNLOAD

### Assurance et comptabilité
- INSURANCE_BORDEREAU_READ
- INSURANCE_BORDEREAU_CREATE
- INSURANCE_BORDEREAU_SEND
- INSURANCE_BORDEREAU_RECEIVE
- INSURANCE_BORDEREAU_ACCEPT
- INSURANCE_BORDEREAU_REJECT
- INSURANCE_BORDEREAU_SETTLE
- ACCOUNTING_EXPORT

### Domaines cliniques

Le catalogue couvre également les patients, consultations, soins infirmiers, laboratoire, pharmacie, stock, urgences, hospitalisation et gestion des lits.

## Lots réalisés

### Lot 1 — Socle backend
- migrations Flyway ;
- catalogue des permissions ;
- rôles système et matrice par défaut ;
- migration des utilisateurs existants ;
- résolution des permissions effectives ;
- API de lecture des rôles/permissions ;
- tests tenant et escalade de privilèges.

### Lot 2 — Administration
- API CRUD des rôles personnalisés ;
- modification de la matrice des permissions ;
- affectation multi-rôles aux utilisateurs ;
- permissions effectives ;
- garde-fous du dernier admin ;
- audit.

### Lot 3 — Angular
- écran Administration > Rôles et permissions ;
- sélection de l'établissement pour les administrateurs plateforme ;
- matrice responsive par domaines ;
- gestion des rôles personnalisés ;
- affectation des rôles depuis la fiche utilisateur ;
- indication des permissions héritées/effectives ;
- light/dark, clavier et responsive.

### Lot 4 — Migration des gardes
- contrôles prioritaires migrés vers les permissions ;
- compatibilité transitoire avec les rôles historiques ;
- menus et routes dérivés des permissions effectives.

## Critères d'acceptation

- [x] DAF, CAISSIER et SECRETAIRE_COMPTABLE sont disponibles dans le catalogue système ;
- [x] un administrateur peut créer un rôle personnalisé ;
- [x] un administrateur peut associer des permissions à un rôle ;
- [x] un utilisateur peut recevoir plusieurs rôles ;
- [x] les permissions effectives sont calculées comme l'union des rôles actifs ;
- [x] le backend refuse toute action non autorisée ;
- [x] aucune mutation cross-tenant n'est possible ;
- [x] aucune auto-élévation de privilège n'est possible ;
- [x] le dernier administrateur d'un établissement est protégé ;
- [x] les utilisateurs existants conservent leurs accès après migration ;
- [x] les changements sont audités ;
- [x] un admin plateforme sans établissement peut sélectionner une clinique et gérer son RBAC ;
- [x] CI backend/frontend verte ;
- [ ] QA visuelle utilisateur sur le sélecteur d'établissement, la matrice et les affectations.

## Validation technique

- frontend Angular : tests et build production verts ;
- backend Maven strict : vert ;
- test d'intégration dédié au périmètre `ADMIN_JOPRELYS → établissement sélectionné → lecture utilisateurs/rôles → création rôle` ;
- Testcontainers PostgreSQL 16 actif lorsque Docker est disponible ;
- rapports Surefire conservés automatiquement en artefact en cas d'échec CI.

## Issue GitHub

#23 — feat(rbac): ajouter les rôles métier manquants et une administration des permissions
