# STORY-0104 — Invitation & Gestion du Personnel (Full-stack)

## 1. Objectif

Cette user story consiste à implémenter la gestion d'équipe clinique par l'Administrateur de Clinique (`ADMIN_CLINIQUE`). Il doit pouvoir lister ses collaborateurs (médecins, infirmiers, agents d'accueil, pharmaciens), inviter de nouveaux membres en générant un mot de passe temporaire à usage unique, modifier leurs informations de base, et suspendre ou réactiver leurs comptes.

## 2. Critères d'acceptation

- [ ] L'administrateur de clinique connecté dispose d'une page de gestion de son équipe `/clinic/staff`.
- [ ] Il peut lister tous les collaborateurs rattachés à son établissement.
- [ ] Il peut créer (inviter) un nouveau collaborateur (Nom, Email unique, Rôle clinique).
- [ ] Le système génère automatiquement un mot de passe temporaire de format `Jop-XXXXXX` et l'affiche à l'écran dans une boîte de succès claire (avec option copier).
- [ ] Il peut modifier le nom et le rôle d'un membre existant.
- [ ] Il peut activer ou désactiver un collaborateur à l'aide d'un interrupteur (Toggle Status). Un collaborateur désactivé ne peut plus se connecter au système.
- [ ] **Sécurité & Isolation** : Un administrateur ne peut ni lister, ni modifier, ni désactiver un utilisateur d'une autre clinique.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0001 (Authentification & Habilitations) |
| User story parent | STORY-0104 |
| Sprint cible | SPRINT-0003 |
| Priorité business | P1 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Intermédiaire |
| Effort senior | 0.5j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | Aucune |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `UserAccountEntity.java` analysé

## 5. Hypothèses

- L'administrateur est connecté et son `organizationId` est récupéré de son principal d'authentification.
- Le mot de passe temporaire s'affiche directement sur l'écran d'administration pour cette clinique pilote, car aucun serveur SMTP n'est pour le moment requis.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Elévation de privilèges | Fort | Vérifier systématiquement que l'utilisateur modifié appartient au même `organizationId` que l'administrateur connecté, et rejeter les requêtes non autorisées avec un `403 Forbidden` ou `404 Not Found`. |
| Doublon d'emails | Moyen | Utiliser une validation d'unicité stricte au niveau du backend et renvoyer une erreur 400 explicite et i18nisée. |

## 7. Action plan

### Phase 1 : Backend Spring Boot
- [x] Créer les DTOs :
   - [x] `InviteStaffRequest.java` (record avec validations)
   - [x] `UpdateStaffRequest.java` (record avec validations)
   - [x] `StaffResponse.java` (record pour la liste et modification)
   - [x] `InviteStaffResponse.java` (record comprenant le mot de passe temporaire)
- [x] Créer le repository ou ajouter les méthodes dans `UserAccountRepository` pour chercher par `organizationId` et exclure l'id de l'admin.
- [x] Créer le contrôleur `StaffController.java` exposé sous `/api/staff` avec la sécurité `@PreAuthorize("hasRole('ADMIN_CLINIQUE')")`.
- [x] Écrire le service de gestion d'équipe `StaffService.java` gérant le CRUD, la génération du mot de passe temporaire et le hachage.
- [x] Écrire les tests d'intégration complets dans `StaffControllerTest.java`.
- [x] Exécuter `mvn -Dtest=StaffControllerTest test` dès que la résolution Maven est disponible.

### Phase 2 : Frontend Angular
- [x] Créer le modèle TypeScript `staff.models.ts` dans `web/src/app/clinic/staff/`.
- [x] Créer le service `staff-api.service.ts`.
- [x] Créer le composant standalone `staff-management.component.ts`.
- [x] Configurer la route `/clinic/staff` réservée au rôle `ADMIN_CLINIQUE` dans `app.routes.ts`.
- [x] Ajouter l'onglet "Personnel" ou "Équipe" dans le menu de navigation de `AppShellComponent` ou `DashboardComponent`.
- [x] Ajouter les traductions requises dans `I18nService`.
- [x] Ajouter les tests Angular de l'écran staff.
- [x] Relancer le build production après résolution du blocage Google Fonts externe.

## 8. Implémentation réalisée

### Backend Spring Boot

- Ajout de `StaffController` sous `/api/staff` :
  - `GET /api/staff`
  - `POST /api/staff`
  - `PUT /api/staff/{id}`
  - `POST /api/staff/{id}/toggle`
- Ajout de `StaffService` :
  - résolution de l'administrateur connecté depuis le principal JWT ;
  - contrôle obligatoire de `organizationId` ;
  - validation des rôles cliniques gérables ;
  - génération de mot de passe temporaire `Jop-XXXXXX` ;
  - hash BCrypt via `PasswordEncoder` ;
  - blocage de l'auto-gestion de l'administrateur.
- Ajout de `StaffResponse`.
- Découplage de `InviteStaffResponse` de l'entité JPA.
- Extension de `UserAccountRepository` pour les recherches tenant-aware.
- Ajout de `StaffControllerTest`.

### Frontend Angular

- Ajout de la route lazy-load `/clinic/staff`, protégée par `roleGuard` et réservée à `ADMIN_CLINIQUE`.
- Ajout de `StaffApiService` avec appels relatifs `/api/staff`.
- Ajout de `StaffManagementComponent` :
  - liste responsive du personnel ;
  - invitation d'un collaborateur ;
  - affichage et copie du mot de passe temporaire ;
  - modification du nom et du rôle ;
  - activation/désactivation ;
  - états loading, empty et error.
- Extraction de `StaffTableComponent` pour la table desktop et les cartes mobiles du personnel.
- Ajout d'une carte d'accès "Équipe clinique" sur le dashboard pour `ADMIN_CLINIQUE`.
- Ajout des traductions FR/EN dans `I18nService`.

## 9. Suivi d'exécution

- 2026-07-02 : reprise de la story après interruption de quota.
- 2026-07-02 : backend implémenté et tests d'intégration ajoutés.
- 2026-07-02 : validation Maven exécutée avec succès (7/7 tests verts pour `StaffControllerTest`).
- 2026-07-02 : frontend Angular implémenté et tests unitaires au vert.

## 10. Tests et vérifications

### Tests ajoutés

- `StaffControllerTest`

Cas couverts :

- liste du personnel limitée à la clinique de l'administrateur ;
- invitation avec retour du mot de passe temporaire ;
- rejet d'un email déjà utilisé ;
- rejet d'un rôle non autorisé par RBAC ;
- rejet cross-tenant sur modification ;
- modification nom/rôle ;
- désactivation avec blocage de connexion future.

### Exécution

- `mvn -Dtest=StaffControllerTest test` : succès (7/7 tests verts).

### Frontend Angular

- `npx ng test --no-watch` : succès (27 tests verts).
- `npx ng build` : succès.

## 11. Documentation
- [x] Spécification fonctionnelle créée : `docs/features/personnel/FUNCTIONAL-SPEC.md`
- [x] Spécification technique créée : `docs/features/personnel/TECHNICAL-DESIGN.md`
- [x] Contrat API créé : `docs/features/personnel/API-CONTRACT.md`
- [x] Modèle de données documenté : `docs/features/personnel/DATA-MODEL.md`
- [x] Plan de test créé : `docs/features/personnel/TEST-PLAN.md`
- [x] Guide utilisateur créé : `docs/features/personnel/USER-GUIDE.md`

## 12. Reste à faire
- Aucun.

## 13. Statut final
Statut : DONE
