# Ticket : Analyse d'alignement du CDC V2 avec le projet Trauma Center

## Description
Analyse par le PO et Lead Dev du document `Cahier_des_charges_Joprelys_Connect_V2.md` par rapport aux spécifications réelles présentes dans `doc_reel_trauma_center`.

## Actions réalisées
- [x] Lecture de `doc_reel_trauma_center/SPECIFICATION_LOGICIEL_GESTION_CLINIQUE_TC2CDK.md`
- [x] Lecture de `Cahier_des_charges_Joprelys_Connect_V2.md`
- [x] Comparaison de l'alignement métier et technique.
- [x] Identification des manques dans le modèle de données (Comptabilité, Rendez-vous, Lits, Conventions).

## Prochaines étapes
- [ ] Mettre à jour le `Cahier_des_charges_Joprelys_Connect_V2.md` pour inclure les tables manquantes.
- [ ] Découper en Epics / Stories selon la méthodologie PM.
- [ ] Créer les documentations (FUNCTIONAL-SPEC, TECHNICAL-DESIGN) du premier Epic.

## Résultat de l'analyse
L'alignement global est excellent (95%), mais le modèle de données (Section 9 du CDC) est incomplet pour supporter les nouveaux modules OHADA, RH, et Logistique. De plus, la gestion spatiale (lits/chambres) et la gestion des assurances/conventions doivent être ajoutées pour être fidèle à la réalité d'une clinique.
