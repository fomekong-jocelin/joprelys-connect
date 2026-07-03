# Spécification Fonctionnelle — Module Hospitalisations, lits et notes journalières (STORY-1202)

## 1. Problème métier

Lorsqu'un patient nécessite une surveillance continue, il doit être admis en hospitalisation au sein de la clinique. Cela exige :
* De suivre la disponibilité en temps réel des chambres et des lits par service (ex: Médecine, Chirurgie, Pédiatrie) pour éviter les sur-réservations.
* De consigner quotidiennement les observations et transmissions cliniques (notes journalières d'évolution) rédigées par les infirmiers et les médecins.
* De sécuriser l'affectation des lits : en cas de saisie simultanée par deux soignants pour le même lit, le système doit rejeter la seconde affectation pour éviter qu'un lit soit attribué à deux patients.
* De générer un compte-rendu ou fiche de sortie d'hospitalisation au format PDF contenant le résumé du séjour, les consignes médicales et la signature du médecin.

## 2. Objectif

Permettre aux cliniciens de gérer le cycle de vie des hospitalisations des patients (Admission, Suivi quotidien par notes d'évolution, Sortie d'hospitalisation avec décharge et génération d'un compte-rendu PDF) tout en maintenant un inventaire précis et sans conflit des lits disponibles.

## 3. Rôles et Autorisations

* **`MEDECIN` / `INFIRMIER` / `ADMIN_CLINIQUE`** :
  * Admettre un patient en hospitalisation (sélection du service, de la chambre et du lit).
  * Ajouter des notes journalières d'évolution.
  * Clôturer l'hospitalisation (déclarer la sortie).
  * Générer la fiche de sortie PDF.
* **`AGENT_ACCUEIL` / `PATIENT`** :
  * Lecture seule (visualisation du statut d'hospitalisation, de la chambre et du lit).

## 4. Parcours Utilisateur

### 4.1 Admission en Hospitalisation
1. Le soignant ouvre le dossier d'un patient ayant une visite active.
2. Il clique sur **"Admettre en hospitalisation"**.
3. Un formulaire s'affiche demandant :
   * **Service** (ex: Médecine Générale, Pédiatrie, Soins Intensifs) - *Obligatoire*
   * **Numéro de Chambre** (ex: Ch. 101, Ch. 102) - *Obligatoire*
   * **Numéro de Lit** (ex: Lit A, Lit B) - *Obligatoire*
   * **Motif d'hospitalisation** - *Obligatoire*
4. À la validation, si le lit est libre, le séjour est créé avec le statut `EN_COURS`. Si le lit vient d'être attribué à un autre patient par un autre soignant, une erreur claire s'affiche.

### 4.2 Saisie des Notes Journalières
1. Sur le dossier du patient hospitalisé, un onglet dédié **"Suivi d'Hospitalisation"** s'affiche.
2. Le soignant peut saisir une observation quotidienne en cliquant sur **"Ajouter une note"**.
3. Il renseigne la note (observations cliniques, état de conscience, constantes, traitements administrés).
4. À la validation, la note est enregistrée et ajoutée à la timeline chronologique du séjour.

### 4.3 Déclaration de Sortie & Génération du PDF
1. Lorsque le patient est prêt à sortir, le médecin clique sur **"Déclarer la sortie"**.
2. Il remplit les informations de décharge :
   * **Date et heure de sortie**
   * **Diagnostic final de sortie** - *Obligatoire*
   * **Consignes de sortie / Prescriptions post-hospitalisation** - *Obligatoire*
3. À la validation, le statut du séjour passe à `SORTIE` et le lit est immédiatement libéré.
4. Un bouton **"Télécharger la fiche de sortie PDF"** apparaît, permettant de générer et de récupérer le compte-rendu officiel signé.

## 5. Règles métier

1. **Unicité d'hospitalisation active** : Un patient ne peut avoir qu'un seul séjour d'hospitalisation actif (`EN_COURS`) à la fois.
2. **Exclusivité du lit** : Un couple `(chambre, lit)` au sein d'une organisation ne peut être occupé que par un seul séjour d'hospitalisation actif à la fois.
3. **Concurrence optimiste** : Le système utilise un verrouillage optimiste sur les séjours et l'état des lits. Si deux requêtes d'admission ciblent le même lit au même instant, la seconde requête échouera avec une exception de conflit (`409 Conflict`), obligeant l'utilisateur à rafraîchir la liste et choisir un autre lit.

## 6. Critères d'acceptation fonctionnels

1. Les formulaires valident les saisies obligatoires côté client et serveur.
2. La liste des lits occupés est mise à jour instantanément.
3. Les notes journalières s'affichent par ordre chronologique décroissant.
4. Les traductions FR/EN sont appliquées sur tous les boutons, libellés et messages d'erreurs.
5. La fiche de sortie PDF est générée dynamiquement et stockée de manière sécurisée.
