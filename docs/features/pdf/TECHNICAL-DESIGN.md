# Conception Technique — Génération de PDF & Vérification par QR Code (EPIC-0006)

## 1. Architecture Globale

Le module de génération PDF et de vérification QR code repose sur :
1. **Un service de génération de document (Spring Boot)** qui utilise `OpenPDF` pour générer un PDF à partir des données de consultation et de prescription, et `ZXing` pour générer le QR code d'authentification publique.
2. **Un modèle de données et stockage physique** : Une table `medical_documents` enregistre les métadonnées (statut, numéro, chemin du fichier sur disque) et un répertoire local configuré stocke les fichiers PDF physiques de manière sécurisée.
3. **Un contrôleur public de vérification** : Un endpoint public `/api/public/documents/{id}/verify` renvoyant les métadonnées non sensibles de vérification sans authentification.
4. **Un contrôleur privé de téléchargement** : Un endpoint sécurisé `/api/visits/{visitId}/document` pour télécharger le fichier PDF.

```
+-----------------------------------------------------------------+
|                         Spring Boot API                         |
|                                                                 |
|  [VisitController] -> Clôture la visite                         |
|                           |                                     |
|                           v                                     |
|  [DocumentService] -> Génère le QR Code (ZXing)                 |
|                    -> Génère le PDF (OpenPDF)                   |
|                    -> Sauvegarde le PDF sur disque               |
|                    -> Enregistre la ligne dans la base          |
|                                                                 |
+-----------------------------------------------------------------+
                               |
                               +---> Disque Local (/var/joprelys/docs)
                               |
                               +---> Database (medical_documents)
```

## 2. Modèle de données & Migration DB

### Script de migration Flyway (`V8__create_medical_documents_table.sql`)
```sql
CREATE TABLE medical_documents (
    id UUID PRIMARY KEY,
    visit_id UUID NOT NULL UNIQUE REFERENCES visits(id) ON DELETE CASCADE,
    document_number VARCHAR(100) NOT NULL UNIQUE,
    file_path VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'VALID', -- 'VALID', 'REVOKED', 'REPLACED'
    organization_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_medical_documents_organization_id ON medical_documents (organization_id);
```

### Entité JPA (`MedicalDocumentEntity.java`)
- `@Entity` et `@Table(name = "medical_documents")`
- `@TenantId` sur `organizationId` pour l'isolation stricte par clinique.
- Relation `@OneToOne` avec `VisitEntity`.

## 3. Contrat d'API

### 1. Route de Vérification Publique (Sans authentification)
* **URL** : `GET /api/public/documents/{id}/verify`
* **Réponse (200 OK)** :
```json
{
  "documentNumber": "DOC-20260702-000001",
  "status": "VALID",
  "clinicName": "Clinique Pilote de Douala",
  "doctorName": "Dr. Jocelin FOMEKONG",
  "patientName": "Joprelys Patient",
  "issuedAt": "2026-07-02T09:12:00Z"
}
```
* **Erreur (404 Not Found)** si l'UUID du document n'existe pas.

### 2. Route de Téléchargement Privée (Habilitée)
* **URL** : `GET /api/visits/{visitId}/document`
* **Rôles autorisés** : `MEDECIN`, `INFIRMIER`, `AGENT_ACCUEIL`, `ADMIN_CLINIQUE`, `PHARMACIEN`.
* **Réponse** : Flux binaire `application/pdf` avec entête `Content-Disposition: attachment; filename="document.pdf"`.

### 3. Routes de Révocation / Annulation (Habilitées - STORY-0604)
* **Routes** :
  - `PATCH /api/documents/{id}/revoke`
  - `PATCH /api/documents/{id}/cancel`
* **Rôles autorisés** : `MEDECIN`, `ADMIN_CLINIQUE`.
* **Corps (Request Body)** :
```json
{
  "reason": "Motif de la révocation ou de l'annulation (min 5, max 500)"
}
```
* **Réponse (200 OK)** :
```json
{
  "id": "c76cda74-7a10-46e8-bb4c-9ed454fa08fc",
  "documentNumber": "DOC-CONS-20260702-000001",
  "status": "REVOQUE",
  "revokedAt": "2026-07-02T12:00:00Z",
  "revokedByUserId": "a98e2281-22fe-4c0d-b4b9-8e7d825c0e44",
  "revocationReason": "Motif saisi"
}
```

### 4. Enrichissement DTO Consultation (STORY-0604)
Pour éviter les requêtes multiples (N+1), le DTO `ConsultationResponse` renvoyé par `/api/patients/{patientId}/consultations` et `/api/visits/{visitId}/consultation` inclura :
- `documentId` (UUID) : Identifiant du document généré (null si aucun).
- `documentStatus` (String) : Statut du document (ex: `VALID`, `REVOQUE`, `ANNULE`, null si aucun).

## 4. Sécurité & Permissions

- La route de vérification `/api/public/documents/{id}/verify` doit être configurée dans `SecurityConfig.java` en `.permitAll()`.
- Le document PDF physique ne doit **jamais** être accessible directement via le serveur web statique (pas de placement dans `/static/` ou `/public/`). L'accès au fichier sur le disque local se fait uniquement via le contrôleur privé habilité.
- L'URL du QR code encodera l'UUID opaque du document (ex: `/verify/c76cda74-7a10-46e8-bb4c-9ed454fa08fc`), rendant impossible la devinette ou le brute-force d'autres documents.
- Les endpoints de révocation et annulation sont strictement sécurisés avec `@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")`. L'isolation multi-tenant est garantie par le filtrage Hibernate automatique et la résolution de l'acteur connecté.

## 5. Intégration Frontend Angular (STORY-0604)

- **Modifications sur `consultation.models.ts`** :
  Ajout des propriétés `documentId?: string` et `documentStatus?: string` dans l'interface `Consultation`.
- **Ajout dans `ConsultationApiService`** :
  ```typescript
  revokeDocument(documentId: string, reason: string): Observable<DocumentStatusResponse>
  cancelDocument(documentId: string, reason: string): Observable<DocumentStatusResponse>
  ```
- **Conception IHM dans `PatientDetailComponent`** :
  - Un badge visuel s'affiche selon la valeur de `consult.documentStatus` :
    - `VALID` ou non spécifié : Aucun badge (document actif).
    - `REVOKED` : Badge orange `RÉVOQUÉ / ANNULÉ` (traduction `verify.status.revoked`).
    - `CANCELLED` : Badge rouge `ANNULÉ` (traduction `verify.status.cancelled`).
  - Un bouton "Révoquer / Annuler" (icône de panneau d'avertissement ou bouton texte compact rouge secondaire) est affiché uniquement si :
    - L'utilisateur connecté possède le rôle `MEDECIN` ou `ADMIN_CLINIQUE`.
    - Le statut du document est `VALID`.
  - Au clic, affichage d'une modale de confirmation glassmorphism contenant :
    - Un radio-bouton ou commutateur entre "Révocation" et "Annulation".
    - Une zone de texte obligatoire pour renseigner le motif (min 5, max 500 chars).
    - Validation client : bouton "Confirmer" désactivé tant que les contraintes ne sont pas respectées.
    - Notification toast/banner en cas de succès ou d'échec de la requête d'API.

## 6. Stratégie de Tests

1. **Génération PDF** : Tester que la clôture d'une visite engendre bien la création du fichier physique et l'enregistrement de l'entité.
2. **Authenticité Publique** : Tester que `/api/public/documents/{id}/verify` renvoie 200 avec les bonnes informations non sensibles, et 404 pour un UUID invalide.
3. **Confidentialité** : S'assurer que le JSON de vérification publique ne contient pas les champs de diagnostic ou de médicaments prescrits.
4. **Téléchargement sécurisé** : Vérifier qu'un utilisateur non authentifié (ou sans rôle adéquat) reçoit 401/403 en tentant d'accéder au PDF par son endpoint de téléchargement.
5. **Révocation / Annulation (STORY-0603/0604)** :
   - Tester backend : Révocation par médecin OK, annulation par admin clinique OK, double révocation conflict (409), pharmacien non autorisé (403).
   - Tester frontend : Affichage et fermeture de la modale, validation de saisie du motif, appel des méthodes de service et mise à jour de l'état local après succès.
