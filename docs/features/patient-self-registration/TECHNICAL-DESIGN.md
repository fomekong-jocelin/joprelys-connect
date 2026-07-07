# Spécification Technique — Enregistrement patient en autonomie (Self-Registration) via QR Code

## 1. Modélisation des Données

Pour implémenter la zone tampon (Staging Area), nous introduisons une nouvelle entité `PatientPreRegistrationEntity`. Elle stocke temporairement les informations saisies par le patient avant validation.

### 1.1. Base de données : Table `patient_pre_registrations`

```sql
CREATE TABLE patient_pre_registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    birth_date DATE NOT NULL,
    blood_group VARCHAR(10),
    phone VARCHAR(30),
    email VARCHAR(100),
    address TEXT,
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(30),
    emergency_contact_relation VARCHAR(50),
    existing_patient_flag BOOLEAN DEFAULT FALSE,
    existing_patient_id UUID REFERENCES patients(id),
    status VARCHAR(30) NOT NULL DEFAULT 'AWAITING_VALIDATION', -- AWAITING_VALIDATION, VALIDATED, REJECTED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    validated_at TIMESTAMP,
    validated_by UUID REFERENCES users(id)
);

CREATE INDEX idx_pre_reg_org_status ON patient_pre_registrations(organization_id, status);
CREATE INDEX idx_pre_reg_created_at ON patient_pre_registrations(created_at);
```

---

## 2. API Contracts & Architecture REST

### 2.1. Endpoints Publics (Non-Authentifiés)

#### `POST /api/public/pre-registrations`
Permet la soumission d'une demande de pré-enregistrement depuis le smartphone du patient.
- **Payload de requête** :
  ```json
  {
    "organizationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "firstName": "John",
    "lastName": "Doe",
    "gender": "MASCULIN",
    "birthDate": "1990-01-15",
    "bloodGroup": "O_PLUS",
    "phone": "+237677123456",
    "email": "john.doe@example.com",
    "address": "Yaoundé, Cameroun",
    "emergencyContactName": "Jane Doe",
    "emergencyContactPhone": "+237699123456",
    "emergencyContactRelation": "SPOUSE",
    "existingPatientFlag": false
  }
  ```
- **Réponse (201 Created)** :
  ```json
  {
    "id": "8c3b7a5e-1234-5678-abcd-ef0123456789",
    "status": "AWAITING_VALIDATION",
    "message": "Informations soumises avec succès. Veuillez vous présenter à l'accueil."
  }
  ```

---

### 2.2. Endpoints Privés / Professionnels (Authentifiés)

#### `GET /api/pre-registrations`
Liste les demandes de pré-enregistrement en attente pour la clinique de l'utilisateur connecté.
- **Rôle requis** : `ROLE_AGENT_ACCUEIL`, `ROLE_ADMIN_CLINIQUE`
- **Paramètres de filtrage** : `status` (default: `AWAITING_VALIDATION`), `page`, `size`
- **Réponse (200 OK)** : Liste paginée de `PatientPreRegistrationResponse`.

#### `GET /api/pre-registrations/{id}`
Récupère le détail d'une demande spécifique de pré-enregistrement.
- **Rôle requis** : `ROLE_AGENT_ACCUEIL`, `ROLE_ADMIN_CLINIQUE`

#### `POST /api/pre-registrations/{id}/validate`
Valide la demande et crée ou fusionne le patient dans le DPU officiel.
- **Rôle requis** : `ROLE_AGENT_ACCUEIL`
- **Payload** (permet de modifier les données avant de valider) :
  ```json
  {
    "firstName": "John",
    "lastName": "Doe",
    "gender": "MASCULIN",
    "birthDate": "1990-01-15",
    "phone": "+237677123456",
    "email": "john.doe@example.com",
    "address": "Yaoundé, Cameroun",
    "reconcileWithPatientId": "9e5c4a3b-2345-6789-abcd-ef0123456789" -- Optionnel si réconciliation
  }
  ```
- **Réponse (200 OK)** :
  ```json
  {
    "patientId": "9e5c4a3b-2345-6789-abcd-ef0123456789",
    "status": "VALIDATED",
    "pdfAdmissionUrl": "/api/documents/admission-sheets/8c3b7a5e-1234.pdf"
  }
  ```

#### `POST /api/pre-registrations/{id}/reject`
Rejette une demande (spam ou informations farfelues).
- **Rôle requis** : `ROLE_AGENT_ACCUEIL`

---

## 3. Algorithme de Réconciliation & Anti-Doublons

Lorsqu'une demande de pré-enregistrement est sélectionnée par l'agent d'accueil dans le back-office, le système interroge automatiquement le `PatientSimilarityService` existant.

1. **Calcul de similarité** :
   - Requête de comparaison sur les critères : `lastName` (Levenshtein), `firstName` (Levenshtein), `birthDate` (exact), `phone` (exact).
2. **Affichage à l'agent** :
   - Si score > 80% : alerte critique. Le système propose de fusionner le pré-enregistrement avec la fiche existante.
   - Si score entre 50% et 80% : avertissement. L'agent examine manuellement s'il s'agit de la même personne.
3. **Fusion technique** :
   - Si l'agent choisit la fusion, les informations du DPU existant sont mises à jour avec les informations plus récentes saisies par le patient, et l'historique d'audit consigne l'action : `PATIENT_PRE_REGISTRATION_RECONCILED`.

---

## 4. Spécifications de Sécurité

### 4.1. Rate Limiting sur l'API Publique
Pour prévenir les attaques par déni de service et la pollution de la base de données :
- Configuration d'un filtre de Rate Limiting (ex: Bucket4j) limitant les appels à `POST /api/public/pre-registrations` à **5 requêtes par minute par adresse IP**.

### 4.2. Protection contre les fuites PII
- L'endpoint `POST /api/public/pre-registrations` ne doit jamais retourner d'informations indiquant si le patient a été retrouvé ou non en base.
- Aucun endpoint public n'autorise la recherche de patients (`GET /api/public/patients` est strictement interdit).

---

## 5. Impression : Génération de la Fiche d'Admission

Après validation par l'agent d'accueil :
1. Appel au `PdfGeneratorService` pour générer la fiche d'admission.
2. Contenu de la fiche :
   - Logo et nom de la clinique (dynamique via `OrganizationEntity`).
   - Code-barres ou QR Code de la visite courante (permettant aux médecins de scanner la fiche physique pour ouvrir directement le dossier patient dans Joprelys Connect).
   - Informations d'identité et de contact d'urgence.
   - Mentions légales du CDC.
3. Format de sortie : PDF téléchargeable et imprimable immédiatement.

---

## 6. Job de Purge Automatique

Une tâche planifiée Spring Boot est implémentée pour nettoyer la table de transition :

```java
@Component
public class PreRegistrationCleanupScheduler {

    @Autowired
    private PatientPreRegistrationRepository repository;

    @Scheduled(cron = "0 0 2 * * ?") // Tous les jours à 2h du matin
    @Transactional
    public void cleanupOldPreRegistrations() {
        LocalDateTime limit = LocalDateTime.now().minusHours(24);
        repository.deleteByStatusAndCreatedAtBefore(PreRegistrationStatus.AWAITING_VALIDATION, limit);
    }
}
```
