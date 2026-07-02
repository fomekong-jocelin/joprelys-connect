# Spécification Fonctionnelle — Ouverture & Clôture de Visite Patient (STORY-0401)

## 1. Problème métier

Lorsqu'un patient arrive dans une clinique pilote, il doit être admis pour qu'un professionnel de santé puisse s'occuper de lui. Cette admission lance une **visite clinique**. Elle permet :
* De tracer le temps d'attente et de traitement du patient.
* D'orienter le patient vers le bon service ou professionnel de santé.
* D'établir une file d'attente active visible par les cliniciens (infirmiers, médecins) pour le tri et la consultation.

## 2. Objectif

Permettre aux agents d'accueil d'ouvrir une visite clinique pour un patient, de définir son motif de visite, de l'orienter, et de lister les visites actives au sein d'une file d'attente globale partagée. Permettre aux médecins de clôturer la visite à la fin de la prise en charge.

## 3. Rôles et Autorisations

* **`AGENT_ACCUEIL` / `INFIRMIER` / `MEDECIN` / `ADMIN_CLINIQUE`** :
  * Ouvrir une visite pour un patient existant de sa clinique.
  * Consulter la file d'attente active (`EN_COURS`) de la clinique.
* **`MEDECIN` / `ADMIN_CLINIQUE`** :
  * Clôturer une visite en cours.

## 4. Parcours Utilisateur

### 4.1 Ouverture de Visite (Admission)
1. L'utilisateur recherche et sélectionne un patient pour accéder à sa fiche détaillée.
2. Sur la fiche détaillée, il clique sur le bouton **"Admettre le patient"** ou **"Ouvrir une visite"**.
3. Un formulaire s'affiche demandant :
   * **Motif de visite** (Ex: *Fièvre et maux de tête depuis 3 jours*).
   * **Service/Médecin d'orientation** (Ex: *Médecine générale, Tri, Pédiatrie*).
4. À la validation, la visite est créée avec :
   * Un numéro unique séquentiel : `VIS-YYYYMMDD-XXXXXX` (Ex: `VIS-20260702-000001`).
   * Le statut initial : `EN_COURS`.
   * L'horodatage d'arrivée : `created_at`.
5. Le patient est redirigé vers le tableau de bord de la file d'attente.

### 4.2 File d'attente active (Dashboard)
1. L'accueil du dashboard affiche la liste des patients actuellement en cours de soins dans la clinique.
2. Les colonnes affichées sont : N° Visite, Patient (DPU), Sexe, Heure d'arrivée, Motif, Orientation, Statut, et Actions.
3. Les visites sont triées de la plus ancienne à la plus récente (`created_at ASC`) pour respecter l'ordre d'arrivée.

### 4.3 Clôture de Visite
1. Le clinicien (ex: le médecin) peut cliquer sur **"Clôturer la visite"** depuis la file d'attente.
2. Le statut de la visite passe à `TERMINEE`, et l'heure de fin `closed_at` est enregistrée.
3. La visite disparaît de la file d'attente active (elle reste consultable dans l'historique du patient).

## 5. Règles métier

1. **Unicité de visite active** : Un patient ne peut pas avoir deux visites avec le statut `EN_COURS` simultanément dans la clinique. Si une visite est déjà active pour ce patient, l'ouverture d'une nouvelle visite est refusée avec un message d'erreur explicite.
2. **Cycle de vie strict** : Les statuts possibles sont `EN_COURS`, `TERMINEE` et `ANNULEE`. Une visite clôturée (`TERMINEE`) ou annulée (`ANNULEE`) ne peut plus repasser à `EN_COURS`.

## 6. Critères d'acceptation fonctionnels

1. Le formulaire d'ouverture de visite valide les champs obligatoires (Motif et Orientation).
2. Le numéro de visite respecte strictement le format `VIS-YYYYMMDD-XXXXXX`.
3. L'isolation multi-tenant est étanche (l'utilisateur d'une clinique ne peut lister ou modifier que les visites de sa propre clinique).
4. L'IMC ne fait pas partie de cette story (il sera implémenté dans `STORY-0402` avec les constantes).
5. L'interface s'adapte sur mobile (mobile-first) en affichant la file d'attente sous forme de cartes descriptives à la place du tableau de données horizontal.
