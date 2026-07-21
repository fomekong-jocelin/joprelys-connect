# Plan de test — Compteur des lits disponibles

## BED-AVAIL-001 — Comptage des quatre états legacy

- **Objectif** : vérifier que seul `FREE` est disponible.
- **Préconditions** : service contenant des lits `FREE`, `OCCUPIED`, `CLEANING`, `MAINTENANCE`.
- **Profil utilisateur** : porteur de `HOSPITALIZATION_READ`.
- **Étapes** : appeler l'endpoint d'occupation.
- **Résultat attendu** : total 4, occupé 1, disponible 1.
- **Contrôles de sécurité** : requête authentifiée et tenant courant.
- **Cas d'erreur** : service absent ou ne permettant pas les chambres.
- **Données à historiser** : aucune mutation dans ce scénario de lecture.

## BED-AVAIL-002 — Maintenance et nettoyage exclus

- **Objectif** : empêcher le retour de la formule `total - occupied`.
- **Préconditions** : un lit libre et un lit occupé.
- **Profil utilisateur** : porteur des permissions de lecture et de gestion existantes.
- **Étapes** : passer le lit libre en maintenance, charger l'occupation, puis le passer en nettoyage et recharger.
- **Résultat attendu** : `availableBedsCount` reste 0 dans les deux états.
- **Contrôles de sécurité** : les permissions existantes restent appliquées.
- **Cas d'erreur** : statut invalide retourne 400 selon le contrat actuel.
- **Données à historiser** : changements de statut déjà audités par l'existant.

## BED-AVAIL-003 — Projection Angular

- **Objectif** : garantir que l'UI utilise le compteur backend.
- **Préconditions** : réponse avec total 4, occupé 1, disponible 1.
- **Profil utilisateur** : lecteur de l'occupation.
- **Étapes** : initialiser la page avec la réponse simulée.
- **Résultat attendu** : la projection affichable vaut 1 et non 3.
- **Contrôles de sécurité** : aucun appel direct hors `SpatialApiService`.
- **Cas d'erreur** : absence de réponse affiche 0.
- **Données à historiser** : aucune.

## Vérifications de livraison

- tests backend ciblés ;
- tests Angular ciblés ;
- build Angular ;
- `git diff --check` ;
- absence de nouvelle configuration, migration, secret, Angular Material ou Tailwind v3.
