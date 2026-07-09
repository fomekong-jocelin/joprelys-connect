# Spécifications Fonctionnelles : EPIC-0016 — Consolidation V2.1 du Socle Clinique (Urgences & Accueil)

## 1. Contexte & Objectif
Suite à la mise à jour du Cahier des Charges en V2.1 (intégration des réalités du Trauma Center), nous devons consolider les modules cliniques existants (Accueil, Constantes, Consultations) avant de développer la facturation ou la comptabilité.

L'objectif de cet Epic est d'implémenter les exigences du **Module 4-bis (Urgences & Réanimation)** et du **Module 5-bis (Circuit Patient Complet - Accueil)**.

## 2. Périmètre (Ce qui manque au code actuel)

### 2.1 Module Accueil (Module 5-bis)
Le système actuel ne gère que les visites médicales standard. Nous devons ajouter le registre d'accueil exhaustif :
- Enregistrement d'un **visiteur** pour un patient hospitalisé (avec contrôle des horaires 06h-08h, 12h-14h, 18h-20h).
- Création d'une **demande d'audience** non médicale (ex: pour rencontrer le médecin chef ou la direction).

### 2.2 Module Urgences & Réanimation (Module 4-bis)
La prise de constantes actuelle est trop basique pour un Trauma Center. Nous devons ajouter :
- Le **triage** d'urgence (Rouge, Orange, Vert).
- Le **mode d'arrivée** (Ambulance, Sapeurs-pompiers, Non médicalisé).
- La **fiche de réanimation** :
  - Accès vasculaires (voies veineuses périphériques).
  - Remplissage vasculaire (horodatage des bolus de fluides).
  - Administration des médicaments d'urgence.
  - Constantes de sortie d'urgence (état stabilisé).

## 3. Critères d'acceptation généraux
- Un agent d'accueil peut enregistrer un visiteur avec sa pièce d'identité.
- Un médecin/infirmier peut ouvrir un dossier "Urgence" avec un clic rapide, sans avoir besoin du dossier complet du patient dans l'immédiat.
- Le module d'urgence permet de tracer chaque soluté (Ringer, NaCl) injecté lors de la réanimation avec l'heure exacte.

## 4. Dépendances & Risques
- Nécessite d'étendre la table `visits` ou de créer des entités `reception_logs` et `emergencies`.
- L'interface d'urgence doit être "Mobile First" ou très rapide à utiliser sur tablette pour l'équipe de réanimation.

## 5. Prochaines étapes
- Rédiger le `DATA-MODEL.md` pour concevoir les entités SQL de ces ajouts.
- Découper cet Epic en User Stories.
