# Spécification Fonctionnelle — Gestion des Visites, Constantes Vitales & IMC (EPIC-0004)

## 1. Problème métier

Lorsqu'un patient arrive dans une clinique pilote, il doit être admis pour qu'un professionnel de santé puisse s'occuper de lui. Cette admission lance une **visite clinique**. Elle permet :
* De tracer le temps d'attente et de traitement du patient.
* D'orienter le patient vers le bon service ou professionnel de santé.
* D'établir une file d'attente active visible par les cliniciens (infirmiers, médecins) pour le tri et la consultation.
* D'enregistrer les constantes vitales de tri initial du patient pour aider au diagnostic du médecin et suivre l'évolution de son état physique.

## 2. Objectif

Permettre aux agents d'accueil et infirmiers d'ouvrir une visite clinique pour un patient, de définir son motif de visite, de l'orienter, d'enregistrer et valider ses constantes vitales (avec calcul de l'IMC automatique), et de lister les visites actives au sein d'une file d'attente globale partagée. Permettre aux médecins de clôturer la visite à la fin de la prise en charge.

## 3. Rôles et Autorisations

* **`AGENT_ACCUEIL` / `INFIRMIER` / `MEDECIN` / `ADMIN_CLINIQUE`** :
  * Ouvrir une visite pour un patient existant de sa clinique.
  * Saisir et modifier les constantes vitales d'une visite active.
  * Consulter la file d'attente active (`EN_COURS`) de la clinique.
* **`MEDECIN` / `ADMIN_CLINIQUE`** :
  * Clôturer une visite en cours.

## 4. Parcours Utilisateur

### 4.1 Ouverture de Visite (Admission)
1. L'utilisateur recherche et sélectionne un patient pour accéder à sa fiche détaillée.
2. Sur la fiche détaillée, il clique sur le bouton **"Ouvrir une visite"**.
3. Un formulaire s'affiche demandant le motif de la visite et l'orientation.
4. À la validation, la visite est créée avec le statut initial `EN_COURS`.

### 4.2 Saisie des Constantes Vitales (Tri)
1. Depuis le tableau de bord de la file d'attente active, l'utilisateur clique sur l'action **"Saisir constantes"** pour un patient en attente.
2. Un formulaire s'affiche demandant de saisir :
   * **Température** (°C)
   * **Poids** (kg)
   * **Taille** (cm)
   * **Pouls** (bpm)
   * **Tension artérielle** (Systolique/Diastolique mmHg)
   * **SpO2** (%)
   * **Glycémie** (g/L)
   * **Fréquence respiratoire** (cycles/min)
3. **Calcul de l'IMC automatique** : Si l'utilisateur saisit à la fois le poids et la taille, le système calcule et affiche instantanément l'IMC sous le formulaire avec 2 décimales.
4. À la validation, les constantes sont sauvegardées et associées à la visite.

### 4.3 Consultation et Clôture
1. Les constantes saisies s'affichent dans les détails de la visite sur le tableau de bord et dans l'historique du patient.
2. Le médecin consulte ces constantes lors de l'examen et clôture la visite à la fin de la consultation.

## 5. Règles métier

1. **Unicité de visite active** : Un patient ne peut pas avoir deux visites avec le statut `EN_COURS` simultanément dans la clinique.
2. **Bornes de validation des constantes** :
   * Température : `[30.0, 45.0]`
   * Poids : `[1.0, 500.0]`
   * Taille : `[30, 250]` (cm)
   * Pouls : `[20, 250]`
   * Systolique : `[40, 250]`, Diastolique : `[30, 150]`
   * SpO2 : `[50, 100]`
   * Glycémie : `[0.1, 10.0]`
   * Fréquence respiratoire : `[5, 100]`
3. **Formule IMC** : `IMC = Poids_en_kg / ((Taille_en_cm / 100) ^ 2)`.

## 6. Critères d'acceptation fonctionnels

1. Le formulaire de saisie des constantes vitales effectue les validations de bornes ci-dessus.
2. L'IMC se met à jour dynamiquement à la volée dès que le poids et la taille sont saisis et valides.
3. Les constantes vitales s'affichent de façon structurée (cartes ou badges colorés) sur le dashboard pour chaque patient de la file d'attente si elles ont été saisies.
4. L'isolation multi-tenant est étanche sur les constantes vitales.
5. Traduction complète FR/EN des étiquettes et des messages d'erreurs de validation.
