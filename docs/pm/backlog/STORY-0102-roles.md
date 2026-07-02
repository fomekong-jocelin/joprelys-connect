# STORY-0102 — Contrôle d'Accès Basé sur les Rôles (RBAC)

## 1. User story

En tant qu'**utilisateur clinique connecté**, je veux que **l'accès aux pages et aux API soit restreint en fonction de mon rôle**, afin d'**assurer la sécurité des données patients et de respecter mon champ d'action professionnel**.

## 2. Critères d'acceptation

- [ ] Les rôles gérés sont : `ADMIN_JOPRELYS`, `ADMIN_CLINIQUE`, `AGENT_ACCUEIL`, `INFIRMIER`, `MEDECIN`.
- [ ] L'agent d'accueil (`AGENT_ACCUEIL`) peut créer un patient, ouvrir une visite et rechercher un patient. Il n'a pas accès aux écrans de saisie des constantes ou de consultation.
- [ ] L'infirmier (`INFIRMIER`) a accès à la recherche de patient, à la liste des visites actives et à l'écran de saisie des constantes vitales de tri. Il ne peut pas voir ni saisir de diagnostics ou de prescriptions.
- [ ] Le médecin (`MEDECIN`) a accès à la recherche patient, aux visites, aux constantes et à la saisie complète des consultations et ordonnances.
- [ ] Les API REST renvoient une erreur `403 Forbidden` si le token JWT ne contient pas le rôle requis.
- [ ] Le menu de navigation principal de l'interface client se met à jour dynamiquement pour n'afficher que les pages autorisées.

## 3. Périmètre

### Inclus

- Configuration de la sécurité des API Backend (Spring Security ou équivalent).
- Gestion des Guards/Routes de navigation Frontend.
- Menu de navigation dynamique.

### Exclus

- Rôles personnalisés dynamiques créés par l'utilisateur (les rôles sont codés en dur dans le MVP).

## 4. Tâches

| ID | Titre | Stack | Profil recommandé | SP | Estimation | Statut |
|---|---|---|---|---:|---:|---|
| TASK-0102-01 | Configuration Spring Security et annotation des contrôleurs | Backend | Senior | 2 | 0.5j | TODO |
| TASK-0102-02 | Guards de navigation pour le filtrage des routes | Frontend | Intermédiaire | 1 | 0.5j | TODO |
| TASK-0102-03 | Composant Menu dynamique basé sur les rôles | Frontend | Junior | 1 | 0.25j | TODO |
| TASK-0102-04 | Tests d'intégration de sécurité (simulation d'appels non autorisés) | Test | Senior | 1 | 0.5j | TODO |

## 5. Estimation

| Champ | Valeur |
|---|---|
| Story points | 3 |
| Complexité | S |
| Profil recommandé | Senior |
| Effort senior | 0.5j |
| Effort intermédiaire | 0.65j |
| Effort junior | 1.1j |
| Risque | Moyen |

## 6. Definition of Ready

- [x] Critères d'acceptation clairs
- [x] Dépendances connues
- [x] Profil recommandé identifié
- [x] Estimation faite
- [x] Reviewer identifié (Lead Developer)

## 7. Definition of Done

- [ ] Code terminé
- [ ] Tests de sécurité OK
- [ ] Review OK
- [ ] QA OK
- [ ] Documentation d'architecture mise à jour

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Mise en place de la sécurité RBAC sur les API et l'interface utilisateur |
| Breaking change | Non |
| Release cible | v0.4.0 |
