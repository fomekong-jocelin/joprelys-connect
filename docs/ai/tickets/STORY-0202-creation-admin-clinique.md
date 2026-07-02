# STORY-0202 — Création de l'Administrateur Clinique par l'Admin Joprelys

## 1. Objectif

Permettre à l'`ADMIN_JOPRELYS` d'affecter un premier compte `ADMIN_CLINIQUE` à une organisation (clinique) depuis l'écran `/organizations`.
C'est le **chaînon manquant** entre la création d'une clinique (STORY-0201) et la gestion de son personnel (STORY-0104) : sans cette story, aucun admin clinique ne peut exister.

## 2. Critères d'acceptation

- [ ] L'`ADMIN_JOPRELYS` peut, depuis l'écran `/organizations`, ouvrir un formulaire d'affectation d'admin pour une clinique donnée.
- [ ] Le formulaire demande : Nom complet, Adresse e-mail.
- [ ] Le backend crée le compte `ADMIN_CLINIQUE` rattaché à l'`organizationId` de la clinique concernée avec un mot de passe temporaire `Jop-XXXXXX`.
- [ ] Le mot de passe temporaire est affiché à l'écran dans une boîte de succès (avec option de copie).
- [ ] Un email déjà utilisé retourne une erreur 409 explicite.
- [ ] **Sécurité** : seul `ADMIN_JOPRELYS` peut appeler cet endpoint.
- [ ] **Sécurité** : l'organisation doit exister sinon 404.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0002 (Gestion de la Clinique Pilote) |
| Sprint cible | SPRINT-0003 |
| Priorité business | P1 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Intermédiaire |
| Effort senior | 0.3j |
| Effort intermédiaire | 0.5j |
| Effort junior | 0.8j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Moyen (création de compte privilegié) |
| Risque technique | Faible |
| Dépendances | STORY-0201 (organisation doit exister), STORY-0104 (pattern identique) |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `OrganizationController.java` analysé
- [x] `StaffService.java` analysé (pattern de création réutilisé)
- [x] `UserAccountEntity.java` analysé
- [x] `organization-list.component.ts` analysé
- [x] `organization-table.component.ts` analysé
- [x] `i18n.service.ts` analysé

## 5. Hypothèses

- Le mot de passe temporaire suit le même pattern que le personnel : `Jop-XXXXXX`.
- Aucun envoi d'email SMTP : le mot de passe s'affiche à l'écran.
- Une clinique peut avoir plusieurs admins (pas de contrainte unique côté DB), mais fonctionnellement l'admin Joprelys en crée un premier.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Création de compte avec rôle élevé (`ADMIN_CLINIQUE`) | Fort | Endpoint strict `ADMIN_JOPRELYS` uniquement, validation `organizationId` existant |
| Email déjà utilisé | Moyen | Validation `existsByEmail` + erreur 409 |

## 7. Action plan

### Phase 1 : Backend Spring Boot
- [x] Créer `CreateClinicAdminRequest.java` (record : displayName, email)
- [x] Créer `CreateClinicAdminResponse.java` (record : id, email, displayName, role, temporaryPassword, createdAt)
- [x] Ajouter `POST /api/organizations/{id}/admin` dans `OrganizationController` sécurisé `ADMIN_JOPRELYS`
- [x] Extraire logique de génération de mot de passe dans `OrganizationController` (réutilise le pattern de StaffService)
- [x] Tests d'intégration `OrganizationAdminControllerTest` (7 cas couverts)

### Phase 2 : Frontend Angular
- [x] Ajouter `createClinicAdmin()` dans `OrganizationApiService`
- [x] Ajouter `CreateClinicAdminRequest` et `CreateClinicAdminResponse` dans `organizations.models.ts`
- [x] Ajouter le bouton "Affecter un admin" dans `OrganizationTableComponent` (desktop + mobile)
- [x] Ajouter le formulaire et la modale de succès dans `OrganizationListComponent`
- [x] Ajouter les traductions FR/EN dans `I18nService`

## 8. Implémentation réalisée

### Backend
- `CreateClinicAdminRequest.java` : record avec validations `@NotBlank`, `@Email`, `@Size`.
- `CreateClinicAdminResponse.java` : record incluant le `temporaryPassword` en clair (affichage unique).
- `OrganizationController.java` : endpoint `POST /api/organizations/{id}/admin` — vérifie l'existence de l'org, unicité email, crée le compte `ADMIN_CLINIQUE` avec `organizationId` rattaché et mot de passe `Jop-XXXXXX` haché BCrypt.
- `OrganizationAdminControllerTest.java` : 7 cas de test (nominal, 403, 401, 404, 409 doublon, 400 email invalide, 400 displayName vide).

### Frontend
- `organizations.models.ts` : ajout de `CreateClinicAdminRequest` et `CreateClinicAdminResponse`.
- `organization-api.service.ts` : ajout de `createClinicAdmin(orgId, request)`.
- `organization-table.component.ts` : ajout du bouton "Affecter un admin" (desktop et mobile), émission de `adminRequested`.
- `organization-list.component.ts` : gestion complète du formulaire admin (signaux, soumission, copie du mot de passe).
- `organization-list.component.html` : modale glassmorphism avec formulaire + affichage du mot de passe temporaire + copie.
- `i18n.service.ts` : 15 nouvelles clés FR/EN.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-02 | Antigravity | 0.05j | 10% | Cadrage et ticket | Aucun | Story créée, implémentation en cours |
| 2026-07-02 | Antigravity | 0.45j | 100% | Aucun | Aucun | Backend et frontend implémentés, tests unitaires Angular validés |
| 2026-07-02 | Antigravity | 0.1j  | 100% | Aucun | Aucun | Tests d'intégration Java validés et changements validés (Build SUCCESS) |

## 10. Tests et vérifications

### Commandes exécutées

```bash
# Backend unit and integration tests
mvn clean verify
# Front unit tests
npx ng test --no-watch
```

### Résultats
- [x] Les 7 cas de test dans `OrganizationAdminControllerTest.java` passent au vert.
- [x] Tous les tests frontend et de compilation Angular passent avec succès.

## 11. Documentation

- [x] Mettre à jour `docs/features/clinique/FUNCTIONAL-SPEC.md`
- [x] Mettre à jour `docs/features/clinique/TECHNICAL-DESIGN.md`

## 12. Reste à faire

- Aucun.

## 13. Statut final

Statut : DONE
