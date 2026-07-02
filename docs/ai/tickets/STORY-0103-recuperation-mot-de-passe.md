# STORY-0103 — Récupération de mot de passe simplifiée

## Métadonnées

| Champ | Valeur |
|---|---|
| ID | STORY-0103 |
| Epic | EPIC-0001 — Authentification & Gestion des Rôles |
| Type | User Story |
| Priorité | P2 |
| Story Points | 2 SP |
| Sprint | SPRINT-0003 |
| Statut | DONE |
| Assigné | Antigravity |
| Reviewer | Lead |
| Profil recommandé | Intermédiaire |
| Est. Senior | 0.3j |
| Est. Intermédiaire | 0.45j |
| Est. Junior | 0.75j |
| Temps passé | 0.3j |
| Dépendances | Aucun |
| Dernière MAJ | 2026-07-03 |

---

## Objectif

Permettre à un professionnel de santé de réinitialiser son mot de passe de manière autonome via un flux de code de sécurité (OTP) temporaire, sans intervention d'un administrateur système.

---

## Critères d'acceptation

- [x] Un lien "Mot de passe oublié ?" est présent sur l'écran de connexion unifiée.
- [x] La demande de réinitialisation avec un e-mail valide et actif génère un code OTP à 6 chiffres affiché dans les logs du serveur.
- [x] La demande de réinitialisation avec un e-mail inconnu ou inactif renvoie la même réponse utilisateur (statut 200), mais aucun OTP n'est généré ni loggé (sécurité anti-énumération).
- [x] Le code OTP expire après 5 minutes.
- [x] Saisir un code OTP incorrect augmente le nombre de tentatives (max 3 tentatives avant invalidation).
- [x] La soumission d'un code OTP valide avec un nouveau mot de passe (min 8 caractères) met à jour le compte en base de données (hashé avec BCrypt) et détruit le code OTP.
- [x] L'utilisateur peut se connecter immédiatement après avec son nouveau mot de passe.

---

## Action plan

- [x] **Backend** :
  - [x] Créer les DTOs `PasswordRecoveryRequest.java` et `PasswordResetRequest.java` avec validations de champs.
  - [x] Créer `PasswordRecoveryService.java` dans le package `application` pour gérer la map d'OTP et la mise à jour en base de données.
  - [x] Créer `PasswordRecoveryController.java` dans le package `api` avec les endpoints `/api/public/auth/password-recovery/request` et `/api/public/auth/password-recovery/reset`.
  - [x] Configurer `SecurityConfig.java` pour accorder l'accès public à `/api/public/auth/password-recovery/**` (déjà géré par l'accès public à `/api/public/**`).
  - [x] Écrire des tests d'intégration complets dans `PasswordRecoveryControllerTest.java`.
- [x] **Frontend** :
  - [x] Ajouter les méthodes `requestPasswordRecovery` et `resetPassword` dans `AuthApiService.ts`.
  - [x] Créer le composant `ForgotPasswordComponent` (`forgot-password.component.ts` + template + styles).
  - [x] Ajouter la route `/forgot-password` dans `app.routes.ts`.
  - [x] Ajouter le lien "Mot de passe oublié ?" dans `LoginComponent` (template HTML).
  - [x] Ajouter les traductions FR/EN requises dans `i18n.service.ts`.
  - [x] Écrire les tests unitaires pour `ForgotPasswordComponent`.
- [x] **Validation & Suivi** :
  - [x] Lancer les tests backend `./mvnw test` et frontend `npm run test -- --watch=false`.
  - [x] Lancer le build de production frontend `npm run build`.
  - [x] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`.

---

## Risques & Régression

* **Faible** : Les modifications backend n'impactent pas les endpoints d'authentification existants, seulement un nouveau contrôleur public est ajouté. Le hashage BCrypt utilise le `PasswordEncoder` configuré.
* **Sécurité** : Protection stricte contre l'énumération d'adresses e-mail (aucun message d'erreur indiquant si l'email existe ou non lors de la demande). Invalidation de l'OTP après 3 échecs ou 5 minutes.
