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

## 3. Stories suivantes

Voir le ticket EPIC `docs/ai/tickets/ECO-20261107-JOPRELYS-ID-PATIENT-LINK.md` (S3 resource server et API éco, S4 outbox et webhooks signés, S7 portail patient).

## 4. Tests

- S1 : démarrage et vérifications manuelles du README (jeton client credentials, refus du *password grant*, refus sans PKCE).
- S3 : tests d'intégration Testcontainers qui chargent ce même fichier de realm, de sorte qu'une régression de configuration casse la CI.
