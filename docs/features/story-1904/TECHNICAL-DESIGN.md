# Technical Design - STORY-1904: Prescriptions et ordonnances conformes CDC

> Feature ID : `prescriptions-ordonnances-cdc`  
> User Story : `STORY-1904`  

## 1. Architecture technique

### Backend
- **JPA Entities** :
  - `PrescriptionEntity` : Ajout de `issuedAt` (Instant), `visitId` (UUID), `documentId` (UUID).
  - `PrescriptionItemEntity` : Ajout de `form` (String), `route` (String), `frequency` (String), `substitutionAllowed` (Boolean).
- **Enums** :
  - `PrescriptionStatus` : `DRAFT`, `ACTIVE`, `PARTIALLY_DISPENSED`, `FULLY_DISPENSED`, `EXPIRED`, `CANCELLED`.
- **Services** :
  - `PrescriptionService` : Logique de validation, d'annulation, d'activation et de génération du PDF.
  - `PrescriptionExpirationScheduler` : Tâche planifiée `@Scheduled` s'exécutant toutes les heures/jours pour expirer les ordonnances actives dépassées.
- **REST API** :
  - `POST /api/prescriptions/{id}/finalize` : Fige la prescription et passe de `DRAFT` à `ACTIVE`.
  - `PATCH /api/prescriptions/{id}/cancel` : Passe au statut `CANCELLED`.

### Frontend
- **Consultation page** :
  - Extension du formulaire de prescription avec les nouveaux attributs (forme, voie, fréquence, substitution autorisée).
  - Gestion des actions Brouillon (`DRAFT`) et Validation (`ACTIVE`).
- **I18n** :
  - Clés de traduction pour les nouveaux champs de prescription et statuts de cycle de vie.

## 2. Modèle de données (Migration V33)
- `prescriptions` :
  - `issued_at` : TIMESTAMP WITH TIME ZONE
  - `visit_id` : UUID REFERENCES visits(id)
  - `document_id` : UUID REFERENCES medical_documents(id)
- `prescription_items` :
  - `form` : VARCHAR(100)
  - `route` : VARCHAR(100)
  - `frequency` : VARCHAR(100)
  - `substitution_allowed` : BOOLEAN DEFAULT TRUE
- `medical_documents` :
  - `document_type` : VARCHAR(50) DEFAULT 'SYNTHESE'
  - Suppression de la contrainte UNIQUE sur `visit_id`.

## 3. Sécurité
- Contrôle d'accès basé sur le tenant (`organization_id`) et les permissions du médecin prescripteur.
- Prévention IDOR via validation dans `validateAccess()` du patient associé.
