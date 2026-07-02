# EPIC-0002 — Gestion de la Clinique Pilote

## 1. Objectif métier

Permettre aux administrateurs de configurer et personnaliser les cliniques pilotes qui utiliseront Joprelys Connect, et assurer que chaque document émis porte les bonnes informations d'identification de la clinique.

## 2. Périmètre

### Inclus

- Création d'une clinique par l'administrateur système (nom, ville, téléphone, logo).
- Association des utilisateurs à une clinique (multi-tenant de base : un utilisateur appartient à une clinique).
- Mise à jour des informations de contact et d'affichage de la clinique.
- Blocage/activation d'une clinique pilote (une clinique inactive bloque toutes ses opérations).

### Exclus

- Modèle de facturation et de paiement d'abonnement clinique.
- Gestion de succursales ou structures multi-sites complexes.

## 3. Utilisateurs concernés

- Administrateur Joprelys
- Administrateur Clinique

## 4. User stories

| ID | Titre | Priorité | SP | Statut | Sprint cible |
|---|---|---|---:|---|---|
| [STORY-0201](STORY-0201-enregistrement-clinique.md) | Enregistrement de la clinique pilote | P0 | 3 | REVIEW | SPRINT-0002 |
| STORY-0202 | Gestion des profils et logos de la clinique | P1 | 2 | BACKLOG | SPRINT-0003 |

## 5. Dépendances

| Dépendance | Type | Impact |
|---|---|---|
| EPIC-0001 (Authentification) | Fonctionnelle | L'utilisateur doit être connecté pour gérer ou s'associer à une clinique. |

## 6. Risques

| Risque | Impact | Mitigation |
|---|---|---|
| Fuite ou mélange de données entre cliniques pilotes distinctes | Très Fort | Validation stricte du `organization_id` dans toutes les requêtes API (isolation des données). |

## 7. Définition de succès

- [ ] Un administrateur clinique ne peut voir ni modifier les informations d'une autre clinique.
- [ ] Le logo et les informations de contact de la clinique s'affichent correctement lors de l'aperçu ou de la génération des documents.

## 8. Estimation globale

| Élément | Valeur |
|---|---|
| Total story points | 5 |
| Effort senior | 1j |
| Effort intermédiaire | 1.3j |
| Effort junior | 2j |
| Nombre de sprints estimé | 1 |

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout de la gestion multi-tenant des cliniques pilotes |
| Breaking change | Non |
| Release cible | v0.4.0 |
