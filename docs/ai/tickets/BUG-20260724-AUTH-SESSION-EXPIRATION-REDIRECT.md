# BUG-20260724-AUTH-SESSION-EXPIRATION-REDIRECT — Redirection automatique et nettoyage lors de l'expiration de session

## 1. Contexte & Problématique

Lorsqu’un jeton d'accès JWT ou une session rafraîchie expirait, l'application Angular se retrouvait dans un état anormal (« ghost state ») :
- Les éléments du menu latéral disparaissaient tous (à l'exception du « Tableau de bord »).
- L'utilisateur restait bloqué sur l'écran en cours au lieu d'être redirigé vers la page de connexion.
- Les clics sur les composants déclenchaient en boucle des erreurs 401 Unauthorized.

### Cause racine :
1. Dans `authTokenInterceptor.ts`, si `tokenStorage.session()` valait `null` (par exemple après un premier effacement de session), le filtre `if (!session) return next(request);` laissait passer les requêtes HTTP protégées `/api/...` sans en-tête `Authorization`.
2. Le serveur répondait en `401 Unauthorized`, mais l'intercepteur n'attrapait pas cette erreur car le traitement d'origine avait contourné le flux de rafraîchissement/expiration.
3. Dans `AuthSessionRecoveryService.expireSession()`, la condition `if (!session)` provoquait un retour anticipé (`return;`), empêchant l'exécution de `router.navigate(['/'], { queryParams: { sessionExpired: 'true' } })`.
4. Le cache RBAC étant purgé, le menu ne calculait plus aucune permission, laissant uniquement l'entrée sans restriction (`/dashboard`), sans jamais rediriger l'utilisateur vers le formulaire de login.

## 2. Correctif apporté

1. **`AuthSessionRecoveryService.ts`** :
   - Suppression du blocage prématuré sur `!session`.
   - Garantir que si `expireSession()` est appelée depuis n'importe quelle route protégée, `tokenStorage.clear()` purge les jetons et le cache RBAC, et `router.navigate(['/'])` redirige impérativement l'utilisateur vers la page de connexion avec `sessionExpired=true`.

2. **`authTokenInterceptor.ts`** :
   - Toute requête vers une API protégée sans session active déclenche immédiatement `expireSession()` et retourne une erreur 401.
   - Les réponses 401 et 403 non récupérables déclenchent systématiquement `expireSession()`.

3. **Gouvernance & Tests** :
   - Mise à jour des tests unitaires Angular dans `auth-token.interceptor.spec.ts` et `auth-session-recovery.service.spec.ts`.
   - Execution complète des suites de tests et validation du build Angular (`npm run build`).

## 3. Statut

En cours (Branche : `fix/auth-session-expiration-redirect`).
