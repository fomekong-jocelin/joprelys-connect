# ADR — Révocation persistante des sessions et access tokens

- Statut : accepté pour STORY-2402
- Date : 2026-07-14
- Décideurs : équipe Joprelys Connect

## Contexte

V66 introduit des sessions persistantes et un claim JWT optionnel `sid`. Le dépôt conserve toutefois une blacklist JTI en mémoire, perdue au redémarrage et non partagée entre instances. La story #33 exige une révocation immédiate, distribuée et tenantée, ainsi qu’une détection du rejeu de refresh token.

## Décision

1. Les JWT du personnel portant `sid` sont validés contre `auth_sessions` à chaque requête authentifiée.
2. La révocation d’une session invalide immédiatement tous les access tokens portant son `sid`.
3. La blacklist mémoire est supprimée.
4. Les JWT historiques sans `sid` utilisent temporairement `revoked_access_tokens`, persistée et purgée après expiration.
5. Le rejeu d’une génération `ROTATED` révoque toute sa famille et écrit un audit append-only.
6. Les audits de session utilisent une table dédiée, distincte du journal de login et du journal RBAC.
7. Les décisions d’autorisation cross-user reposent sur `AUTH_SESSION_MANAGE` et le tenant, pas sur un nom de rôle codé en dur.

## Justification

- `sid` permet une révocation immédiate sans stocker chaque JTI moderne.
- La table JTI bornée préserve la compatibilité de déploiement et remplace réellement le mécanisme mémoire.
- La vérification en base garantit le même résultat après redémarrage et sur plusieurs instances.
- Un audit spécialisé conserve le vocabulaire sécurité sans détourner les tables existantes.
- Une permission dédiée respecte le principe du moindre privilège et les rôles personnalisés.

## Alternatives rejetées

### Conserver la blacklist mémoire

Rejetée : non distribuée, non durable et incompatible avec une application de santé en production.

### Révoquer tous les JWT uniquement par leur JTI

Rejetée : croissance et écritures inutiles pour les tokens modernes alors que `sid` permet de révoquer la session entière.

### Vérifier seulement la courte durée du JWT

Rejetée : une session compromise resterait utilisable jusqu’à quinze minutes après une révocation explicite.

### Contrôler uniquement les rôles ADMIN

Rejetée : couplage aux rôles système, impossible à déléguer proprement et contraire au RBAC administrable.

## Conséquences

- une lecture de session est ajoutée aux requêtes personnel avec `sid` ;
- les JWT patients et historiques restent compatibles via le chemin sans `sid` ;
- V67 est additive ;
- la révocation devient immédiatement visible sur toutes les instances ;
- #34 pourra construire l’interface sans logique de sécurité côté Angular.

## Rollback

La migration V67 ne doit pas être supprimée après application. En cas de rollback applicatif, les tables additives peuvent rester présentes. La sécurité ne doit pas revenir à une blacklist mémoire.