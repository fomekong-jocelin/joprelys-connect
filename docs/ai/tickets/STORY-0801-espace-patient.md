# STORY-0801 — Espace patient sécurisé et historique personnel

## 1. Objectif

Permettre aux patients de se connecter de manière autonome et sécurisée à leur espace personnel (portail patient) en utilisant leurs informations d'enregistrement (N° DPU, Téléphone, Date de naissance) afin de consulter leur profil et l'historique chronologique de leurs visites de santé.

## 2. Critères d'acceptation

- [ ] L'interface de connexion patient est disponible sur la route publique `/patient/login`.
- [ ] La connexion s'effectue en deux étapes :
  1. Saisie du **N° DPU**, du **Téléphone** et de la **Date de naissance**.
  2. Saisie du **Code de sécurité (OTP)** à 6 chiffres (imprimé en console backend pour les tests).
- [ ] Le code OTP généré expire après 5 minutes, et un maximum de 3 essais erronés est toléré avant rejet de la session.
- [ ] Une fois authentifié, le patient est redirigé vers `/patient/dashboard` avec le rôle `PATIENT` dans le token.
- [ ] Le tableau de bord du patient affiche :
  - Ses informations d'identité (Nom complet, Sexe, Date de naissance, Téléphone, Adresse).
  - Ses informations médicales statiques (Allergies connues, Antécédents médicaux).
  - La liste chronologique de ses visites médicales clôturées avec date, nom de l'établissement et nom du médecin.
- [ ] Un patient ne peut en aucun cas accéder aux données d'un autre patient (contrôle strict du sujet dans le JWT).
- [ ] Les composants de l'espace patient respectent les styles du design system central (coins carrés/arrondis sobres de 4px à 6px, ombres légères, pas de Tailwind v3 ni d'Angular Material).

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0008 (Portail Patient & Consentement) |
| User story parent | STORY-0801 |
| Sprint cible | SPRINT-0003 (en cours) |
| Priorité business | P1 |
| Complexité | M |
| Story points | 5 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 1.2j |
| Effort estimé intermédiaire | 1.8j |
| Effort estimé junior | 3j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | EPIC-0001 (Auth), EPIC-0003 (Patient DPU) |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu
- [x] `DESIGN.md` (Design System centralisé créé et lu)
- [x] `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md` lu
- [x] `docs/standards/DESIGN-SYSTEM-STANDARDS.md` lu
- [x] `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md` lu
- [x] `docs/standards/DOCUMENTATION-FIRST.md` lu

## 5. Hypothèses

- Le code OTP généré localement pour les tests sera affiché dans la console du backend Spring Boot.
- Le patient n'ayant pas de compte utilisateur dans la table `users`, l'authentification utilisera un jeton JWT spécifique généré à partir de son entité `PatientEntity`.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Fuite de données personnelles de santé (PII) | Fort | Isolation logique stricte côté backend. Le jeton JWT du patient contiendra son identifiant unique de patient, vérifié sur chaque appel API. |
| Bruteforce de l'OTP | Moyen | Limitation à 3 tentatives de validation par code généré. |

## 7. Action plan

### Phase 1 : Backend Spring Boot
- [ ] Mettre à jour `JwtService` pour prendre en compte le rôle `PATIENT` et le numéro de DPU comme subject.
- [ ] Mettre à jour `SecurityConfig` pour autoriser publiquement `/api/public/patient/auth/**`.
- [ ] Créer `PatientAuthService` gérant la génération d'OTP, l'invalidation après 3 essais ou 5 minutes, et la signature du token JWT.
- [ ] Créer `PatientAuthController` exposant les endpoints `/api/public/patient/auth/otp` et `/api/public/patient/auth/verify`.
- [ ] Créer `PatientPortalController` (sécurisé, réservé à `ROLE_PATIENT`) exposant `/api/patient/me` (renvoyant le profil et la liste de ses visites clôturées).
- [ ] Écrire les tests d'intégration dans `PatientAuthControllerTest` et `PatientPortalControllerTest`.

### Phase 2 : Frontend Angular
- [ ] Créer `PatientPortalService` pour gérer les appels d'authentification et de chargement des données.
- [ ] Créer les routes et composants :
  - `PatientLoginComponent` (saisie DPU / Téléphone / Date de naissance, puis OTP).
  - `PatientDashboardComponent` (affichage profil et historique).
- [ ] Ajouter les libellés de traduction français / anglais dans `I18nService`.
- [ ] Écrire les tests unitaires frontend.

## 8. Implémentation réalisée
*(À compléter à la fin du développement)*

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-02 | Antigravity | 0.1j | 10% | Phase 1 & Phase 2 | Aucun | Cadrage initial et ticket créé |

## 10. Tests et vérifications
*(À compléter à la fin)*

## 11. Documentation
- [x] Documentation fonctionnelle initiale créée : `docs/features/patient-portal/FUNCTIONAL-SPEC.md`
- [x] Documentation technique initiale créée : `docs/features/patient-portal/TECHNICAL-DESIGN.md`

## 12. Reste à faire
- [ ] Implémenter la Phase 1 (Backend Spring Boot)
- [ ] Implémenter la Phase 2 (Frontend Angular)
- [ ] Exécuter les tests unitaires et d'intégration

## 13. Statut final
Statut : **TODO**

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout du portail patient et de son mécanisme d'authentification |
| Breaking change | Non |

## 15. Impact thème / i18n / branding
- [ ] Textes `fr` / `en` intégrés dans `I18nService`.
- [ ] Arrondis et ombres conformes à `DESIGN.md` (coins de 4px à 8px maximum).
