# Conception Technique — Ouverture & Clôture de Visite Patient (STORY-0401)

## 1. Modèle de Données (Base de données)

Nous créons la table `visits` rattachée à l'organisation (tenant) et au patient :
* `id` : UUID Clé primaire.
* `organization_id` : UUID Clé étrangère vers `organizations` (NOT NULL).
* `patient_id` : UUID Clé étrangère vers `patients` (NOT NULL).
* `visit_number` : VARCHAR(50) NOT NULL UNIQUE (Ex: `VIS-YYYYMMDD-XXXXXX`).
* `reason` : TEXT NOT NULL (Motif de la visite).
* `orientation` : VARCHAR(100) NOT NULL (Service ou médecin d'orientation).
* `status` : VARCHAR(20) NOT NULL DEFAULT 'EN_COURS' (Valeurs : `EN_COURS`, `TERMINEE`, `ANNULEE`).
* `created_at` : TIMESTAMP WITH TIME ZONE (Heure d'arrivée / ouverture).
* `updated_at` : TIMESTAMP WITH TIME ZONE.
* `closed_at` : TIMESTAMP WITH TIME ZONE (Heure de fin / clôture).

### Migration Flyway `V4__create_visits_table.sql`
```sql
CREATE TABLE visits (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    patient_id UUID NOT NULL REFERENCES patients(id),
    visit_number VARCHAR(50) NOT NULL UNIQUE,
    reason TEXT NOT NULL,
    orientation VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'EN_COURS',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_visits_organization_id ON visits (organization_id);
CREATE INDEX idx_visits_patient_id ON visits (patient_id);
CREATE INDEX idx_visits_status ON visits (status);
CREATE INDEX idx_visits_visit_number ON visits (visit_number);
```

## 2. Implémentation Backend (Spring Boot)

### 2.1 Entité `VisitEntity`
* L'entité utilisera l'annotation `@TenantId` sur le champ `organizationId` pour assurer le cloisonnement automatique des requêtes par Hibernate.
* Relation avec le patient :
  ```java
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private PatientEntity patient;
  ```
  L'utilisation de `FetchType.LAZY` évite de charger l'intégralité de l'objet Patient en mémoire lors du listage de la file d'attente (conformément à l'ADR-0002).

### 2.2 Génération du Numéro de Visite (`VisitNumberGenerator`)
* Similaire à `PatientNumberGenerator`, ce service utilisera `JdbcTemplate` pour effectuer un comptage global des visites créées le jour même, afin de compiler le format séquentiel `VIS-YYYYMMDD-XXXXXX` sans collision inter-tenant et sans être bloqué par le filtre Hibernate de tenant.

### 2.3 Endpoints API REST (`VisitController`)
* `POST /api/visits` : Ouvre une nouvelle visite pour le patient spécifié.
  * Payloads : `patientId`, `reason`, `orientation`.
  * Rôles : `AGENT_ACCUEIL`, `INFIRMIER`, `MEDECIN`, `ADMIN_CLINIQUE`.
* `GET /api/visits/active` : Liste toutes les visites de la clinique à l'état `EN_COURS` (file d'attente).
  * Rôles : `AGENT_ACCUEIL`, `INFIRMIER`, `MEDECIN`, `ADMIN_CLINIQUE`.
* `POST /api/visits/{id}/close` : Clôture la visite spécifiée.
  * Rôles : `MEDECIN`, `ADMIN_CLINIQUE`.

## 3. Implémentation Frontend (Angular)

### 3.1 Service API `VisitApiService`
* Méthodes : `create(dto)`, `getActiveVisits()`, `closeVisit(id)`.

### 3.2 Formulaire d'ouverture de visite (`VisitFormComponent`)
* Intégré sur l'écran du profil patient (fiche détaillée).
* Affiche un bouton "Admettre le patient" qui ouvre une boîte de dialogue ou un sous-formulaire pour saisir le motif et l'orientation.

### 3.3 File d'attente active du Dashboard (`QueueListComponent`)
* Affiché sur l'écran d'accueil du dashboard clinique.
* Mobile-first : rendu sous forme de cartes d'attente sur mobile (`md:hidden`), et de tableau horizontal structuré sur desktop (`hidden md:block`).

## 4. Tests et QA

* **Backend** : Écrire `VisitControllerTest` validant :
  1. L'ouverture réussie d'une visite et la génération correcte du numéro.
  2. Le blocage si le patient possède déjà une visite active.
  3. L'isolation stricte des visites d'un tenant à l'autre.
  4. La clôture d'une visite active par un médecin et le blocage pour les autres rôles non autorisés.
* **Frontend** : Écrire les specs unitaires des nouveaux composants.
