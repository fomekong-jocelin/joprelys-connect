# EPIC-0001 — Authentification & Gestion des Rôles

## 1. Objectif métier

Permettre aux professionnels de santé de se connecter de façon sécurisée à la plateforme clinique et de réguler les accès aux données médicales sensibles en fonction de leur rôle.

## 2. Périmètre

### Inclus

- Connexion sécurisée via e-mail et mot de passe.
- Déconnexion de l'application.
- Hashage fort des mots de passe (ex. BCrypt).
- Rôles prédéfinis : `ADMIN_JOPRELYS`, `ADMIN_CLINIQUE`, `AGENT_ACCUEIL`, `INFIRMIER`, `MEDECIN`.
- Gestion simplifiée des sessions utilisateurs (Token JWT ou session sécurisée).

### Exclus

- Double authentification (MFA).
- Intégration SSO externe ou annuaire LDAP/Active Directory.
- Gestion avancée des mots de passe (expiration forcée, historique).

## 3. Utilisateurs concernés

- Agent d'accueil
- Infirmier
- Médecin
- Administrateur Clinique
- Administrateur Joprelys

## 4. User stories

| ID | Titre | Priorité | SP | Statut | Sprint cible |
|---|---|---|---:|---|---|
| [STORY-0101](STORY-0101-connexion.md) | Connexion et Déconnexion sécurisée | P0 | 3 | REVIEW | SPRINT-0002 |
| [STORY-0102](STORY-0102-roles.md) | Contrôle d'accès basé sur les rôles | P0 | 3 | BACKLOG | SPRINT-0002 |
| STORY-0103 | Récupération de mot de passe simplifiée | P2 | 2 | BACKLOG | SPRINT-0003 |
| [STORY-0104](STORY-0104-gestion-personnel.md) | Invitation et gestion du personnel de clinique | P1 | 3 | BACKLOG | SPRINT-0003 |

## 5. Dépendances

| Dépendance | Type | Impact |
|---|---|---|
| Initialisation de la base de données | Technique | Nécessaire pour stocker les comptes utilisateurs et rôles. |

## 6. Risques

| Risque | Impact | Mitigation |
|---|---|---|
| Fuite de données due à des sessions mal sécurisées | Fort | Implémenter JWT avec expiration courte et HTTPS obligatoire. |

## 7. Définition de succès

- [ ] Un utilisateur ne peut pas accéder aux pages de l'application sans être authentifié.
- [ ] Les menus affichés dans l'interface sont filtrés strictement selon le rôle de l'utilisateur.
- [ ] Les mots de passe sont stockés cryptés en base de données.

## 8. Estimation globale

| Élément | Valeur |
|---|---|
| Total story points | 11 |
| Effort senior | 2.5j |
| Effort intermédiaire | 3.4j |
| Effort junior | 5.3j |
| Nombre de sprints estimé | 1 |

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout du module d'authentification et de gestion des rôles de base |
| Breaking change | Non |
| Release cible | v0.4.0 |
