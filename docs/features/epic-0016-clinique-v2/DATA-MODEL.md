# Modèle de Données : EPIC-0016 — Consolidation V2.1 du Socle Clinique

Ce document décrit le schéma relationnel et les tables nécessaires pour supporter les modules **Accueil (Circuit Patient)** et **Urgences & Réanimation**, tels qu'exigés par le cahier des charges V2.1.

---

## 1. Module Accueil : `reception_logs` (Registre d'accueil)

Cette table permet de tracer toute personne se présentant à l'accueil de la clinique, qu'il s'agisse d'un visiteur, d'une demande d'audience ou d'un patient.

| Colonne | Type SQL | Description | Contraintes / Remarques |
|---|---|---|---|
| `id` | UUID | Identifiant unique | PRIMARY KEY |
| `organization_id` | UUID | Clinique concernée | FOREIGN KEY, INDEX |
| `log_type` | VARCHAR(50) | Type de passage | Enum: `VISITOR`, `AUDIENCE`, `PATIENT` |
| `first_name` | VARCHAR(100) | Prénom de la personne | Obligatoire |
| `last_name` | VARCHAR(100) | Nom de la personne | Obligatoire |
| `id_document_type` | VARCHAR(50) | Type de pièce (CNI, Passeport) | Nullable |
| `id_document_number` | VARCHAR(100)| Numéro de la pièce | Nullable |
| `target_patient_id` | UUID | Patient visité | FOREIGN KEY (Nullable), si type = VISITOR |
| `target_staff_id` | UUID | Personnel demandé | FOREIGN KEY (Nullable), si type = AUDIENCE |
| `reason` | TEXT | Motif de la visite/audience | Nullable |
| `arrival_at` | TIMESTAMP | Heure d'arrivée | `NOW()` par défaut |
| `departure_at` | TIMESTAMP | Heure de départ | Nullable |
| `created_by` | UUID | Agent d'accueil ayant saisi | FOREIGN KEY |
| `created_at` | TIMESTAMP | Date de création technique | |

---

## 2. Module Urgences : `emergencies` (Dossier de Réanimation)

Cette table étend la consultation classique pour gérer le flux critique, le triage et le suivi hémodynamique immédiat.

| Colonne | Type SQL | Description | Contraintes / Remarques |
|---|---|---|---|
| `id` | UUID | Identifiant unique | PRIMARY KEY |
| `organization_id` | UUID | Clinique concernée | FOREIGN KEY, INDEX |
| `patient_id` | UUID | Patient concerné | FOREIGN KEY |
| `visit_id` | UUID | Visite associée | FOREIGN KEY, UNIQUE |
| `arrival_mode` | VARCHAR(50) | Mode de transport | Enum: `AMBULANCE`, `FIRE_DEPT`, `WALK_IN` |
| `triage_level` | VARCHAR(20) | Niveau de priorité | Enum: `RED` (Choc), `ORANGE`, `YELLOW`, `GREEN` |
| `hemodynamic_status` | VARCHAR(50) | État circulatoire | Enum: `STABLE`, `UNSTABLE`, `SHOCK` |
| `chief_complaint` | TEXT | Motif (ex: AVP, Plaie) | Obligatoire |
| `initial_bp_systolic` | INTEGER | Tension systolique initiale| Nullable |
| `initial_bp_diastolic`| INTEGER | Tension diastolique initiale| Nullable |
| `initial_hr` | INTEGER | Fréquence cardiaque initiale | Nullable |
| `initial_temp` | DECIMAL(4,2) | Température initiale | Nullable |
| `stabilized_at` | TIMESTAMP | Date/Heure de stabilisation | Nullable |
| `orientation` | VARCHAR(50) | Décision médicale post-urgence| Enum: `OR_DIRECT` (Bloc), `ADMISSION`, `DISCHARGE`, `DEATH` |
| `created_by` | UUID | Infirmier/Médecin trieur | FOREIGN KEY |
| `created_at` | TIMESTAMP | Date de création | |

---

## 3. Module Urgences : `resuscitation_logs` (Traçabilité des soins)

Cette table horodate chaque action vitale effectuée sur le patient en état de choc (pose de voie, remplissage, drogues).

| Colonne | Type SQL | Description | Contraintes / Remarques |
|---|---|---|---|
| `id` | UUID | Identifiant unique | PRIMARY KEY |
| `emergency_id` | UUID | Fiche d'urgence rattachée | FOREIGN KEY, INDEX |
| `action_type` | VARCHAR(50) | Type d'acte | Enum: `VASCULAR_ACCESS`, `FLUID_BOLUS`, `MEDICATION` |
| `description` | VARCHAR(255) | Détail (ex: "Ringer Lactate", "VVP calibre 18G") | Obligatoire |
| `quantity` | DECIMAL(10,2) | Quantité administrée | Nullable |
| `unit` | VARCHAR(20) | Unité (ml, mg, ampoule) | Nullable |
| `administered_at` | TIMESTAMP | Heure exacte d'administration | `NOW()` par défaut |
| `administered_by` | UUID | Infirmier/Médecin exécutant | FOREIGN KEY |

---

## 4. Recommandations JPA (Spring Boot)

- Utiliser `@Entity` et `@Table(name = "...")` pour chaque modèle.
- La relation entre `Patient` et `Emergency` est de type `@ManyToOne`.
- La relation entre `Emergency` et `ResuscitationLog` est de type `@OneToMany(mappedBy = "emergency", cascade = CascadeType.ALL, orphanRemoval = true)`.
- Ne pas oublier le champ `@TenantId` d'Hibernate sur les entités pour garantir l'isolation des données par clinique (`organization_id`).
