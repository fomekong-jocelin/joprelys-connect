# STORY-2208 — RBAC administrable par établissement

## Statut
IN_PROGRESS

## Priorité
P0 — Sécurité et cohérence des postes métier

## Problème

Le modèle actuel stocke un rôle sous forme de chaîne libre dans `users.role`. Les rôles peuvent être séparés par des virgules, les contrôles d'accès sont dispersés et l'administrateur clinique ne peut pas définir qui a le droit de réaliser une action.

Les rôles `DAF`, `CAISSIER` et `SECRETAIRE_COMPTABLE` sont déjà utilisés dans les parcours financiers, mais ils ne sont pas garantis par un catalogue central et ne sont pas administrables.

## Objectif

Mettre en place un RBAC tenanté permettant à l'administrateur clinique de gérer les rôles, les permissions et les affectations utilisateurs sans modifier le code.

## Rôles système initiaux

- SUPER_ADMIN
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
3. Un utilisateur ne peut pas modifier ses propres rôles ou permissions.
4. Le dernier administrateur actif d'un établissement ne peut pas être désactivé ni privé de `RBAC_MANAGE`.
5. Les rôles système ne peuvent pas être supprimés.
6. Les changements de permissions doivent invalider les sessions concernées ou être résolus côté serveur à chaque requête.
7. Le backend reste l'autorité : masquer un bouton dans Angular ne suffit jamais.
8. Toutes les mutations sont auditées.

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
Le catalogue doit aussi couvrir accueil, consultations, soins infirmiers, laboratoire, pharmacie, stock et hospitalisation.

## Découpage

### Lot 1 — Socle backend
- migrations Flyway ;
- entités et repositories ;
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
- matrice responsive par domaines ;
- gestion des rôles personnalisés ;
- affectation des rôles depuis la fiche utilisateur ;
- indication des permissions héritées/effectives ;
- light/dark, clavier et responsive.

### Lot 4 — Migration des gardes
- remplacer progressivement les contrôles de rôles par des permissions ;
- maintenir une compatibilité transitoire ;
- supprimer le parsing des rôles séparés par virgules une fois tous les endpoints migrés.

## Critères d'acceptation

- DAF, CAISSIER et SECRETAIRE_COMPTABLE sont disponibles dans le catalogue système ;
- un administrateur peut créer un rôle personnalisé ;
- un administrateur peut associer des permissions à un rôle ;
- un utilisateur peut recevoir plusieurs rôles ;
- les permissions effectives sont calculées comme l'union des rôles actifs ;
- le backend refuse toute action non autorisée ;
- aucune mutation cross-tenant n'est possible ;
- aucune auto-élévation de privilège n'est possible ;
- le dernier administrateur d'un établissement est protégé ;
- les utilisateurs existants conservent leurs accès après migration ;
- les changements sont audités ;
- CI backend/frontend verte.

## Issue GitHub

#23 — feat(rbac): ajouter les rôles métier manquants et une administration des permissions
