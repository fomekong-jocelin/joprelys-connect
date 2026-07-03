# TICKET — Cadrage & Architecture : Modules Laboratoire et Pharmacie

**Date** : 2026-07-03  
**Mode** : Architecture / Project Manager  
**Statut** : READY  
**Priorité** : P1  
**Sprint cible** : SPRINT-0004  
**Responsable** : Antigravity  

---

## 1. Objectif

Établir le cadrage fonctionnel, l'architecture technique cible (Abstractions, Modèles de données, API, Sécurité) et le découpage en Epics / Stories pour les deux nouveaux modules métiers conformément au cahier des charges :
1. **Module Laboratoire (lab-integration + portail laboratoire)** : Permettre le raccordement, la consultation des examens biologiques au sein du DPU et les écrans dédiés au laboratoire/biologiste (Modules 8, 9 et section 13.3 du Cahier des Charges).
2. **Module Pharmacie (pharmacy-dispensation)** : Gérer la délivrance sécurisée des prescriptions par les pharmacies partenaires externes (Module 7 du Cahier des Charges).

### Alignement strict avec le Cahier des Charges

Le Cahier des Charges prévoit explicitement :

- un **Portail laboratoire** : tableau de bord labo, demandes reçues, détail demande, saisie résultat, validation résultat, envoi PDF, historique résultats ;
- un **Portail pharmacie** : vérification ordonnance, détail ordonnance, disponibilité médicaments, délivrance partielle/totale, historique délivrances.

Les stories déjà terminées de l'`EPIC-0009` couvrent l'intégration laboratoire API et l'affichage praticien, mais ne couvrent pas encore le portail laboratoire complet. L'epic ne doit donc plus être considéré comme entièrement terminé tant que les stories `STORY-0904` à `STORY-0906` ne sont pas réalisées.

### Référence UI obligatoire

Les écrans laboratoire et pharmacie doivent reprendre les améliorations du portail patient :

- réduire les marges gauche/droite sur desktop avec un conteneur plus large et plus utile ;
- éviter les cartes trop isolées et les espacements excessifs ;
- harmoniser les couleurs avec `DESIGN.md` et les dernières vues patient ;
- conserver une hiérarchie dense, lisible et professionnelle ;
- respecter les rayons sobres : 4px à 6px recommandés, 8px maximum sans ADR ;
- utiliser Tailwind CSS v4, composants Angular internes, i18n FR/EN, light/dark et configuration centralisée.

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
* **STORY-0904** : Portail laboratoire — tableau de bord et demandes reçues
  * *Description* : Écran laboratoire listant les demandes reçues, les statuts et les priorités selon les droits du biologiste/laboratoire.
  * *Est.* : 2 SP (Intermédiaire)
* **STORY-0905** : Portail laboratoire — détail demande et changement de statut
  * *Description* : Écran de détail permettant de consulter la demande, le patient minimal nécessaire et de faire évoluer le statut selon les droits.
  * *Est.* : 2 SP (Intermédiaire)
* **STORY-0906** : Portail laboratoire — saisie, validation résultat, PDF et historique
  * *Description* : Écran de saisie/validation des résultats structurés, dépôt PDF et historique des résultats.
  * *Est.* : 3 SP (Senior)

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
* **STORY-1004** : Portail pharmacie — vérification et détail ordonnance
  * *Description* : Écran pharmacie de saisie/sc scan du numéro d'ordonnance et PIN, puis affichage du détail strictement nécessaire à la délivrance.
  * *Est.* : 2.5 SP (Intermédiaire)
* **STORY-1005** : Portail pharmacie — délivrance et historique
  * *Description* : Écran de délivrance partielle/totale, disponibilité déclarative des médicaments et historique des délivrances.
  * *Est.* : 2.5 SP (Intermédiaire)

---

## 3. Action plan de cadrage

- [x] Rédiger la spécification fonctionnelle minimale de l'intégration laboratoire (`docs/features/lab-integration/FUNCTIONAL-SPEC.md`) alignée sur le CDC.
- [x] Rédiger l'architecture technique minimale de l'intégration laboratoire (`docs/features/lab-integration/TECHNICAL-DESIGN.md`) alignée sur le CDC.
- [x] Rédiger la spécification fonctionnelle minimale de la dispensation en pharmacie (`docs/features/pharmacy-dispensation/FUNCTIONAL-SPEC.md`) alignée sur le CDC.
- [x] Rédiger l'architecture technique minimale de la dispensation en pharmacie (`docs/features/pharmacy-dispensation/TECHNICAL-DESIGN.md`) alignée sur le CDC.
- [x] Ajouter le complément d'alignement CDC pour les écrans portail laboratoire et pharmacie.
- [x] Ajouter les règles UI issues du dashboard patient : réduction des marges, densité, harmonisation des couleurs.
- [ ] Soumettre le cadrage pour validation technique et fonctionnelle.
- [x] Ajouter les Epics et Stories dans `PROJECT-TRACKING.md` à l'état `TODO`/`BACKLOG`.
