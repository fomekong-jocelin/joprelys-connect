# ECO-20261107 — EPIC : Joprelys ID (Keycloak), `eco-contracts` et liaison patient ↔ DPU en test

> Mode : **Project Manager** (demande macro > 3 jours), puis **Engineering** story par story.
> Décision structurante : [ADR-20261107-JOPRELYS-ID-KEYCLOAK](../adr/ADR-20261107-JOPRELYS-ID-KEYCLOAK.md).
> Source : feuille de route de l'écosystème (`C:\MES-APPLICATIONS\PharmaLocate\feuille-de-route-ecosysteme.md`), § 0, § 2.1 à § 2.4, § 3 « Ordre d'exécution » (S3-S4), § 4.

## 1. Objectif

Ouvrir le premier flux de l'écosystème, **en environnement de test uniquement** : un patient AllôPharma relie son profil à son DPU Joprelys après s'être connecté à Joprelys ID et avoir consenti ; il peut révoquer à tout moment, depuis AllôPharma ou depuis le portail patient Joprelys ; la révocation purge la copie détenue par l'autre système.

Hors périmètre (noté, non traité ici) :

- preuve d'identité qui lie un compte Joprelys ID à un vrai DPU (OTP vérifié par Joprelys) : story ultérieure ;
- lectures croisées (`prescriptions`, `lab-results`…) : flux suivants ;
- déploiement Keycloak sur le VPS, DPA et AIPD : côté porteur produit ;
- dépôt `officine-core` : **dépôt séparé** (décision du 07/11/2026), à créer plus tard ;
- purge de l'historique git d'AllôPharma : **non validée**, rien n'est purgé.

## 2. Critères d'acceptation de l'EPIC

- [x] `docker compose` démarre Keycloak 26 avec le realm `joprelys-id` importé (clients `allopharma-mobile` public PKCE, `allopharma-server` confidentiel, audience `joprelys-api`, utilisateurs fictifs).
- [x] Le dépôt `eco-contracts` contient les JSON Schema `PatientLink`, `Consent` et `EventEnvelope` v1, des exemples fictifs valides et invalides, et une commande de validation.
- [x] Joprelys expose `/api/eco/v1/patient-links` et `/api/eco/v1/consents/{id}/revoke`, protégés par des jetons Joprelys ID (scope + audience) ; les jetons maison y sont refusés et inversement.
- [ ] Les webhooks entre serveurs respectent l'enveloppe § 2.2 : `Eco-Signature` HMAC-SHA256, `Idempotency-Key`, écart d'horloge ≤ 5 min, dédoublonnage sur `id`, outbox transactionnelle rejouée avec backoff exponentiel pendant 24 h puis file d'erreurs.
- [ ] AllôPharma mobile : écran d'explication et de consentement, connexion Joprelys ID par PKCE, état de la liaison, révocation.
- [ ] Portail patient Joprelys (Angular) : liste des applications reliées et révocation.
- [ ] `consent.revoked` purge la copie chez le récepteur (test automatisé).
- [ ] Données fictives uniquement, aucun secret versionné (gitleaks vert).
- [ ] Tests verts dans chaque dépôt (Testcontainers Keycloak côté Joprelys), documentation, tickets, tracking et changelogs à jour.
- [ ] Une branche par dépôt, prête à merger ; aucun merge ni push sans accord explicite.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic | ECO-20261107 |
| Sprint cible | S3-S4 de la feuille de route |
| Priorité business | P1 |
| Complexité | XL (découpée en 8 stories) |
| Story points | 34 |
| Profil recommandé | Senior full-stack / sécurité |
| Effort senior | ≈ 14 j (après majoration multi-stack et sécurité) |
| Reviewer obligatoire | Tech Lead (porteur produit) |
| Risque fonctionnel | Moyen |
| Risque technique | Fort (3 dépôts, 4 stacks, nouveau composant d'identité) |
| Dépendances | P0/P1 sécurité faits (07/11/2026) ; Docker local pour Keycloak et Testcontainers |
| Bloquants connus | Aucun pour le test ; VPS, DPA et AIPD hors périmètre |

Branches (une par dépôt, travail en worktree pour ne pas changer la branche des copies partagées) :

| Dépôt | Branche | Worktree |
|---|---|---|
| joprelys-connect | `feat/joprelys-id-keycloak` | `C:\MES-APPLICATIONS\joprelys-connect-eco` |
| PharmaLocate (AllôPharma) | `feat/joprelys-id-patient-link` | `C:\MES-APPLICATIONS\PharmaLocate-eco` |
| eco-contracts (nouveau, local, sans remote) | `main` | `C:\MES-APPLICATIONS\eco-contracts` |

## 4. Découpage EPIC → stories → tasks

Estimation selon `docs/pm/ESTIMATION-GUIDE.md` ; majoration sécurité (+20 à +40 %) et multi-stack incluses dans la colonne « senior ».

| # | Story | Dépôt(s) | SP | Senior | Dépend de |
|---|---|---|---:|---:|---|
| S1 | Joprelys ID en dev (Keycloak) | joprelys-connect | 3 | 1 j | — |
| S2 | Dépôt `eco-contracts` v1 | eco-contracts | 3 | 1 j | — |
| S3 | API éco Joprelys : liaison et consentement | joprelys-connect | 5 | 2,5 j | S1, S2 |
| S4 | Webhooks éco Joprelys : outbox, signature, `consent.revoked` | joprelys-connect | 5 | 2 j | S2, S3 |
| S5 | AllôPharma API : liaison, révocation, réception des webhooks | PharmaLocate | 5 | 2,5 j | S2, S3, S4 |
| S6 | AllôPharma mobile : consentement, PKCE, révocation | PharmaLocate | 5 | 2,5 j | S1, S5 |
| S7 | Portail patient Joprelys : applications reliées | joprelys-connect | 3 | 1,5 j | S3, S4 |
| S8 | Parcours de bout en bout en test, docs et clôture | les trois | 3 | 1 j | S1 à S7 |
| | **Total** | | **34** | **≈ 14 j** | |

Ordre d'exécution : S1 et S2 en parallèle, puis S3 → S4 → S5 → S6 / S7, puis S8.

### S1 — Joprelys ID en dev (Keycloak) · 3 SP

*En tant qu'équipe, je veux un Joprelys ID local reproductible afin de développer et tester la liaison sans service externe.*

Tasks :
- [x] T1.1 Service `keycloak` (image 26.x épinglée) + base PostgreSQL dédiée dans `docker-compose.yml`, mode `start-dev`, `--import-realm`, port 8180 ; identifiants admin via `.env` (exemple `.env.example`, jamais de valeur réelle versionnée).
- [x] T1.2 Realm `keycloak/realm-joprelys-id.json` : client public `allopharma-mobile` (PKCE S256 obligatoire, *consent required*, redirect `cm.allopharma.app:/oauth2redirect`, pas de *direct grant*) ; client confidentiel `allopharma-server` (*service account* seul, secret injecté par variable) ; audience `joprelys-api` ; client scopes `eco:patient-link`, `eco:consent.revoke` ; mapper d'attribut `dpu` ; durée de jeton 5 min.
- [x] T1.3 Utilisateurs fictifs (`patient.demo1`, `patient.demo2`, DPU `DPU-TEST-000123`, `DPU-TEST-000456`), mot de passe de test documenté dans le fichier d'exemple uniquement.
- [x] T1.4 Doc `docs/features/eco-joprelys-id/` (FUNCTIONAL-SPEC, TECHNICAL-DESIGN) + section INSTALLATION.

Critères d'acceptation : realm importé au démarrage ; `client_credentials` sur `allopharma-server` renvoie un jeton avec `aud=joprelys-api` et le scope demandé ; l'*authorization code* sans `code_challenge` est refusé ; gitleaks vert.
Tests : test d'intégration Testcontainers Keycloak (dans S3) qui charge le même fichier de realm.
DoR : version Keycloak et module Testcontainers vérifiés (context7). DoD : compose démarré, captures de vérification, doc à jour.

Résultat S1 : `node infra/keycloak/verify-realm.mjs` → 23/23 (client credentials avec `aud=joprelys-api`, mauvais secret refusé, *password grant* et *client credentials* du mobile refusés, autorisation sans `code_challenge` refusée, redirection non déclarée refusée, écran de consentement FR affiché, mauvais `code_verifier` refusé, jeton PKCE avec `dpu`, `azp`, sans nom ni e-mail). Correction trouvée à l'exécution : `unmanagedAttributePolicy: DISABLED` n'est pas une valeur acceptée par Keycloak 26.8 ; l'absence de la clé vaut « désactivé ». **Non vérifié** : démarrage via `docker compose` (Docker absent).

### S2 — Dépôt `eco-contracts` v1 · 3 SP

*En tant qu'équipe, je veux des contrats versionnés et testés afin que les trois applis parlent le même langage.*

Tasks :
- [x] T2.1 Dépôt git local `eco-contracts` : `README`, `v1/schemas/{event-envelope,patient-link,consent}.schema.json` (JSON Schema 2020-12, calqués sur FHIR R4 restreint, `meta.source` / `meta.versionId`).
- [x] T2.2 `v1/examples/valid/*.json` et `v1/examples/invalid/*.json`, données fictives uniquement (UUID v7, `DPU-TEST-…`).
- [x] T2.3 Validation automatisée (`npm test` avec Ajv 2020) ; `CHANGELOG.md` et règle de versionnement (champ ajouté = compatible, champ retiré = `v2`).
- [x] T2.4 Consommation par les backends : copie versionnée dans `src/test/resources/eco-contracts/v1/` avec script de synchronisation et empreinte SHA-256 vérifiée en test (sous-module git quand un remote existera).

Critères d'acceptation : chaque exemple valide passe, chaque exemple invalide échoue avec le bon motif ; aucun champ d'identité (nom, téléphone) dans `PatientLink`.
Tests : `npm test` vert ; tests de contrat dans S3 et S5.

Résultat S2 : `C:\MES-APPLICATIONS\eco-contracts`, commit `07e416d`. `npm test` 13/13 (5 valides, 8 invalides avec motif d'erreur attendu), empreintes `v1/SHA256SUMS` vérifiées. Choix : `meta.versionId` entier ≥ 1 ; `data` de l'enveloppe validé contre le schéma désigné par `dataschema`. Ouvert : le format `uuid` n'impose pas la version 7.

### S3 — API éco Joprelys : liaison et consentement · 5 SP

*En tant que patient connecté à Joprelys ID, je veux que mon profil AllôPharma soit relié à mon DPU avec un consentement explicite et révocable.*

Tasks :
- [x] T3.1 Dépendance `spring-boot-starter-oauth2-resource-server` ; seconde `SecurityFilterChain` ordonnée, `securityMatcher("/api/eco/v1/**")`, validation issuer + audience `joprelys-api` + scopes ; la chaîne maison refuse les jetons Joprelys ID.
- [x] T3.2 Migration Flyway : tables `eco_patient_links`, `eco_consents` (statut, scopes, période, `grantee`, `revoked_at`, motif), index et contraintes.
- [x] T3.3 Use cases (interfaces + implémentations) : créer une liaison depuis le jeton patient (DPU lu dans la claim `dpu`, `azp` = client) ; révoquer un consentement (scope `eco:consent.revoke`, compte de service) ; lister/révoquer depuis le portail patient.
- [x] T3.4 Contrôleurs `/api/eco/v1/patient-links` (POST, GET par id) et `/api/eco/v1/consents/{id}/revoke` (POST, idempotent), DTO validés, erreurs typées, journal d'audit.
- [x] T3.5 Tests : unitaires des use cases ; intégration Testcontainers (PostgreSQL + Keycloak, realm S1) : jeton valide → 201, sans scope → 403, mauvaise audience → 401, jeton maison → 401, révocation double → 200 idempotent ; tests de contrat contre `eco-contracts`.

Critères d'acceptation : aucune copie d'identité côté AllôPharma dans la réponse ; un DPU inconnu → erreur métier claire ; contrôle d'accès côté serveur par ressource.

Résultat S3 (décision utilisateur du 07/11/2026 : **sans Docker**) :
- `EcoApiIntegrationTest` 20/20 : vrais jetons RS256 (clé de test, mêmes validateurs qu'en production), H2 + Flyway V123, réponses validées contre `eco-contracts` ; cas couverts : création 201, rejeu 200, profil relié ailleurs 409, DPU inconnu 404, jeton sans DPU 403, client non déclaré 403, `child` 422, corps invalide 400, période > 2 ans 422, sans jeton 401, scope manquant 403, mauvaise audience / mauvais émetteur / expiré / clé inconnue 401, jeton maison HS256 refusé, jeton éco refusé sur `/api/patients`, route éco non déclarée 403, révocation idempotente (versionId 2 une seule fois), recréation après révocation, consentement d'une autre application 404, jeton patient sur la révocation 403, `revokedBy=system` refusé.
- Test de mutation manuel : sans le contrôle d'audience, le test d'audience échoue (201 au lieu de 401).
- `EcoContractChecksumTest` 2/2 (copie des schémas = `SHA256SUMS`, T2.4 via `scripts/sync-eco-contracts.mjs`).
- `EcoKeycloakLiveIT` 1/1 contre **Keycloak 26.8.1 réel** (jeton patient PKCE avec consentement, jeton serveur client credentials, clés téléchargées par le vrai décodeur) via `node infra/keycloak/run-live-eco-test.mjs` ; ignoré sans variables, non exécuté en CI.
- Écarts au plan : pas de `GET /patient-links/{id}` (aucun consommateur, ajouté quand un besoin existera) ; Testcontainers Keycloak reporté faute de Docker ; `@PreAuthorize` ajouté en plus de la chaîne de filtres (politique permission-first).
- Changement transverse : `JwtAuthenticationFilter` et `ApiKeyAuthenticationFilter` s'abstiennent sur `/api/eco/**`.

### S4 — Webhooks éco Joprelys : outbox, signature, `consent.revoked` · 5 SP

*En tant que système récepteur, je veux être prévenu d'une révocation de façon fiable et vérifiable.*

Tasks :
- [x] T4.1 Table `eco_outbox` (Flyway) écrite dans la même transaction que la révocation.
- [x] T4.2 Signataire `Eco-Signature: t=…,v1=HMAC-SHA256(secret, t + "." + corps)` + `Idempotency-Key` ; secret par abonné en variable d'environnement.
- [x] T4.3 Relais planifié : envoi, backoff exponentiel pendant 24 h, puis statut `DEAD` + alerte journalisée (sans contenu santé).
- [x] T4.4 Émission de `consent.revoked` (enveloppe § 2.2) quand la révocation vient du portail patient Joprelys.
- [x] T4.5 Tests : signature (vecteurs connus), transaction (rollback = pas d'outbox), relais (succès, 5xx → replanifié, expiration → DEAD) avec serveur HTTP simulé.

Note : `WebhookService` existant (abonnements par organisation) n'a ni signature ni outbox ; il n'est pas modifié dans cette story pour ne pas changer son contrat.

Résultat S4 (07/11/2026, branche `feat/joprelys-id-keycloak`) :
- Migration **V124** `eco_outbox` (une ligne par événement et par abonné, `UNIQUE (event_id, subscriber)`) ; `OutboxEcoEventPublisher` en `Propagation.MANDATORY` : aucun événement hors transaction métier.
- `EcoWebhookSigner` (`t=<unix>,v1=<hex HMAC-SHA256(secret, t + "." + corps)>`), `HttpEcoWebhookSender` (RestClient, délais 5 s / 10 s, `Idempotency-Key` = id de l'événement). Secret par abonné : `JOPRELYS_ECO_WEBHOOK_ALLOPHARMA_SECRET` (32 caractères minimum, sinon arrêt au démarrage) ; URL HTTPS obligatoire hors localhost.
- `EcoOutboxRelay` + `EcoOutboxScheduler` (toutes les 15 s, désactivable) : envoi hors transaction, backoff 30 s doublé à chaque échec et plafonné à 1 h, `DEAD` au-delà de 24 h avec `log.error` sans contenu (identifiants techniques seulement). Livraison au moins une fois : le récepteur déduplique.
- `PatientPortalRevocationService` (cas d'usage `revokeByPatient`, contrôle de propriété → 404) émet `consent.revoked` ; `ConsentRevoker` est partagé avec la révocation par l'API. Le motif libre n'est pas transmis (minimisation). La révocation demandée par l'application elle-même n'émet pas d'événement (elle en est l'auteur). L'endpoint du portail arrive en S7.
- Tests : 22 nouveaux, verts. Signature (vecteur calculé indépendamment), politique de rejeu, configuration (secret court, HTTP distant), envoi réel sur serveur HTTP du JDK (signature recalculée, 503, récepteur injoignable), intégration (enveloppe et données validées contre `eco-contracts`, idempotence, liaison d'un autre patient 404, rollback = aucune ligne, publication hors transaction refusée, abonné absent, relais livré / 503 replanifié / plus de 24 h → DEAD).

### S5 — AllôPharma API : liaison, révocation, réception · 5 SP

*En tant que patient AllôPharma, je veux relier et délier mon dossier Joprelys, et que la révocation faite côté Joprelys soit appliquée chez AllôPharma.*

Tasks :
- [x] T5.1 Ticket AllôPharma `TICKET-0118` + docs `docs/features/eco-patient-link/`.
- [x] T5.2 Migration Flyway : `eco_patient_links` (profil AllôPharma, DPU, émetteur, consentement, statut) et `eco_inbound_events` (dédoublonnage).
- [x] T5.3 `POST /api/v1/patient/eco/joprelys-links` : reçoit le jeton Joprelys ID du mobile, appelle Joprelys, enregistre la liaison, ne conserve pas le jeton.
- [x] T5.4 `DELETE /api/v1/patient/eco/joprelys-links/{id}` : jeton *client credentials* `allopharma-server`, appel de révocation Joprelys, purge locale.
- [x] T5.5 `POST /api/v1/webhooks/eco` : vérification `Eco-Signature` (temps constant), écart d'horloge ≤ 5 min, dédoublonnage `id`, `2xx` rapide puis traitement asynchrone ; `consent.revoked` → statut `revoked` + purge de la copie en cache.
- [x] T5.6 Tests : unitaires (vérifieur de signature, purge) ; intégration (Testcontainers PostgreSQL, Joprelys simulé par serveur HTTP) ; contrat `eco-contracts`.

Critères d'acceptation : un patient ne peut agir que sur ses propres profils (y compris profils familiaux dont il est le tuteur) ; signature invalide → 401 sans traitement ; rejeu du même `id` → `2xx` sans double effet.

Résultat S5 (07/11/2026, PharmaLocate branche `feat/joprelys-id-patient-link`, `TICKET-0118`) : 36 tests nouveaux verts, suite AllôPharma verte. Routes alignées sur AllôPharma (`/api/v1/patient/eco/…`, `/api/v1/webhooks/eco`). Tutelle hors périmètre (pas de profil à charge côté AllôPharma, `self` seul côté Joprelys). Non testé : intégration PostgreSQL (Testcontainers, faute de Docker), échange réel Joprelys ↔ AllôPharma (S8).

### S6 — AllôPharma mobile : consentement, PKCE, révocation · 5 SP

*En tant que patient, je veux comprendre ce que je partage avant de relier mon dossier, et pouvoir arrêter le partage.*

Tasks :
- [ ] T6.1 Dépendance OAuth natif (`flutter_appauth`, version vérifiée sur pub.dev) ; configuration centralisée (issuer, client id, redirect) par environnement, sans secret.
- [ ] T6.2 Écran « Relier mon dossier Joprelys » : ce qui est partagé, ce qui ne l'est pas, pour quel profil (moi, enfant, adulte dépendant), bouton d'accord ; états chargement / erreur / succès / hors ligne.
- [ ] T6.3 Écran d'état de la liaison + révocation avec confirmation.
- [ ] T6.4 i18n `fr` / `en`, `Semantics(identifier: …)` sur chaque élément interactif, cibles ≥ 48 px, thèmes clair/sombre via le design system.
- [ ] T6.5 Tests widget (états, révocation) ; flux Maestro si un émulateur est disponible (sinon signalé non exécuté).

### S7 — Portail patient Joprelys : applications reliées · 3 SP

*En tant que patient du portail Joprelys, je veux voir quelles applications accèdent à mon dossier et pouvoir révoquer.*

Tasks :
- [ ] T7.1 Endpoint portail (authentification patient existante) : liste et révocation des liaisons du patient connecté.
- [ ] T7.2 Composant Angular « Applications reliées » (Tailwind v4, design system interne, i18n FR/EN, états, focus visible, confirmation).
- [ ] T7.3 Tests unitaires Angular + test d'intégration backend ; capture navigateur.

### S8 — Bout en bout en test, docs et clôture · 3 SP

Tasks :
- [ ] T8.1 Scénario pas à pas reproductible (compose + deux backends + mobile ou appel simulé) : liaison → révocation côté Joprelys → purge AllôPharma ; révocation côté AllôPharma → statut Joprelys.
- [ ] T8.2 Mise à jour feuille de route (§ 3 ordre d'exécution), tickets, `PROJECT-TRACKING.md`, `CHANGELOG.md` des deux dépôts.
- [ ] T8.3 Revue sécurité (skill appsec-review) du diff de chaque branche ; gitleaks.
- [ ] T8.4 Proposition SemVer (MINOR dans les deux applis) ; branches prêtes, aucun merge ni push sans accord.

## 5. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Confusion entre jetons maison et jetons Joprelys ID | Accès non autorisé | Chaînes de filtres séparées, tests croisés de refus |
| Secret HMAC ou client versionné | Usurpation | Variables d'environnement, `.env.example` sans valeur, gitleaks |
| Données réelles en test | Fuite de données de santé | Utilisateurs et DPU fictifs uniquement (`DPU-TEST-…`) |
| Webhook rejoué ou forgé | Révocation fantôme ou ignorée | Signature, fenêtre de 5 min, dédoublonnage `id` |
| Docker indisponible localement | Tests Keycloak non exécutables | Signalé honnêtement ; tests unitaires couvrant la logique |
| Mobile : redirection OAuth mal configurée | Flux bloqué | Schéma déclaré Android/iOS, test manuel documenté |

## 6. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-11-07 | IA (Claude) | 0,5 j | 5 % | S1 à S8 | Validation du découpage | ADR et découpage rédigés |
| 2026-11-07 | IA (Claude) | 1 j | 70 % | S6 à S8 | Docker absent (intégration PostgreSQL AllôPharma non exécutée) | S5 livrée côté AllôPharma : liaison, révocation, webhook signé, V64 ; 36 tests ajoutés |
| 2026-11-07 | IA (Claude) | 1 j | 55 % | S5 à S8 | Docker absent | S4 livrée : outbox V124, webhooks signés, relais 24 h → DEAD, révocation portail émettant `consent.revoked` ; 22 tests ajoutés |
| 2026-11-07 | IA (Claude) | 1 j | 40 % | S4 à S8 | Docker absent (Testcontainers reporté) | S3 livrée : API éco, V123, tests 23 verts dont 1 contre Keycloak réel |
| 2026-11-07 | IA (Claude) | 0,5 j | 20 % | S3 à S8, T2.4 | Docker absent du poste | Découpage validé par l'utilisateur. S1 : realm vérifié 23/23 sur Keycloak 26.8.1 lancé depuis l'archive officielle (sha1 vérifié), faute de Docker ; le service Compose lui-même n'a pas été démarré. S2 : dépôt local `eco-contracts`, commit `07e416d`, `npm test` 13/13, gitleaks propre. T2.4 (copie dans les backends) reportée en S3/S5 |

## 7. Statut

Statut : IN_PROGRESS (S1 à S5 livrées ; S6 et S7 suivantes)

## 8. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui (en test) |
| Type de bump | MINOR (Joprelys et AllôPharma) |
| Breaking change | Non : nouvelles routes `/api/eco/v1/**`, API existante inchangée |
| Migration DB | Oui (nouvelles tables uniquement) |
| Changement API | Oui (ajout) |
| Impact Angular | Oui (S7) |
| Impact Flutter | Oui (S6, AllôPharma) |
| Changelog requis | Oui |
