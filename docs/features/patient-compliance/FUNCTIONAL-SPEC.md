# Spécification Fonctionnelle — Conformité Module Patient (EPIC-0013)

Ce document détaille les spécifications fonctionnelles pour combler l'ensemble des écarts identifiés dans le **Module Patient** par rapport au cahier des charges de Joprelys Connect.

---

## 1. Problème métier & Objectif

Bien que Joprelys Connect dispose déjà d'un système de Dossier Patient Unique (DPU), de gestion des antécédents/allergies et de demandes d'accès temporaires, plusieurs exigences réglementaires et de sécurité métier manquent à l'appel :
1. **Identité Patient (Module 3)** : Le système ne détecte pas les doublons et ne permet pas de fusionner deux dossiers patients doublonnés, ce qui présente des risques d'erreurs médicales (dispensation d'ordonnances ou consultation de constantes sur le mauvais dossier). Le numéro de téléphone est bloqué comme obligatoire alors qu'il doit être optionnel.
2. **Synthèse Médicale (Module 4)** : Les urgentistes ou médecins ont besoin d'accéder instantanément à une micro-fiche de synthèse allégée (antécédents, allergies actives, traitements en cours, constantes récentes) téléchargeable et imprimable, plutôt que de parcourir tout l'historique.
3. **Accès Granulaire & Consentement (Module 12 & 13)** : Le patient ne peut pas choisir quel type d'information médicale il partage avec un établissement. L'approbation d'une demande d'accès externe ou d'un consentement donne un accès global binaire.

---

## 2. Rôles et Autorisations

* **`AGENT_ACCUEIL`** :
  * Alerte automatique lors de la création d'un patient s'il y a un risque de doublon.
* **`MEDECIN` / `INFIRMIER`** :
  * Consulter et télécharger la fiche de synthèse médicale d'un patient.
* **`ADMIN_CLINIQUE`** :
  * Consulter la liste des doublons candidats détectés.
  * Valider ou rejeter une fusion de deux dossiers patients.
* **`PATIENT`** :
  * Configurer de façon granulaire les scopes d'accès à son dossier (autoriser/bloquer individuellement : Consultations, Prescriptions, Résultats de laboratoire, Antécédents, Historique de sécurité).
  * Consulter l'historique et le canal de validation (SMS OTP, Email) ayant servi à enregistrer son consentement.

---

## 3. Parcours Utilisateurs & Règles Métier

### 3.1 Détection automatique de doublons (Création de Patient)
Lorsqu'un agent d'accueil soumet le formulaire de création d'un patient :
1. Le système exécute un algorithme de calcul de similarité (phonétique Soundex/Metaphone ou distance de Levenshtein sur les champs `fullName` et comparant la date de naissance).
2. Si le score de similarité dépasse **85%** et que la date de naissance est identique :
   * L'agent d'accueil reçoit une alerte visuelle : *"Un patient similaire existe déjà (DPU-XXXXXX). Souhaitez-vous continuer ?"*
   * Si l'agent force la création, le système crée le patient et l'enregistre automatiquement dans la table `patient_duplicate_candidates` pour validation ultérieure.
3. **Exigence Téléphone** : Le numéro de téléphone devient optionnel lors de la création (suppression de la validation obligatoire stricte).

### 3.2 Fusion de dossiers patients (Administrateur de Clinique)
1. L'administrateur de la clinique accède à l'écran "Gestion des Doublons".
2. Il visualise les paires de patients suspectées d'être des doublons.
3. Il clique sur "Fusionner les dossiers".
4. Il sélectionne le **Patient Principal** (celui qui conserve l'identité finale) et le **Patient Secondaire** (celui qui va être fusionné et désactivé).
5. Lors de la fusion :
   * Les consultations, prescriptions, hospitalisations, consentements, allergies, antécédents et logs du patient secondaire sont rattachés au patient principal.
   * Le statut du patient secondaire passe à `MERGED` (il n'apparaît plus dans les recherches).
   * L'ancien numéro DPU du patient secondaire est conservé dans l'historique post-fusion pour assurer la traçabilité.
   * L'action est enregistrée dans le journal de sécurité (`patient_merged_history` et `audit_logs`).

### 3.3 Fiche de synthèse médicale autonome (Cliniciens)
1. Dans le dossier d'un patient, un bouton "Télécharger la fiche de synthèse" est visible.
2. Au clic, le système génère un document PDF allégé sur une page contenant :
   * Identité et groupe sanguin.
   * Allergies actives et leur niveau de gravité.
   * Antécédents chirurgicaux et médicaux structurés majeurs.
   * Dernières constantes vitales relevées (avec date).
3. Ce document contient un QR Code unique de vérification d'authenticité.

### 3.4 Scopes de Consentement et d'Accès Granulaire
1. Lorsqu'un patient gère ses consentements ou approuve une demande d'accès externe, il dispose de checkboxes pour cocher les modules autorisés :
   * [x] `medical_records` (Consultations, notes cliniques)
   * [x] `prescriptions` (Ordonnances)
   * [x] `lab_results` (Résultats biologiques et examens)
   * [x] `allergies_history` (Allergies et antécédents)
2. Si une clinique ou un médecin tente d'interroger un endpoint API d'un module non autorisé (ex: `/api/lab-orders/...` alors que `lab_results` est décoché), le serveur backend Spring Boot renvoie un code d'erreur `403 Forbidden` avec un message traduisible : *"Accès refusé par le consentement du patient."*
3. Le consentement stocke le **canal de validation** (`SMS_OTP`, `EMAIL_OTP` ou `MANUAL_SIGNED`).

---

## 4. Critères d'acceptation fonctionnels

1. **Doublons** : Créer un patient avec un nom et une date de naissance similaires à 90% à un patient existant doit lever un avertissement et créer une ligne de doublon candidat.
2. **Fusion** : Après fusion, le patient secondaire est désactivé, ses visites apparaissent dans le dossier du patient principal, et son profil affiche "Fusionné avec DPU-XXX".
3. **Téléphone** : Il doit être possible d'enregistrer un patient sans numéro de téléphone.
4. **Fiche de Synthèse** : Le PDF de synthèse doit être téléchargeable en moins de 2 secondes et ne contenir que les antécédents critiques et les constantes récentes de moins de 3 mois.
5. **Scopes** : Bloquer l'accès aux examens de labo pour une clinique révoquée de ce scope, tout en lui permettant de voir les ordonnances si le scope prescriptions est actif.
