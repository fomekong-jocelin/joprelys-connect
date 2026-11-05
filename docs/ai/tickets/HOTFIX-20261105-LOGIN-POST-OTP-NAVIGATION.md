# HOTFIX-20261105 — Connexion du personnel : retour silencieux à l'écran du code après un OTP valide

- **Mode** : Diagnostic puis Engineering (hotfix production).
- **Base** : `e5114415` (version déployée sur joprelys.com, vérifiée le 2026-11-05 : `index.html` et `assets/i18n` datés du 18/10/2026 ; la vérification publique d'une ordonnance inconnue répond 404, comportement d'avant l'audit).
- **Branche** : `hotfix/login-post-otp-navigation`, à reporter sur `main`.
- **Estimation** : 3 SP (diagnostic compris). **Priorité** : P0, production bloquée pour le personnel concerné.

## Constat (production)

1. Le membre du personnel saisit le code reçu par e-mail et clique sur « Se connecter ».
2. L'écran clignote puis revient à l'étape du code, code toujours saisi, sans message.
3. Un second clic affiche « Aucun code valide en cours (code déjà annulé ou serveur redémarré)… ».

Le premier envoi a donc **réussi** : le code a été consommé et la session ouverte. C'est la navigation vers la page d'accueil qui n'a pas abouti, et l'écran de connexion ne l'a pas signalé.

## Analyse

- **Défaut certain**, en production comme sur `main` : après un OTP valide, `LoginComponent` réactivait le bouton **avant** la fin de la navigation et ignorait son résultat (échec ou annulation). L'utilisateur restait devant un code déjà consommé ; le second envoi échouait avec un message trompeur.
- **Déclencheurs possibles de l'échec de navigation**, non départagés faute d'accès aux journaux du navigateur :
  - un onglet chargé avant un déploiement référence des fichiers JavaScript qui n'existent plus (le serveur répond 404 : vérifié) ;
  - une vérification d'accès lente ou interrompue sur un réseau faible.
- **Non reproduit en local** : le front exact de production (servi depuis joprelys.com), branché sur le backend en configuration de production (jeton de 15 min, inactivité de 30 min, cookie `Secure`), connecte correctement un compte `ADMIN_CLINIQUE` (flow Maestro vert). La chaîne backend connexion → OTP → `/api/rbac/me` → refresh répond en moins de 0,3 s.

## Correction

| Fichier | Changement |
|---|---|
| `auth/post-login-navigation.service.ts` (nouveau) | Ouvre la destination après authentification ; dit si la page de connexion a bien été quittée ; une seule navigation pour des appels simultanés. |
| `auth/login.component.ts` | Le bouton reste en chargement jusqu'à l'ouverture de la page. En cas d'échec : message clair, et un nouveau clic **réessaie d'ouvrir la page sans renvoyer le code**. Si la session a été perdue entre-temps : retour à la première étape avec un message. Même logique pour le portail patient. |
| `core/routing/stale-chunk-recovery.ts` (nouveau) + `app.config.ts` | `withNavigationErrorHandler` : un fichier JavaScript chargé à la demande introuvable déclenche **un** rechargement de la page cible (garde de 30 s par URL, aucun rechargement si le stockage de session est indisponible, pour ne jamais boucler). |
| `assets/i18n/fr.json`, `en.json` | `login.error.navigationFailed`, `login.error.sessionNotEstablished`. |

Aucun changement backend ni de contrat d'API. Déployable seul (front).

## Tests

- [x] `login.component.spec.ts` : +4 cas (chargement maintenu, échec expliqué sans renvoi du code, erreur de navigation, session perdue).
- [x] `post-login-navigation.service.spec.ts` (4) et `stale-chunk-recovery.spec.ts` (4).
- [x] Suite web complète du hotfix : 684 tests verts (124 fichiers) ; `ng build` de production réussi.
- [x] Maestro, **avant correctif** : le front de production exact, branché sur le backend local en configuration de production, connecte un compte `ADMIN_CLINIQUE` (bug non reproduit avec un navigateur neuf).
- [ ] Maestro sur le build du hotfix (onglet périmé simulé) : non abouti le 2026-11-05, poste saturé (0,5 Go de mémoire libre). Flow et proxy de test conservés hors dépôt.
- [ ] À faire en production après déploiement : connexion d'un compte réel ; en cas d'échec, relever la console (F12) et le `X-Trace-Id` de la réponse.

## Reste à faire

- Déploiement en production : **sur décision du porteur du projet**.
- Report sur `main` (conflit attendu dans `login.component.ts`, qui utilise `safeInternalUrl` depuis l'audit).
- `LoginComponent` fait 348 lignes (seuil d'alerte : 300) : extraire le parcours patient dans un composant dédié (lot C4 d'EPIC-0030).
