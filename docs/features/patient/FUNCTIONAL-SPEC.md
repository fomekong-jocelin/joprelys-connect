# Spécification Fonctionnelle — Dossier Patient Unique & Enregistrement (STORY-0301)

## 1. Problème métier

Dans le système de santé, le suivi des patients souffre souvent de l'absence d'un identifiant unique de santé à travers les cliniques ou de l'éparpillement des dossiers. Joprelys Connect vise à standardiser le suivi médical à travers le **Dossier Patient Unique (DPU)**. Chaque patient enregistré dans une clinique pilote doit avoir un identifiant national DPU harmonisé et un identifiant local propre à l'établissement.

## 2. Objectif

Permettre aux professionnels de santé autorisés (en particulier l'**agent d'accueil**, l'**infirmier**, le **médecin** et l'**administrateur de clinique**) d'enregistrer des patients, de leur générer automatiquement un numéro DPU Joprelys ainsi qu'un numéro patient local, et d'isoler strictement leurs données au sein de chaque établissement.

## 3. Rôles et Autorisations

* **`AGENT_ACCUEIL`** :
  * Enregistrer un nouveau patient.
  * Rechercher un patient existant par nom, téléphone ou DPU.
  * Consulter les informations administratives du patient.
* **`INFIRMIER` / `MEDECIN` / `ADMIN_CLINIQUE`** :
  * Rechercher un patient existant par nom, téléphone ou DPU.
  * Consulter le dossier patient.
* **`ADMIN_JOPRELYS`** :
  * Aucun accès aux dossiers patients (isolation stricte pour la protection des données de santé).

## 4. Parcours Utilisateur

### 4.1 Enregistrement du patient (Agent d'accueil)
* L'utilisateur se connecte et accède à la rubrique "Patients".
* Il clique sur "Nouveau Patient".
* Un formulaire mobile-first lui demande de saisir :
  * **Champs obligatoires** : Nom complet, Sexe (Masculin / Féminin), Date de naissance, Téléphone, Ville, Quartier.
  * **Champs facultatifs** : Email, Adresse complète, Contact d'urgence (Nom et Téléphone), Groupe sanguin, Allergies, Antécédents médicaux.
* Lors de la validation, le système :
  * Génère le numéro DPU : `DPU-JOP-YYYYMMDD-XXXXXX` (où XXXXXX est un numéro incrémenté quotidiennement).
  * Génère le numéro local : `PAT-YYYYMMDD-XXXXXX` (où XXXXXX est également un numéro incrémenté quotidiennement).
  * Enregistre le patient sous l'identifiant de la clinique courante (`organization_id`).
* L'utilisateur est ensuite redirigé vers le profil de base du patient créé.

### 4.2 Consultation et recherche (Cliniciens)
* L'infirmier ou le médecin accède à l'écran "Patients".
* Un champ de recherche multicritère permet de retrouver le patient par son Nom, son Téléphone ou son numéro DPU.
* En sélectionnant le patient, l'utilisateur accède au profil complet contenant l'identité administrative, les antécédents, les allergies, et plus tard, l'historique des visites et consultations.

## 5. Critères d'acceptation fonctionnels

1. **Génération d'identifiants** : Tout patient enregistré doit obligatoirement recevoir un DPU Joprelys unique et un identifiant local.
2. **Validation des champs** :
   * Le nom complet doit contenir au moins 3 caractères.
   * La date de naissance ne doit pas être dans le futur.
   * Le téléphone doit être valide.
3. **Multi-tenant / Confidentialité** : Un patient créé dans la clinique A n'apparaît jamais dans la recherche de la clinique B. Les requêtes directes via API sur d'autres cliniques sont bloquées (403/404).
4. **Traductions** : L'écran de liste, de création et de profil du patient est entièrement disponible en français et en anglais.
5. **Aesthetics & Responsive** : L'interface respecte la charte graphique Montserrat/Inter, s'affiche en cartes optimisées sur mobile, et bascule sur un tableau clair sur desktop.

## 6. Correction UI — Lisibilité du tableau desktop (2026-07-03)

Le tableau desktop de la liste patients doit conserver les identifiants DPU et patient local sur une seule ligne, comme le tableau de gestion des cliniques pilotes.

Critères complémentaires :

1. Les colonnes `N° DPU (National)` et `N° Local` ne doivent pas couper les identifiants sur deux lignes.
2. L'action `Voir le dossier` doit rester lisible sur une seule ligne.
3. En cas de largeur insuffisante, le conteneur desktop doit conserver le défilement horizontal existant plutôt que compresser les données critiques.
4. La vue mobile en cartes n'est pas modifiée par cette correction desktop historique.

## 7. Refonte UI — Poste de travail Patients mobile-first (2026-07-29)

La liste Patients privilégie désormais la tâche opérationnelle : retrouver un dossier et l'ouvrir rapidement.

1. Le titre visible est `Patients`; le breadcrumb global remplace le lien Retour redondant du PageHeader.
2. La recherche `Nom, téléphone ou DPU` est l'action dominante et peut être lancée avec Enter.
3. Une recherche active peut être effacée en un geste ; le bouton texte `Rechercher` séparé n'est plus nécessaire.
4. `Nouvelle admission` reste immédiatement accessible mais n'occupe plus le hero de la page.
5. Sur mobile, chaque patient est une carte compacte et entièrement cliquable avec nom, sexe, DPU, téléphone et ville ; le bouton interne `Voir le dossier` est supprimé.
6. Les cartes mobiles sont indépendantes et ne sont plus enfermées dans une grande carte parent.
7. Pendant une recherche, la page affiche le nombre de résultats ; hors recherche, elle affiche simplement `Liste des patients` sans prétendre à un tri par récence.
8. Sur desktop, le tableau comparatif, les identifiants non wrap et l'action `Voir le dossier` sont conservés.
