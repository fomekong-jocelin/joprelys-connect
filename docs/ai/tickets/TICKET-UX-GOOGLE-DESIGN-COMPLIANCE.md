# TICKET-UX-GOOGLE-DESIGN-COMPLIANCE — Conformité Complète Module Patient

Ce ticket suit la réalisation de l'ensemble des tâches nécessaires pour combler les écarts fonctionnels du module Patient par rapport au cahier des charges de Joprelys Connect.

---

## 1. Objectif & Critères d'Acceptation Globaux

Résoudre les écarts identifiés dans les modules 3 (Identité unique), 4 (Dossier partagé), 12 (Consentement) et 13 (Accès externe) :
1. Détecter automatiquement les doublons potentiels (score >= 85% Nom + Date de Naissance identique).
2. Permettre à un administrateur de clinique de fusionner manuellement deux doublons en réaffectant l'historique et désactivant le doublon.
3. Rendre le numéro de téléphone optionnel lors de la création.
4. Permettre la génération et le téléchargement d'un PDF d'une page de synthèse médicale d'urgence.
5. Permettre la restriction granulaire par scopes (consultations, ordonnances, biologie, antécédents) pour les consentements et demandes d'accès externes.
6. Enregistrer le canal de validation du consentement.

---

## 2. Découpage des Tâches (Backlog & Statut)

- [ ] **Tâche 1 : Rendre le téléphone optionnel (Backend & Frontend)**
  - Retirer la validation `@NotBlank` ou obligatoire sur le champ `phone` dans `PatientCreateDto` et l'IHM.
  - Estimation : **1 SP** / 0.1j senior.
  - Test attendu : Création réussie via POST `/api/patients` sans téléphone.

- [ ] **Tâche 2 : Service de détection de doublons (Backend)**
  - Écrire la migration Flyway `V21__patient_duplicates_schema.sql`.
  - Implémenter le service `PatientSimilarityService` (algorithme Levenshtein).
  - Enregistrer automatiquement les candidats aux doublons lors de la création de patient.
  - Estimation : **5 SP** / 0.5j senior.
  - Test attendu : Un patient nommé "Dupont Jean" né le 10/10/1980 déclenche une détection si "Dupond Jean" né le 10/10/1980 existe.

- [ ] **Tâche 3 : API et Logique transactionnelle de fusion (Backend)**
  - Implémenter `PatientService.mergePatients(primaryId, secondaryId, actorId)`.
  - Réaffecter toutes les tables associées et désactiver le patient secondaire (`status` = 'MERGED').
  - Enregistrer la fusion dans `patient_merged_history` et auditer l'action.
  - Estimation : **5 SP** / 0.5j senior.
  - Test attendu : Intégrité référentielle après fusion (les visites du secondaire pointent sur le principal).

- [ ] **Tâche 4 : IHM d'administration des doublons et assistant de fusion (Frontend)**
  - Page `/clinic/duplicates` listant les doublons avec assistant visuel de fusion côte à côte.
  - Estimation : **3 SP** / 0.3j senior.
  - Test attendu : Comparaison et fusion validée par clic.

- [ ] **Tâche 5 : Fiche de synthèse médicale en PDF (Backend & Frontend)**
  - Endpoint `GET /api/patients/{id}/summary-pdf` générant un document condensé sur 1 page A4 (allergies, antécédents, constantes récentes).
  - Bouton de téléchargement Angular avec icône premium SVG dans le header de détails patient.
  - Estimation : **3 SP** / 0.3j senior.
  - Test attendu : Téléchargement du PDF avec QR Code.

- [ ] **Tâche 6 : Scopes granulaires de consentements & canal de validation (Backend & Frontend)**
  - Migration SQL : Ajout de `scopes` et `validation_channel` dans `patient_consents`.
  - Validation des scopes dans les services et endpoints cliniques (403 si scope manquant).
  - Formulaire Angular dans le portail patient avec checkboxes de scopes et affichage du canal.
  - Estimation : **3 SP** / 0.3j senior.
  - Test attendu : Blocage des ordonnances si le scope prescriptions est révoqué.

- [ ] **Tâche 7 : Scopes granulaires pour les demandes d'accès externes (Backend & Frontend)**
  - Migration SQL : Ajout de `scopes` dans `external_access_requests`.
  - Validation du scope lors de l'accès clinique externe.
  - Formulaire Angular dans le portail pour valider la demande d'accès avec checkboxes de scopes.
  - Estimation : **2 SP** / 0.2j senior.
  - Test attendu : Validation réussie avec scopes restreints.

---

## 3. Informations de Delivery
* **Epic concerné** : EPIC-0013 (Conformité Module Patient)
* **Sprint** : SPRINT-0009
* **Reviewer** : Lead Developer
* **Estimations Totales** : **22 Story Points** / **2.2j** (Est. Senior)
* **Definition of Ready (DoR)** : Validée.
* **Definition of Done (DoD)** :
  * [ ] Migrations SQL Flyway exécutées sans erreur.
  * [ ] Compilation backend Maven et frontend Angular réussie.
  * [ ] Tests unitaires et d'intégration validés.
  * [ ] Documentation spec et technique à jour.
  * [ ] Suivi delivery et changelog mis à jour.
