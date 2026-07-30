# FUNCTIONAL SPEC — MOB-2804 Réseau mobile

## Objectif

Donner à Joprelys Connect mobile une couche réseau unique, fiable et testable avant l’authentification et les écrans métier. Cette couche transporte les appels vers le backend Joprelys sans dupliquer les règles métier, sans exposer les secrets et sans introduire de cache clinique générique.

## Environnements

Trois environnements publics sont reconnus :

- `dev` ;
- `recette` ;
- `prod`.

La base URL est injectée au build avec `API_BASE_URL`. `APP_ENV` indique l’environnement courant. HTTPS est obligatoire en recette et en production. Ces valeurs sont publiques et ne constituent jamais un coffre-fort.

## Requêtes

Chaque appel transporte, selon le contexte :

- `Accept: application/json` ;
- `Content-Type: application/json` lorsqu’un corps est présent ;
- `Accept-Language` selon la locale Flutter active ;
- `X-Trace-Id` pour la corrélation mobile/backend ;
- `Authorization: Bearer ...` uniquement lorsqu’une session valide l’autorise ;
- `Idempotency-Key` uniquement lorsqu’une opération l’exige explicitement.

Les widgets et écrans ne manipulent jamais Dio directement. Ils consommeront les repositories et use cases de leurs features, qui utiliseront `ApiClient` dans la couche data.

## Sessions

### Session professionnelle

Une réponse `401` peut déclencher une récupération de session professionnelle :

1. vérifier si un token plus récent est déjà disponible ;
2. sinon coordonner un seul refresh pour toutes les requêtes concurrentes ;
3. rejouer la requête une seule fois avec le nouveau token ;
4. expirer la session si la récupération échoue.

Le backend actuel utilise un refresh token en cookie HttpOnly. MOB-2804 conserve les cookies uniquement en mémoire. MOB-2805 fournira l’implémentation de session et la persistance sécurisée appropriée, sans inventer un contrat serveur parallèle.

### Session patient

Une session patient ne déclenche jamais de refresh professionnel. Une réponse `401` expire l’état local patient et laisse l’authentification patient reprendre selon son propre parcours.

### Autorisation

Une réponse `403` signifie que la session est authentifiée mais non autorisée. Elle ne déclenche ni refresh, ni logout automatique.

## Résilience et retry

Le retry automatique est volontairement limité :

- au maximum une nouvelle tentative ;
- uniquement sur une erreur transitoire ou un timeout ;
- uniquement pour `GET`, `HEAD`, `OPTIONS` ;
- ou pour une écriture munie explicitement d’une clé d’idempotence ;
- jamais pour un flux, un upload multipart ou une écriture non déclarée idempotente.

Les réponses `429` sont qualifiées comme rejouables dans le modèle d’erreur, mais ne sont pas rejouées automatiquement dans ce lot : une future politique tenant compte de `Retry-After` devra être validée avant activation.

## Erreurs

Le mobile normalise :

- l’enveloppe canonique `{ error: { code, message, trace_id, action, required_scope }, detail }` ;
- les erreurs d’authentification RFC 7807 / `ProblemDetail` ;
- les timeouts ;
- l’absence de connexion ;
- l’annulation ;
- les erreurs TLS ;
- les erreurs serveur et statuts HTTP.

Le code, le statut et le trace ID peuvent être transmis aux couches supérieures. Aucun body clinique, token ou donnée personnelle n’est écrit dans les logs.

## Internationalisation

Le client transporte la langue active via `Accept-Language`. Les messages techniques de fallback restent des codes stables ; les écrans métier les présenteront via les traductions ARB appropriées.

## Frontière avec le thème

MOB-2804 ne modifie pas :

- `AppTheme` ;
- `AppDesignTokens` ;
- les compositions UX des features.

La couche réseau est strictement non visuelle.

## Hors périmètre

- écran de connexion ;
- stockage sécurisé des tokens/cookies ;
- biométrie ;
- repositories patients, planning ou constantes ;
- cache clinique hors ligne ;
- upload audio segmenté ;
- pinning TLS ;
- déploiement recette/production.

## Critères d’acceptation

- [x] configuration `dev/recette/prod` validée ;
- [x] HTTPS obligatoire hors développement ;
- [x] client Dio encapsulé par `ApiClient` ;
- [x] locale et trace ID ajoutés aux requêtes ;
- [x] bearer conditionnel ;
- [x] refresh professionnel unique et borné ;
- [x] aucune récupération professionnelle pour le patient ;
- [x] `403` sans refresh/logout ;
- [x] retry borné aux opérations rejouables ;
- [x] erreurs backend et transport normalisées ;
- [x] aucune modification du thème ;
- [x] tests, analyse statique et APK debug verts dans le gate runtime #2046.
