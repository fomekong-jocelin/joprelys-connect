# TICKET-PRESCRIPTION-SEPARATION-AND-CONSULTATION-FIX — Résolution des erreurs de consultation et affichage/téléchargement séparé des ordonnances

## 1. Objectif

Ce ticket vise à corriger deux problèmes importants soulevés par le médecin :
1. **Erreur 500 au démarrage de la consultation** : Correction de la `LazyInitializationException` provoquée par le chargement différé (lazy) du patient et des constantes vitales dans l'endpoint `GET /api/visits/{id}`.
2. **Gestion séparée de l'ordonnance dans le dossier patient** : Permettre au médecin de consulter le numéro d'ordonnance et son code PIN de validation de manière distincte dans l'onglet des consultations, et de télécharger spécifiquement le PDF de l'ordonnance (type `ORDONNANCE`) plutôt que la fiche de consultation complète (type `COMPTE_RENDU_CONSULTATION`).
3. **Séparation de l'ordonnance côté Patient** : Étendre cette fonctionnalité au portail patient afin que le patient puisse également visualiser son code PIN de validation, son numéro d'ordonnance et télécharger directement son ordonnance PDF (type `ORDONNANCE`) séparément du compte-rendu.

## 2. Critères d'acceptation

- [x] L'appel `GET /api/visits/{id}` ne retourne plus une erreur 500 et charge correctement la visite et ses informations.
- [x] Dans l'IHM du dossier patient (vue médecin, onglet "Dossier Médical" / consultations), chaque consultation finalisée contenant des médicaments affiche :
  - Le numéro d'ordonnance officiel.
  - Le code PIN de validation à transmettre au patient pour la pharmacie.
  - Un bouton "Télécharger l'ordonnance" qui récupère uniquement le PDF de l'ordonnance (type `ORDONNANCE`).
  - Un bouton "Télécharger la fiche de consultation" (existant) qui télécharge la fiche de consultation complète.
- [x] Dans le portail patient (vue patient, onglet "Ordonnances"), chaque consultation finalisée contenant des médicaments affiche :
  - Le numéro d'ordonnance et son code PIN de validation.
  - Un bouton "Télécharger Ordonnance (PDF)" qui télécharge spécifiquement le document de type `ORDONNANCE`.
- [x] Un endpoint sécurisé `GET /api/patient/documents/{documentId}/download` est disponible pour le patient afin de lui permettre de télécharger son document PDF après validation stricte qu'il lui appartient.
- [x] La compilation du backend via Maven et du frontend Angular via `npm run build` s'effectue sans aucune erreur.
- [x] L'ensemble des tests unitaires backend et frontend passe avec succès.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0014 |
| User story parent | STORY-1910 |
| Sprint cible | SPRINT-0011 |
| Priorité business | P0 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.25j |
| Effort estimé intermédiaire | 0.4j |
| Effort estimé junior | 0.6j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé :
  - `backend/src/main/java/com/joprelys/backend/visit/application/VisitService.java`
  - `backend/src/main/java/com/joprelys/backend/visit/infrastructure/persistence/VisitEntity.java`
  - `backend/src/main/java/com/joprelys/backend/consultation/api/ConsultationResponse.java`
  - `backend/src/main/java/com/joprelys/backend/consultation/api/ConsultationController.java`
  - `backend/src/main/java/com/joprelys/backend/consultation/application/ConsultationService.java`
  - `backend/src/main/java/com/joprelys/backend/patient/api/PatientPortalController.java`
  - `backend/src/main/java/com/joprelys/backend/patient/api/PatientPortalMeResponse.java`
  - `web/src/app/consultation/consultation.models.ts`
  - `web/src/app/patient/detail/patient-consultations-tab.component.ts`
  - `web/src/app/patient/portal/services/patient-portal.service.ts`
  - `web/src/app/patient/portal/components/patient-visits-list.component.ts`
  - `web/src/app/patient/portal/pages/patient-prescriptions-page.component.ts`
- [x] Tests unitaires et d'intégration validés

## 5. Hypothèses

- En forçant l'initialisation des entités associées `Patient` et `Vitals` dans la transaction JPA de `VisitService.getVisit(id)`, l'erreur `LazyInitializationException` (qui cause l'erreur HTTP 500) sera définitivement résolue.
- L'exposition des attributs `prescriptionDocumentId` et `pinCode` aux DTO médecin et patient permet aux deux côtés de proposer l'affichage des informations et de télécharger le PDF séparément.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Régression sur le mapping DTO | Risque d'erreur de compilation sur d'autres contrôleurs ou tests s'il manque des paramètres aux constructeurs | Utiliser des méthodes statiques surchargées et des valeurs par défaut pour les nouveaux paramètres. |

## 7. Action plan

- [x] **Correction de l'Erreur 500** :
  - Modifier `VisitService.getVisit(id)` pour forcer l'accès/initialisation des propriétés lazy `patient` et `vitals`.
- [x] **Enrichissement de la Réponse de Consultation (Médecin)** :
  - Ajouter `UUID prescriptionDocumentId` et `String pinCode` à `ConsultationResponse` (backend).
  - Modifier le constructeur et la méthode `fromEntity` principale pour peupler ces nouveaux champs depuis l'entité `PrescriptionEntity`.
  - Ajouter une méthode de service `getDetailedConsultationByVisitId(UUID visitId)` dans `ConsultationService` pour retourner un DTO de consultation complet.
  - Modifier `ConsultationController.getConsultation(id)` pour appeler cette nouvelle méthode.
- [x] **Enrichissement de la Réponse Portail Patient (Patient)** :
  - Ajouter `UUID prescriptionDocumentId` et `String pinCode` à `PatientPortalMeResponse.PatientPortalConsultation` (backend).
  - Mettre à jour la logique de mapping dans `PatientPortalController.getMe` pour peupler ces champs.
  - Ajouter un endpoint sécurisé `GET /api/patient/documents/{documentId}/download` vérifiant l'appartenance du document.
- [x] **Mise à jour du Frontend Angular** :
  - Ajouter les champs `prescriptionDocumentId` et `pinCode` à l'interface `Consultation` dans `consultation.models.ts` et `PatientPortalConsultation` dans `patient-portal.service.ts`.
  - Modifier le template et la logique de `patient-consultations-tab.component.ts` (Médecin) et `patient-visits-list.component.ts` (Patient) pour afficher le code PIN, le numéro d'ordonnance et ajouter le bouton de téléchargement de l'ordonnance PDF.
- [x] **Internationalisation** :
  - Ajouter les clés de traduction nécessaires dans `fr.json` et `en.json`.
- [x] **Validation technique** :
  - Exécuter `./mvnw test` pour vérifier le backend.
  - Exécuter `npm run test` pour vérifier le frontend.
  - Compiler l'application Angular de production.
- [x] **Mises à jour réglementaires** :
  - Mettre à jour `docs/ai/PROJECT-TRACKING.md` et `docs/ai/CHANGELOG.md`.

## 8. Implémentation réalisée

- **Backend** :
  - Dans [VisitService.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/visit/application/VisitService.java), chargement explicite de `patient.getFullName()`, `patient.getGlobalPatientNumber()` et `vitals.getTemperature()` au sein de `getVisit()` pour éviter toute `LazyInitializationException`.
  - Dans [ConsultationResponse.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/consultation/api/ConsultationResponse.java), ajout des attributs `prescriptionDocumentId` et `pinCode` et mise à jour du mapping.
  - Dans [ConsultationService.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/consultation/application/ConsultationService.java), implémentation de `getDetailedConsultationByVisitId()`.
  - Dans [ConsultationController.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/consultation/api/ConsultationController.java), appel de la méthode détaillée dans l'endpoint `/api/visits/{id}/consultation`.
  - Dans [PatientPortalMeResponse.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/patient/api/PatientPortalMeResponse.java) et [PatientPortalController.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/patient/api/PatientPortalController.java), ajout et mapping de `prescriptionDocumentId` et `pinCode`.
  - Dans [PatientPortalController.java](file:///C:/MES-APPLICATIONS/joprelys-connect/backend/src/main/java/com/joprelys/backend/patient/api/PatientPortalController.java), ajout de l'endpoint sécurisé `GET /api/patient/documents/{documentId}/download`.
- **Frontend** :
  - Dans [consultation.models.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/consultation/consultation.models.ts) et [patient-portal.service.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/patient/portal/services/patient-portal.service.ts), ajout des clés `prescriptionDocumentId` et `pinCode`.
  - Dans [patient-consultations-tab.component.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/patient/detail/patient-consultations-tab.component.ts) (Médecin) et [patient-visits-list.component.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/patient/portal/components/patient-visits-list.component.ts) (Patient), affichage du numéro d'ordonnance/code PIN et ajout du bouton de téléchargement de l'ordonnance PDF.
  - Dans [patient-prescriptions-page.component.ts](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/app/patient/portal/pages/patient-prescriptions-page.component.ts), liaison de l'événement et implémentation de `onDownloadPrescription()`.
  - Ajout des traductions i18n correspondantes dans `fr.json` et `en.json`.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-06 | Antigravity | 0.4j | 100% | Aucun | Aucun | Implémentations médecin et patient terminées. Tests unitaires et builds OK. |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Backend unit tests
cd backend && ./mvnw test

# Frontend unit tests
cd web && npm run test -- --watch=false

# Frontend production build
cd web && npm run build
```

### Résultats

- [x] Tests unitaires backend : OK (`BUILD SUCCESS`, 240/240 tests passés avec succès).
- [x] Tests unitaires frontend : OK (79/79 tests passés avec succès).
- [x] Build de production : OK.

## 11. Documentation

- [x] Changelog mis à jour
- [x] Suivi projet mis à jour

## 12. Reste à faire

- Aucun

## 13. Statut final

Statut : DONE

## 14. Notes finales

L'initialisation des relations JPA a résolu l'erreur 500. La dissociation claire entre la Fiche de Consultation (compte-rendu) et l'Ordonnance (pour la pharmacie, avec le code PIN de validation) répond entièrement aux besoins du médecin et est maintenant disponible côté patient.

## 15. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout d'une fonctionnalité de téléchargement d'ordonnance et affichage du code PIN + correction de bug de consultation. |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Oui (Ajout de champs optionnels dans la réponse Consultation et portail patient) |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |

## 16. Impact thème / i18n / branding

- [x] Clés i18n de dossier et d'ordonnance ajoutées dans les dictionnaires FR/EN.
- [x] Intégration visuelle en conformité avec le design system (arrondis et contrastes respectés).

## 17. Verification `.gitignore`

- [x] `.gitignore` présent à la racine
