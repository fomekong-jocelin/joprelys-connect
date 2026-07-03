# Conception Technique — Allergies & Antécédents Médicaux (STORY-1201)

## 1. Modèle de Données (Base de données)

Pour conserver les informations de façon structurée et avec une isolation multi-tenant, nous concevons deux tables : `patient_allergies` et `patient_medical_history`.

### 1.1 Migration Flyway `V18__create_allergies_and_history_tables.sql`
```sql
-- V18: Création des tables d'allergies et antécédents médicaux (STORY-1201)
CREATE TABLE IF NOT EXISTS patient_allergies (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    substance VARCHAR(255) NOT NULL,
    severity VARCHAR(50) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    reaction VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    discovered_at DATE,
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS patient_medical_history (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    category VARCHAR(50) NOT NULL, -- MEDICAL, SURGICAL, FAMILY, OBSTETRICAL, OTHER
    description VARCHAR(255) NOT NULL,
    onset_date DATE,
    is_ongoing BOOLEAN NOT NULL DEFAULT FALSE,
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_patient_allergies_patient_id ON patient_allergies(patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_allergies_org_id ON patient_allergies(organization_id);
CREATE INDEX IF NOT EXISTS idx_patient_medical_history_patient_id ON patient_medical_history(patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_medical_history_org_id ON patient_medical_history(organization_id);
```

## 2. Implémentation Backend (Spring Boot)

### 2.1 Entités JPA
- `PatientAllergyEntity` :
  - `@Entity` et `@Table(name = "patient_allergies")`.
  - `@TenantId` sur le champ `organizationId`.
  - `@Version` sur le champ `version` pour la concurrence optimiste.
  - Relation : `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "patient_id") private PatientEntity patient;`.
- `PatientMedicalHistoryEntity` :
  - `@Entity` et `@Table(name = "patient_medical_history")`.
  - `@TenantId` sur le champ `organizationId`.
  - `@Version` sur le champ `version` pour la concurrence optimiste.
  - Relation : `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "patient_id") private PatientEntity patient;`.

### 2.2 Endpoints API REST (`PatientMedicalInfoController`)
- `GET /api/patients/{patientId}/allergies` : Récupère la liste des allergies.
- `POST /api/patients/{patientId}/allergies` : Ajoute une allergie.
- `PUT /api/patients/{patientId}/allergies/{allergyId}` : Met à jour ou désactive une allergie.
- `GET /api/patients/{patientId}/medical-history` : Récupère la liste des antécédents.
- `POST /api/patients/{patientId}/medical-history` : Ajoute un antécédent.
- `PUT /api/patients/{patientId}/medical-history/{historyId}` : Met à jour ou désactive un antécédent.

*Sécurité* : `@PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")` pour les requêtes en modification.

## 3. Implémentation Frontend (Angular)

### 3.1 Modèle de Données TypeScript
```typescript
export interface PatientAllergy {
  id?: string;
  patientId: string;
  substance: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  reaction?: string;
  status: 'ACTIVE' | 'INACTIVE';
  discoveredAt?: string;
  comment?: string;
  updatedAt?: string;
}

export interface PatientMedicalHistory {
  id?: string;
  patientId: string;
  category: 'MEDICAL' | 'SURGICAL' | 'FAMILY' | 'OBSTETRICAL' | 'OTHER';
  description: string;
  onsetDate?: string;
  isOngoing: boolean;
  comment?: string;
  updatedAt?: string;
}
```

### 3.2 Services et Composants UI
- Intégrer un sous-composant `PatientMedicalInfoComponent` dans l'onglet d'informations médicales de la fiche patient.
- Gérer l'affichage des alertes d'allergie critiques sur la page d'accueil de la fiche patient.
- Raccorder les formulaires d'ajouts rapides.
