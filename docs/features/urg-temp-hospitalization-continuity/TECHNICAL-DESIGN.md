# Conception technique — continuité URG-TEMP

## Architecture

```text
Urgence / rapprochement
        │
        ▼
PatientCanonicalResolver
        │
        ├── HospitalizationAdmissionService
        │     ├── EmergencyRepository
        │     ├── VisitRepository
        │     ├── BedRepository
        │     └── HospitalizationRepository
        │
        ├── EmergencyDocumentService
        │     ├── EmergencyMedicoLegalService
        │     ├── MedicalDocumentRepository
        │     └── EmergencyDocumentPdfService
        │
        ├── CanonicalHospitalizationQueryService
        ├── CanonicalPatientDocumentService
        └── InvoiceCrudService
```

Le rattachement reste logique, conformément à l'ADR-0001 : les lignes historiques conservent leur `patient_id` d'origine et les lectures du DPU canonique agrègent les identités contributrices.

## Migration V75

### Hospitalisation

```sql
hospitalizations.emergency_id UUID NULL
```

- clé étrangère vers `emergencies.id` ;
- index dédié ;
- valeur nullable pour préserver les hospitalisations historiques et les admissions non urgentes.

### Facturation

```sql
invoices.regularization_status VARCHAR(40) NOT NULL
```

Valeurs applicatives :

- `RESOLVED` ;
- `REGULARIZATION_PENDING`.

La migration qualifie les factures historiques `RESOLVED`, puis retire la valeur par défaut afin que l'application porte explicitement la décision métier.

## Admission

`CreateHospitalizationRequest` accepte :

- `visitId` ;
- `emergencyId` ;
- ou les deux lorsque l'urgence est déjà reliée à une visite.

Une validation de niveau DTO impose au moins un contexte de soins.

### Résolution du patient

1. résolution du patient demandé par `PatientCanonicalResolver` ;
2. vérification que l'urgence appartient au même DPU canonique ;
3. refus si l'organisation ne correspond pas ;
4. recherche d'une hospitalisation active parmi toutes les identités contributrices.

### Résolution de la visite

Ordre :

1. `visitId` explicite ;
2. visite déjà reliée à l'urgence ;
3. visite active du patient de continuité ;
4. création d'une visite `HOSPITALISATION` issue de l'urgence.

La visite créée conserve l'heure d'arrivée de l'urgence et est reliée à celle-ci.

### Concurrence

- contrôle d'une hospitalisation active dans le contexte canonique ;
- unicité fonctionnelle d'une hospitalisation par urgence ;
- réservation du lit par mise à jour atomique `FREE → OCCUPIED` ;
- aucune création implicite de service, chambre ou lit.

## Documents

Les documents d'urgence sont des `MedicalDocumentEntity` associés à la visite de continuité.

Types ajoutés :

- `FICHE_URGENCE` ;
- `FEUILLE_REANIMATION` ;
- `CONSTAT_INCAPACITE_URGENCE` ;
- `FICHE_TIERS_URGENCE` ;
- `INVENTAIRE_EFFETS_URGENCE` ;
- `SYNTHESE_RAPPROCHEMENT` réservé au parcours E2E.

### Idempotence

Pour chaque visite et type, `EmergencyDocumentService` recherche d'abord la version la plus récente. Si elle existe, elle est retournée sans générer de nouvelle ligne.

### Preuve

Chaque fichier reçoit :

- numéro métier généré ;
- hash SHA-256 ;
- version ;
- URL de vérification ;
- QR code ;
- auteur ;
- audit.

Le billet d'entrée existant devient également un document persisté de type `FICHE_HOSPITALISATION` lors du premier téléchargement.

## Finance

`InvoiceCrudService` :

- conserve le patient d'origine de la visite ;
- refuse une visite hors du contexte canonique ;
- marque les identités provisoires/déclarées `REGULARIZATION_PENDING` ;
- agrège les factures par `contributingPatientIds` ;
- ne modifie aucune facture au moment du rapprochement.

## Contrats REST

### Hospitalisation

```http
POST /api/hospitalizations
```

Exemple urgence sans visite préalable :

```json
{
  "patientId": "uuid",
  "serviceName": "Médecine",
  "roomNumber": "101",
  "bedNumber": "101-A",
  "admissionReason": "Surveillance post-stabilisation",
  "emergencyId": "uuid",
  "responsiblePractitionerId": "uuid"
}
```

La réponse contient `emergencyId` et le `visitId` créé ou réutilisé.

### Documents

```http
POST /api/emergencies/{emergencyId}/documents
GET  /api/emergencies/{emergencyId}/documents
GET  /api/patients/{patientId}/documents
```

La dernière route retourne la vue canonique agrégée et expose `originPatientId`.

### Factures

Le contrat `InvoiceResponse` ajoute :

```json
{
  "regularizationStatus": "REGULARIZATION_PENDING"
}
```

## Angular

`EmergencyHospitalizationContinuationComponent` :

- reçoit `patientId`, `emergencyId`, état d'identité et numéro provisoire ;
- charge la configuration spatiale stricte et les praticiens ;
- filtre les services via `allowsRooms` ;
- soumet l'admission avec `emergencyId` ;
- déclenche ensuite le lot documentaire ;
- distingue un échec documentaire d'une admission déjà créée.

`PatientHospitalizationsTabComponent` lit `emergencyId` dans les paramètres de requête et affiche le panneau de continuité sans modifier le parcours d'hospitalisation habituel.

## Compatibilité

- les admissions historiques avec `visitId` restent compatibles ;
- `emergencyId` et `regularizationStatus` sont additifs au niveau REST ;
- la migration qualifie les factures existantes ;
- les requêtes normales d'hospitalisation restent disponibles ;
- aucune logique n'est déplacée vers les contrôleurs.

## Risques contrôlés

- un échec de génération documentaire après admission ne doit pas provoquer une seconde admission ;
- les fichiers générés avant un rollback de transaction doivent faire l'objet d'une surveillance d'exploitation ;
- les requêtes agrégées doivent rester tenantées par Hibernate et le résolveur canonique ;
- les permissions documentaires doivent être présentes dans les profils de démonstration.
