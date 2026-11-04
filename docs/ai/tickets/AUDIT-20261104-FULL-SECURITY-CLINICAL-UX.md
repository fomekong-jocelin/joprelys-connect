# AUDIT-20261104 — Audit complet et correctifs : sécurité, sécurité clinique, parcours et interfaces

## Mode

QA / Diagnostic (audit en 4 volets), puis Engineering (correctifs) et Project Manager (découpage du reste en EPIC-0030).

## Demande

« Audite cette application de fond en comble, fais ton constat, crée une branche et corrige, de la sécurité à l'interface utilisateur : un parcours clair et sans ambiguïté, des fonctionnalités complètes qui marchent bien. »

## Statut

READY_FOR_REVIEW. Correctifs livrés sur la branche locale `fix/audit-20261104-security-clinical-ux`, **ni commités ni poussés**. Avant fusion :
- relecture clinique par un médecin référent et le pharmacien (ADR-20261104 au statut « proposé ») ;
- avis du DPO sur les journaux et le bris de glace ;
- recette navigateur et appareil.

## Références

[AUDIT-REPORT](../../features/audit-20261104-full/AUDIT-REPORT.md) · [FUNCTIONAL-SPEC](../../features/audit-20261104-full/FUNCTIONAL-SPEC.md) · [TECHNICAL-DESIGN](../../features/audit-20261104-full/TECHNICAL-DESIGN.md) · [ADR-20261104](../../architecture/adr/ADR-20261104-audit-security-clinical-hardening.md) · [EPIC-0030](../../pm/backlog/EPIC-0030-audit-20261104-remediation.md)

## Actions

- [x] Audit backend AppSec (SEC-01 à SEC-21).
- [x] Audit clinique (CLIN-01 à CLIN-16, FA restants).
- [x] Audit web (UX-01 à UX-18).
- [x] Audit mobile (MOB-01 à MOB-20).
- [x] Secret JWT : plus de valeur par défaut versionnée ; secret aléatoire en dev ; refus de démarrer avec l'ancien secret publié.
- [x] Dépendances web : Angular 22.2.4 ; garde `npm audit --omit=dev` en CI.
- [x] Scripts SSH : `RejectPolicy`.
- [x] Sécurité clinique :
  - CLIN-04 : le patient ne voit que du validé, avec un message gradué ;
  - CLIN-06 : ordonnance réservée au praticien en charge, sur visite en cours ;
  - CLIN-07 / CLIN-08 / FA-37 : actes réservés au médecin ;
  - CLIN-09 : heure d'administration bornée ;
  - CLIN-11 : visite du même patient ;
  - CLIN-12 : vérification pharmacie fidèle, quantité positive, substitution ;
  - CLIN-13 : aucun soin après le décès.
- [x] CLIN-05 (partie) : brouillon par défaut, pas de validation par simple statut, échec PDF journalisé.
- [x] CLIN-10 (partie) : triage en liste fermée, bornes physiques des constantes initiales.
- [x] SEC-01 / FA-39 : PIN 6 chiffres `SecureRandom`, verrouillage persistant atomique, 401 uniforme (V121).
- [x] Authentification et infrastructure (worktree intégré) : SEC-02 (motif), 03, 04, 05, 06, 07 (partiel), 09, 11, 13, 16, 19, 20 (webhook).
- [x] Web :
  - UX-01, 04, 05, 06, 08, 11, 13 ;
  - UX-09, 10 et 12 en partie ;
  - adaptations aux nouveaux contrats : motif de bris de glace ≥ 10 caractères, ses propres rôles en lecture seule, texte AUDITEUR.
- [x] Mobile :
  - MOB-02, 03, 04, 05, 06, 07, 13 ;
  - MOB-01 et MOB-19 en partie ;
  - nom iOS.
- [x] Documentation : rapport d'audit, spécification fonctionnelle, conception technique, ADR, EPIC-0030, `DESIGN.md` (contrastes), CHANGELOG, PROJECT-TRACKING.
- [ ] Relecture clinique : libellés des notifications patient, règles de décès et d'administration.
- [ ] Recette navigateur complète (backend lancé) et recette Maestro sur appareil : `FLAG_SECURE`, verrou pendant une dictée.
- [ ] Changer `JOPRELYS_JWT_SECRET` sur tout environnement qui a tourné avec l'ancien secret par défaut.
- [x] PDF de test `storage/documents/DOC-20260702-000001.pdf` et `backend/storage/documents/DOC-20260702-000001.pdf` retirés de l'index Git (conservés sur disque, chemins déjà ignorés) ; `.claude/worktrees/` ajouté au `.gitignore`.
- [x] Commits conventionnels et merge local dans `main` à la demande de l'utilisateur (2026-11-04). Push : non demandé, non fait.

## Sécurité : authentification et infrastructure

Ces correctifs ont été développés dans un worktree isolé (`.claude/worktrees/agent-a9aa838d3f06e9211`), puis appliqués par `git apply` (52 fichiers). Seul `application.yml` était modifié des deux côtés ; les deux changements ont été conservés.

| ID | Statut | Détail |
|---|---|---|
| SEC-02 | ◐ | Motif obligatoire, 10 à 500 caractères. Le modèle par utilisateur reste à faire (B1). |
| SEC-03 | ✅ | AUDITEUR et `AUDIT_CROSS_TENANT_READ` réservés à la plateforme. Un administrateur ne peut plus modifier ses propres rôles. Les affectations AUDITEUR existantes en établissement ne sont pas migrées : à vérifier en base. |
| SEC-04 | ✅ | Audit des lectures persisté (`REQUIRES_NEW`). Le test prouvait la perte avant correctif. |
| SEC-05 | ◐ | OTP en temps constant et compteurs atomiques ; IP via `getRemoteAddr` et `forward-headers-strategy: native`. Toujours en mémoire, donc mono-instance (B8). Restreindre `server.tomcat.remoteip.internal-proxies` à l'IP du proxy. |
| SEC-06 | ✅ | Réponse générique et tentatives conservées sur le portail patient. |
| SEC-07 | ◐ | Repli par e-mail supprimé. Refus des jetons sans `sid` non appliqué (≈ 130 jetons de test sans session). |
| SEC-09 | ✅ | Écriture clinique limitée à l'établissement de la visite. Restent `LabOrderController`, `AuthorizedPrescriptionService` et `PatientMedicalInfoController` (décision métier sur l'écriture avec consentement). |
| SEC-11 | ✅ | Plus de données de santé dans les journaux, `AlloPharmaClient` compris ; `X-Trace-Id` validé. |
| SEC-13 | ✅ | 400, 405, 413 et 415 au lieu de 500. |
| SEC-16 | ✅ | Sessions révoquées et événement audité après réinitialisation du mot de passe. |
| SEC-19 | ◐ | Origines WebSocket restreintes ; permission exigée au handshake. Le jeton passe encore en query string (B5). |
| SEC-20 | ◐ | Secret de webhook masqué hors création. Restent : nom complet sur la vérification publique de document, PDF de test suivis par Git. |

## Vérifications

Toutes les commandes ci-dessous ont été lancées le 2026-11-04 sur la branche.

| Périmètre | Commande | Résultat |
|---|---|---|
| Backend (tous correctifs réunis) | `./mvnw -q -o test` | **1028 tests, 0 échec, 0 erreur, 9 ignorés** (PostgreSQL/Docker, déjà ignorés avant). Référence avant le lot : 961. |
| Migration V121 | `psql` sur PostgreSQL 17 local (base en V120), dans une transaction annulée | OK : `pin_code` VARCHAR(8), `pin_failed_attempts` INTEGER NOT NULL DEFAULT 0, `pin_locked_until` TIMESTAMP ; rien ne reste après `ROLLBACK`. |
| Web | `npx ng test --watch=false` | **742 tests verts** (134 fichiers). Référence avant le lot : 672. |
| Web | `npx ng build`, `npm run i18n:check` | Build OK ; i18n shell : 51 clés en fr et en. |
| Web | `npm audit --omit=dev` | 0 vulnérabilité. 5 restent dans l'outillage de dev, non livrées. |
| Web (navigateur, sans backend) | Playwright à 390 px, thèmes clair et sombre | Page 404 OK ; route protégée → « Connexion au serveur impossible » avec `returnUrl` ; bouton primaire `#077a96` sur texte blanc. Coquille « Tous droits reserves » corrigée. Captures dans `.playwright-mcp/` (ignoré par Git). |
| Mobile | `flutter analyze --no-pub` | No issues found. |
| Mobile | `flutter test --no-pub` | 226 tests verts (201 avant). |
| Mobile | `flutter build apk --debug` (agent) | OK. Manifeste fusionné : `allowBackup=false`, `dataExtractionRules`. |

**Non testé** :
- rendu réel dans un navigateur avec le backend (parcours complets, contrastes perçus, focus clavier) ;
- comportement sur appareil (`FLAG_SECURE`, verrou pendant une dictée, purge) ;
- multi-instance ;
- import labo partenaire en conditions réelles (probablement non fonctionnel, voir TECHNICAL-DESIGN § 10).

## Estimation et capacité

| Élément | Estimation |
|---|---|
| Audit | ≈ 8 SP |
| Correctifs livrés | ≈ 40 SP. Hors sprint planifié : capacité à régulariser par le PO selon `docs/pm/CAPACITY-PLANNING.md`. |
| Reste (EPIC-0030, hors lot D) | 173 SP. Ordre recommandé : A1, A4, B1, A2, A3, en parallèle de C1 et C6. |

## SemVer

**MAJOR candidat**. Le contrat d'API change de façon visible pour les clients :
- 401 au lieu de 404 pour une ordonnance inconnue ;
- nouveaux refus 403 et 409 sur des actes auparavant permis ;
- secret de webhook masqué ;
- jetons dont le `sub` est un e-mail refusés ;
- démarrage refusé sans `JOPRELYS_JWT_SECRET`.

Pour un usage interne où web, mobile et backend sont livrés ensemble, un MINOR est défendable. La décision revient au PO (`docs/release/SEMANTIC-VERSIONING.md`). Aucune version n'est préparée.

## Risques restants

- **P0 cliniques non corrigés** : allergies et doublons à la prescription (A1), posologie libre (A2), prescriptions de séjour (A3). Aucun dépannage n'est livré pour ces trois points.
- **Alerte au prescripteur** : un résultat critique n'est encore signalé à aucun médecin (A4).
- **Mobile** : release signée avec la clé debug (C6), **bloquant avant toute diffusion d'APK**.
- **Audit** : en `REQUIRES_NEW`, il consomme deux connexions par requête (pool de 10) ; à mesurer en charge.
- **Décisions métier en attente** : FA-27 (constantes saisies par l'accueil, Q-05) et FA-38 (unité de dispensation).
