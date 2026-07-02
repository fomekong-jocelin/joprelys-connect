# STORY-0101 — Connexion & Déconnexion Sécurisée

## 1. User story

En tant qu'**utilisateur clinique**, je veux **me connecter par e-mail et mot de passe de manière sécurisée**, afin d'**accéder aux fonctionnalités de mon espace de travail**.

## 2. Critères d'acceptation

- [x] L'utilisateur doit renseigner un e-mail au format valide et un mot de passe.
- [x] Le mot de passe en base est stocké sous forme cryptée (BCrypt).
- [x] Une authentification réussie génère un token JWT contenant l'e-mail, le nom et le rôle de l'utilisateur.
- [x] Un e-mail ou mot de passe incorrect retourne un message d'erreur générique sans spécifier lequel est faux (prévention de l'énumération des comptes).
- [x] La déconnexion détruit le token JWT côté client et invalide la session côté serveur.
- [x] Toute tentative d'authentification (réussie ou échouée) produit un log d'audit (date/heure, IP, e-mail, statut de réussite).

## 3. Périmètre

### Inclus

- Écran de connexion (Login).
- Services d'authentification REST Backend.
- Hashage des mots de passe.
- Gestion des sessions et interception des requêtes JWT côté client.

### Exclus

- Mécanisme de blocage temporaire du compte après X tentatives (remis à une version future).
- Mot de passe oublié / réinitialisation par e-mail.

## 4. Tâches

| ID | Titre | Stack | Profil recommandé | SP | Estimation | Statut |
|---|---|---|---|---:|---:|---|
| TASK-0101-01 | Conception de la table `users` et migration DB | DB / SQL | Intermédiaire | 1 | 0.25j | DONE |
| TASK-0101-02 | API REST de login/logout avec JWT (Spring Boot) | Backend | Senior | 2 | 0.5j | REVIEW |
| TASK-0101-03 | Écran de connexion responsive (Angular/Web) | Frontend | Junior | 1 | 0.5j | DONE |
| TASK-0101-04 | Intercepteur HTTP et stockage local du JWT | Frontend | Intermédiaire | 1 | 0.5j | DONE |
| TASK-0101-05 | Tests unitaires Backend & Frontend | Test | Intermédiaire | 1 | 0.5j | REVIEW |

## 5. Estimation

| Champ | Valeur |
|---|---|
| Story points | 3 |
| Complexité | S |
| Profil recommandé | Intermédiaire |
| Effort senior | 0.5j |
| Effort intermédiaire | 0.65j |
| Effort junior | 1.1j |
| Risque | Faible |

## 6. Definition of Ready

- [x] Critères d'acceptation clairs
- [x] Dépendances connues
- [x] Données de test disponibles
- [x] Profil recommandé identifié
- [x] Estimation faite
- [x] Reviewer identifié (Lead Developer)

## 7. Definition of Done

- [x] Code terminé
- [ ] Tests OK (taux de couverture minimal de 80 %)
- [ ] Review OK
- [ ] QA OK
- [ ] Documentation mise à jour si nécessaire
- [ ] Changelog mis à jour si nécessaire

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout de la brique d'authentification et JWT |
| Breaking change | Non |
| Release cible | v0.4.0 |
