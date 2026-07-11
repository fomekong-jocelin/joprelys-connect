# ISSUE-21 — Administration RBAC clinique

- **Statut** : QA
- **Priorité** : P0
- **Issue GitHub** : #21
- **Pull Request** : #22
- **Branche** : `feature/issue-21-clinic-rbac-management`

## Constat

Des rôles financiers étaient déjà utilisés dans les contrôles de sécurité (`CAISSIER`, `DAF`, `SECRETAIRE_COMPTABLE`) sans être proposés dans l’administration du personnel. L’interface limitait également chaque utilisateur à un seul rôle, alors que le backend acceptait déjà une chaîne multi-rôles.

## Objectif

Permettre à l’administrateur clinique d’affecter un ou plusieurs rôles système à chaque collaborateur et de comprendre les responsabilités associées.

## Contrat retenu

- catalogue de rôles système centralisé côté backend ;
- rôles attribuables : `ADMIN_CLINIQUE`, `DAF`, `SECRETAIRE_COMPTABLE`, `CAISSIER`, `AUDITEUR`, `MEDECIN`, `INFIRMIER`, `AGENT_ACCUEIL`, `PHARMACIEN`, `BIOLOGISTE` ;
- rôles `ADMIN_JOPRELYS` et `PATIENT` non administrables par une clinique ;
- attribution multi-rôles, normalisée et dédupliquée ;
- changements de rôle et désactivation appliqués aux jetons existants dès la requête suivante ;
- impossibilité de modifier ses propres rôles ou son propre statut ;
- impossibilité de retirer/désactiver le dernier administrateur clinique actif ;
- isolation stricte par établissement ;
- annuaire financier et gouvernance visible uniquement des administrateurs.

## Hors périmètre

- création libre de rôles personnalisés ;
- modification dynamique des permissions techniques d’un rôle système ;
- délégation inter-établissements.

## Validation technique

- [x] compilation backend ;
- [x] tests backend du catalogue, du multi-rôle, du tenant et des jetons existants ;
- [x] vérification de l’application immédiate des changements et désactivations ;
- [x] tests Angular de la matrice de rôles ;
- [x] tests du guard avec actualisation serveur ;
- [x] build Angular production ;
- [x] pipeline permanent restauré et CI finale verte ;
- [ ] QA responsive et thèmes clair/sombre ;
- [ ] validation utilisateur des libellés et responsabilités ;
- [ ] revue et fusion après validation utilisateur.

## Résultat CI

- Maven `clean verify` : succès ;
- H2 et PostgreSQL 16 via Testcontainers : succès ;
- tests Angular : succès ;
- build Angular production : succès.
