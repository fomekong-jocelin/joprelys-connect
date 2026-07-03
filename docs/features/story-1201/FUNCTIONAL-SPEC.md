# Spécification Fonctionnelle — Allergies & Antécédents Médicaux (STORY-1201)

## 1. Problème métier

Lors de la prise en charge médicale d'un patient, la connaissance immédiate de ses **allergies connues** et de ses **antécédents cliniques** (médicaux, chirurgicaux, familiaux) est une exigence absolue de sécurité des soins.
* L'absence de signalement d'une allergie (ex: à la pénicilline) peut mener à des accidents thérapeutiques graves (choc anaphylactique).
* Les antécédents médicaux (ex: diabète, hypertension) et chirurgicaux guident les décisions de diagnostic et de traitement du médecin.
* Actuellement, ces informations sont dispersées ou saisies sous forme de texte libre non structuré, ce qui empêche les contrôles automatiques futurs lors de la prescription.

## 2. Objectif

Permettre aux cliniciens autorisés (infirmiers, médecins) de renseigner de façon structurée et centralisée les allergies et antécédents d'un patient au sein de son Dossier Patient Unique (DPU), de les visualiser en un clin d'œil sur sa fiche d'accueil, et de conserver un historique traçable de toutes les modifications (audit logs).

## 3. Rôles et Autorisations

* **`MEDECIN` / `INFIRMIER` / `ADMIN_CLINIQUE`** :
  * Ajouter une allergie ou un antécédent sur le dossier d'un patient.
  * Modifier ou désactiver une allergie / un antécédent existant.
  * Consulter la liste complète des allergies et antécédents d'un patient.
* **`AGENT_ACCUEIL` / `PATIENT`** :
  * Lecture seule (consultation sur la fiche patient).

## 4. Parcours Utilisateur

### 4.1 Ajout d'une allergie
1. Le clinicien sélectionne un patient et ouvre sa fiche DPU.
2. Dans l'onglet "Informations Médicales", sous la section **Allergies**, il clique sur **"Ajouter une allergie"**.
3. Un formulaire s'ouvre demandant :
   * **Substance** (ex: Pénicilline, Lactose, Aspirine) - *Obligatoire*
   * **Gravité** (Faible, Moyenne, Élevée, Critique) - *Obligatoire*
   * **Type de réaction** (ex: Urticaire, Difficultés respiratoires, Anaphylaxie) - *Optionnel*
   * **Statut** (Actif, Inactif) - *Par défaut Actif*
   * **Date de découverte** - *Optionnel*
   * **Commentaire** - *Optionnel*
4. À la validation, l'allergie est sauvegardée, associée au patient sous isolation multi-tenant, et auditée.

### 4.2 Ajout d'un antécédent
1. Dans l'onglet "Informations Médicales", sous la section **Antécédents**, le clinicien clique sur **"Ajouter un antécédent"**.
2. Un formulaire s'ouvre demandant :
   * **Catégorie** (Médical, Chirurgical, Familial, Obstétrical, Autre) - *Obligatoire*
   * **Description / Diagnostic** (ex: Hypertension Artérielle, Césarienne en 2021) - *Obligatoire*
   * **Date d'apparition / d'événement** - *Optionnel*
   * **Traitement en cours** (Oui/Non) - *Optionnel*
   * **Commentaire** - *Optionnel*
3. À la validation, l'antécédent est sauvegardé et audité.

### 4.3 Visualisation et Alertes
1. Sur le profil résumé du patient, les allergies actives de gravité **Élevée** ou **Critique** s'affichent en évidence sous forme de badges rouges clignotants ou contrastés pour capter l'attention du clinicien avant toute prescription.
2. Les antécédents sont listés par catégorie pour une lecture structurée et rapide.

## 5. Règles métier

1. **Pas de suppression physique** : Un antécédent ou une allergie saisis par erreur ne sont jamais supprimés de la base de données. Ils sont marqués comme `Inactifs` (soft delete) avec une note de justification, afin de conserver l'historique complet des soins.
2. **Historisation de l'audit** : Toute création ou modification d'une allergie ou d'un antécédent génère une entrée d'audit associée précisant l'auteur, la date et la nature de la modification.

## 6. Critères d'acceptation fonctionnels

1. Les champs obligatoires des formulaires (Substance, Gravité, Catégorie, Description) sont validés côté client et serveur.
2. Les badges d'alertes des allergies s'affichent de façon contrastée sur la fiche résumé du patient.
3. Les traductions FR/EN sont appliquées à l'ensemble de l'IHM via `I18nService`.
