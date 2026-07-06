# Conception Technique : Documents médicaux vérifiables conformes CDC (STORY-1908)

## 1. Modèle de données & Migration Flyway
Création du fichier de migration `V37__medical_documents_cdc_alignment.sql` dans `src/main/resources/db/migration` :
- Création de la séquence `medical_document_number_seq`.
- Ajout de colonnes sur `medical_documents` :
  - `qr_code_url` VARCHAR(500)
  - `verification_url` VARCHAR(500)
  - `author_user_id` UUID
  - `version` INT NOT NULL DEFAULT 1
  - `previous_document_id` UUID
- Ajout de clés étrangères pour `author_user_id` et `previous_document_id`.
- Ajout d'une contrainte CHECK sur `status` permettant les valeurs : `VALID`, `REVOQUE`, `ANNULE`, `REMPLACE`.

## 2. Classes Backend impactées et modifications

### Enums
- `DocumentType` (enum) : Contient les 12 types de documents du CDC.
- `DocumentStatus` (enum) : Contient `VALID`, `REVOQUE`, `ANNULE`, `REMPLACE`.

### `MedicalDocumentEntity`
- Mappage des nouveaux champs :
  - `@Enumerated(EnumType.STRING) private DocumentStatus status`
  - `@Enumerated(EnumType.STRING) private DocumentType documentType`
  - `hash` (SHA-256)
  - `qrCodeUrl`
  - `verificationUrl`
  - `authorUserId`
  - `version` (int)
  - `previousDocumentId` (UUID)

### `DocumentNumberGenerator`
- Refactorisation pour appeler la séquence `medical_document_number_seq` via `JdbcTemplate` au lieu de `COUNT(*)`.

### `DocumentService`
- **Versionnement** :
  - Recherche du dernier document de même type pour la même visite avant d'enregistrer le nouveau.
  - S'il existe :
    - Passage du statut du précédent document à `REMPLACE` (en base).
    - Report de `previous_document_id = précédent.getId()`.
    - Report de `version = précédent.getVersion() + 1`.
  - Sinon :
    - `version = 1`.
- **Calcul de Hash** :
  - Méthode utilitaire de calcul du hash SHA-256 sur les bytes du PDF.
  - Sauvegarde du hash dans `MedicalDocumentEntity`.
- **QR Code et URL** :
  - Sauvegarde de `verificationUrl` et `qrCodeUrl` (qui pointe vers l'API publique de récupération d'image de QR code).
- **Vérification Publique** :
  - Ajout des nouveaux champs retournés dans `DocumentVerificationResponse` : `documentType`, `serviceName`, et `legalNotice`.

### `DocumentController`
- Exposition de l'endpoint d'image de QR code : `GET /api/public/documents/{id}/qr`.
- Mise à jour de l'API de vérification anonyme `GET /api/public/documents/{id}/verify`.

### `HospitalizationService`
- Association du médecin responsable comme auteur (`authorUserId`) du document de sortie `FICHE_SORTIE`.
- Enregistrement du document de sortie par le biais de la logique de versionnement.

### `LabResultService`
- Calcul de hash et génération de `MedicalDocumentEntity` de type `RESULTAT_LABORATOIRE` lors du téléversement de résultats d'analyses (si PDF fourni).
- Association du biologiste validateur comme auteur du document.

## 3. Conception Frontend

### Page de saisie publique (`/verify`)
- Nouveau composant accessible sans authentification pour saisir manuellement le numéro de document (si scan QR impossible).

### Page de résultat de vérification
- Composant `VerificationComponent` mis à jour pour afficher :
  - Le statut du document (badge coloré).
  - Le type de document et l'établissement.
  - L'auteur / service signataire.
  - La date de génération.
  - La mention légale stricte du CDC.
  - Un bouton/lien d'action pour demander l'accès au dossier médical associé.

### Internationalisation (i18n)
- Clés de traduction FR/EN pour les types de documents, statuts, mention légale, et boutons de demande d'accès.
