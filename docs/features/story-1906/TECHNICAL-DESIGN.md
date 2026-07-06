# TECHNICAL-DESIGN.md — Alignement Module 9 : Résultats d'examens

## 1. Modèle de Données & Migration DB

### 1.1 Séquence SQL pour `result_number`
Création d'une séquence pour garantir des numéros d'examens uniques et séquentiels :
```sql
CREATE SEQUENCE IF NOT EXISTS lab_result_number_seq START WITH 1 INCREMENT BY 1;
```

### 1.2 Migration SQL `V35__lab_results_cdc_alignment.sql`
Modifications de la table `lab_results` :
- `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' (contrainte check: `DRAFT`, `VALIDATED`, `CANCELLED`)
- `validator_user_id` UUID REFERENCES users(id)
- `conclusion` TEXT
- `document_id` UUID REFERENCES medical_documents(id)
- `version` INT NOT NULL DEFAULT 1
- `parent_result_id` UUID REFERENCES lab_results(id)
- Suppression ou dépréciation de `validator_name` (optionnel ou conservé pour rétrocompatibilité/migration, nous allons le rendre nullable).

```sql
ALTER TABLE lab_results ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'VALIDATED';
ALTER TABLE lab_results ADD CONSTRAINT chk_lab_result_status CHECK (status IN ('DRAFT', 'VALIDATED', 'CANCELLED'));

ALTER TABLE lab_results ADD COLUMN IF NOT EXISTS validator_user_id UUID REFERENCES users(id);
ALTER TABLE lab_results ADD COLUMN IF NOT EXISTS conclusion TEXT;
ALTER TABLE lab_results ADD COLUMN IF NOT EXISTS document_id UUID REFERENCES medical_documents(id);
ALTER TABLE lab_results ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 1;
ALTER TABLE lab_results ADD COLUMN IF NOT EXISTS parent_result_id UUID REFERENCES lab_results(id);

ALTER TABLE lab_results ALTER COLUMN validator_name DROP NOT NULL;
```

---

## 2. Backend Spring Boot

### 2.1 Enum `LabResultStatus`
```java
public enum LabResultStatus {
    DRAFT,
    VALIDATED,
    CANCELLED
}
```

### 2.2 Entité `LabResultEntity`
Mise à jour pour inclure les nouveaux champs avec les annotations Hibernate appropriées.

### 2.3 `LabResultService`
- Implémenter l'immutabilité : lors d'une tentative de mise à jour, si le statut actuel est `VALIDATED`, jeter une exception ou créer une nouvelle entité avec une version incrémentée.
- Ajouter la méthode d'exportation structurée.

### 2.4 Endpoints d'exportation
Dans `LabOrderController` ou un nouveau contrôleur, ajouter :
- `GET /api/patients/{id}/exam-results/export?format=csv|json`

### 2.5 Endpoints FHIR
Dans `FhirController` et `FhirService` :
- `GET /fhir/DiagnosticReport?patient={id}`
- `GET /fhir/Observation?patient={id}`

---

## 3. Frontend Angular

### 3.1 Nouveaux modèles dans `lab.models.ts`
Mettre à jour `LabResult` pour correspondre aux nouveaux champs.

### 3.2 Nouvelle page `PatientResultsPageComponent`
Création de `web/src/app/patient/portal/pages/patient-results-page.component.ts`.
Affichage sous forme de timeline premium, avec filtres, conclusion, et téléchargement du PDF.

### 3.3 Mise à jour de `lab-orders-page.component.ts`
Modifier le formulaire de saisie de résultats pour inclure :
- Sélection du statut (`DRAFT` ou `VALIDATED`).
- Saisie de la conclusion globale.
- Passage du `validatorUserId` de l'utilisateur connecté s'il y a lieu (ou sélection).
