# TICKET — Cadrage & Architecture : Modules Laboratoire et Pharmacie

**Date** : 2026-07-03  
**Mode** : Architecture / Project Manager  
**Statut** : TODO  
**Priorité** : P1  
**Sprint cible** : SPRINT-0004  
**Responsable** : Antigravity  

---

## 1. Objectif

Établir le cadrage fonctionnel, l'architecture technique cible (Abstractions, Modèles de données, API, Sécurité) et le découpage en Epics / Stories pour les deux nouveaux modules métiers conformément au cahier des charges :
1. **Module Laboratoire (lab-integration)** : Permettre le raccordement et la consultation des examens biologiques au sein du DPU (Module 8 & 9 du Cahier des Charges).
2. **Module Pharmacie (pharmacy-dispensation)** : Gérer la délivrance sécurisée des prescriptions par les pharmacies partenaires externes (Module 7 du Cahier des Charges).

---

## 2. Découpage en Epics & Stories

### EPIC-0009 — Intégration Laboratoire & Examens Biologiques (Module 8 & 9)
* **STORY-0901** : Demande d'examens biologiques (médecin)  
  * *Description* : Un médecin peut prescrire un bilan ou examen biologique pour un patient lors de sa consultation (statut initial `REQUESTED`).
  * *Est.* : 1.5 SP (Intermédiaire)
* **STORY-0902** : API d'intégration labo externe pour téléversement  
  * *Description* : Endpoint sécurisé avec clé API (`X-API-KEY`) permettant de téléverser les résultats (valeurs structurées + PDF) et de passer le statut à `VALIDATED`.
  * *Est.* : 3 SP (Senior)
* **STORY-0903** : Écran praticien de visualisation des résultats  
  * *Description* : Onglet d'historique biologique dans le DPU avec courbe d'évolution des marqueurs clés et affichage des alertes d'interprétation (`ELEVÉ`, `BAS`, `CRITIQUE`).
  * *Est.* : 2.5 SP (Intermédiaire)

### EPIC-0010 — Dispensation en Pharmacie & Gestion des Prescriptions (Module 7)
* **STORY-1001** : API de récupération sécurisée d'ordonnance  
  * *Description* : Endpoint pour les pharmaciens externes permettant de lire une ordonnance à l'aide de son ID unique (`prescription_number`) et d'un code PIN à 4 chiffres.
  * *Est.* : 2 SP (Intermédiaire)
* **STORY-1002** : Enregistrement de dispensation  
  * *Description* : Possibilité d'enregistrer la délivrance des médicaments et de passer le statut de l'ordonnance à `PARTIALLY_DISPENSED` ou `FULLY_DISPENSED`.
  * *Est.* : 3 SP (Senior)
* **STORY-1003** : Traçabilité & Statut ordonnance  
  * *Description* : Suivi en temps réel de l'état de l'ordonnance pour le patient et le médecin, avec log d'audit de délivrance et blocage de double dispensation.
  * *Est.* : 2 SP (Intermédiaire)

---

## 3. Action plan de cadrage

- [x] Rédiger la spécification fonctionnelle minimale de l'intégration laboratoire (`docs/features/lab-integration/FUNCTIONAL-SPEC.md`) alignée sur le CDC.
- [x] Rédiger l'architecture technique minimale de l'intégration laboratoire (`docs/features/lab-integration/TECHNICAL-DESIGN.md`) alignée sur le CDC.
- [x] Rédiger la spécification fonctionnelle minimale de la dispensation en pharmacie (`docs/features/pharmacy-dispensation/FUNCTIONAL-SPEC.md`) alignée sur le CDC.
- [x] Rédiger l'architecture technique minimale de la dispensation en pharmacie (`docs/features/pharmacy-dispensation/TECHNICAL-DESIGN.md`) alignée sur le CDC.
- [ ] Soumettre le cadrage pour validation technique et fonctionnelle.
- [x] Ajouter les Epics et Stories dans `PROJECT-TRACKING.md` à l'état `TODO`/`BACKLOG`.
