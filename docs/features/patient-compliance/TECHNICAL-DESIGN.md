# Conception Technique — Conformité Module Patient (EPIC-0013)

Ce document décrit la conception technique, les choix d'architecture et la modélisation de base de données pour implémenter la conformité du module Patient.

---

## 1. Modélisation de la Base de Données (Flyway Migrations)

Pour stocker les candidats aux doublons, l'historique des fusions, et gérer les scopes fins d'accès, les tables suivantes seront créées ou modifiées via des scripts de migration SQL Flyway.

### 1.1 Nouvelles Tables
```sql
-- Table des doublons candidats détectés automatiquement
CREATE TABLE patient_duplicate_candidates (
    id VARCHAR(36) PRIMARY KEY,
    source_patient_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_patient_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    similarity_score DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, RESOLVED, IGNORED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_patient_pair UNIQUE (source_patient_id, target_patient_id)
);

-- Table de traçabilité des fusions de dossiers patients
CREATE TABLE patient_merged_history (
    id VARCHAR(36) PRIMARY KEY,
    primary_patient_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    merged_patient_id VARCHAR(36) NOT NULL, -- UUID de l'ancien patient (supprimé/désactivé)
    merged_patient_dpu VARCHAR(50) NOT NULL, -- Sauvegarde de son ancien numéro DPU
    merged_by VARCHAR(36) NOT NULL REFERENCES users(id),
    merged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 1.2 Mises à jour des Tables Existantes
```sql
-- Ajout des scopes et canal dans la table des consentements
ALTER TABLE patient_consents ADD COLUMN scopes VARCHAR(255) DEFAULT 'medical_records,prescriptions,lab_results,allergies_history';
ALTER TABLE patient_consents ADD COLUMN validation_channel VARCHAR(50) DEFAULT 'MANUAL';

-- Ajout des scopes dans la table des demandes d'accès externes
ALTER TABLE external_access_requests ADD COLUMN scopes VARCHAR(255) DEFAULT 'medical_records,prescriptions,lab_results,allergies_history';
```

---

## 2. Architecture Logicielle Backend (Spring Boot)

### 2.1 Détection des Doublons (Levenshtein & Phonétique)
Un service `PatientSimilarityService` sera créé pour calculer le score de ressemblance entre deux patients :
* **Algorithme de Levenshtein** : Utilisé sur le nom complet normalisé (lettres minuscules, sans accents ni espaces superflus).
* **Vérification complémentaire** : Si la date de naissance correspond exactement et que le score Levenshtein nom/prénom est supérieur ou égal à **85%**, le couple est marqué comme candidat de doublon potentiel.
* **Intégration** : À la création d'un patient dans `PatientService.createPatient()`, le système vérifie s'il y a un doublon potentiel. Si oui, il insère l'entrée dans `patient_duplicate_candidates` et loggue un événement d'audit.

### 2.2 Logique de Fusion Transactionnelle
Dans `PatientService` :
```java
@Transactional
public void mergePatients(String primaryId, String secondaryId, String actorId) {
    // 1. Récupérer les deux patients
    // 2. Mettre à jour toutes les clés étrangères liées dans les autres tables de secondaryId vers primaryId
    //    Tables concernées : visits, prescriptions, patient_consents, patient_allergies, patient_medical_history, external_access_requests, medical_documents, hospitalizations
    // 3. Marquer le patient secondaire comme "Désactivé" et changer son type ou ajouter un marqueur
    // 4. Enregistrer l'historique dans patient_merged_history
    // 5. Auditer l'action avec l'IP/User-Agent
}
```

### 2.3 Génération de la Fiche de Synthèse PDF
* **Framework** : Utilisation d'OpenPDF/iText pour générer un template A4 épuré (sur 1 page max pour rester condensé).
* **Endpoint** : `GET /api/patients/{id}/summary-pdf`
* **Sécurité** : Accessible uniquement aux professionnels de santé (`expectedRoles` : MEDECIN, INFIRMIER, ADMIN_CLINIQUE).

### 2.4 Contrôle d'Accès Granulaire (Scopes)
Lorsqu'un praticien accède aux données cliniques d'un patient hors de sa clinique (via une demande d'accès temporaire) ou au sein du dossier partagé :
* Le filtre d'autorisation intercepte la requête et récupère la liste des scopes autorisés dans l'entité `PatientConsent` ou `ExternalAccessRequest`.
* Si le scope requis pour le endpoint n'est pas présent, renvoyer une erreur 403 :
  * Endpoint de visites/consultations -> requiert le scope `medical_records`
  * Endpoint d'ordonnances -> requiert le scope `prescriptions`
  * Endpoint d'examens/analyses -> requiert le scope `lab_results`
  * Endpoint d'allergies/antécédents -> requiert le scope `allergies_history`

---

## 3. Architecture Logicielle Frontend (Angular & Tailwind CSS v4)

### 3.1 Interface de Gestion des Doublons (Admin)
* Création d'une page `/clinic/duplicates` affichant la liste des paires candidates.
* Bouton pour lancer l'assistant de fusion : un modal interactif à deux colonnes permettant de comparer côte à côte les deux profils, de sélectionner le profil principal, puis de valider la fusion.

### 3.2 Fiche de Synthèse (Dossier Patient)
* Ajout d'un bouton d'action "Fiche de Synthèse" avec une icône de téléchargement dans le header du profil patient.

### 3.3 Sélection Granulaire des Scopes (Portail Patient)
* Modification du formulaire d'autorisation de consentement et de validation de demande d'accès : ajout de cases à cocher pour chaque scope.
* Affichage du canal de validation dans la liste des consentements.

---

## 4. Stratégie de Tests

1. **Tests Unitaires Backend** :
   * Tester `PatientSimilarityService` avec plusieurs cas de noms (noms proches, inversés, identiques, phonétiquement proches) pour valider le calcul de score.
   * Tester la logique de fusion transactionnelle en s'assurant que toutes les entités associées (visites, ordonnances) sont bien réassignées au patient principal et que le patient secondaire est désactivé.
2. **Tests d'Intégration REST** :
   * Valider qu'un médecin sans le scope `lab_results` reçoit un 403 lors d'une tentative de consultation des examens biologiques.
3. **Tests Frontend** :
   * Valider le bon affichage des checkboxes de scopes et la bonne transmission des valeurs booléennes à l'API lors de la sauvegarde.
