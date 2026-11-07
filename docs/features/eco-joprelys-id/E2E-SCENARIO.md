# Scénario E2E — liaison et révocation Joprelys ↔ AllôPharma (EPIC ECO-20261107, S8)

Le scénario complet (pile locale sans Docker, parcours A et B, script) est maintenu côté AllôPharma, qui porte
la vérification de purge :
`PharmaLocate/docs/features/eco-patient-link/E2E-SCENARIO.md` et `PharmaLocate/scripts/e2e/eco_revocation_loop.py`.

## Ce que Joprelys garantit dans la boucle

| Étape | Mécanisme | Test automatisé |
|---|---|---|
| Liaison créée avec un jeton Joprelys ID (RS256, audience `joprelys-api`, scope `eco:patient-link`) | `/api/eco/v1/patient-links` | `EcoApiIntegrationTest`, `EcoKeycloakLiveIT` |
| Révocation par le patient depuis le portail | `DELETE /api/patient/linked-apps/{id}` (`PATIENT_PORTAL_ACCESS`, patient résolu depuis le jeton) | `PatientLinkedAppsControllerTest` |
| Notification de l'application | outbox transactionnelle → webhook `consent.revoked` signé HMAC-SHA256, relances 24 h | `EcoOutboxIntegrationTest`, `EcoWebhookSignerTest`, `HttpEcoWebhookSenderTest` |
| Révocation depuis AllôPharma | `/api/eco/v1/consents/{id}/revoke` → statut `REVOKED` par `PATIENT`, disparition du portail | `EcoApiIntegrationTest` |

## Exécution du 07/11/2026

- Galaxy A56 et serveurs locaux réels. La révocation depuis le portail donne 204 ; liaison et consentement
  passent à `REVOKED` par `PATIENT` ; l'outbox est `DELIVERED` à la première tentative ; AllôPharma révoque sa
  copie et efface le DPU.
- La révocation depuis AllôPharma donne une liaison `REVOKED` côté Joprelys.
- Non couvert : connexion OTP réelle au portail (session fictive signée par le secret dev local), iOS.
