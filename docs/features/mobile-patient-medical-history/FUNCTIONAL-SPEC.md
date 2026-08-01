# Spécifications Fonctionnelles — Historique Médical & Chronologie des Visites Patient (MOB-2810)

## 1. Objectif
Fournir aux praticiens de santé sur l'application mobile Joprelys Connect une vue synthétique et chronologique complète du dossier du patient sélectionné : antécédents médicaux/chirurgicaux, allergies déclarées, traitements chroniques et l'historique de l'ensemble des consultations et visites passées avec leurs constantes et notes SOAP associés.

## 2. Cas d'usage principaux
- **UC-01** : Consultation des synthèses d'antécédents (Médicaux, Chirurgicaux, Familiaux) et des allergies enregistrées.
- **UC-02** : Visualisation de la chronologie temporelle des visites passées (visites urgences, consultations externes, hospitalisations).
- **UC-03** : Inspection détaillée d'une visite passée (constantes saisies, notes cliniques SOAP, médecin référant).
- **UC-04** : Recherche et filtrage rapide par date ou type de soin.

## 3. Exigences d'interface & UX (Design System Joprelys)
- Découpage en onglets ou sections rétractables (Antécédents / Chronologie des Visites).
- Badges de gravité des allergies (Faible, Modérée, Sévère).
- Coins sobres (4px à 8px max) et cartes épurées sans accentuation latérale.
- Prise en charge 100% i18n FR/EN et thèmes Clair (`#F8FAFC`) / Sombre (`#071124`).
