# Conception Technique — Dossier Patient Unique & Enregistrement (STORY-0301)

## 1. Architecture Multi-tenant Logique

Pour isoler les patients par clinique (tenant) sans ajouter de clauses manuelles `where organization_id = :orgId` dans tous les Repository JPA, nous exploitons la fonctionnalité native `@TenantId` de Hibernate 6 (Spring Boot 4.x) :
1. **`TenantContext`** : Classe utilitaire contenant un `ThreadLocal<UUID>` pour stocker l'ID d'organisation de la requête courante.
2. **`TenantIdentifierResolver`** : Implémente `CurrentTenantIdentifierResolver<UUID>` pour fournir le tenant courant à Hibernate.
3. **`JwtAuthenticationFilter`** : Extrait la revendication `"org"` du JWT décodé, la convertit en UUID, l'affecte au `TenantContext` avant l'exécution du filtre, et la nettoie dans un bloc `finally`.

## 2. Modèle de Données (Flyway Migration `V3`)

Création de la table `patients` avec indexes et contraintes :
```sql
CREATE TABLE patients (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    global_patient_number VARCHAR(50) NOT NULL UNIQUE,
    local_patient_number VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    birth_date DATE NOT NULL,
    phone VARCHAR(50) NOT NULL,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    address VARCHAR(255),
    emergency_contact_name VARCHAR(150),
    emergency_contact_phone VARCHAR(50),
    allergies TEXT,
    medical_history TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_patients_organization_id ON patients (organization_id);
CREATE INDEX idx_patients_global_patient_number ON patients (global_patient_number);
CREATE INDEX idx_patients_local_patient_number ON patients (local_patient_number);
CREATE INDEX idx_patients_full_name ON patients (full_name);
```

## 3. Génération d'Identifiants (DPU et Patient Local)

Pour générer des identifiants lisibles et séquentiels par jour :
- **DPU** : `DPU-JOP-YYYYMMDD-XXXXXX`
- **Local** : `PAT-YYYYMMDD-XXXXXX`
Où `YYYYMMDD` est la date du jour de création et `XXXXXX` est un compteur numérique formaté sur 6 chiffres (ex: `000001`, `000002`).
Un service dédié `PatientNumberGenerator` effectue une requête de comptage atomique pour les patients créés le jour même afin de déterminer le prochain numéro, évitant ainsi les doublons et les collisions de séquences globales.

## 4. Endpoints REST Backend

Tous les endpoints de `/api/patients` requièrent une authentification et sont filtrés automatiquement par le tenant ID de l'utilisateur connecté.
- `POST /api/patients` : Crée un patient
- `GET /api/patients?search=...` : Liste et recherche multicritère (nom, téléphone, DPU)
- `GET /api/patients/{id}` : Récupère les détails d'un patient

## 5. Intégration Frontend Angular

1. **Modèle** : `Patient` défini dans `src/app/patient/patient.models.ts`.
2. **API Service** : `PatientApiService` fournissant les appels HTTP vers le backend.
3. **Composants** :
   - `PatientListComponent` : Liste des patients sous forme de cartes responsive (mobile) ou de tableau élégant (desktop).
   - `PatientFormComponent` : Formulaire d'enregistrement avec validation réactive et gestion i18n des libellés (FR/EN).
   - `PatientDetailComponent` : Page de résumé du dossier patient (identité, contacts d'urgence, allergies, historique).

## 6. Stratégie de Test

1. **Tests Unitaires Backend** : Validation de la génération de DPU / Patient local séquentielle et sans collision.
2. **Tests d'Intégration** : Validation de la création de patients et de la recherche.
3. **Tests d'Isolation** : Vérification stricte qu'un utilisateur de l'organisation A ne peut pas lister ou lire par ID les patients de l'organisation B.
4. **Tests Frontend** : Validation de l'affichage mobile-first et de la soumission de formulaires réactifs.

## 7. Correction UI — Tableau desktop patient (2026-07-03)

La correction de lisibilité du tableau patient reste limitee au template Angular `patient-list.component.html`.

Approche technique :

1. Ajouter une largeur minimale au tableau desktop pour eviter la compression excessive des colonnes contenant les identifiants.
2. Definir des largeurs de colonnes via `colgroup` pour stabiliser la mise en page.
3. Appliquer `whitespace-nowrap` sur les donnees critiques : DPU national, numero local, sexe, telephone et action.
4. Conserver `overflow-x-auto` sur le conteneur afin de preserver le responsive desktop/tablette sans impacter la vue mobile.

Impacts exclus :

- Aucun endpoint REST modifie.
- Aucun modele TypeScript modifie.
- Aucun changement de logique metier ou de securite.
- Aucun changement de theme global.
