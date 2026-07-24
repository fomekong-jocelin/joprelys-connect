# Functional Specification: Structured Hospital Service and Orientation Selection in Patient Admission & Visits

## 1. Context & Business Need
In the patient registration and admission process ("Nouvelle admission", "Nouvelle visite"), form fields for **Orientation** and **Service** were previously free-text input fields (`<input type="text">`).
Free-text entry leads to data quality degradation, typos, inconsistent reporting, and invalid billing/spatial assignments across the hospital.

Since hospital organization services and units are fully configured in the system, selecting a service or orientation when registering a patient MUST be controlled via drop-down select controls (`<select>`) connected to the official hospital service catalog and standardized clinical orientations.

## 2. Functional Requirements
1. **Orientation Field**:
   - Must be a required `<select>` dropdown (no longer free text).
   - Standardized choices:
     - Consultation générale / Examen (`CONSULTATION`)
     - Consultation spécialisée (`SPECIALIZED_CONSULTATION`)
     - Tri / Urgences (`EMERGENCY`)
     - Hospitalisation (`HOSPITALIZATION`)
     - Soins ambulatoires (`AMBULATORY`)
     - Hospitalisation de jour (`DAY_CARE`)
     - Bilan / Visite de contrôle (`CHECKUP`)
     - Autre (`OTHER`)

2. **Service Field**:
   - Must be a `<select>` dropdown connected to the configured hospital services catalog (`/api/hospital-organization/catalogs/services`).
   - Includes standard healthcare departments (Médecine générale, Pédiatrie, Gynécologie-Obstétrique, Chirurgie, Urgences, Réanimation, Cardiologie, Radiologie, Laboratoire, Pharmacie, etc.) merged with active backend catalog entries.
   - If "Autre" is selected, an optional precision text field is revealed to accommodate edge cases without compromising structure.

3. **Internationalization (i18n)**:
   - Full support for FR and EN. No hardcoded labels in templates.

4. **Consistency**:
   - Applies to the unified admission modal (`app-unified-admission`) and the patient visit modal in patient details.

## 3. Acceptance Criteria
- [x] No free-text input for Orientation in the main admission form.
- [x] No free-text input for Service in the main admission form.
- [x] Service options are loaded dynamically from configured hospital services and standard referentials.
- [x] Full i18n support in French and English.
- [x] Existing tests pass and Angular build completes cleanly.
