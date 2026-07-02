# STORY-0104 — Invitation & Gestion du Personnel de Clinique

## 1. User story

En tant qu'**administrateur Joprelys** (ou **administrateur local de clinique**), je veux **pouvoir inviter et enregistrer des professionnels de santé (médecin, infirmier, agent d'accueil) au sein d'une clinique**, afin de **leur attribuer un accès sécurisé et d'associer leurs actions au bon espace clinique (tenant)**.

## 2. Critères d'acceptation

- [ ] L'administrateur peut saisir les informations de l'employé : Nom complet, Adresse e-mail, Rôle (parmi `AGENT_ACCUEIL`, `INFIRMIER`, `MEDECIN`, `ADMIN_CLINIQUE`) et l'associer à une clinique (`organization_id`).
- [ ] L'e-mail doit être unique dans tout le système.
- [ ] À la création, l'utilisateur reçoit ses identifiants temporaires de connexion ou un e-mail d'activation.
- [ ] L'accès aux API de gestion du personnel est sécurisé et réservé aux rôles `ADMIN_JOPRELYS` (pour toutes les cliniques) et `ADMIN_CLINIQUE` (uniquement pour le personnel de sa propre clinique).
- [ ] L'administrateur de clinique peut lister et désactiver les comptes des professionnels de son établissement.

## 3. Périmètre

### Inclus

- API REST de gestion des utilisateurs (`/api/users`) avec recherche et filtrage par clinique.
- Validation des formats d'adresse e-mail et unicité en base de données.
- Écran Angular de gestion du personnel de clinique (liste, ajout, désactivation) intégré au shell applicatif.
- Traduction complète en français et en anglais.

### Exclus

- Workflow complexe d'invitation avec lien d'activation sécurisé par jeton temporaire (les comptes seront créés directement avec mot de passe temporaire dans un premier temps).

## 4. Tâches

| ID | Titre | Stack | Profil recommandé | SP | Estimation | Statut |
|---|---|---|---|---:|---:|---|
| TASK-0104-01 | Extension de l'API REST `UserController` pour la création/mise à jour | Backend | Intermédiaire | 1 | 0.4j | TODO |
| TASK-0104-02 | Service métier et contrôles d'habilitations de rattachement | Backend | Senior | 1 | 0.3j | TODO |
| TASK-0104-03 | Écran frontend de gestion du personnel de clinique (Tailwind CSS) | Frontend | Intermédiaire | 1 | 0.5j | TODO |
| TASK-0104-04 | Tests d'intégration et d'isolation (un administrateur local ne gère que sa clinique) | Test | Senior | 1 | 0.3j | TODO |

## 5. Estimation

| Champ | Valeur |
|---|---|
| Story points | 3 |
| Complexité | M |
| Profil recommandé | Intermédiaire |
| Effort senior | 1j |
| Effort intermédiaire | 1.5j |
| Effort junior | 2.5j |
| Risque | Moyen |

## 6. Definition of Ready

- [x] Critères d'acceptation clairs
- [x] Rôles et habilitations définis
- [x] Estimation faite
- [x] Reviewer identifié (Lead Developer)

## 7. Definition of Done

- [ ] Code backend et frontend implémenté et revu
- [ ] Tests d'intégration d'isolation validés (un administrateur de clinique A ne peut pas gérer les utilisateurs de la clinique B)
- [ ] Traduction FR/EN et conformité mobile-first OK
- [ ] Build production Angular validé

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout de la gestion d'utilisateurs et de personnel rattaché aux cliniques |
| Breaking change | Non |
| Release cible | v0.4.0 |
