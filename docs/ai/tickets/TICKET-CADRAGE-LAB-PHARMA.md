# TICKET — Cadrage & Architecture : Modules Laboratoire et Pharmacie

**Date** : 2026-07-03  
**Mode** : Architecture / Project Manager  
**Statut** : TODO  
**Priorité** : P1  
**Sprint cible** : SPRINT-0004  
**Responsable** : Antigravity  

---

## 1. Objectif

Établir le cadrage fonctionnel, l'architecture technique cible (Abstractions, Modèles de données, API, Sécurité) et le découpage en Epics / Stories pour les deux nouveaux modules métiers :
1. **Module Laboratoire (lab-integration)** : Permettre le raccordement et la consultation des examens biologiques au sein du DPU.
2. **Module Pharmacie (pharmacy-dispensation)** : Gérer la délivrance sécurisée des prescriptions par les pharmacies partenaires externes.

---

## 2. Découpage en Epics & Stories

### EPIC-0009 — Intégration Laboratoire & Examens Biologiques
* **STORY-0901** : Demande d'examens biologiques (médecin)  
  * *Description* : Un médecin peut prescrire un bilan ou examen biologique pour un patient lors de sa consultation.
  * *Est.* : 1.5 SP (Intermédiaire)
* **STORY-0902** : API d'intégration labo externe pour téléversement  
  * *Description* : Endpoint sécurisé permettant à un automate ou système de laboratoire partenaire d'envoyer les résultats (valeurs structurées + PDF).
  * *Est.* : 3 SP (Senior)
* **STORY-0903** : Écran praticien de visualisation des résultats  
  * *Description* : Onglet d'historique biologique dans le DPU avec courbe d'évolution des marqueurs clés.
  * *Est.* : 2 SP (Intermédiaire)

### EPIC-0010 — Dispensation en Pharmacie & Gestion des Prescriptions
* **STORY-1001** : API de récupération sécurisée d'ordonnance  
  * *Description* : Endpoint pour les pharmaciens externes permettant de lire et valider une ordonnance à l'aide d'un jeton à usage unique.
  * *Est.* : 2 SP (Intermédiaire)
* **STORY-1002** : Enregistrement de dispensation  
  * *Description* : Possibilité de marquer les médicaments d'une ordonnance comme dispensés (totalement, partiellement, ou substitution).
  * *Est.* : 3 SP (Senior)
* **STORY-1003** : Traçabilité & Statut ordonnance  
  * *Description* : Suivi en temps réel de l'état de l'ordonnance ("Servie", "Partiellement servie") pour le patient et le médecin, avec log d'audit.
  * *Est.* : 2 SP (Intermédiaire)

---

## 3. Action plan de cadrage

- [x] Rédiger la spécification fonctionnelle minimale de l'intégration laboratoire (`docs/features/lab-integration/FUNCTIONAL-SPEC.md`).
- [x] Rédiger l'architecture technique minimale de l'intégration laboratoire (`docs/features/lab-integration/TECHNICAL-DESIGN.md`).
- [x] Rédiger la spécification fonctionnelle minimale de la dispensation en pharmacie (`docs/features/pharmacy-dispensation/FUNCTIONAL-SPEC.md`).
- [x] Rédiger l'architecture technique minimale de la dispensation en pharmacie (`docs/features/pharmacy-dispensation/TECHNICAL-DESIGN.md`).
- [ ] Soumettre le cadrage pour validation technique et fonctionnelle.
- [ ] Ajouter les Epics et Stories dans `PROJECT-TRACKING.md` à l'état `TODO`/`BACKLOG`.
