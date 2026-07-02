# Spécification Fonctionnelle — Invitation & Gestion du Personnel (STORY-0104)

## 1. Problème métier

Pour qu'une clinique pilote fonctionne sur Joprelys Connect, son administrateur local (`ADMIN_CLINIQUE`) doit pouvoir constituer et gérer son équipe de soin (médecins, infirmiers, agents d'accueil) de manière autonome. Sans cette fonctionnalité, la création d'utilisateurs cliniques nécessiterait des interventions manuelles en base de données, ce qui est incompatible avec un déploiement pilote agile.

## 2. Objectif

Permettre à l'administrateur de clinique de :
1. Consulter la liste complète du personnel de son établissement.
2. Inviter (créer) de nouveaux membres en leur assignant un rôle clinique (`MEDECIN`, `INFIRMIER`, `AGENT_ACCUEIL`, `PHARMACIEN`).
3. Modifier les informations d'un membre (nom, rôle).
4. Suspendre (désactiver) ou réactiver un compte utilisateur pour bloquer/rétablir immédiatement ses accès en cas de départ ou d'absence.

## 3. Rôles et Autorisations

* **`ADMIN_CLINIQUE`** :
  * Accès complet en lecture, écriture, modification et activation/désactivation sur le personnel de **sa** clinique uniquement.
* **Autres rôles (`MEDECIN`, `INFIRMIER`, `AGENT_ACCUEIL`, `PHARMACIEN`, `ADMIN_JOPRELYS`)** :
  * Aucun accès aux APIs de gestion du personnel de clinique (403 Forbidden).

## 4. Parcours Utilisateur

### 4.1 Visualisation de l'équipe
* L'administrateur se connecte et voit un nouvel onglet ou bouton "Gérer le personnel" sur son tableau de bord.
* Il accède à la liste affichant le nom, l'email, le rôle (avec badge de couleur distinctive) et le statut (Actif en vert, Suspendu en gris) de chaque collaborateur.

### 4.2 Invitation d'un nouveau collaborateur
* L'administrateur clique sur "Inviter un collaborateur".
* Un formulaire s'ouvre demandant :
  * **Nom complet** (ex: Dr. Marc LENOIR)
  * **Adresse email** (doit être unique et formater proprement)
  * **Rôle** (liste déroulante : Médecin, Infirmier, Agent d'accueil, Pharmacien)
* À la validation :
  * Le système crée le compte utilisateur rattaché à l'organisation de l'administrateur.
  * Le système génère un mot de passe temporaire sécurisé (ex: `Jop-XXXXX`).
  * Ce mot de passe temporaire s'affiche dans une zone claire (qu'on peut copier en un clic) sur l'écran pour que l'administrateur puisse le transmettre de manière sécurisée au collaborateur.

### 4.3 Modification & Changement de statut
* L'administrateur peut cliquer sur "Modifier" en face de chaque collaborateur pour changer son nom complet ou son rôle.
* Il dispose d'un bouton d'activation/désactivation. Désactiver un compte met le statut à `Suspendu` et bloque immédiatement les connexions futures de cet utilisateur.

## 5. Critères d'acceptation fonctionnels

1. **Isolation stricte** : L'administrateur de la clinique A ne peut lister ou modifier que le personnel de la clinique A. Toute tentative d'accès sur le personnel d'une autre clinique renvoie un `404 Not Found` ou `403 Forbidden`.
2. **Unicité de l'email** : Si l'email saisi existe déjà dans le système globally, l'API renvoie une erreur explicite.
3. **Mot de passe temporaire** : L'interface doit afficher de façon très visible le mot de passe temporaire généré lors de la création d'un utilisateur, car aucun service d'envoi d'email n'est configuré pour le pilote.
4. **i18n & Responsivité** : L'interface est mobile-first (Tailwind CSS) et entièrement localisée en français et en anglais.

## 6. État d'implémentation au 2026-07-02

### Backend

- API `/api/staff` implémentée pour lister, inviter, modifier et activer/désactiver les collaborateurs.
- Mot de passe temporaire généré au format `Jop-XXXXXX`, retourné uniquement dans la réponse d'invitation.
- Email normalisé en minuscules avant persistance.
- Rôles cliniques autorisés : `MEDECIN`, `INFIRMIER`, `AGENT_ACCUEIL`, `PHARMACIEN`.
- Isolation stricte par `organizationId` de l'administrateur connecté.

### Frontend

- Page `/clinic/staff` implémentée en Angular standalone.
- Accès ajouté au tableau de bord pour `ADMIN_CLINIQUE`.
- Liste responsive desktop/mobile.
- Formulaire d'invitation et formulaire de modification.
- Bandeau de succès avec mot de passe temporaire et bouton de copie.
- Textes localisés via `I18nService` en français et en anglais.
- Thème light/dark préservé via les tokens CSS et Tailwind existants.
