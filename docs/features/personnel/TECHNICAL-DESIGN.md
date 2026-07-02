# Conception Technique — Invitation & Gestion du Personnel (STORY-0104)

## 1. Architecture Globale

Cette fonctionnalité repose sur la manipulation directe des entités `UserAccountEntity` (table `users`) par l'intermédiaire d'un nouveau contrôleur Spring Boot et d'une nouvelle page d'administration locale côté Angular.

```
+-------------------------------------------------------------------+
|                           Spring Boot API                         |
|                                                                   |
|  [StaffController]                                                |
|    - GET  /api/staff         -> Liste le personnel du tenant      |
|    - POST /api/staff         -> Crée un membre (génère mdp temp)  |
|    - PUT  /api/staff/{id}    -> Modifie les infos d'un membre     |
|    - POST /api/staff/{id}/toggle -> Active/Désactive un compte    |
|                                                                   |
+-------------------------------------------------------------------+
                               |
                               v
                  Database (table `users`)
```

## 2. API Endpoints

### 2.1 Liste du personnel
* **Méthode / URL** : `GET /api/staff`
* **Habilitation** : Uniquement `ADMIN_CLINIQUE`.
* **Description** : Renvoie la liste des utilisateurs ayant la même `organizationId` que l'administrateur connecté. Exclut l'administrateur connecté de la liste (pour éviter de se suspendre lui-même).
* **Réponse (200 OK)** :
```json
[
  {
    "id": "7c76cda7-7a10-46e8-bb4c-9ed454fa08fc",
    "email": "medecin@joprelys.local",
    "displayName": "Dr. Jocelin FOMEKONG",
    "role": "MEDECIN",
    "enabled": true,
    "createdAt": "2026-07-01T12:00:00Z"
  }
]
```

### 2.2 Invitation (Création) d'un nouveau membre
* **Méthode / URL** : `POST /api/staff`
* **Habilitation** : Uniquement `ADMIN_CLINIQUE`.
* **Request Body** :
```json
{
  "email": "nouveau.medecin@joprelys.local",
  "displayName": "Dr. Marc LENOIR",
  "role": "MEDECIN"
}
```
* **Validation** :
  * `email` : Valide, obligatoire, et unique en base globale (sinon `400 Bad Request`).
  * `displayName` : Obligatoire, min 3 chars.
  * `role` : Doit être l'un des rôles cliniques (`MEDECIN`, `INFIRMIER`, `AGENT_ACCUEIL`, `PHARMACIEN`).
* **Comportement** :
  * Génère un mot de passe temporaire : format `Jop-XXXXXX` (où XXXXXX est une chaîne aléatoire alphanumérique de 6 caractères).
  * Chiffre le mot de passe avec le `PasswordEncoder`.
  * Sauvegarde la nouvelle entité `UserAccountEntity` liée à l'`organizationId` de l'admin.
* **Réponse (201 Created)** :
```json
{
  "id": "c76cda74-7a10-46e8-bb4c-9ed454fa08fc",
  "email": "nouveau.medecin@joprelys.local",
  "displayName": "Dr. Marc LENOIR",
  "role": "MEDECIN",
  "enabled": true,
  "temporaryPassword": "Jop-F3B8G9",
  "createdAt": "2026-07-02T10:00:00Z"
}
```

### 2.3 Modification d'un membre
* **Méthode / URL** : `PUT /api/staff/{id}`
* **Request Body** :
```json
{
  "displayName": "Dr. Marc L. LENOIR",
  "role": "MEDECIN"
}
```
* **Comportement** : Met à jour uniquement le nom et le rôle du collaborateur. Vérifie d'abord que le collaborateur appartient bien au même `organizationId` que l'administrateur.

### 2.4 Activation/Désactivation d'un membre
* **Méthode / URL** : `POST /api/staff/{id}/toggle`
* **Comportement** : Alterne la valeur de `enabled` (true <-> false). Bloque l'action si le membre ciblé appartient à un autre établissement.

## 3. Sécurité & Habilitations

- Tous les endpoints sont sécurisés par `@PreAuthorize("hasRole('ADMIN_CLINIQUE')")`.
- Résolution de l'organisation de l'administrateur via son email extrait du Principal Spring Security JWT.

## 4. Frontend Angular

### 4.1 Modèle & Service API
* Création de `web/src/app/clinic/staff/staff.models.ts` :
```typescript
export interface StaffMember {
  id: string;
  email: string;
  displayName: string;
  role: string;
  enabled: boolean;
  createdAt: string;
}

export interface InviteStaffRequest {
  email: string;
  displayName: string;
  role: string;
}

export interface InviteStaffResponse extends StaffMember {
  temporaryPassword?: string;
}
```
* Création de `web/src/app/clinic/staff/staff-api.service.ts` avec les appels HTTP correspondants.

### 4.2 Composant Angular (`StaffManagementComponent`)
* **Chemin** : `web/src/app/clinic/staff/staff-management.component.ts`.
* Standalone component, Tailwind CSS uniquement.
* Onglet d'accès visible uniquement pour le rôle `ADMIN_CLINIQUE`.
* **Formulaire d'invitation** : Modal d'ajout avec validation des champs.
* **Affichage du mot de passe temporaire** : Si création réussie, affiche un panneau vert proéminent contenant le mot de passe temporaire avec un bouton "Copier le mot de passe".
* **Tableau de gestion** :
  * Liste les membres de l'équipe.
  * Toggles de statut (actif/suspendu).
  * Bouton modifier.

## 5. Stratégie de Tests

1. **StaffControllerTest.java** :
   - `givenAdmin_whenListStaff_thenReturnsOnlyClinicStaff`
   - `givenAdmin_whenInviteNewStaff_thenReturnsTemporaryPasswordAndStoresHash`
   - `givenAdmin_whenInviteExistingEmail_thenReturnsBadRequest`
   - `givenAdmin_whenToggleStaffStatus_thenStatusChanges`
   - `givenMedecin_whenListStaff_thenReturnsForbidden` (403)
   - `givenAdminA_whenModifyStaffOfClinicB_thenReturnsNotFoundOrForbidden`

## 6. Implémentation backend réalisée au 2026-07-02

Fichiers ajoutés ou modifiés :

- `StaffController.java` : endpoints REST `/api/staff`.
- `StaffService.java` : orchestration métier, validation des rôles, génération du mot de passe temporaire, isolation tenant.
- `StaffResponse.java` et `InviteStaffResponse.java` : DTOs API immuables.
- `UserAccountRepository.java` : méthodes de recherche par organisation et par couple `(id, organizationId)`.
- `StaffControllerTest.java` : tests d'intégration `MockMvc` avec JWT réel.

Décisions techniques :

- Pas de migration Flyway ajoutée : la fonctionnalité réutilise la table `users` et la colonne `organization_id`.
- Le mot de passe temporaire n'est jamais stocké en clair ; seul le hash BCrypt est persisté.
- Le endpoint `POST /api/staff/{id}/toggle` conserve le comportement défini dans la story.
- Les comptes `ADMIN_CLINIQUE` ne sont pas gérés comme collaborateurs par cette API afin d'éviter la modification ou suspension d'administrateurs via l'écran équipe.

Vérification :

- Tests non exécutés dans l'environnement courant à cause de la résolution Maven bloquée ; voir `TEST-PLAN.md`.

## 7. Implémentation frontend réalisée au 2026-07-02

Fichiers ajoutés ou modifiés :

- `staff.models.ts` : modèles TypeScript `StaffMember`, `InviteStaffRequest`, `UpdateStaffRequest`, `InviteStaffResponse`.
- `staff-api.service.ts` : appels HTTP relatifs vers `/api/staff`.
- `staff-management.component.ts` : écran Angular standalone `/clinic/staff`.
- `staff-table.component.ts` : composant présentations pour la table et les cartes mobiles du personnel.
- `staff-management.component.spec.ts` : tests unitaires du chargement, de l'invitation, de la modification et du toggle statut.
- `app.routes.ts` : route lazy-load protégée par `roleGuard`, réservée à `ADMIN_CLINIQUE`.
- `dashboard.component.html` : carte d'accès "Équipe clinique" visible uniquement pour `ADMIN_CLINIQUE`.
- `i18n.service.ts` : clés FR/EN de l'écran staff.

Décisions techniques :

- Aucun Angular Material introduit.
- Les appels backend utilisent des chemins relatifs et le proxy Angular existant.
- L'écran réutilise `AppShellComponent`, `PageHeaderComponent`, `CardComponent`, `ButtonComponent`, `AlertComponent`, `EmptyStateComponent` et `StatusBadgeComponent`, avec extraction de la table responsive dans `StaffTableComponent`.
- Les textes visibles ajoutés passent par `I18nService`.

Vérification :

- `npm run test -- --watch=false` : 8 fichiers, 21 tests passés.
- `npm run build -- --configuration development` : succès, chunk lazy `staff-management-component` généré.
- `npm run build` production : bloqué par l'inlining Google Fonts externe sans accès réseau.
