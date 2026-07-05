# TICKET-0114 — Alignement modules 4 à 12 du CDC : DPU, soins, documents, consentements

| Champ | Valeur |
|---|---|
| **ID** | TICKET-0114 |
| **Type** | Epic / Diagnostic |
| **Epic** | EPIC-0014 |
| **Titre** | Alignement des modules 4 à 12 du CDC avec le code back et front actuel |
| **Statut** | DONE |
| **Priorité** | P0 |
| **Stack** | Full-stack |
| **Profil recommandé** | Tech Lead / Senior Full-stack |
| **Estimation Senior** | 8.0j |
| **Estimation Intermédiaire** | 12.0j |
| **Estimation Junior** | 20.0j |
| **Sprint cible** | SPRINT-0011 |
| **Assigné** | À assigner |
| **Reviewer** | Lead Developer |
| **Dernière MAJ** | 2026-07-05 |

---

## 1. Contexte et objectif

Le Cahier des charges (CDC) Joprelys Connect v1.0 définit 12 modules fonctionnels détaillés (modules 4 à 12) couvrant le cœur métier du produit :

- Module 4 — Dossier patient partagé (DPU)
- Module 5 — Visites et consultations
- Module 6 — Allergies et antécédents
- Module 7 — Prescriptions et ordonnances
- Module 8 — Examens médicaux
- Module 9 — Résultats d’examens
- Module 10 — Hospitalisation
- Module 11 — Documents médicaux vérifiables
- Module 12 — Consentement patient

Ces modules sont **partiellement implémentés** aujourd’hui. L’objectif de ce ticket est de faire un **état des lieux complet**, d’identifier **tous les écarts** par rapport au CDC, et de produire un **découpage en User Stories hautement prioritaires** pour aligner back et front sans compromis sur la qualité du code et le design.

---

## 2. Méthode d’audit

L’audit a été réalisé en parallèle sur :

- **Backend** : `backend/src/main/java/com/joprelys/backend` (entités, repositories, services, controllers, DTOs, migrations Flyway, tests)
- **Frontend** : `web/src/app` (composants Angular, services, design system, routes, i18n, tests)
- **Standards projet** : `AGENTS.md`, `SKILL.md`, `DESIGN.md`, `docs/standards/*`
- **CDC** : modules 4 à 12, exigences fonctionnelles (FR-*) et sections 13 (interfaces)

Commandes de vérification exécutées :

```bash
cd backend && ./mvnw test
# Résultat : BUILD SUCCESS — 231 tests, 0 échec, 0 erreur
```

---

## 3. État des lieux global

### 3.1 Ce qui fonctionne déjà

- Authentification, rôles multi-rôles, OTP staff (Module 2).
- Gestion des établissements, clés API, multi-tenant (Module 1).
- Création patient, DPU, détection de doublons, fusion (Module 3).
- Visites, constantes vitales, consultations, PDF compte-rendu (Module 5 partiel).
- Allergies et antécédents structurés (Module 6 partiel).
- Prescriptions, vérification pharmacie, dispensation, stocks (Module 7 partiel).
- Demandes d’examens, portail labo, upload résultats FHIR (Module 8 partiel).
- Hospitalisations, notes journalières, sortie PDF (Module 10 partiel).
- Documents médicaux, vérification publique, révocation (Module 11 partiel).
- Consentements par établissement, demandes d’accès externe, expiration (Module 12 partiel).
- Audit logs, notifications, API Gateway, rate limiting, webhooks, FHIR (modules transversaux).

### 3.2 Écarts transverses majeurs

| Thème | Écart | Impact |
|---|---|---|
| **Architecture SOLID** | Plusieurs controllers injectent directement des repositories (PatientController, ConsultationController, PrescriptionController, LabOrderController, etc.) | Violation standard `ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md` |
| **God classes** | `PatientService` dépasse 570 lignes avec ~20 dépendances ; `consultation.component.ts` fait 799 lignes | Dépassement des limites de taille, SRP |
| **Entités exposées** | `PatientPortalController` retourne `NotificationEntity` ; `AuditService` expose `AuditLogEntity` | Fuite de la couche persistence |
| **Modèle de données** | Statuts stockés en `String` sans enum/contrainte CHECK ; `LabOrderEntity.exams` en CSV | Risque d’incohérence, non scalable |
| **Sécurité / 12-Factor** | Secrets par défaut dans `application.yml` (JWT, clé API labo) | Dette sécurité critique |
| **Gradle résiduel** | Dossier `backend/gradle/` et scripts Gradle présents alors que Maven est le standard | Dette technique |
| **Design system** | `rounded-2xl`/`rounded-3xl` utilisés, couleurs hardcodées, textes en dur | Violation `UI-RADIUS-AND-SHADOW-STANDARDS.md` et i18n |
| **i18n** | Dictionnaire inline de 1171 lignes dans `i18n.service.ts` | Dette maintenance |

---

## 4. Écarts par module

### Module 4 — Dossier patient partagé

| Exigence CDC | Écart | Priorité |
|---|---|---|
| FR-DPU-001 : plusieurs visites | ✅ OK | — |
| FR-DPU-002 : plusieurs documents | ✅ OK | — |
| FR-DPU-003 : synthèse médicale rapide | PDF synthèse utilise les champs texte libre `patient.allergies` / `medicalHistory`, pas les tables structurées ; ne remonte pas maladies chroniques, traitements, diagnostics, prescriptions, résultats critiques | P0 |
| FR-DPU-004 : masquage selon niveau d’accès | Contrôle binaire par scope ; pas de masquage au niveau des champs des DTO | P1 |
| FR-DPU-005 : historique des accès patient | ✅ OK via `/api/patient/audit-logs` | — |
| Sections du dossier | Pas d’endpoint agrégé `GET /api/patients/{id}/dpu` retournant les 20 sections | P1 |
| Maladies chroniques / traitements en cours | Pas d’entité dédiée ; seuls `isOngoing` sur antécédents et prescriptions actives | P1 |
| Contacts | Un seul contact d’urgence ; pas de liste de contacts multiples | P2 |

### Module 5 — Visites et consultations

| Exigence CDC | Écart | Priorité |
|---|---|---|
| `service` dans la visite | Champ absent ; `orientation` utilisé à la place | P0 |
| `main_practitioner_id` | Champ absent | P0 |
| `arrival_at` | Champ absent ; `created_at` utilisé | P0 |
| Diagnostic | Un seul champ `diagnosis` ; manquent `suspected_diagnosis`, `final_diagnosis`, `conclusion` | P0 |
| FR-VISIT-005 : correction traçable | Visite terminée bloquée en écriture, mais pas de mécanisme de trace de correction | P1 |
| Constantes : douleur 0-10 | Absente | P1 |
| FR-VISIT-003 : PDF consultation | ✅ OK | — |
| FR-VISIT-004 : unités cohérentes | ✅ OK | — |

### Module 6 — Allergies et antécédents

| Exigence CDC | Écart | Priorité |
|---|---|---|
| FR-HIST-001 : allergies actives dans synthèse | Non implémenté dans le PDF synthèse | P0 |
| FR-HIST-002 : antécédents importants | Pas de flag `important` ; pas de remontée prioritaire | P1 |
| FR-HIST-003 : journalisation | ✅ OK | — |
| FR-HIST-004 : pas de suppression sans traçabilité | Pas de suppression physique, mais pas de soft-delete traçable non plus | P1 |
| Catégories d’antécédents | Manquent `ALLERGIC` et `SOCIAL` | P1 |
| Duplication de données | `PatientEntity.allergies` / `medicalHistory` texte libre coexistent avec tables structurées sans synchronisation | P1 |

### Module 7 — Prescriptions et ordonnances

| Exigence CDC | Écart | Priorité |
|---|---|---|
| Champs médicament (`form`, `route`, `frequency`, `substitution_allowed`) | Absents | P0 |
| `issued_at` | Absent ; `created_at` utilisé | P1 |
| `document_id` / PDF ordonnance | Pas de lien document ; PDF généré au niveau visite | P0 |
| Statut `DRAFT` | Non implémenté | P1 |
| Annulation prescripteur | Statut `CANCELLED` existe mais aucun endpoint prescripteur | P0 |
| QR code dédié ordonnance | Pas de QR code / URL publique dédiée | P1 |
| AllôPharma API | `AlloPharmaClient` est une simulation (log JSON), pas d’appel HTTP réel | P1 |
| `substitution_allowed` | Toujours `true` en dur | P1 |

### Module 8 — Examens médicaux

| Exigence CDC | Écart | Priorité |
|---|---|---|
| Types d’examen contrôlés | `exam_type` est une chaîne libre ; pas d’enum | P1 |
| Statuts `AWAITING_PAYMENT`, `PAID` | Absents | P1 |
| `source_organization_id` | Pas de colonne dédiée | P1 |
| Droits labo | `BIOLOGISTE` peut changer n’importe quel statut sans vérifier `target_organization_id` | P0 |
| Liste `exams` | Stockée en CSV au lieu d’une table fille | P1 |
| FR-EXAM-001 à 005 | Partiellement OK ; cycle de vie incomplet | P1 |

### Module 9 — Résultats d’examens

| Exigence CDC | Écart | Priorité |
|---|---|---|
| `status` du résultat | Absent de `LabResultEntity` | P0 |
| `validator_user_id` | Remplacé par `validator_name` (String) | P0 |
| `conclusion` | Présent dans DTO mais non persisté | P0 |
| `document_id` | Remplacé par `pdf_file_path` ; pas de lien `medical_documents` | P0 |
| FR-RESULT-001 : immutabilité résultat validé | Non implémentée | P0 |
| FR-RESULT-004 : export structuré | Aucun endpoint d’export | P1 |
| FR-RESULT-005 : mapping FHIR export | Uniquement import FHIR | P1 |
| `result_number` | Généré par `count() + 1` (non sûr en concurrence) | P1 |

### Module 10 — Hospitalisation

| Exigence CDC | Écart | Priorité |
|---|---|---|
| Numéro de séjour | Absent | P0 |
| `visit_id` | Absent ; pas de lien visite/épisode de soins | P0 |
| Médecin responsable | Absent | P0 |
| Actes / examens / prescriptions internes | Absents du modèle | P1 |
| Document de sortie vérifiable | PDF généré mais stocké via `pdf_file_path`, pas dans `medical_documents` | P0 |
| Contrainte DB occupation lit | Uniquement applicatif ; race condition possible | P1 |

### Module 11 — Documents médicaux vérifiables

| Exigence CDC | Écart | Priorité |
|---|---|---|
| `document_type` | Absent de l’entité | P0 |
| `hash` | Non calculé, non stocké | P0 |
| `qr_code_url`, `verification_url` | QR généré dans PDF mais URLs non persistées | P1 |
| `version` + statut `REMPLACE` | Pas de versionnement ; relation `visit_id` en UNIQUE 1:1 | P0 |
| `author_user_id` | Non stocké | P1 |
| Types de documents (12 types) | Seul compte-rendu de consultation géré dans `medical_documents` ; sortie hospi et résultat labo stockés ailleurs | P0 |
| Mention légale vérification publique | Absente du DTO | P1 |
| FR-DOC-006 : téléchargement journalisé | ✅ OK | — |

### Module 12 — Consentement patient

| Exigence CDC | Écart | Priorité |
|---|---|---|
| Types de consentement | Seul le consentement par établissement binaire existe ; manquent ponctuel, temporaire, par professionnel, limité, urgence | P0 |
| `requester_user_id`, `requester_organization_id` | Absents | P1 |
| `reason` | Absent | P1 |
| `requested_at`, `approved_at`, `expires_at` | Absents ; pas de durée de consentement | P0 |
| Statuts demandé/accepté/refusé/expiré/révoqué | `ACTIVE/REVOKED/NONE` uniquement | P0 |
| `validation_channel` contrôlé | Valeur libre ; pas d’enum `OTP`/`APP`/`AGENT_HABILITE` | P1 |

### Module 13 — Demande d’accès externe (lié au 12)

| Exigence CDC | Écart | Priorité |
|---|---|---|
| `approved_at` | Absent | P1 |
| Statut `révoqué` | Absent côté patient | P0 |
| OTP d’approbation | Notification mock ; pas d’OTP dédié | P1 |
| Lien demande ↔ consentement | Une demande externe n’engendre pas de `PatientConsent` | P1 |

---

## 5. Écarts frontend

### Portail patient (CDC 13.1)

| Écran CDC | État | Priorité |
|---|---|---|
| Connexion | Composant isolé non routé | P1 |
| Tableau de bord | ✅ OK | — |
| Mon profil | ❌ Manquant | P1 |
| Ma synthèse médicale | ❌ Manquant | P0 |
| Mes visites | ⚠️ Partiel (composant réutilisé) | P2 |
| Mes documents | ❌ Manquant | P1 |
| Mes ordonnances | ⚠️ Partiel | P1 |
| Mes résultats | ❌ Manquant | P0 |
| Demandes d’accès | ✅ OK | — |
| Historique des accès | ✅ OK | — |
| QR code temporaire | ❌ Manquant | P1 |
| Paramètres confidentialité | ⚠️ Partiel | P1 |

### Portail professionnel (CDC 13.2)

| Écran CDC | État | Priorité |
|---|---|---|
| Documents générés | ❌ Pas d’écran dédié | P1 |
| Historique patient | ❌ Pas d’écran dédié | P1 |
| Consultation médicale | ⚠️ Existe mais 799 lignes, à refactorer | P0 |

### Portail labo (CDC 13.3)

| Écran CDC | État | Priorité |
|---|---|---|
| Tableau de bord labo | ❌ Manquant | P1 |
| Détail demande dédié | ⚠️ Panneau dans la même page | P1 |
| Validation résultat | ⚠️ Partiel | P1 |
| Envoi PDF | ⚠️ Upload présent, pas d’écran dédié | P1 |

### Portail pharmacie (CDC 13.4)

| Écran CDC | État | Priorité |
|---|---|---|
| Détail ordonnance dédié | ⚠️ Affiché après vérification | P1 |
| Liaison stocks ↔ vérification | ❌ Non liée | P1 |
| Déliverance partielle/totale explicite | ⚠️ Vocabulaire non explicite | P1 |

### Vérification publique (CDC 13.6)

| Écran CDC | État | Priorité |
|---|---|---|
| Page de saisie du numéro document | ❌ Manquante | P1 |
| Demande d’accès au dossier | ❌ Manquante | P1 |

### Violations design system

- `consultation.component.ts` : 799 lignes (> 500).
- `app-shell.component.ts` : 541 lignes (> 300).
- `lab-orders-page.component.ts` : 546 lignes, `patient-consultations-tab.component.ts` : 492, etc.
- `rounded-xl` / `rounded-2xl` / `rounded-3xl` dans plusieurs composants.
- Textes français codés en dur dans `login.component.html`, `patient-login.component.ts`, `patient-profile-tab.component.ts`, etc.
- `<title>Web</title>` dans `index.html`.
- Dictionnaire i18n inline de 1171 lignes.

---

## 6. Découpage en User Stories

Ce ticket génère l’**EPIC-0014** et les User Stories suivantes :

| ID | Module | Titre | Priorité | Est. Senior |
|---|---|---|---|---|
| STORY-1901 | 4 | Dossier patient partagé et synthèse médicale conforme CDC | P0 | 2.0j |
| STORY-1902 | 5 | Visites et consultations conformes CDC | P0 | 1.5j |
| STORY-1903 | 6 | Allergies et antécédents conformes CDC | P0 | 1.0j |
| STORY-1904 | 7 | Prescriptions et ordonnances conformes CDC | P0 | 1.5j |
| STORY-1905 | 8 | Examens médicaux conformes CDC | P0 | 1.0j |
| STORY-1906 | 9 | Résultats d’examens conformes CDC | P0 | 1.5j |
| STORY-1907 | 10 | Hospitalisations conformes CDC | P0 | 1.0j |
| STORY-1908 | 11 | Documents médicaux vérifiables conformes CDC | P0 | 1.5j |
| STORY-1909 | 12 | Consentements patient et accès externe conformes CDC | P0 | 1.5j |
| STORY-1910 | 13.1-13.6 | Portails patient, pro, labo, pharmacie et vérification publique conformes CDC | P0 | 2.0j |
| STORY-1911 | Transverse | Dette technique architecture / SOLID / design system | P1 | 2.0j |

---

## 7. Définition de prêt (DoR)

- [x] CDC modules 4 à 12 relus et approuvés.
- [x] État des lieux validé par le Tech Lead.
- [x] Capacity SPRINT-0011 disponible (charge totale estimée ~16j Senior).
- [x] Standards `DESIGN.md`, `DESIGN-SYSTEM-STANDARDS.md`, `UI-RADIUS-AND-SHADOW-STANDARDS.md`, `ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md` relus.
- [x] Documentation `docs/features/alignment-modules-4-12/FUNCTIONAL-SPEC.md` et `TECHNICAL-DESIGN.md` créée.

## 8. Définition de fini (DoD)

- [x] Chaque User Story a son propre ticket dans `docs/ai/tickets/`.
- [x] Chaque US a des critères d’acceptation explicites.
- [x] Les migrations DB sont additives (pas de breaking change sans ADR) — principe documenté.
- [ ] Les tests backend passent (`./mvnw test`) — à valider lors de l’implémentation.
- [ ] Les tests frontend passent (`npm run test`, `npm run build`) — à valider lors de l’implémentation.
- [x] La documentation fonctionnelle et technique est à jour.
- [x] `PROJECT-TRACKING.md` et `CHANGELOG.md` sont mis à jour.
- [ ] Aucune régression sur les modules 1, 2, 3 déjà livrés — à valider lors de l’implémentation.

---

## 9. Risques

| Risque | Impact | Mitigation |
|---|---|---|
| Taille du chantier (9 modules) | Élevé | Découpage strict par module, livraisons incrémentales |
| Régressions sur modules existants | Élevé | Tests automatisés obligatoires, migrations additives |
| Dette architecture (controllers → repositories) | Moyen | STORY-1911 dédiée, pas de contournement |
| Divergence design system | Moyen | Review bloquante sur arrondis, tokens, i18n |
| Contraintes de temps | Moyen | Priorisation P0 stricte, P1 en buffer |

---

## 10. Action plan

- [x] Lire le CDC modules 4 à 12.
- [x] Auditer le backend (entités, services, controllers, tests).
- [x] Auditer le frontend (portails, composants, design system).
- [x] Identifier les écarts et violations des standards.
- [x] Rédiger le ticket d’audit global (ce fichier).
- [x] Créer les tickets User Stories STORY-1901 à STORY-1911.
- [x] Créer `docs/features/alignment-modules-4-12/FUNCTIONAL-SPEC.md`.
- [x] Créer `docs/features/alignment-modules-4-12/TECHNICAL-DESIGN.md`.
- [x] Mettre à jour `docs/ai/PROJECT-TRACKING.md`.
- [x] Mettre à jour `docs/ai/CHANGELOG.md`.
- [ ] Soumettre à review Lead Developer.

---

## 11. Impact version / SemVer

- **Version actuelle** : 0.9.0
- **Bump proposé** : MINOR (0.10.0) car l’alignement introduit de nouvelles fonctionnalités rétrocompatibles pour la plupart.
- **Breaking changes potentiels** : certains champs DB, statuts et endpoints peuvent devenir des contraintes fortes ; à documenter dans les ADR si nécessaire.

---

## 12. Références

- `Cahier_des_charges_Joprelys_Connect_Complet.md` — modules 4 à 12.
- `AGENTS.md` — règles générales et standards imposés.
- `SKILL.md` — posture, documentation first, tests, sécurité.
- `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`
- `docs/standards/DESIGN-SYSTEM-STANDARDS.md`
- `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md`
- `docs/standards/DOCUMENTATION-FIRST.md`
- `docs/ai/PROJECT-TRACKING.md`
- `docs/ai/CHANGELOG.md`
