# BUG-20260728-LOGIN-FLASHING-SESSION-CARD-REMOVAL — Suppression de la carte temporaire de session/rôle au login et redirection directe dashboard

## Metadata
- **ID** : `BUG-20260728-LOGIN-FLASHING-SESSION-CARD-REMOVAL`
- **Epic** : `AUTHENTICATION` / `UI_UX`
- **Composants** : Angular (`auth/login.component`, `auth/login.guard`, `app.routes`)
- **Statut** : `DONE`
- **Priorité** : P1 (UX / Fluidité de connexion)
- **Profil recommandé** : Senior Frontend Angular

## Diagnostic de l'anomalie
- **Symptôme** : Lors de la connexion ou de la navigation vers la racine `/`, une carte intermédiaire affichant le nom, l'e-mail et le rôle de l'utilisateur (`login-session-card`) flashait brièvement à l'écran avant de disparaître lors de la redirection vers le tableau de bord.
- **Cause racine** :
  1. `login.component.html` contenait un bloc conditionnel `@if (session(); as currentSession)` affichant `login-session-card` avec les informations d'identité dès que le signal `session` devenait actif.
  2. La redirection vers `/dashboard` était déclenchée par un `effect()` dans `login.component.ts` de manière asynchrone après le rendu initial de la vue, ce qui provoquait l'affichage temporaire (flash) de cette carte.
  3. L'accès à la route racine `/` par un utilisateur déjà connecté chargeait le composant `LoginComponent` avant d'exécuter la navigation vers le tableau de bord.

## Correctif apporté
1. **Création du guard `loginGuard` (`web/src/app/auth/login.guard.ts`)** :
   - Redirige immédiatement les utilisateurs déjà authentifiés vers `/dashboard` (ou `/patient/dashboard`) avant même le chargement du composant `LoginComponent`.
   - Attaché aux routes racine `path: ''` et `path: 'patient/login'` dans `app.routes.ts`.
2. **Suppression de `login-session-card` (`web/src/app/auth/login.component.html`)** :
   - Retrait du bloc conditionnel de carte de session active.
   - Connexion et redirection directes sans affichage ni flash d'une carte d'identité temporaire.

## Actions réalisées
- [x] Créer `web/src/app/auth/login.guard.ts`
- [x] Attacher `loginGuard` dans `web/src/app/app.routes.ts`
- [x] Retirer `login-session-card` de `web/src/app/auth/login.component.html`
- [x] Vérifier le passage de la suite de tests unitaires Vitest Angular
- [x] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`
