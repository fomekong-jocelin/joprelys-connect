# PROJECT TRACKING — Suivi central technique et delivery

> Ce fichier doit être mis à jour après chaque intervention IA ou humaine.

## Statut global

| Champ | Valeur |
|---|---|
| Dernière mise à jour | 2026-07-02 |
| Responsable mise à jour | Antigravity |
| État global | En cours (Sprint 0002, STORY-0401, STORY-0301, STORY-0302 et STORY-0201 en review) |
| Risques majeurs | Aucun (le build de production réussit sous Node.js 25.9.0) |
| Prochaine priorité | Démarrer STORY-0402 (Saisie des constantes vitales et calcul IMC) |
| Sprint courant | SPRINT-0002 |
| Capacité sprint | 15.0j |
| Charge engagée | 5.70j |
| Dérive globale | 0.0j |

## Tableau de suivi consolidé

| ID | Epic | Type | Titre | Stack | Statut | Priorité | SP | Profil recommandé | Est. Senior | Est. Intermédiaire | Est. Junior | Assigné | Reviewer | Sprint | Temps passé | Reste à faire | Risque | Dernière MAJ |
|---|---|---|---|---|---|---|---:|---|---:|---:|---:|---|---|---|---:|---|---|---|
| TICKET-0001 | GOV | Gouvernance | Mettre en place la documentation IA centralisée | Full-stack | DONE | P0 | 3 | Senior | 0.5j | 0.75j | 1j | Gemini | Lead | SPRINT-0001 | 0.5j | Adapter les tests et créer le backlog | Faible | 2026-07-01 |
| TICKET-0002 | QUAL | Qualité | Appliquer la checklist aux prochaines PR | Full-stack | TODO | P0 | 2 | Intermédiaire | 0.5j | 0.75j | 1j | À assigner | Lead | À planifier | 0j | Créer les tickets réels | Moyen | 2026-07-01 |
| TICKET-0102 | QUAL | Bug | Configuration de la DataSource PostgreSQL au démarrage | Back-end | DONE | P0 | 1 | Intermédiaire | 0.1j | 0.2j | 0.3j | Gemini | Lead | SPRINT-0002 | 0.1j | Aucun | Faible | 2026-07-02 |
| TICKET-0103 | QUAL | Bug | Configuration du Proxy de Développement Frontend | Front-end | DONE | P0 | 1 | Intermédiaire | 0.05j | 0.1j | 0.2j | Gemini | Lead | SPRINT-0002 | 0.1j | Aucun | Faible | 2026-07-02 |
| TICKET-0104 | QUAL | DevOps | Migration Gradle -> Maven (Backend) & Intégration Tailwind v4 (Frontend) | Full-stack | DONE | P0 | 3 | Senior | 0.2j | 0.35j | 0.6j | Gemini | Lead | SPRINT-0002 | 0.3j | Aucun | Faible | 2026-07-02 |
| TICKET-0105 | QUAL | DevOps | Stylisation UI Tailwind CSS & Mise à jour de la Gouvernance v0.3.4 | Full-stack | DONE | P0 | 2 | Intermédiaire | 0.1j | 0.2j | 0.35j | Gemini | Lead | SPRINT-0002 | 0.2j | Aucun | Faible | 2026-07-02 |
| TICKET-0106 | QUAL | Front-end | Refonte UI Épurée & Intégration de la Charte Graphique | Front-end | DONE | P0 | 2 | Senior | 0.1j | 0.15j | 0.3j | Gemini | Lead | SPRINT-0002 | 0.15j | Aucun | Faible | 2026-07-02 |
| TICKET-0107 | QUAL | Front-end | Correction de l'Accessibilité et des Contrastes Visuels (WCAG) | Front-end | DONE | P0 | 1 | Intermédiaire | 0.03j | 0.05j | 0.1j | Gemini | Lead | SPRINT-0002 | 0.05j | Aucun | Faible | 2026-07-02 |
| EPIC-0001 | AUTH | Epic | Authentification & Gestion des Rôles | Full-stack | BACKLOG | P0 | 11 | Senior | 2.5j | 3.4j | 5.3j | À assigner | Lead | À planifier | 0j | Stories initiales rédigées | Moyen | 2026-07-02 |
| STORY-0101 | AUTH | User Story | Connexion & Déconnexion Sécurisée | Full-stack | REVIEW | P0 | 3 | Intermédiaire | 0.5j | 0.65j | 1.1j | Codex | Lead | SPRINT-0002 | 0.5j | Aucun (tests validés avec succès) | Moyen | 2026-07-02 |
| STORY-0102 | AUTH | User Story | Contrôle d'Accès Basé sur les Rôles (RBAC) | Full-stack | REVIEW | P0 | 3 | Senior | 0.4j | 0.65j | 1.1j | Gemini | Lead | SPRINT-0002 | 0.4j | Aucun (tests et specs au vert) | Moyen | 2026-07-02 |
| STORY-0103 | AUTH | User Story | Récupération de Mot de Passe Simplifiée | Back-end | Intermédiaire | TODO | P2 | 2 | Intermédiaire | 0.3j | 0.45j | 0.75j | À assigner | Lead | SPRINT-0003 | 0j | Prêt pour le dev | Faible | 2026-07-02 |
| STORY-0104 | AUTH | User Story | Invitation & Gestion du Personnel de Clinique | Full-stack | Intermédiaire | TODO | P1 | 3 | Intermédiaire | 0.5j | 0.8j | 1.3j | À assigner | Lead | SPRINT-0003 | 0j | Prêt pour le dev | Moyen | 2026-07-02 |
| EPIC-0002 | CLIN | Epic | Gestion de la Clinique Pilote | Full-stack | READY | P1 | 5 | Intermédiaire | 1j | 1.3j | 2j | Gemini | Lead | SPRINT-0002 | 0.55j | Prêt pour le dev | Faible | 2026-07-02 |
| STORY-0201 | CLIN | User Story | Enregistrement de la Clinique Pilote & Multi-tenant | Full-stack | REVIEW | P0 | 3 | Intermédiaire | 1j | 1.3j | 2.2j | Gemini / Codex | Lead | SPRINT-0002 | 0.65j | Validation visuelle mobile + build production sous Node pair/LTS | Moyen | 2026-07-02 |
| EPIC-0003 | PAT | Epic | Dossier Patient Unique (DPU) & Recherche | Full-stack | BACKLOG | P0 | 8 | Senior | 2j | 2.6j | 4j | À assigner | Lead | À planifier | 0j | Stories initiales rédigées | Moyen | 2026-07-01 |
| STORY-0301 | PAT | User Story | Enregistrement Patient & Génération du DPU | Full-stack | REVIEW | P0 | 3 | Intermédiaire | 1j | 1.3j | 2.2j | Antigravity | Lead | SPRINT-0002 | 0.65j | Aucun | Moyen | 2026-07-02 |
| STORY-0302 | PAT | User Story | Recherche de Patients Multicritères | Full-stack | REVIEW | P0 | 3 | Intermédiaire | 0.5j | 0.65j | 1.1j | Antigravity | Lead | SPRINT-0002 | 0.1j | Aucun | Faible | 2026-07-02 |
| EPIC-0004 | VISIT | Epic | Gestion des Visites & Constantes Vitales | Full-stack | READY | P0 | 5 | Intermédiaire | 1.5j | 2j | 3j | Antigravity | Lead | SPRINT-0002 | 0.52j | Story 0401 complétée | Moyen | 2026-07-02 |
| STORY-0401 | VISIT | User Story | Ouverture & Clôture de Visite Patient | Full-stack | REVIEW | P0 | 2 | Junior | 0.5j | 0.65j | 1.1j | Antigravity | Lead | SPRINT-0002 | 0.5j | Aucun (tests unitaires et intégration validés avec succès) | Faible | 2026-07-02 |
| EPIC-0005 | CLINIC | Epic | Consultation Médicale & Prescription | Full-stack | BACKLOG | P0 | 8 | Senior | 2j | 2.6j | 4j | À assigner | Lead | À planifier | 0j | Définir les stories détaillées | Moyen | 2026-07-01 |
| EPIC-0006 | DOC | Epic | Génération PDF & Vérification par QR Code | Full-stack | BACKLOG | P0 | 8 | Senior | 2.5j | 3.2j | 5j | À assigner | Lead | À planifier | 0j | Définir les stories détaillées | Fort | 2026-07-01 |
| EPIC-0007 | AUDIT | Epic | Traçabilité & Audit Logs | Back-end | BACKLOG | P1 | 5 | Intermédiaire | 1.5j | 2j | 3j | À assigner | Lead | À planifier | 0j | Définir les stories détaillées | Moyen | 2026-07-01 |

## Statuts autorisés

- BACKLOG
- READY
- TODO
- IN_PROGRESS
- BLOCKED
- REVIEW
- QA
- DONE
- CANCELLED

## Types autorisés

- Epic
- User Story
- Task
- Bug
- Refactoring
- Security
- Documentation
- DevOps
- Architecture
- UI/UX
- Test
- Gouvernance
- Spike

## Priorités

- P0 — Bloquant / sécurité / régression critique
- P1 — Important avant livraison
- P2 — Amélioration nécessaire
- P3 — Confort / dette mineure

## Règles de mise à jour

À chaque ticket traité :

- [ ] Ajouter ou mettre à jour une ligne dans le tableau
- [ ] Vérifier que le statut correspond au ticket
- [ ] Indiquer le sprint cible
- [ ] Indiquer le profil recommandé
- [ ] Indiquer l'estimation et le temps passé
- [ ] Indiquer ce qui reste à faire
- [ ] Mettre à jour la date
- [ ] Reporter les risques majeurs dans la section statut global

## Lecture des dérives

| Cas | Interprétation possible |
|---|---|
| Temps passé > estimation sans blocage | Revoir estimation, profil, autonomie ou performance |
| Beaucoup de tickets BLOCKED | Problème de dépendance ou cadrage |
| Beaucoup de tickets REVIEW longtemps | Problème review, qualité ou disponibilité lead |
| Beaucoup de réouvertures | Problème qualité, tests ou DoD |
| Beaucoup de tickets IN_PROGRESS | WIP trop élevé, dispersion |
