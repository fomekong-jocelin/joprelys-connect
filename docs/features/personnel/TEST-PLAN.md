# Plan de Test — Gestion du Personnel Clinique (STORY-0104)

## 1. Tests backend automatisés

Classe cible :

```text
backend/src/test/java/com/joprelys/backend/auth/api/StaffControllerTest.java
```

Cas couverts :
- `givenAdmin_whenListStaff_thenReturnsOnlyClinicStaff` : liste limitée au tenant de l'administrateur et exclusion du compte admin.
- `givenAdmin_whenInviteNewStaff_thenReturnsTemporaryPasswordAndStoresHash` : création, normalisation email/rôle, mot de passe temporaire `Jop-XXXXXX`, hash en base.
- `givenAdmin_whenInviteExistingEmail_thenReturnsBadRequest` : unicité globale de l'email.
- `givenMedecin_whenListStaff_thenReturnsForbidden` : RBAC sur rôle non autorisé.
- `givenAdminA_whenModifyStaffOfClinicB_thenReturnsNotFound` : isolation cross-tenant.
- `givenAdmin_whenUpdateStaff_thenNameAndRoleAreChanged` : modification nom/rôle.
- `givenAdmin_whenToggleStaffStatus_thenLoginIsBlocked` : désactivation et blocage des connexions futures.

## 2. Commandes de vérification attendues

```powershell
cd backend
mvn -Dtest=StaffControllerTest test
mvn test
```

## 3. État de vérification au 2026-07-02

Les tests ont été ajoutés mais n'ont pas pu être exécutés dans l'environnement courant :

- `backend/mvnw.cmd -Dtest=StaffControllerTest test` échoue avant Maven avec `Cannot index into a null array`.
- `mvn -Dtest=StaffControllerTest test` échoue car le parent `spring-boot-starter-parent:4.1.0` doit être résolu depuis Maven Central et l'accès réseau est bloqué.
- `mvn -o -Dtest=StaffControllerTest test` confirme que l'artefact parent `4.1.0` n'est pas disponible en cache Maven utilisable.

## 4. Tests restants

- Relancer les tests backend dès que la résolution Maven est disponible.
- Relancer le build production Angular dans un environnement autorisant l'accès à Google Fonts ou avec une stratégie de polices locales.

## 5. Tests frontend automatisés

Classe cible :

```text
web/src/app/clinic/staff/staff-management.component.spec.ts
```

Cas couverts :

- chargement de la liste du personnel au démarrage ;
- invitation d'un collaborateur et affichage du mot de passe temporaire ;
- modification d'un collaborateur sélectionné ;
- activation/désactivation d'un collaborateur.

Commandes exécutées le 2026-07-02 :

```powershell
cd web
npm run test -- --watch=false
npm run build -- --configuration development
```

Résultats :

- `npm run test -- --watch=false` : succès, 8 fichiers et 21 tests passés.
- `npm run build -- --configuration development` : succès.
- `npm run lint` : impossible, script `lint` absent de `package.json`.
- `npm run build` : échec environnemental sur l'inlining Google Fonts (`connect EACCES`), après compilation Angular.
