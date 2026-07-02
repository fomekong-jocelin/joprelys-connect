# STORY-0201 — Enregistrement de la Clinique Pilote & Multi-tenant

## 1. User story

En tant qu'**administrateur Joprelys**, je veux **pouvoir créer et configurer une clinique pilote dans le système**, afin d'**initialiser son espace dédié et de permettre l'association de ses professionnels**.

## 2. Critères d'acceptation

- [x] L'administrateur système peut saisir les détails de la clinique : nom, e-mail de contact, téléphone, adresse, ville et charger un logo.
- [x] Une clinique possède un statut `ACTIVE` ou `INACTIVE`. Si elle est inactive, aucun de ses utilisateurs ne peut s'authentifier.
- [x] Chaque utilisateur du système est associé à une seule clinique via une clé étrangère `organization_id`.
- [x] Toutes les données (patients, visites, consultations) sont filtrées de façon transparente au niveau de la base de données ou des services par cet `organization_id` (isolation stricte).
- [x] Le logo de la clinique est stocké dans un répertoire configuré et accessible par URL pour les futures générations de PDF.
- [x] L'écran de gestion des cliniques conserve le shell applicatif (logo, session utilisateur, footer) lors de la navigation.
- [x] Le mode création affiche le formulaire seul, sans tableau sous le formulaire.
- [x] La liste des cliniques est mobile-first : cartes sur mobile, tableau sur desktop.

## 3. Périmètre

### Inclus

- Modèle de données et tables SQL pour `organizations`.
- API REST d'administration pour les organisations.
- Écran d'administration Joprelys pour gérer les organisations cliniques.
- Service de stockage de fichiers locaux pour les logos.

### Exclus

- Personnalisation dynamique des thèmes CSS par clinique (couleurs spécifiques, etc. dans le MVP).

## 4. Tâches

| ID | Titre | Stack | Profil recommandé | SP | Estimation | Statut |
|---|---|---|---|---:|---:|---|
| TASK-0201-01 | Création de la table `organizations` et lien avec `users` | DB / SQL | Intermédiaire | 1 | 0.25j | DONE |
| TASK-0201-02 | API REST de gestion d'organisation (Spring Boot) | Backend | Intermédiaire | 1 | 0.5j | DONE |
| TASK-0201-03 | Implémentation du filtre multi-tenant globale sur les requêtes | Backend | Senior | 2 | 0.75j | DONE |
| TASK-0201-04 | Téléchargement et enregistrement du fichier logo | Backend / Frontend | Intermédiaire | 1 | 0.5j | DONE |
| TASK-0201-05 | Écran d'administration des cliniques pilotes | Frontend | Junior | 1 | 0.5j | DONE |
| TASK-0201-05B | Reprise UI mobile-first, shell applicatif et composants réutilisables | Frontend | Intermédiaire | 1 | 0.15j | REVIEW |
| TASK-0201-06 | Tests de non-interférence des données (isolation tenant) | Test | Senior | 1 | 0.5j | DONE |

## 5. Estimation

| Champ | Valeur |
|---|---|
| Story points | 3 |
| Complexité | M |
| Profil recommandé | Intermédiaire |
| Effort senior | 1j |
| Effort intermédiaire | 1.3j |
| Effort junior | 2.2j |
| Risque | Moyen |

## 6. Definition of Ready

- [x] Critères d'acceptation clairs
- [x] Dépendances connues (Modèle utilisateur)
- [x] Profil recommandé identifié
- [x] Estimation faite
- [x] Reviewer identifié (Lead Developer)

## 7. Definition of Done

- [x] Code de migration et services d'isolation implémentés
- [x] Tests automatiques confirmant l'étanchéité des cliniques OK
- [x] Shell applicatif, composants UI et présentation mobile-first implémentés
- [ ] Validation visuelle mobile OK
- [ ] Build production Angular validé sous Node pair/LTS
- [ ] Review OK

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Mise en place de la structure multi-tenant organisationnelle de base |
| Breaking change | Non |
| Release cible | v0.4.0 |
