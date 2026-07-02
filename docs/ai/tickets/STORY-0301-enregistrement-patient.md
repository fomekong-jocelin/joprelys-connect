# STORY-0301 — Enregistrement Patient & Génération du DPU

## 1. Objectif

Cette user story consiste à concevoir et à implémenter l'enregistrement des patients dans Joprelys Connect avec génération automatique du Dossier Patient Unique (DPU) :
1. **Modèle de données** : Création de la table `patients` liée à une organisation (`organization_id`).
2. **Génération d'identifiants** : Génération automatique du DPU (`DPU-JOP-YYYYMMDD-XXXXXX`) et du numéro patient local (`PAT-YYYYMMDD-XXXXXX`).
3. **Multi-tenant** : Isolation stricte des données patient par organisation (tenant) grâce au tenant ID extrait du jeton JWT.
4. **API REST** : Endpoints de création, liste, détails et mise à jour des patients.
5. **Interface Angular** : Formulaire de création de patient (mobile-first), liste et profil de base.

## 2. Critères d'acceptation

- [x] L'agent d'accueil ou le clinicien connecté peut créer un patient en saisissant les informations requises (Nom complet, Sexe, Date de naissance, Téléphone, Ville/Quartier).
- [x] À la création, le système génère un numéro DPU unique au format `DPU-JOP-YYYYMMDD-XXXXXX` et un numéro local au format `PAT-YYYYMMDD-XXXXXX`.
- [x] Deux patients ne peuvent pas avoir le même DPU.
- [x] Les données des patients sont isolées par clinique pilote. Un utilisateur connecté ne peut voir ou modifier que les patients de sa clinique.
- [x] La recherche fonctionne par nom complet, téléphone ou DPU.
- [x] Les interfaces sont disponibles en français et anglais.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0003 (Dossier Patient Unique (DPU) & Recherche) |
| User story parent | STORY-0301 |
| Sprint cible | SPRINT-0002 |
| Priorité business | P0 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Intermédiaire |
| Effort senior | 1j |
| Effort intermédiaire | 1.3j |
| Effort junior | 2.2j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Fort (risque de fuite de données de santé) |
| Risque technique | Moyen |
| Dépendances | STORY-0201 |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu si nécessaire
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé
- [x] Tests existants analysés
- [x] Contrats API analysés
- [x] Impacts sécurité analysés
- [x] Impacts données analysés
- [x] Impacts Angular analysés
- [x] Backend Maven uniquement vérifié
- [x] Backend `application.yml` / profils YAML vérifiés
- [x] Frontend Tailwind CSS vérifié
- [x] Absence Angular Material vérifiée
- [x] Angular `proxy.conf.json` présent et référencé dans `angular.json`
- [x] Aucun appel API Angular avec URL backend hardcodée

## 5. Hypothèses

- Le format de la date de naissance est `yyyy-MM-dd`.
- Le numéro DPU est généré en concaténant le préfixe, la date courante et un compteur incrémenté quotidiennement.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Fuite de données de santé inter-tenant | Très Fort | Tests d'isolation inter-tenant stricts et filtrage automatique Hibernate. |
| Doublon de DPU sous forte concurrence | Moyen | Contrainte d'unicité en DB et gestion propre des exceptions de violation de contrainte. |

## 7. Action plan

- [x] **Specs & Documentation** : Rédiger la spécification fonctionnelle et technique dans `docs/features/patient/`.
- [x] **DB/Migration** (TASK-0301-01) : Créer la table `patients` via Flyway.
- [x] **Backend/Security** (TASK-0301-02) : Mettre en œuvre le context thread-local tenant et injecter l'organizationId du token.
- [x] **Backend/MultiTenancy** (TASK-0301-03) : Configurer le resolveur de tenant Hibernate.
- [x] **Backend/DPU** (TASK-0301-04) : Implémenter le générateur de numéros DPU et patient local.
- [x] **Backend/API** (TASK-0301-05) : Créer l'entité, le repository, le service et les controllers de Patient.
- [x] **Frontend/API** (TASK-0301-06) : Créer le modèle patient et le service `PatientApiService`.
- [x] **Frontend/UI** (TASK-0301-07) : Créer les composants Angular de liste, création et profil de patient (mobile-first, thème, i18n).
- [x] **Tests/QA** (TASK-0301-08) : Écrire les tests unitaires, d'intégration, d'isolation et frontend.
- [x] Ajouter ou modifier les tests
- [x] Exécuter les vérifications
- [x] Mettre à jour la documentation
- [x] Mettre à jour `CHANGELOG.md`
- [x] Mettre à jour `PROJECT-TRACKING.md`

## 8. Implémentation réalisée

- **DB/Migration** : Migration Flyway `V3__create_patients_table.sql` créée pour définir le schéma de la table `patients` avec clés étrangères et indexes de recherche.
- **Backend/Security** : Ajouté `TenantContext` de type thread-local et mis à jour `JwtAuthenticationFilter` pour injecter et nettoyer le tenant ID à chaque requête.
- **Backend/MultiTenancy** : Écrit `TenantIdentifierResolver` implémentant `CurrentTenantIdentifierResolver` pour propager l'ID à Hibernate 6 de façon transparente.
- **Backend/DPU** : Service `PatientNumberGenerator` implémenté utilisant `JdbcTemplate` pour compter les patients créés le jour même au niveau global (hors isolation) afin de générer séquentiellement le DPU et l'ID local sans doublon.
- **Backend/API** : Créé les classes `PatientEntity`, `PatientRepository`, `PatientService` et `PatientController` avec les endpoints REST sous `/api/patients`.
- **Backend/API/Errors** : Mis à jour `AuthExceptionHandler` pour extraire et retourner la liste concaténée des messages d'erreurs de validation spécifiques (`FieldError::getDefaultMessage`) dans le champ `detail` de la réponse `ProblemDetail`.
- **Frontend/API** : Écrit le modèle de données `Patient` et le service Angular `PatientApiService` effectuant les appels vers l'API.
- **Frontend/UI** : Implémenté `PatientListComponent`, `PatientFormComponent` et `PatientDetailComponent` intégrant la charte graphique Montserrat/Inter, les contrastes WCAG, la structure i18n FR/EN et une mise en page mobile-first (affichage en cartes sur mobile et en tableau sur desktop).
- **Frontend/UI Theme & Inputs** : Refactorisé la classe globale `.ui-input` pour s'aligner sur l'esthétique du formulaire de connexion (coins arrondis de 12px `rounded-xl`, variables de couleurs d'arrière-plan du thème). Créé les classes `.ui-select` et `.ui-textarea` et implémenté le style des options déroulantes pour s'adapter automatiquement aux modes light et dark globaux de l'application sans utiliser de styles inline.
- **Frontend/UI/Errors** : Mis à jour les formulaires de création de clinique et de patient pour extraire et afficher le message d'erreur d'API détaillé `err.error.detail` à la place des messages d'erreurs génériques.
- **Frontend/Routing & Dashboard** : Ajouté la route `/patients` dans `app.routes.ts` et lié l'icône de gestion du dossier clinique depuis le dashboard.
- **Tests/QA** : Écrit et validé `PatientControllerTest` (5 cas d'intégration dont la création, l'isolation inter-tenant étanche, la génération séquentielle et la restriction de l'admin) et `patient-list.component.spec.ts` pour le composant Angular.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-02 | Antigravity | 0.05j | 5% | Rédaction des spécifications et développement | Aucun | Lancement du ticket |
| 2026-07-02 | Antigravity | 0.65j | 100% | Aucun | Aucun | Développement, tests et build de production validés. |

## 10. Tests et vérifications

- Backend : `./mvnw test` -> **BUILD SUCCESS** (23 tests passed, including 5 multi-tenant patient tests)
- Frontend : `npm test` -> **13 tests passed** (100% OK, including patient specs)
- Build Frontend : `npm run build` -> **Application bundle generation complete** (5.3s, including patient components)

## 11. Documentation

- [x] Spécification fonctionnelle créée : `docs/features/patient/FUNCTIONAL-SPEC.md`
- [x] Spécification technique créée : `docs/features/patient/TECHNICAL-DESIGN.md`

## 12. Reste à faire

- [x] Aucun

## 13. Statut final

Statut : REVIEW

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout d'une nouvelle fonctionnalité majeure (dossier patient & DPU) sans rupture de rétrocompatibilité. |
| Breaking change | Non |
| Migration DB | Oui |
| Changement API | Oui |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Oui |

## 15. Impact thème / i18n / branding

- [x] Impact Angular UI analysé
- [x] Thème centralisé vérifié (light/dark supporté)
- [x] Textes `fr` / `en` prévus
- [x] Composants/widgets réutilisables prévus
- [x] Aucun texte ou branding hardcodé prévu
