# TECHNICAL-DESIGN — Joprelys ID et liaison patient ↔ DPU (EPIC ECO-20261107)

Décision : [ADR-20261107-JOPRELYS-ID-KEYCLOAK](../../ai/adr/ADR-20261107-JOPRELYS-ID-KEYCLOAK.md).

## 1. Composants

```text
AllôPharma mobile ──PKCE──▶ Joprelys ID (Keycloak, realm joprelys-id)
        │ jeton patient (aud joprelys-api, scope eco:patient-link, claim dpu)
        ▼
AllôPharma API ──POST /api/eco/v1/patient-links (jeton patient)──▶ Joprelys API
AllôPharma API ──POST /api/eco/v1/consents/{id}/revoke (client credentials)──▶ Joprelys API
Joprelys API ──webhook consent.revoked (Eco-Signature, outbox)──▶ AllôPharma API
```

## 2. S1 — Joprelys ID en dev (livré dans cette branche)

| Fichier | Rôle |
|---|---|
| `docker-compose.yml` | Services `keycloak` (26.8.1) et `keycloak-db` (PostgreSQL 16), profil `eco`, port `127.0.0.1:8180` |
| `infra/keycloak/import/joprelys-id-realm.json` | Realm importé au démarrage (`--import-realm`) ; aussi chargé par les tests Testcontainers (S3) |
| `infra/keycloak/.env.example` | Modèle des variables ; `.env` reste ignoré par git |
| `infra/keycloak/README.md` | Démarrage, contenu du realm, vérifications |

Choix de configuration :

- **Placeholders d'environnement** dans le realm (`${ALLOPHARMA_SERVER_CLIENT_SECRET}`, `${JOPRELYS_ID_TEST_USER_PASSWORD}`) : aucune valeur sensible versionnée.
- **Scope `basic` explicite** : quand un import déclare `clientScopes`, Keycloak ne crée pas ses scopes par défaut ; `basic` porte la claim `sub` (Keycloak ≥ 25).
- **Minimisation** : la claim `dpu` n'est émise que si le scope `eco:patient-link` est accordé ; pas de nom ni d'e-mail dans le jeton d'accès (pas de scopes `profile` / `email`).
- **Audience** : mapper `oidc-audience-mapper` vers le client `joprelys-api` sur chaque scope de contrat ; Joprelys refusera tout jeton sans cette audience.
- **Mobile** : public, PKCE S256 obligatoire, `consentRequired`, pas de *direct access grant*, redirection à schéma privé `cm.allopharma.app:/oauth2redirect` (RFC 8252). L'identifiant Android `cm.allopharma.allopharma_mobile` contient un `_`, interdit dans un schéma d'URI : d'où un schéma dédié.
- **Serveur** : `allopharma-server` confidentiel, compte de service seul, `fullScopeAllowed=false`.
- **Profil utilisateur déclaratif** : attributs non gérés désactivés ; `dpu` en lecture/écriture administrateur seulement, validé par motif.
- **Durées** : jeton d'accès 5 min, session SSO 15 min d'inactivité / 1 h max, *refresh token* à usage unique.

## 3. S3 — API éco Joprelys (livré dans cette branche)

Contrat : [API-CONTRACT.md](API-CONTRACT.md). Paquet `com.joprelys.backend.eco` :

| Couche | Classes |
|---|---|
| `config` | `EcoSecurityConfig` (chaîne `/api/eco/**` en tête, resource server JWT, `denyAll` par défaut), `EcoJwtValidators` (expiration, émetteur, audience), `EcoProperties` |
| `api` | `EcoPatientLinkController`, `EcoConsentController` (aucune logique métier), `EcoCallerResolver` (`azp` → application déclarée), `EcoResourceMapper`, `EcoDtos`, `EcoExceptionHandler` (ProblemDetail + `code`) |
| `application` | `PatientLinkUseCase` / `PatientLinkService`, `ConsentRevocationUseCase` / `ConsentRevocationService`, `EcoBusinessException` (codes typés) |
| `domain` | `EcoStatus`, `LinkRelationship`, `ConsentScope`, `RevokedBy`, `GranteeType` |
| `infrastructure` | `EcoPatientLinkEntity`, `EcoConsentEntity` (révocation idempotente, `versionId`), dépôts JPA |

Données : migration `V123__eco_patient_links_and_consents.sql` (tables `eco_patient_links`, `eco_consents`, contraintes de statut et de cohérence de révocation). La liaison stocke le DPU, l'application, l'identifiant opaque du profil externe, le sujet et le client Joprelys ID : aucune donnée d'identité.

Points de conception :

- **Filtres maison** : `JwtAuthenticationFilter` et `ApiKeyAuthenticationFilter` sont des `@Component`, donc enregistrés pour toutes les requêtes ; ils s'abstiennent désormais sur `/api/eco/**` (`shouldNotFilter`), sinon ils refuseraient un jeton Keycloak avant la chaîne éco.
- **Décodeur paresseux** (`withJwkSetUri`) : pas d'appel à Keycloak au démarrage.
- **Contrôle d'accès par ressource** : une application ne révoque que les consentements dont elle est bénéficiaire ; sinon `404`.
- **Idempotence** : liaison rejouée → `200` avec la liaison existante ; révocation rejouée → état inchangé.
- **Relation** : seule `self` est acceptée tant que la preuve de tutelle n'existe pas.
- **Audit** : création et révocation journalisées (`ECO_PATIENT_LINK`, `ECO_CONSENT`), sans contenu santé.

## 4. Stories suivantes

Voir le ticket EPIC `docs/ai/tickets/ECO-20261107-JOPRELYS-ID-PATIENT-LINK.md` (S4 outbox et webhooks signés, S7 portail patient).

## 5. Tests

- S1 : `node infra/keycloak/verify-realm.mjs` (23 vérifications) contre un Keycloak démarré.
- S3, sans Docker (décision du 07/11/2026) :
  - `EcoApiIntegrationTest` (20 tests) : contexte Spring complet, H2 + Flyway, **vrais jetons RS256** signés par une clé de test et validés par les mêmes validateurs qu'en production (`EcoTestTokens`, `EcoTestSecurityConfiguration`) ; réponses validées contre les schémas `eco-contracts` (`EcoContractSchemas`) ;
  - `EcoContractChecksumTest` : la copie des schémas correspond à `SHA256SUMS` ;
  - `EcoKeycloakLiveIT` : jetons émis par un vrai Keycloak, clés téléchargées par le vrai décodeur ; désactivé sans variables, lancé par `node infra/keycloak/run-live-eco-test.mjs`. Non exécuté en CI.
- Testcontainers Keycloak : reporté jusqu'à disponibilité de Docker.
