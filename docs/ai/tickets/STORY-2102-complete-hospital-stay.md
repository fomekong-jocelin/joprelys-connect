# STORY-2102 — Séjour hospitalier complet et documents d'entrée/sortie

> Ticket pour la complétion du séjour hospitalier avec documents d'entrée/sortie, sortie contre avis médical et consentements opératoires.

## 1. Objectif

Permettre la gestion complète d'un séjour hospitalier dans le backend et le frontend en ajoutant :
- Le billet d'entrée d'hospitalisation au format PDF.
- La possibilité de déclarer une sortie contre avis médical avec génération d'une fiche de sortie spécifique et audit dédié.
- La signature et le rattachement de consentements opératoires (anesthésie / opération) liés au séjour.

## 2. Critères d'acceptation

- [x] L'utilisateur peut générer et télécharger un billet d'entrée PDF dès l'admission du patient.
- [x] L'utilisateur peut déclarer une sortie contre avis médical, ce qui met à jour le statut du séjour à `SORTI_CONTRE_AVIS` et génère un PDF spécifique de "fiche de sortie contre avis médical".
- [x] Une entrée d'audit spécifique est enregistrée lors d'une sortie contre avis médical.
- [x] L'utilisateur peut créer et rattacher des consentements opératoires (type ANESTHESIE ou CHIRURGIE, signature patient et témoin optionnel) rattachés à une hospitalisation active.
- [x] Des tests unitaires et d'intégration valident ces nouveaux flux sur le backend.
- [x] Les nouveaux endpoints et actions sont intégrés dans l'interface Angular, respectant le design système et la limite de taille des composants (< 500 lignes).

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0017 — Complétion Hospitalisation, Facturation et Caisse |
| User story parent | STORY-2102 |
| Sprint cible | SPRINT-0012 |
| Priorité business | P0 |
| Complexité | M |
| Story points | 5 |
| Profil recommandé | Senior Full-stack |
| Effort estimé senior | 1.5j |
| Effort estimé intermédiaire | 2.2j |
| Effort estimé junior | 3.5j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer + Médecin Chef |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | STORY-2101 |
| Bloquants connus | Aucun |

## 4. Action plan

- [x] Créer la migration de base de données `V46__hospitalization_complete_stay_tables.sql`.
- [x] Créer l'entité `SurgicalConsentEntity`, son repository, ses requêtes de création et sa réponse.
- [x] Modifier `HospitalizationEntity` pour accepter le statut `SORTI_CONTRE_AVIS` et ajouter les champs nécessaires ou adapter le flow.
- [x] Mettre à jour `PdfGeneratorService` pour :
  - Générer le Billet d'entrée PDF (`generateHospitalizationEntryPdf`).
  - Générer le PDF spécifique de sortie contre avis médical (`generateHospitalizationDischargeAgainstAdvicePdf` ou adapter `generateHospitalizationDischargePdf`).
- [x] Mettre à jour `HospitalizationService` pour :
  - Générer/télécharger le billet d'entrée.
  - Supporter la sortie contre avis médical via un paramètre optionnel ou un nouvel endpoint.
  - Gérer les consentements opératoires (création, liste, liaison au séjour).
- [x] Mettre à jour `HospitalizationController` pour exposer ces nouveaux endpoints.
- [x] Ajouter des tests unitaires/d'intégration dans le backend.
- [x] Mettre à jour l'interface Angular (composants et services) pour intégrer ces fonctionnalités.
- [x] Exécuter les builds et tests backend et frontend.
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## 5. Livrables

| Fichier | Type | Statut |
|---|---|---|
| `V46__hospitalization_complete_stay_tables.sql` | Migration SQL | DONE |
| `SurgicalConsentEntity.java` | Entité JPA | DONE |
| `SurgicalConsentRepository.java` | Repository | DONE |
| `HospitalizationService.java` | Service | DONE |
| `HospitalizationController.java` | Controller | DONE |
| `PdfGeneratorService.java` | Service PDF | DONE |
| Composants/Services Angular | Code frontend | DONE |

## 13. Statut final

Statut : **DONE**

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout de fonctionnalités rétrocompatibles pour le séjour hospitalier |
| Breaking change | Non |
| Migration DB | Oui |
| Changement API | Oui |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Oui |
