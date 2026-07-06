# TECHNICAL-DESIGN — Profils enrichis et ressources graphiques (Logos, Signatures, Cachets, Photos)

## 1. Objectif technique

Mettre en œuvre les extensions de base de données, les APIs d'upload sécurisé de fichiers et les formulaires IHM avec drag-and-drop pour enrichir les profils de la clinique et de son personnel, puis intégrer ces ressources visuelles dans la génération des PDF.

## 2. Stack concernée

- [x] Spring Boot (Backend, APIs, stockage et génération PDF)
- [x] Angular (Frontend, composant drag-and-drop, formulaires)
- [x] Base de données (PostgreSQL & Flyway)
- [x] Documentation (Documentation First & Changement SemVer)

## 3. Contraintes projet obligatoires

- Backend Spring Boot : Maven uniquement (`pom.xml`, `mvnw`).
- Backend Spring Boot : `application.yml` pour la configuration du stockage.
- Angular : Tailwind CSS v4 CSS-first.
- Angular : `proxy.conf.json` obligatoire et URLs API relatives.
- Angular / Flutter : thème centralisé, light/dark, i18n FR/EN.
- Documentation fonctionnelle et technique maintenue dès le démarrage.

## 4. Architecture cible

Un controlleur de fichiers unifié gère les téléversements physiques des images dans un répertoire local `storage/uploads/` en validant la taille et le type de fichier.
Les chemins d'accès relatifs sont ensuite enregistrés dans les entités `OrganizationEntity` et `UserAccountEntity` via les services existants.
La classe `PdfGeneratorService` charge ces fichiers locaux pour les dessiner dans les PDF à l'aide d'iText.

```text
Angular:
  [Drag & Drop UI Component] → Uploads image to FileController
                             → Receives URL path (e.g., "/api/files/view?path=...")
  [Profile / Admin Form]     → Submits profile data with relative image path

Spring Boot Backend:
  FileController         → Upload & download endpoint, calls FileStorageService
  ProfileController      → Custom endpoint for self-updating profile fields
  PdfGeneratorService    → Reads local image files and inserts them into OpenPDF documents
```

## 5. Fichiers ou modules impactés

| Module | Fichier | Type d’impact |
|---|---|---|
| Database | `src/main/resources/db/migration/V39__enrich_users_and_organizations_profile.sql` | Création de la migration Flyway |
| Backend (Auth) | `UserAccountEntity.java` | Ajout des colonnes profile, photo, signature, cachet |
| Backend (Auth) | `ProfileController.java` | Nouveau controller pour la gestion du profil propre de l'utilisateur |
| Backend (File) | `FileController.java`, `FileStorageService.java` | Nouveaux composants pour gérer l'upload et le service d'images |
| Backend (PDF) | `PdfGeneratorService.java` | Modification des méthodes de génération PDF pour intégrer les images |
| Frontend (UI) | `src/app/shared/ui/file-drag-drop/` | Création d'un composant de drag-and-drop réutilisable |
| Frontend (Profile) | `src/app/clinic/profile/` | Création de la page profil utilisateur (Mon Profil) |
| Frontend (Staff) | `staff-management.component.html/.ts` | Ajout des champs pro dans le formulaire d'édition du personnel |
| Frontend (Clinic) | `organization-form.component.html/.ts` | Ajout du composant d'upload du logo |

## 6. Contrats API

### 6.1 Upload de Fichiers (Commun)
- **Méthode** : `POST`
- **Endpoint** : `/api/files/upload`
- **Request Type** : `multipart/form-data` (File payload name: `file`)
- **Query Params** : `type` (`logo` | `photo` | `signature` | `stamp`)
- **Response** : `201 Created`
```json
{
  "filePath": "/api/files/view?path=uploads/organizations/abc-123/logo.png"
}
```
- **Erreurs** :
  - `400 Bad Request` : Fichier vide, taille > 2 Mo, format non accepté (PNG/JPEG).
  - `401 Unauthorized` : Utilisateur non authentifié.

### 6.2 Lecture d'un Fichier
- **Méthode** : `GET`
- **Endpoint** : `/api/files/view`
- **Query Params** : `path` (chemin relatif du fichier)
- **Response** : `200 OK` (Binaire de l'image, Content-Type: `image/png` ou `image/jpeg`)
- **Erreurs** :
  - `403 Forbidden` : Tentative de Path Traversal (ex: `../../etc/passwd`).
  - `404 Not Found` : Fichier inexistant.

### 6.3 Mise à jour du Profil (Personnel)
- **Méthode** : `PUT`
- **Endpoint** : `/api/profile`
- **Request Body** :
```json
{
  "displayName": "Dr. Jean Dupont",
  "phone": "+237 699 99 99 99",
  "specialty": "Cardiologue",
  "registrationNumber": "ONMC-8932",
  "department": "Cardiologie",
  "bio": "Spécialiste en chirurgie cardiaque...",
  "photoPath": "uploads/users/userId/photo.jpg",
  "signaturePath": "uploads/users/userId/signature.png",
  "stampPath": "uploads/users/userId/stamp.png"
}
```
- **Response** : `200 OK` (Profil mis à jour)

## 7. Modèle de données / migrations

### Migration Flyway `V39__enrich_users_and_organizations_profile.sql`
```sql
ALTER TABLE users ADD COLUMN photo_path VARCHAR(255);
ALTER TABLE users ADD COLUMN signature_path VARCHAR(255);
ALTER TABLE users ADD COLUMN stamp_path VARCHAR(255);
ALTER TABLE users ADD COLUMN phone VARCHAR(50);
ALTER TABLE users ADD COLUMN specialty VARCHAR(150);
ALTER TABLE users ADD COLUMN registration_number VARCHAR(100);
ALTER TABLE users ADD COLUMN department VARCHAR(150);
ALTER TABLE users ADD COLUMN bio TEXT;
```

## 8. Configuration

### Paramètres `application.yml`
```yaml
joprelys:
  storage:
    upload-dir: "storage/uploads"
    max-file-size: 2097152 # 2 Mo en octets
```

## 9. Sécurité

- [x] Authentification requise pour tout upload de fichier et modification de profil.
- [x] Vérification du format MIME réel (Magic Numbers) et non uniquement de l'extension de fichier.
- [x] Restriction stricte de la taille à 2 Mo.
- [x] Nettoyage des noms de fichiers pour éviter le Path Traversal (Interdiction des caractères `..`, `/`, `\`).
- [x] Masquage des chemins de stockage réels dans le système en exposant uniquement des URLs relatives.

## 10. Observabilité

- Logs de warning en cas de tentatives d'upload de types MIME incorrects.
- Logs d'audit pour chaque changement d'image légale (Signature / Cachet) avec trace de l'ID utilisateur.

## 11. Tests prévus

- **Unitaires** :
  - Validation du format et de la taille de fichier dans `FileStorageServiceTest`.
  - Mise à jour du profil utilisateur avec règles RBAC dans `ProfileServiceTest`.
- **Intégration** :
  - `MockMvc` pour tester l'upload de fichier `MultipartFile` sur `/api/files/upload`.
  - `MockMvc` pour tester la modification de profil.
  - Vérification de l'intégration dans `PdfGeneratorServiceTest` en injectant des images mockées dans le rendu PDF.

## 12. Impact version / SemVer

- **Type de bump** : `MINOR` (Ajout de fonctionnalités de profil enrichi rétrocompatibles).
- **Breaking change** : Non. Les anciens profils fonctionnent sans logo ou signature.
- **Migration requise** : Oui (Ajout des colonnes de profil dans la base de données).

## 13. Risques techniques

| Risque | Impact | Mitigation |
|---|---|---|
| Upload de scripts malveillants | Fort (RCE) | Vérification stricte des octets d'en-tête de l'image (MIME magic bytes) et stockage hors du répertoire public d'exécution d'Angular. |
| Path Traversal dans `/api/files/view` | Fort (Lecture de secrets) | Normalisation et vérification que le chemin demandé commence impérativement par le répertoire d'upload racine et ne contient aucun `..`. |
| Mise en page du PDF cassée par une image géante | Moyen (PDF illisible) | Redimensionnement dynamique obligatoire de toutes les images lors de leur inclusion via OpenPDF (`scaleAbsolute`). |

## 14. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-06 | Antigravity | Création initiale |
