# Conception Technique — Module Hospitalisations, lits et notes journalières (STORY-1202)

## 1. Modèle de Données (Base de données)

### 1.1 Migration Flyway `V19__create_hospitalizations_tables.sql`
```sql
-- V19: Création des tables d'hospitalisation et notes d'évolution (STORY-1202)
CREATE TABLE IF NOT EXISTS hospitalizations (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    service_name VARCHAR(100) NOT NULL,
    room_number VARCHAR(50) NOT NULL,
    bed_number VARCHAR(50) NOT NULL,
    admission_reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'EN_COURS', -- EN_COURS, SORTI
    admitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    discharged_at TIMESTAMP WITH TIME ZONE,
    discharge_diagnosis TEXT,
    discharge_instructions TEXT,
    pdf_file_path VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS hospitalization_notes (
    id UUID PRIMARY KEY,
    hospitalization_id UUID NOT NULL REFERENCES hospitalizations(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    note_content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Index unique d'exclusivité de lit actif : un lit ne peut être occupé que par une seule hospitalisation active (status = 'EN_COURS')
CREATE UNIQUE INDEX idx_hospitalization_active_bed 
ON hospitalizations (organization_id, room_number, bed_number) 
WHERE status = 'EN_COURS';

CREATE INDEX IF NOT EXISTS idx_hospitalizations_patient_id ON hospitalizations(patient_id);
CREATE INDEX IF NOT EXISTS idx_hospitalization_notes_hosp_id ON hospitalization_notes(hospitalization_id);
```

## 2. Implémentation Backend (Spring Boot)

### 2.1 Entités JPA
- `HospitalizationEntity` :
  - `@Entity` et `@Table(name = "hospitalizations")`.
  - `@TenantId` sur `organizationId`.
  - `@Version` sur `version` (concurrence optimiste).
- `HospitalizationNoteEntity` :
  - `@Entity` et `@Table(name = "hospitalization_notes")`.
  - `@TenantId` sur `organizationId`.

### 2.2 Endpoints REST (`HospitalizationController`)
- `POST /api/hospitalizations` : Admettre un patient.
- `GET /api/hospitalizations/patient/{patientId}` : Historique des hospitalisations du patient.
- `GET /api/hospitalizations/{id}` : Détails d'une hospitalisation.
- `POST /api/hospitalizations/{id}/notes` : Ajouter une note d'évolution.
- `GET /api/hospitalizations/{id}/notes` : Récupérer les notes d'évolution.
- `POST /api/hospitalizations/{id}/discharge` : Déclarer la sortie.
- `GET /api/hospitalizations/{id}/pdf` : Télécharger la fiche de sortie PDF.

*Sécurité* : `@PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")` pour les modifications.

### 2.3 Génération PDF de Sortie
- Nous allons implémenter la génération d'une fiche de sortie PDF épurée et professionnelle (utilisant OpenPDF ou iText selon les dépendances du projet).
- Les signatures et informations de décharge (Diagnostic final, consignes post-hospitalisation) y seront incluses.

## 3. Implémentation Frontend (Angular)

### 3.1 Services & Composants
- Un onglet de suivi dans le DPU.
- Formulaire d'admission d'hospitalisation avec choix de lit/chambre.
- Timeline chronologique des notes journalières.
- Dialogue de décharge et bouton de téléchargement du PDF.
