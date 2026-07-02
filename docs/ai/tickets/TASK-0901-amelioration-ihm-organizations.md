# TASK-0901 — Amélioration IHM Gestion des Cliniques Pilotes

## Métadonnées

| Champ | Valeur |
|---|---|
| ID | TASK-0901 |
| Epic | EPIC-0002 — Gestion de la Clinique Pilote |
| Type | Amélioration UI / Engineering |
| Priorité | P1 |
| Story Points | 1 SP |
| Sprint | SPRINT-0003 |
| Statut | DONE |
| Assigné | Antigravity |
| Reviewer | Lead |
| Profil recommandé | Intermédiaire |
| Est. Senior | 0.2j |
| Est. Intermédiaire | 0.35j |
| Est. Junior | 0.6j |
| Temps passé | 0.2j |
| Dépendances | STORY-0201 |
| Dernière MAJ | 2026-07-03 |

---

## Objectif

Améliorer l'interface de gestion des cliniques pilotes pour les administrateurs Joprelys. Rendre l'IHM esthétique, intuitive et riche en fonctionnalités :
1. Afficher directement les administrateurs de cliniques dans le tableau de bord.
2. Permettre la consultation détaillée d'une clinique dans un tiroir latéral (Drawer) moderne et animé.
3. Permettre la modification en place des informations d'une clinique.
4. Faciliter l'affectation et le remplacement des administrateurs cliniques.

---

## Critères d'acceptation

- [x] L'administrateur Joprelys peut voir directement le nom et l'email de l'administrateur de chaque clinique dans la table desktop et les fiches mobiles.
- [x] Cliquer sur une clinique ouvre un magnifique panneau latéral (tiroir) à droite de l'écran avec une transition de glissement fluide.
- [x] Le tiroir affiche les détails complets (Identité, Contacts, Administrateur avec bouton d'affectation/remplacement).
- [x] Un bouton "Modifier les détails" dans le tiroir bascule les informations en mode formulaire modifiable.
- [x] La modification d'une clinique appelle l'endpoint `PUT /api/organizations/{id}` et met à jour les informations en temps réel dans le tableau.
- [x] Le statut d'une clinique (Active/Inactive) peut être changé directement depuis le tiroir de détails ou le tableau.

---

## Action plan

- [x] **Backend** :
  - [x] Créer le DTO `UpdateOrganizationRequest.java` pour recevoir les modifications de clinique.
  - [x] Mettre à jour `OrganizationResponse.java` pour inclure `adminEmail` et `adminDisplayName`.
  - [x] Implémenter le endpoint de modification `PUT /api/organizations/{id}` dans `OrganizationController.java`.
  - [x] Mettre à jour `mapToResponse` dans `OrganizationController` pour récupérer l'utilisateur avec le rôle `ADMIN_CLINIQUE` associé à la clinique.
  - [x] Écrire les tests unitaires et d'intégration dans `OrganizationControllerTest.java`.
- [x] **Frontend** :
  - [x] Ajouter la méthode `update` dans le service `OrganizationApiService.ts`.
  - [x] Mettre à jour l'interface `Organization` dans `organizations.models.ts` pour inclure les champs de l'administrateur.
  - [x] Mettre à jour `OrganizationTableComponent` pour inclure la colonne Administrateur et émettre l'événement de détails au clic.
  - [x] Mettre à jour le template `organization-list.component.html` avec le tiroir latéral interactif (Drawer) et les modes lecture/édition.
  - [x] Mettre à jour la logique de `OrganizationListComponent` pour gérer la sélection, l'édition, la mise à jour et la synchronisation avec le tableau.
- [x] **Validation & Suivi** :
  - [x] Lancer les tests unitaires backend et frontend (100% au vert).
  - [x] Lancer le build de production frontend (Build Success).
  - [x] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`.

---

## Risques & Régression

* **Faible** : Les modifications s'intègrent dans la gestion des cliniques. L'authentification multi-tenant et la sécurité RBAC (`hasRole('ADMIN_JOPRELYS')`) sont préservées et testées.
