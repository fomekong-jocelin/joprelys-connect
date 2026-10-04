# TECHNICAL-DESIGN — Durcissement de l’attribution du lit

## Architecture

Le backend reste la source de vérité. Angular applique uniquement le même filtre d’affichage que le contrat de disponibilité et orchestre la récupération documentaire.

```text
PatientHospitalizationComponent
  -> filtre available + OPEN + READY

EmergencyHospitalizationContinuationComponent
  -> POST admission
  -> POST documents
  -> état recovery si documents en échec

HospitalizationAdmissionService
  -> UserAccountRepository
  -> StaffOrganizationalUnitAssignmentRepository
  -> validation tenant / enabled / rôle / affectation active
```

## Sécurité

- validation d’établissement par `findByIdAndOrganizationId` ;
- compte responsable obligatoirement actif ;
- rôle clinique `MEDECIN` obligatoire pour le praticien responsable ;
- affectation unité-personnel active à `Instant.now()` ;
- aucune autorisation critique confiée au frontend.

## Compatibilité

Les endpoints existants restent inchangés. Les erreurs de praticien invalide utilisent HTTP 409 comme conflit métier, sans migration de base.

## Tests

- tests Angular sur le filtrage des lits et la récupération documentaire ;
- tests unitaires Spring sur le praticien invalide et le chemin nominal ;
- build Angular et suite Maven ciblée.
