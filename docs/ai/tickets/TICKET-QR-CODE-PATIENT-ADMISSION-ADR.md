# TICKET-QR-CODE-PATIENT-ADMISSION-ADR — Cadrage et ADR pour l'enregistrement patient par QR Code

> Fichier obligatoire pour chaque ticket ou intervention IA.

## 1. Objectif

Cette tâche consiste à réaliser l'analyse d'opportunité, le cadrage fonctionnel, le technical design initial et l'Architectural Decision Record (ADR-0003) pour la fonctionnalité d'enregistrement de patient en autonomie (Self-Registration) via QR Code sur le comptoir d'admission de l'hôpital.

## 2. Critères d'acceptation du cadrage

- [x] Réaliser l'analyse d'expert en systèmes d'information médicale (bénéfices, limites, risques).
- [x] Rédiger le document `ADR-0003-qr-code-self-registration.md` détaillant la décision d'architecture (Staging Area, sécurité PII, politique anti-doublon).
- [x] Rédiger la documentation fonctionnelle initiale `docs/features/patient-self-registration/FUNCTIONAL-SPEC.md`.
- [x] Rédiger la documentation technique initiale `docs/features/patient-self-registration/TECHNICAL-DESIGN.md` décrivant les entités (`PatientPreRegistrationEntity`), le cycle de vie de validation, l'API REST publique, et la réconciliation.
- [x] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0003 — Dossier Patient Unique (DPU) & Recherche |
| User story parent | STORY-0301 — Enregistrement Patient & Génération du DPU (Extension) |
| Sprint cible | SPRINT-0011 |
| Priorité business | P1 |
| Complexité | S (Pour la phase de cadrage) / L (Pour l'implémentation future) |
| Story points | 2 |
| Profil recommandé | Tech Lead / Architecte |
| Effort estimé senior | 0.2j |
| Effort estimé intermédiaire | 0.3j |
| Effort estimé junior | 0.5j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Moyen |
| Risque technique | Fort (Sécurité PII, Spam, Réconciliation) |
| Dépendances | STORY-0301, TICKET-1302 (Service de doublons) |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Service de détection de doublons (`PatientSimilarityService`) existant analysé
- [x] Formulaire d'admission patient Angular existant analysé
- [x] Backend Spring Boot Maven et profils YAML vérifiés

## 5. Hypothèses

1. Le formulaire de pré-enregistrement sera accessible publiquement sans authentification (via un QR Code pointant sur une URL publique du type `https://joprelys.com/public/register?orgId=X`).
2. Aucun historique médical ou donnée existante ne sera exposé au patient sur ce formulaire public pour des raisons de sécurité strictes.
3. Les données seront temporairement persistées dans une table tampon `patient_pre_registrations` avant d'être validées par l'agent d'accueil.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Fuite de données personnelles (PII) | Fort | Interdire la divulgation d'informations de patients existants sur le formulaire public. Seul l'accueil authentifié gère la réconciliation. |
| Spam et déni de service sur l'API publique | Moyen | Implémentation d'un Rate Limiting strict sur l'endpoint public et d'un captcha ou code de validation. |
| Pollution de la base de données par des pré-enregistrements fantômes | Moyen | Auto-nettoyage automatique des pré-enregistrements non validés après 24 heures par un job planifié. |
| Augmentation des doublons | Moyen | Utilisation systématique du `PatientSimilarityService` existant pour alerter l'agent d'accueil si des informations saisies ressemblent à un patient existant. |

## 7. Action plan

- [x] Analyser le workflow proposé et formuler l'avis expert.
- [x] Créer l'ADR `docs/ai/adr/ADR-0003-qr-code-self-registration.md`.
- [x] Créer la spécification fonctionnelle `docs/features/patient-self-registration/FUNCTIONAL-SPEC.md`.
- [x] Créer la conception technique `docs/features/patient-self-registration/TECHNICAL-DESIGN.md`.
- [x] Mettre à jour `docs/ai/PROJECT-TRACKING.md` pour référencer cette phase de cadrage.
- [x] Mettre à jour `docs/ai/CHANGELOG.md` pour acter le cadrage de la feature.

## 8. Implémentation réalisée

- Phase de cadrage et architecture complétée : création de l'ADR-0003, de la spécification fonctionnelle et technique.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Commentaire |
|---|---|---:|---:|---:|---|
| 2026-07-07 | Antigravity | 0.15j | 100% | Néant (Cadrage fini) | Documents rédigés pour validation client |

## 10. Tests et vérifications

*Non applicable pour cette phase d'architecture/cadrage (aucun code source de production modifié).*

## 11. Documentation

- [x] ADR créé : [ADR-0003-qr-code-self-registration.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/adr/ADR-0003-qr-code-self-registration.md)
- [x] Spécification fonctionnelle créée : [FUNCTIONAL-SPEC.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/features/patient-self-registration/FUNCTIONAL-SPEC.md)
- [x] Spécification technique créée : [TECHNICAL-DESIGN.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/features/patient-self-registration/TECHNICAL-DESIGN.md)
- [x] Changelog mis à jour
- [x] Suivi projet mis à jour

## 12. Reste à faire

- [ ] Valider le cadrage fonctionnel et technique avec le client.
- [ ] Créer les tickets d'implémentation (User Stories / Tasks) suite à l'acceptation de l'ADR.

## 13. Statut final

Statut : **DONE** (pour la phase de cadrage et d'architecture).

## 14. Notes finales

Le cadrage met en évidence la nécessité absolue de séparer la zone de pré-enregistrement publique (Staging Area) de la base patient officielle, et d'interdire l'affichage d'informations de patients existants sur l'écran public du patient pour se conformer au RGPD/HIPAA.

## 15. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Non (Phase de spécifications) |
| Type de bump | Aucun |
| Justification | Spécifications et ADR uniquement |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Non |
| Impact Flutter | Non |
| Changelog requis | Oui (section Added / Unreleased pour documenter les specs) |
| Release note requise | Non |
