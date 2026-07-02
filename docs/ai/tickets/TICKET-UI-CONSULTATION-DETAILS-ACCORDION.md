# TICKET — Affichage détaillé des consultations (accordéon en ligne avec constantes et prescriptions)

**Date** : 2026-07-02
**Mode** : Engineering / UI
**Statut** : DONE
**Version impact** : PATCH

---

## Objectif

Permettre aux médecins (dans le dossier médical du patient) et aux patients (dans leur espace personnel) de déplier chaque consultation passée sous forme d'accordéon pour afficher les constantes vitales de la visite, les détails cliniques (symptômes, examen, conseils, suivi) et la liste des médicaments prescrits (ordonnance) directement en ligne, sans obligation de télécharger le PDF.

---

## Critères d'acceptation

- [x] **Enrichissement de l'API Consultation** :
  - `ConsultationResponse` inclut un objet optionnel `vitals` avec l'ensemble des constantes vitales (température, poids, taille, pouls, tension systolique/diastolique, spo2, glycémie, fréquence respiratoire, bmi).
  - `ConsultationResponse` inclut une liste optionnelle de médicaments `prescriptionItems` (nom, dosage, posologie, durée, quantité, instructions).
- [x] **Composant IHM Accordéon (Médecin)** :
  - Dans la liste d'historique médical de `PatientDetailComponent`, chaque ligne de consultation est cliquable (avec un chevron haut/bas de statut de déploiement).
  - Une fois dépliée, elle affiche de manière esthétique les constantes vitales dans une grille compacte, les commentaires médicaux, et les détails de la prescription.
- [x] **Composant IHM Accordéon (Patient)** :
  - Même comportement dans l'espace patient `PatientDashboardComponent` pour ses propres consultations.

---

## Actions à réaliser

- [x] **Backend** :
  - [x] Mettre à jour `ConsultationResponse.java` pour inclure les enregistrements de constantes et d'items d'ordonnance.
  - [x] Injecter `PrescriptionRepository` dans `ConsultationHistoryController` et mapper les prescriptions associées.
  - [x] Adapter les tests JUnit si nécessaire.
- [x] **Frontend** :
  - [x] Mettre à jour les modèles Typescript `Consultation` et `PatientPortalConsultation`.
  - [x] Intégrer l'accordéon dans le template de `PatientDetailComponent` (médecin).
  - [x] Intégrer l'accordéon dans le template de `PatientDashboardComponent` (patient).
  - [x] S'assurer que le rendu respecte la charte graphique et s'adapte en mode responsive.
  - [x] Valider le bon fonctionnement général et la compilation.
