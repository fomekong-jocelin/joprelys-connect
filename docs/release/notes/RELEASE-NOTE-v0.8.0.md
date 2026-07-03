# RELEASE NOTE — v0.8.0

## 1. Résumé

La version `0.8.0` de Joprelys Connect enrichit les fonctionnalités cliniques et consolide la qualité de la base de code :
1. **Allergies & Antécédents Médicaux (STORY-1201)** : Suivi structuré des allergies avec badges de criticité visuelle (Tailwind CSS v4) et des antécédents médicaux/chirurgicaux/familiaux. Isolation multi-tenant et traçabilité par journal d'audit.
2. **Hospitalisations & Notes d'Évolution (STORY-1202)** : Module d'admission en hospitalisation avec sélection de chambre et lit, transmissions journalières, et libération. Gestion des accès concurrents aux lits via verrouillage optimiste JPA (`@Version`). Génération de la fiche de sortie PDF avec QR Code d'authenticité.
3. **Audit de Revue & Stabilisation QA (TICKET-0002)** : Relecture exhaustive du code selon les critères OWASP, SOLID et i18n. Résolution des conflits de contraintes de clés étrangères sur le nettoyage de base H2 et mise à niveau de la couverture des mocks de tests unitaires frontend.

## 2. Version

| Champ | Valeur |
|---|---|
| Version précédente | 0.7.0 |
| Version publiée | 0.8.0 |
| Type de bump | MINOR |
| Date | 2026-07-04 |
| Responsable release | Antigravity |

## 3. Justification SemVer

Le bump est de type **MINOR** :
- Ajout de nouvelles fonctionnalités rétrocompatibles (endpoints CRUD d'allergies, antécédents et séjours hospitaliers).
- Nouvelles tables Flyway `patient_allergies`, `patient_medical_history`, `hospitalizations` et `hospitalization_notes` additives.
- Aucun changement de contrat destructif sur les API existantes.

## 4. Tickets inclus

| Ticket | Titre | Type | Statut |
|---|---|---|---|
| [STORY-1201](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-1201-allergies-antecedents.md) | Module Allergies & Antécédents Médicaux | User Story | DONE |
| [STORY-1202](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-1202-hospitalisations.md) | Module Hospitalisations, lits et notes journalières | User Story | DONE |
| [TICKET-0002](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/TICKET-0002-checklist-pr.md) | Application de la checklist de review aux futures PR | Task | DONE |

## 5. Changements

### Added

- **Allergies & Antécédents** : Entités JPA `PatientAllergyEntity` et `PatientMedicalHistoryEntity` isolées par tenant. Contrôleur `PatientMedicalInfoController` sécurisé (rôles `MEDECIN`, `ADMIN_CLINIQUE`, `INFIRMIER`). Composant Angular `PatientMedicalInfoComponent`.
- **Hospitalisations** : Entités `HospitalizationEntity` et `HospitalizationNoteEntity` JPA avec `@Version` pour concurrence optimiste. Contrôleur `HospitalizationController`. Composant Angular `PatientHospitalizationComponent`.
- **Fiche de sortie PDF** : Méthode `generateHospitalizationDischargePdf` dans `PdfGeneratorService` avec QR code de vérification publique anonyme.

### Changed

- **Alignement Layout AppShell** : Proposer le même en-tête et pied de page partout, y compris sur la page de gestion des stocks de pharmacie et d'accès refusé, en encapsulant dans `AppShellComponent`.

### Fixed

- **Intégrité Référentielle H2** : Ajout de la suppression des dépendances (`visits`, `consultations`, `prescriptions`, `medical_documents`, `vitals`, `dispensation_items`, `lab_orders`, `patient_allergies`, `patient_medical_history`, `hospitalizations`) avant le nettoyage des patients dans les méthodes `@BeforeEach` pour éviter les échecs de test.
- **Tests unitaires Angular** : Résolution de l'erreur `BrowserXhr` et du manque de mock pour `getAllergies` dans `PatientDetailComponent` tests.

## 6. Breaking changes

- Aucun.

## 7. Migrations

- Flyway `V18__create_allergies_and_history_tables.sql` (additive).
- Flyway `V19__create_hospitalizations_tables.sql` (additive).

## 8. Tests et QA

| Vérification | Résultat | Preuve |
|---|---|---|
| Backend tests | ✅ Succès | 139 tests unitaires et d'intégration au vert (`./mvnw test`) |
| Angular build | ✅ Succès | Build Angular compilé sans erreur (`npm run build`) |
| Angular tests | ✅ Succès | 58 tests unitaires Angular passés au vert (`npm run test`) |

## 9. Déploiement

```bash
# Packaging backend
./mvnw clean package

# Build frontend
cd web && npm ci && npm run build
```

## 10. Tag Git

```bash
git tag -a v0.8.0 -m "Release v0.8.0"
git push origin v0.8.0
```

## 11. Rollback

En cas d'anomalie critique en production :
1. Revenir sur le tag précédent `v0.7.0`.
2. Déployer à nouveau les packages correspondants.
3. Les tables créées par Flyway V18/V19 sont additives et n'altèrent pas les données pré-existantes.

## 12. Risques restants

- Concurrence sur l'attribution des lits en hospitalisation : les échecs de concurrence lèveront une `ObjectOptimisticLockingFailureException` HTTP 409 Conflict, ce qui est le comportement attendu.

---

## Vérification Maven / Tailwind / Angular Material

- [x] Backend Spring Boot vérifié avec Maven uniquement.
- [x] Angular vérifié avec Tailwind CSS v4.
- [x] Aucun Angular Material introduit.
- [x] Spring Boot utilise `application.yml` et profils YAML ; aucun `application.properties`.
- [x] Angular possède `proxy.conf.json`, `proxyConfig` dans `angular.json`, et aucune URL backend hardcodée.
