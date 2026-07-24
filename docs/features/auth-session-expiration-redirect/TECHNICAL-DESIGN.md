# TECHNICAL-DESIGN — Gestion robuste de l'expiration de session et redirection Angular

## 1. Problématique Technique

En cas d'échec du rafraîchissement d'un token JWT expiré, `AuthSessionRecoveryService.expireSession()` nettoyait `tokenStorage.clear()`. Cependant :
1. Les requêtes HTTP protégées subséquentes trouvaient `session` à `null` et contournaient l'intercepteur `authTokenInterceptor`.
2. Le retour anticipé `if (!session) return;` dans `expireSession()` annulait la navigation vers la page de login si `session` avait déjà été effacé.
3. Le composant `AppShellNavComponent` recalculait les éléments du menu avec un ensemble de permissions vide `Set()`, n'affichant plus que le lien par défaut `/dashboard`.

## 2. Solution d'Ingénierie

1. **Reprise de `AuthSessionRecoveryService.ts`** :
   ```typescript
   expireSession(): void {
     const currentUrl = this.router.url;
     const returnUrl = this.resolveReturnUrl(currentUrl);

     this.tokenStorage.clear();

     if (this.isRedirectingToLogin) {
       return;
     }

     if (currentUrl && currentUrl !== '/' && !currentUrl.startsWith('/auth/login') && !currentUrl.startsWith('/?')) {
       this.isRedirectingToLogin = true;
       void this.router.navigate(['/'], {
         queryParams: {
           sessionExpired: 'true',
           ...(returnUrl ? { returnUrl } : {}),
         },
         replaceUrl: true,
       }).finally(() => {
         this.isRedirectingToLogin = false;
       });
     }
   }
   ```

2. **Reprise de `authTokenInterceptor.ts`** :
   - Si `session` est `null` lors d'un appel à une API protégée `/api/...`, l'intercepteur appelle immédiatement `sessionRecovery.expireSession()` et rejette la promesse avec une `HttpErrorResponse` 401.
   - Si le rafraîchissement d'accès échoue (401/403/erreur), `expireSession()` est déclenché de manière dédoublonnée via le flag `isRedirectingToLogin`.
