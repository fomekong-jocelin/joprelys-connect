# TASK-20261017-LOCAL-MAIL-SERVER — Démarrage du serveur mail local et configuration pour réception des OTP

## Metadata
- **ID** : TASK-20261017-LOCAL-MAIL-SERVER
- **Epic** : OPERATIONS / LOCAL_DEV_TOOLING
- **Type** : Engineering / Outillage local
- **Statut** : DONE
- **Priorité** : P1
- **Auteur** : Antigravity
- **Date** : 2026-10-17
- **Stack** : Node.js / MailDev / Spring Boot 4 / Java 21 / PowerShell

## Objectif
Démarrer un serveur de messagerie local (MailDev) pour intercepter les emails transactionnels (codes OTP de connexion, mot de passe temporaire, récupération de compte) en environnement de développement sans dépendre d'une connexion internet ni d'un relais SMTP de production distant.

## Contexte analysé
- Les endpoints d'authentification (`AuthenticationService`, `PasswordRecoveryService`, `PatientAuthService`) envoient désormais les codes OTP par e-mail via `AccountMailService` / `SmtpAccountMailService`.
- En l'absence de serveur mail local actif ou avec une configuration pointant par défaut vers `mail.joprelys.com:465` sans mot de passe, l'application renvoyait une erreur `503 MAIL_DELIVERY_UNAVAILABLE`.
- Docker n'étant pas disponible sur le poste hôte Windows, l'usage de MailDev via Node.js / `npx` offre une solution légère, autonome et dotée d'une interface web moderne (HTTP 1080) et d'un port SMTP standard (TCP 1025).

## Critères d'acceptation
- [x] Le serveur mail local (MailDev) est démarré et écoute sur le port SMTP 1025 et l'interface Web 1080.
- [x] Les e-mails transactionnels Joprelys Connect (OTP staff, récupération, mot de passe temporaire) sont interceptés localement.
- [x] L'interface Web de consultation des emails est accessible sur `http://localhost:1080/`.
- [x] Un script PowerShell `scripts/start-local-mail-server.ps1` est fourni pour piloter le démarrage, l'arrêt (`-Stop`) et le redémarrage (`-Restart`), et vérifier le statut du service.
- [x] Un script PowerShell `scripts/get-latest-otp.ps1` est fourni pour récupérer immédiatement le dernier code OTP reçu directement en ligne de commande.
- [x] La configuration `application.yml` du backend utilise par défaut les paramètres du serveur mail local (`localhost:1025`, `auth: true`, `ssl: false`, `localdev`).
- [x] La configuration de production `application-prod.yml` préserve explicitement les paramètres de production (`mail.joprelys.com:465`, `ssl: true`).
- [x] Le profil de développement `application-dev.yml` est créé conformément au standard `CONFIGURATION-STANDARDS.md`.
- [x] La suite de tests unitaires Maven `*Mail*Test` passe à 100% sans régression.

## Action plan
- [x] Installer et démarrer le serveur MailDev sur les ports 1025 (SMTP) et 1080 (Web UI).
- [x] Tester l'envoi SMTP et l'affichage d'un e-mail avec code OTP via l'API REST `/api/email`.
- [x] Adapter `application.yml` pour pointer sur `localhost:1025` par défaut avec authentification `localdev`.
- [x] Sécuriser la configuration de production dans `application-prod.yml` (`mail.joprelys.com:465`).
- [x] Créer `application-dev.yml` avec la configuration de dev locale.
- [x] Créer `scripts/start-local-mail-server.ps1` (enrichi avec switches `-Restart` et `-Stop`).
- [x] Créer `scripts/get-latest-otp.ps1`.
- [x] Exécuter les tests unitaires backend Maven.
- [x] Valider le redémarrage complet du serveur (arrêt de l'ancien processus PID 45704, relance du PID 35172, libération et réouverture des sockets 1025 et 1080).
- [x] Mettre à jour `PROJECT-TRACKING.md`, `CHANGELOG.md` et `docs/features/auth-email-delivery/TECHNICAL-DESIGN.md`.

- [x] Test de connexion E2E navigateur validé : saisie de `qa.admin@joprelys.local`, récupération du code OTP par `.\scripts\get-latest-otp.ps1 -Email qa.admin@joprelys.local` (ex: `576531`), validation du second facteur et redirection réussie vers `/dashboard`.

## Résultats & Vérifications
- Serveur MailDev actif en arrière-plan (PID 35172 après redémarrage propre).
- Interface Web : `http://localhost:1080/` (HTTP 200 OK).
- Port SMTP : `1025` (LISTEN).
- API REST : `http://localhost:1080/api/email` (HTTP 200 OK, boîte réinitialisée).
- Test d'extraction OTP : validé via `scripts/get-latest-otp.ps1 -Email qa.admin@joprelys.local`.
- Test Navigateur Playwright : Connexion réussie sur `http://localhost:4200/login` avec le mot de passe initialisé `Re12#He10@2021!`, validation OTP et atterrissage sur le tableau de bord (`/dashboard`) en tant que **QA Admin Clinique**.
- Tests Maven : 13 tests `*Mail*Test` exécutés, 0 échec, 0 erreur.
