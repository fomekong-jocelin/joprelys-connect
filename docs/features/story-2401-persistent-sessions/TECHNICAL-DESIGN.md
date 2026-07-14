# STORY-2401 — Conception technique

## Architecture cible

```text
AuthController
  -> AuthenticationService
      -> AuthSessionIssuer
          -> RefreshTokenGenerator
          -> RefreshTokenHasher
          -> AuthSessionRepository
          -> JwtService

RefreshSessionController
  -> RefreshSessionUseCase
      -> AuthSessionRepository (verrou pessimiste)
      -> RefreshTokenHasher
      -> RefreshTokenGenerator
      -> JwtService

Controllers
  -> RefreshTokenCookieManager (transport HTTP uniquement)
```

## Responsabilités

### API

- reçoit et valide les requêtes ;
- extrait ou écrit le cookie via un composant dédié ;
- délègue au use case ;
- ne décide aucune règle de rotation ou d’expiration.

### Application

- orchestre login, émission et refresh ;
- porte les transactions ;
- valide l’état utilisateur/organisation ;
- applique les expirations et la rotation.

### Domaine

- représente les statuts et motifs de révocation ;
- calcule l’expiration effective ;
- interdit la rotation d’une session non active.

### Infrastructure

- persiste les sessions ;
- verrouille la génération présentée ;
- génère et hash les secrets ;
- exécute la purge planifiée.

## Modèle de rotation

Une génération de token correspond à une ligne `auth_sessions`.

```text
family F
S1 ACTIVE --refresh--> S1 ROTATED -> S2 ACTIVE
S2 ACTIVE --refresh--> S2 ROTATED -> S3 ACTIVE
```

Les lignes partagent `token_family_id`. L’ancienne ligne conserve son hash et pointe vers la nouvelle via `replaced_by_session_id`. La conservation de l’ancien hash permet à #33 d’identifier un rejeu ultérieur.

## Transaction de refresh

1. Calculer le SHA-256 du token reçu.
2. Charger la ligne par hash avec `PESSIMISTIC_WRITE` et son utilisateur.
3. Refuser si absente, révoquée ou expirée.
4. Vérifier l’utilisateur et l’organisation.
5. Générer le nouveau token et son hash.
6. Créer la nouvelle ligne avec la même famille et la même expiration absolue.
7. Marquer l’ancienne ligne `ROTATED`, `revoked_at=now` et `replaced_by_session_id`.
8. Sauvegarder dans la même transaction.
9. Émettre un access token portant le nouveau `sid`.
10. Retourner le secret brut uniquement à la couche cookie, jamais à la persistance ou aux logs.

## Concurrence

Le verrou pessimiste est retenu plutôt qu’un simple verrou optimiste :

- il donne un résultat déterministe pour une opération de sécurité à usage unique ;
- la section critique est courte ;
- un second appel attend puis observe la ligne révoquée ;
- exactement une rotation réussit.

`@Version` est également conservé comme garde-fou sur les mises à jour hors requête verrouillée.

## JWT

Les nouveaux access tokens contiennent le claim optionnel `sid`.

- `sid` lie l’access token à la génération active ;
- #33 pourra contrôler une révocation de session sur les opérations sensibles ;
- le parseur accepte temporairement les JWT historiques sans `sid` ;
- les tokens patients restent inchangés.

## Cookie

Le composant HTTP construit un `ResponseCookie` :

- nom configuré ;
- `httpOnly=true` ;
- `secure` selon profil ;
- `sameSite=Lax` ;
- `path=/api/auth` ;
- `maxAge` borné par l’expiration effective.

Le contrôleur ne connaît pas les attributs du cookie.

## Configuration

Préfixe YAML : `joprelys.security.sessions`.

- `absolute-ttl-hours` ;
- `inactivity-ttl-minutes` ;
- `refresh-cookie-name` ;
- `refresh-cookie-secure` ;
- `refresh-cookie-same-site` ;
- `refresh-cookie-path` ;
- `cleanup-cron`.

Les valeurs de test sont explicites. Le profil de production impose le cookie sécurisé.

## Purge

Un job planifié supprime uniquement les générations dont :

- l’expiration absolue est dépassée depuis la durée de conservation ; ou
- la révocation est ancienne et aucune ligne ne la référence encore.

Pour #31, une purge prudente des sessions expirées est mise en place. #33 pourra étendre la conservation des preuves de rejeu.

## Compatibilité et migration

- V66 est additive ;
- aucun JWT existant n’est invalidé à la mise en production ;
- les anciens access tokens continuent jusqu’à leur expiration ;
- le login Angular actuel ignore les nouveaux champs JSON ;
- le cookie est émis sans nécessiter encore l’intercepteur de #34.

## Sécurité

- 32 octets aléatoires minimum ;
- aucune sérialisation du token brut dans une entité ou un audit ;
- aucun `toString()` contenant un secret ;
- comparaison via hash exact ;
- erreurs 401 génériques ;
- aucune donnée patient dans le modèle ;
- adresse IP masquée avant persistance ;
- user-agent normalisé et tronqué.

## SemVer

Le changement est rétrocompatible au niveau JSON existant mais ajoute un endpoint, un cookie et des champs. Impact prévu : **MINOR**.