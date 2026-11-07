# API-CONTRACT — API écosystème v1 (ECO-20261107 S3-S4)

Base : `/api/eco/v1`. Authentification : **jetons Joprelys ID uniquement** (RS256, émetteur `joprelys.eco.identity.issuer-uri`, audience `joprelys-api`). Les jetons maison et les clés API y sont refusés ; les jetons Joprelys ID sont refusés sur le reste de l'API.

Ressources au format `eco-contracts` v1 (copie vérifiée par empreinte dans `backend/src/test/resources/eco-contracts/v1`, synchronisée par `node scripts/sync-eco-contracts.mjs`).

## POST `/patient-links`

Relie un profil AllôPharma au DPU du patient connecté.

- Jeton : patient, client `allopharma-mobile`, scope `eco:patient-link`, claim `dpu`.
- Corps :

```json
{
  "allopharmaProfileId": "0192f0c4-6e55-7b10-8a00-000000000201",
  "relationship": "self",
  "scope": ["prescriptions", "lab-results"],
  "periodEnd": "2027-11-07T09:10:00Z"
}
```

`periodEnd` est facultatif (1 an par défaut, 2 ans au plus). `scope` : 1 à 5 valeurs parmi `prescriptions`, `lab-results`, `immunizations`, `appointments`, `emergency-summary`.

- Réponses :
  - `201` liaison créée ; `200` même liaison active déjà existante (appel rejouable). Corps : `{ "patientLink": PatientLink, "consent": Consent }` ;
  - `400 INVALID_REQUEST` corps invalide ;
  - `401` jeton absent, expiré, mal signé, mauvais émetteur ou mauvaise audience ;
  - `403` scope manquant, `403 UNKNOWN_CLIENT` client non déclaré, `403 MISSING_DPU` jeton sans DPU ;
  - `404 UNKNOWN_DPU` aucun dossier pour ce DPU ;
  - `409 PROFILE_LINKED_ELSEWHERE` profil déjà relié à un autre DPU ;
  - `422 UNSUPPORTED_RELATIONSHIP` (`child`, `dependent-adult` : preuve de tutelle à venir), `422 INVALID_PERIOD`.

## POST `/consents/{consentId}/revoke`

Révocation par l'application bénéficiaire. Idempotente.

- Jeton : compte de service `allopharma-server` (client credentials), scope `eco:consent.revoke`.
- Corps : `{ "revokedBy": "patient" | "guardian", "reason": "≤ 200 caractères, facultatif" }`.
- Réponses :
  - `200` : `{ patientLink, consent }`, consentement et liaison `revoked`, `meta.versionId` incrémenté une seule fois ;
  - `400`, `401`, `403` comme ci-dessus ;
  - `404 CONSENT_NOT_FOUND` : consentement inexistant **ou** appartenant à une autre application (pas de divulgation).

## Erreurs

`application/problem+json` : `status`, `detail` (français, sans donnée personnelle), `code` stable.

## Configuration

| Propriété | Variable | Défaut dev |
|---|---|---|
| `joprelys.eco.source-tenant` | `JOPRELYS_ECO_SOURCE_TENANT` | `clinique-demo` |
| `joprelys.eco.identity.issuer-uri` | `JOPRELYS_ID_ISSUER_URI` | `http://localhost:8180/realms/joprelys-id` |
| `joprelys.eco.identity.jwk-set-uri` | `JOPRELYS_ID_JWK_SET_URI` | `…/protocol/openid-connect/certs` |
| `joprelys.eco.identity.audience` | `JOPRELYS_ID_AUDIENCE` | `joprelys-api` |
| `joprelys.eco.applications` | — | `allopharma-mobile`, `allopharma-server` → `allopharma` |

| `joprelys.eco.webhooks.subscribers.allopharma.url` | `JOPRELYS_ECO_WEBHOOK_ALLOPHARMA_URL` | vide (aucun envoi) |
| `joprelys.eco.webhooks.subscribers.allopharma.secret` | `JOPRELYS_ECO_WEBHOOK_ALLOPHARMA_SECRET` | vide ; 32 caractères minimum si renseigné |
| `joprelys.eco.webhooks.outbox.relay-enabled` | `JOPRELYS_ECO_OUTBOX_RELAY_ENABLED` | `true` |
| `joprelys.eco.webhooks.outbox.poll-interval` | `JOPRELYS_ECO_OUTBOX_POLL_INTERVAL` | `PT15S` |

Les clés de signature sont téléchargées au premier jeton : si Joprelys ID est injoignable, l'API éco répond `401` et le reste de Joprelys fonctionne normalement.

## Webhooks sortants (S4)

Joprelys → abonné : `POST <url de l'abonné>` (AllôPharma : `/api/eco/v1/webhooks`, livré en S5).

- En-têtes : `Content-Type: application/json`, `Eco-Signature: t=<secondes unix>,v1=<hex HMAC-SHA256(secret, t + "." + corps)>`, `Idempotency-Key: <id de l'événement>`.
- Corps : enveloppe CloudEvents 1.0 (`eco-contracts` `event-envelope.schema.json`) ; pour `consent.revoked`, `data` est conforme à `consent-revoked.data.schema.json` et `subject` vaut `Consent/<consentId>`.
- Le récepteur vérifie la signature en temps constant sur le corps brut, refuse un `t` décalé de plus de 5 minutes, déduplique sur `Idempotency-Key` et répond `2xx`, y compris pour un doublon.
- Toute autre réponse, ou une erreur réseau, est rejouée : 30 s, 1 min, 2 min… plafonné à 1 h, pendant 24 h ; ensuite l'événement passe en `DEAD`.
- Émis aujourd'hui : `consent.revoked` quand le patient révoque depuis le portail Joprelys.
