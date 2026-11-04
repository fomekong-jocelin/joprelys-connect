# TECHNICAL-DESIGN — Correctifs de l'audit du 2026-11-04

Références : [FUNCTIONAL-SPEC](FUNCTIONAL-SPEC.md), [ADR-20261104](../../architecture/adr/ADR-20261104-audit-security-clinical-hardening.md), [AUDIT-REPORT](AUDIT-REPORT.md).

Préfixe des chemins : `B/` = `backend/src/main/java/com/joprelys/backend/`.

## 1. Principes

- **Le backend reste maître de chaque règle.** Le web et le mobile reflètent la règle (bouton désactivé, aide, message), mais ne la portent jamais seuls.
- **Pas de nouvel endpoint, pas de champ retiré.** Les changements de contrat sont uniquement de nouveaux refus (400/401/403/409/413/415/429), plus quelques champs ajoutés en lecture seule sur la vérification pharmacie.
- **Une seule migration** : `V121__prescription_pin_brute_force_protection.sql`.

## 2. Actes médicaux et ordonnances

- **`B/consultation/application/MedicalSigningPolicy`** :
  - `requirePhysician(actorId)` et `requirePhysicianFor(actorId, message)` renvoient 403 si le compte n'est pas un médecin actif ;
  - `isPhysician(user)` sert pour les appelants qui ont déjà résolu l'utilisateur.
- **Appelants de cette politique** :
  - `ConsultationService.saveConsultation` ;
  - `HospitalizationDischargeWorkflowService.decideDischarge` ;
  - `PatientDeathService.declareDeath`, qui couvre aussi l'orientation `DEATH` de `EmergencyService.stabilize`.
- **`PrescriptionService`** :
  - `savePrescription` exige une visite `EN_COURS` ;
  - `finalizePrescription` et `cancelPrescription` appellent `requirePractitionerInCharge` (règle `consultingPractitionerId`, sans effet sur une ordonnance sans visite) ;
  - l'annulation est limitée à `CANCELLABLE_STATUSES = {DRAFT, ACTIVE}`.
- **`DocumentService.cancelDocumentIfValid`** n'annule que si le document est `VALID`. Avant, `cancelDocument` levait une exception dans une méthode `@Transactional` ; l'appelant l'avalait, mais la transaction était déjà marquée rollback-only, d'où une erreur 500 à l'annulation d'une ordonnance dont le document était déjà annulé.

## 3. PIN d'ordonnance (`B/prescription/application/PrescriptionPinGuard`)

- **Génération** : `generatePin()`, 6 chiffres tirés par `SecureRandom`. La colonne `pin_code` passe en `VARCHAR(8)`.
- **Vérification** : `verify(number, storedPin, providedPin)` procède en trois temps :
  1. **réservation atomique**, dans une transaction `REQUIRES_NEW` :
     ```sql
     UPDATE prescriptions SET pin_failed_attempts = pin_failed_attempts + 1
     WHERE prescription_number = ? AND (pin_locked_until IS NULL OR pin_locked_until <= now) AND pin_failed_attempts < 3
     ```
     Zéro ligne mise à jour : 401 si le numéro est inconnu, 429 si l'ordonnance est verrouillée.
  2. **comparaison** en temps constant (`MessageDigest.isEqual`) ;
  3. **résultat** : en cas d'échec, verrouillage 15 min si le seuil est atteint (429), sinon 401 ; en cas de succès, remise à zéro du compteur.
- **Pourquoi `REQUIRES_NEW`** : l'échec lève une exception qui annule la transaction de la requête. Le compteur doit pourtant survivre à cette annulation.
- **Usage** : `PharmacyService.verify`, `dispense` et `history` passent par `requireValidPin`. Les `ConcurrentHashMap` en mémoire sont supprimées.

## 4. Décès, identité et laboratoire

- **`PatientDeathService`** :
  - `rejectCareIfDeceased(patientId)` ;
  - `rejectCareAfterDeath(patientId, actAt)`, qui compare l'heure de l'acte à `deceasedAt`.

  Les deux passent par `PatientCanonicalResolver` pour suivre les dossiers fusionnés.
- **`HospitalizationCareService`** : une heure d'administration au-delà de `now + 5 min` donne 400, puis `rejectCareAfterDeath` est appelé.
- **`PharmacyService.rejectIfPatientDeceased`** : une requête SQL remonte ordonnance → consultation → visite → patient, puis le lien canonique. Elle est appelée par `dispense` et `validatePrescription`.
- **`LabOrderService.create`** :
  - patient canonique ;
  - `rejectIfDeceased` ;
  - visite d'un autre patient canonique : 400.
- **`LabOrderService.updateItemStatus`** : `VALIDATED` est refusé avec 409. La validation passe par `LabResultService.upload`, qui valide les résultats et l'examen ensemble.
- **`LabResultService`** :
  - saisie personnel sans statut : `DRAFT` ;
  - `notifyPatient` gradué (CRITICAL → `EMERGENCY`, ABNORMAL → `INFO`) ;
  - l'échec de création du document PDF est journalisé en ERROR, sans données de santé.
- **`PatientSummaryService`** :
  - consultations `VALIDEE` uniquement ;
  - résultats critiques calculés sur `latestValidatedResults`, c'est-à-dire la dernière version validée par `resultNumber` et analyte.
- **`PatientExamResultsController`** : le rôle patient passe par `getPatientPublishedResults`.

## 5. Pharmacie et urgences

- **`PharmacyVerifyItem`** expose maintenant les champs prescrits : `form`, `substitutionAllowed`, `posology`, `route`, `frequency`, `duration`. Ce sont des ajouts en fin de record, rétrocompatibles en JSON.
- **`dispense`** :
  - quantité ≤ 0 : 400 ;
  - `substitutedWith` renseigné alors que `substitutionAllowed = false` : 409.
- **`CreateEmergencyRequest` et `EmergencyTriageRequest`** :
  - `@Pattern(RED|ORANGE|YELLOW|GREEN)` sur le niveau de triage ;
  - bornes physiques `@Min`/`@Max`/`@DecimalMin`/`@DecimalMax` sur PAS, PAD, FC et T° initiales.

## 6. Authentification et infrastructure

Ces correctifs ont été développés dans un worktree isolé, puis intégrés par `git apply`.

| Point | Implémentation |
|---|---|
| Secret JWT | `application.yml` : `${JOPRELYS_JWT_SECRET:}` (démarrage refusé sans secret). `application-dev.yml` : `${random.value}${random.value}`, soit 64 caractères tirés à chaque démarrage. `JwtProperties` refuse l'empreinte SHA-256 de l'ancien secret publié. `spring-boot.run.profiles=dev` dans le `pom.xml`. |
| SEC-04 | `AuditServiceImpl` écrit en `REQUIRES_NEW` ; `AuditLogEntity implements Persistable` pour un `persist` au lieu d'un `merge`. Test qui prouvait la perte : `AuditLogReadOnlyTransactionPersistenceTest`. |
| SEC-03 | `AUDITEUR` et `AUDIT_CROSS_TENANT_READ` deviennent plateforme (`RbacCatalog`). `RbacAdministrationService.replaceUserRoles` refuse de modifier ses propres rôles (403). Le web rend ses propres rôles en lecture seule. |
| SEC-05 | `B/auth/security/OtpChallengeStore` : comparaison en temps constant, compteurs atomiques (`compute`), purge. BCrypt factice pour un utilisateur inconnu. `RateLimitingFilter` lit `getRemoteAddr()` avec `server.forward-headers-strategy: native`, et ses compteurs sont bornés et purgés. |
| SEC-07 | Le repli de résolution par e-mail est supprimé dans `JwtAuthenticationFilter`. Le refus des jetons sans `sid` n'est **pas** appliqué : environ 130 jetons de test sans session. |
| SEC-16 | `RevokeAllUserSessionsUseCase` et `SystemSessionRevocationService` : révocation `PASSWORD_RESET` et audit. |
| SEC-06 | Même 404 générique pour toute demande d'OTP patient refusée ; tentatives conservées 15 min ; 429 en cas de verrouillage. |
| SEC-02 | Motif d'accès d'urgence : `@NotBlank @Size(min = 10, max = 500)`. Le web applique la même règle (`patient/emergency-access-reason.ts`). |
| SEC-09 | `ClinicalWriteScopePolicy` et `VisitOrganizationClinicalWriteScopePolicy` : écriture clinique uniquement dans l'établissement de la visite (403 `CLINICAL_WRITE_OUT_OF_SCOPE`). |
| SEC-11 | Plus aucune donnée de santé dans les logs : `NotificationService`, `PatientSimilarityService`, `WebhookService`, `PdfGeneratorService`, `AlloPharmaClient`. `TraceIdFilter` valide `[A-Za-z0-9_-]{1,64}`. |
| SEC-13 | `GlobalExceptionHandler` : 400, 405, 413 et 415 au format `ApiErrorResponse`, sans détail technique. |
| SEC-19 | Origines WebSocket = `joprelys.web.base-url` (démarrage refusé avec `*`). Le handshake exige `CLINICAL_WRITE` ou `VISIT_VITALS_WRITE`. |
| SEC-20 | `WebhookResponse` : le secret n'est renvoyé qu'à la création. |

## 7. Web Angular (`web/src/app`)

- **Composants et utilitaires partagés** :
  - `shared/ui/load-error.component.ts` : état d'erreur avec « Réessayer » ;
  - `consultation/consultation-patient-context.store.ts` : état « allergies vérifiées », qui conditionne clôture et ordonnance ;
  - `shared/pages/not-found-page` et `server-unavailable-page`, routes `**` et `/server-unavailable` ;
  - `auth/safe-return-url.ts` : n'accepte que `/x`, jamais `//` ni une URL absolue ;
  - `auth/dashboard-route.service.ts`.
- **Confirmations** : `pharmacy/pharmacy-dispensation-confirmation.ts`, `clinic/lab/lab-result-confirmation.ts`. `dialog-host` place le focus sur « Annuler » en ton `danger`.
- **Formats** : `shared/formatting/` (pipes `xaf` et `appDate`) ; `registerLocaleData(fr)` et `LOCALE_ID` fixés au démarrage.
- **Tokens** : `styles.css` passe `--text-muted` à `#5b6c82` et le primaire à `#077a96` (thème clair).
- **i18n** : nouvelles clés dans `assets/i18n/features/ux-hardening/{fr,en}.json` ; `document.documentElement.lang` est synchronisé.

## 8. Mobile Flutter (`mobile/`)

- **Jeton et session** :
  - `core/network/api_access_token_resolver.dart` : jeton relu et rafraîchi à chaque (re)connexion WebSocket ;
  - `api_recovery_interceptor.dart` : `expire()` uniquement sur 401/403 ;
  - `AuthSessionManager.sessionExpirations` → `AuthController` ;
  - `auth_session_scope.dart` : les providers de données dépendent de la session.
- **Logs** : `core/security/safe_debug_log.dart`, actif en debug uniquement, avec expurgation de `token=`, `ticket=` et `Bearer`.
- **Verrou** : `AuthLifecycleLock` dans `MaterialApp.router(builder:)`, avec son propre `Navigator`.
- **Android** :
  - `FLAG_SECURE` posé avant `super.onCreate` ;
  - `allowBackup=false` et `data_extraction_rules.xml` qui excluent tout ;
  - libellé `@string/app_name`.
- **iOS** : `CFBundleDisplayName` vaut « Joprelys Connect ».
- **Purge de session** : `LocalSessionDataPurger` efface les brouillons de dictée et les PDF temporaires (`lab_result_pdf_temporary_store.dart`).

## 9. Tests ajoutés

- **Backend** :
  - `MedicalActAuthorityControllerTest` : sortie et consultation par un administrateur → 403 ; administration future → 400 ; administration après décès → 409, avant décès → 201 ;
  - tests ajoutés à `PrescriptionControllerTest` : autre médecin → 409, visite close, ordonnance expirée ;
  - tests ajoutés à `EmergencyControllerTest` : décès déclaré par un non-médecin, triage et constantes hors bornes ;
  - tests ajoutés à `LabOrderControllerTest` : visite d'un autre patient, patient décédé, validation par simple statut ;
  - test ajouté à `LabIntegrationControllerTest` : notification graduée ;
  - tests ajoutés à `PharmacyControllerTest` : numéro inconnu → 401, quantité négative, patient décédé, substitution interdite ;
  - `PatientSummaryServiceTest`, `PrescriptionPinGuardTest` et `JwtPropertiesTest` (y compris le secret aléatoire du profil dev) ;
  - les tests de sécurité du worktree (§ 6).
- **Web** : specs des composants modifiés, `emergency-access-reason.spec.ts`, RBAC en lecture seule pour soi-même.
- **Mobile** : 25 tests (session, jeton WebSocket, journaux, verrou, purge).

## 10. Risques techniques connus

- **Audit en `REQUIRES_NEW`** : deux connexions par requête auditée (pool Hikari de 10), et une ligne SUCCESS reste écrite même si la requête métier est annulée. À mesurer en charge.
- **Import labo partenaire** : il ne retrouve la demande que si le contexte d'établissement est déjà posé. Le test d'intégration le réinjecte ; en production anonyme, l'import échoue probablement. À corriger avec une clé par établissement (A5).
- **PIN** : stocké en clair, et identifiant d'ordonnance séquentiel (B9).
- **Compteurs OTP** : en mémoire, donc une seule instance supportée (B8).
- **Taille des fichiers** :
  - déjà au-delà de 500 lignes avant ce lot : `PdfGeneratorService` (1 184), `lab-orders-page.component.ts` (778), `patient-consultations-tab.component.ts` (555), `cloud_speech_streaming_service.dart` (1 326) ;
  - aucun service backend modifié dans ce lot ne dépasse 500 lignes. Au-dessus du seuil d'alerte de 300 lignes : `LabResultService` (429), `PharmacyService` (389), `LabOrderService` (384). À découper en priorité (C4).
