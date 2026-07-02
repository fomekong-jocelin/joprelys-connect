# PROJECT TRACKING — Suivi central technique et delivery

> Ce fichier doit être mis à jour après chaque intervention IA ou humaine.

## Statut global

| Champ | Valeur |
|---|---|
| Dernière mise à jour | 2026-07-03 |
| Responsable mise à jour | Antigravity |
| État global | En cours (Sprint 0003 — Authentification, DPU, Visites, Consultation, Ordonnance & Portail Patient finalisés à 100%) |
| Risques majeurs | Aucun risque technique ou environnemental bloquant. Stratégie de build et tests hors réseau validée. |
| Prochaine priorité | Démarrer le cadrage de la version mobile (Flutter) ou la planification du Sprint 0004. |
| Sprint courant | SPRINT-0003 |
| Capacité sprint | 15.0j |
| Charge engagée | 12.5j |
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
| TICKET-0109 | QUAL | Bug | Connexion Unifiée, Sélecteur de langue & LazyInit Bug | Full-stack | DONE | P0 | 2 | Intermédiaire | 0.2j | 0.3j | 0.5j | Antigravity | Lead | SPRINT-0003 | 0.25j | Aucun | Faible | 2026-07-02 |
| EPIC-0001 | AUTH | Epic | Authentification & Gestion des Rôles | Full-stack | BACKLOG | P0 | 11 | Senior | 2.5j | 3.4j | 5.3j | À assigner | Lead | À planifier | 0j | Stories initiales rédigées | Moyen | 2026-07-02 |
| STORY-0101 | AUTH | User Story | Connexion & Déconnexion Sécurisée | Full-stack | DONE | P0 | 3 | Intermédiaire | 0.5j | 0.65j | 1.1j | Codex | Lead | SPRINT-0002 | 0.5j | Aucun (tests validés avec succès) | Moyen | 2026-07-03 |
| STORY-0102 | AUTH | User Story | Contrôle d'Accès Basé sur les Rôles (RBAC) | Full-stack | DONE | P0 | 3 | Senior | 0.4j | 0.65j | 1.1j | Gemini | Lead | SPRINT-0002 | 0.4j | Aucun (tests et specs au vert) | Moyen | 2026-07-03 |
| STORY-0103 | AUTH | User Story | Récupération de Mot de Passe Simplifiée | Full-stack | DONE | P2 | 2 | Intermédiaire | 0.3j | 0.45j | 0.75j | Antigravity | Lead | SPRINT-0003 | 0.3j | Aucun (tests backend/frontend validés, release 0.5.0) | Faible | 2026-07-03 |
| STORY-0104 | AUTH | User Story | Invitation & Gestion du Personnel de Clinique | Full-stack | DONE | P1 | 3 | Intermédiaire | 0.5j | 0.8j | 1.3j | Antigravity / Codex | Lead | SPRINT-0003 | 0.55j | Aucun (tests unitaires et d'intégration validés avec succès) | Faible | 2026-07-02 |
| EPIC-0002 | CLIN | Epic | Gestion de la Clinique Pilote | Full-stack | DONE | P1 | 6 | Intermédiaire | 1.2j | 1.65j | 2.6j | Antigravity | Lead | SPRINT-0003 | 1.45j | Epic complétée et livrée en version 0.5.0 | Faible | 2026-07-03 |
| STORY-0201 | CLIN | User Story | Enregistrement de la Clinique Pilote & Multi-tenant | Full-stack | DONE | P0 | 3 | Intermédiaire | 1j | 1.3j | 2.2j | Gemini / Codex | Lead | SPRINT-0002 | 0.65j | Aucun | Moyen | 2026-07-02 |
| STORY-0202 | CLIN | User Story | Création de l'Administrateur Clinique par l'Admin Joprelys | Full-stack | DONE | P1 | 2 | Intermédiaire | 0.3j | 0.5j | 0.8j | Antigravity | Lead | SPRINT-0003 | 0.6j | Aucun (tests unitaires et d'intégration validés) | Faible | 2026-07-02 |
| TASK-0901 | CLIN | Amélioration UI | Amélioration IHM Gestion des Cliniques Pilotes | Full-stack | DONE | P1 | 1 | Intermédiaire | 0.2j | 0.35j | 0.6j | Antigravity | Lead | SPRINT-0003 | 0.2j | Aucun (tiroir détails, édition et admin en place, release 0.5.0) | Faible | 2026-07-03 |
| EPIC-0003 | PAT | Epic | Dossier Patient Unique (DPU) & Recherche | Full-stack | BACKLOG | P0 | 8 | Senior | 2j | 2.6j | 4j | À assigner | Lead | À planifier | 0j | Stories initiales rédigées | Moyen | 2026-07-01 |
| STORY-0301 | PAT | User Story | Enregistrement Patient & Génération du DPU | Full-stack | DONE | P0 | 3 | Intermédiaire | 1j | 1.3j | 2.2j | Antigravity | Lead | SPRINT-0002 | 0.65j | Aucun | Moyen | 2026-07-03 |
| STORY-0302 | PAT | User Story | Recherche de Patients Multicritères | Full-stack | DONE | P0 | 3 | Intermédiaire | 0.5j | 0.65j | 1.1j | Antigravity | Lead | SPRINT-0002 | 0.1j | Aucun | Faible | 2026-07-03 |
| EPIC-0004 | VISIT | Epic | Gestion des Visites & Constantes Vitales | Full-stack | READY | P0 | 5 | Intermédiaire | 1.5j | 2j | 3j | Antigravity / Codex | Lead | SPRINT-0002 | 1.50j | STORY-0402 terminée, STORY-0401 en review | Moyen | 2026-07-02 |
| STORY-0401 | VISIT | User Story | Ouverture & Clôture de Visite Patient | Full-stack | DONE | P0 | 2 | Junior | 0.5j | 0.65j | 1.1j | Antigravity | Lead | SPRINT-0002 | 0.5j | Aucun (tests unitaires et intégration validés avec succès) | Faible | 2026-07-03 |
| STORY-0402 | VISIT | User Story | Saisie des Constantes Vitales & Calcul IMC | Full-stack | DONE | P0 | 3 | Intermédiaire | 0.8j | 1.1j | 1.8j | Antigravity / Codex | Lead | SPRINT-0002 | 1.00j | Aucun | Faible | 2026-07-02 |
| EPIC-0005 | CONS | Epic | Consultation Médicale & Prescription | Full-stack | DONE | P0 | 8 | Senior | 2j | 2.6j | 4j | Antigravity | Lead | SPRINT-0003 | 2.7j | Validation visuelle frontend, build production | Moyen | 2026-07-03 |
| STORY-0501 | CONS | User Story | Saisie de la consultation médicale (backend) | Backend | DONE | P0 | 3 | Intermédiaire | 0.7j | 0.9j | 1.5j | Antigravity | Lead | SPRINT-0003 | 0.7j | Aucun (11/11 tests au vert) | Faible | 2026-07-03 |
| STORY-0502 | CONS | User Story | Saisie de la prescription simple (backend) | Backend | DONE | P0 | 2 | Intermédiaire | 0.5j | 0.65j | 1.1j | Antigravity | Lead | SPRINT-0003 | 0.65j | Aucun (9/9 tests au vert) | Faible | 2026-07-03 |
| STORY-0503 | CONS | User Story | Écran de consultation médecin (frontend Angular) | Frontend | DONE | P0 | 2 | Intermédiaire | 0.7j | 0.9j | 1.5j | Antigravity | Lead | SPRINT-0003 | 0.9j | Validation visuelle et build prod validés | Moyen | 2026-07-03 |
| STORY-0504 | CONS | User Story | Affichage historique des consultations | Full-stack | DONE | P1 | 1 | Junior | 0.35j | 0.45j | 0.7j | Antigravity | Lead | SPRINT-0003 | 0.55j | Aucun (affichage accordéon en direct médecin et patient validé) | Faible | 2026-07-02 |
| EPIC-0006 | DOC | Epic | Génération PDF & Vérification par QR Code | Full-stack | DONE | P0 | 8 | Senior | 2.5j | 3.2j | 5j | Antigravity | Lead | SPRINT-0003 | 1.0j | Aucun (Epic entièrement terminée) | Faible | 2026-07-02 |
| STORY-0601 | DOC | User Story | Génération et stockage du PDF de consultation | Backend | DONE | P0 | 3 | Senior | 1j | 1.3j | 2j | Antigravity | Lead | SPRINT-0003 | 1.0j | Aucun | Faible | 2026-07-02 |
| STORY-0602 | DOC | User Story | Page publique de vérification d'authenticité | Full-stack | DONE | P0 | 3 | Intermédiaire | 0.8j | 1.1j | 1.8j | Antigravity | Lead | SPRINT-0003 | 0.4j | Aucun (test et visual mapping ok) | Moyen | 2026-07-02 |
| STORY-0603 | DOC | User Story | Révocation et annulation de documents (backend) | Backend | DONE | P1 | 2 | Intermédiaire | 0.7j | 0.9j | 1.5j | Antigravity | Lead | SPRINT-0004 | 0.7j | Aucun (5/5 tests au vert) | Faible | 2026-07-02 |
| STORY-0604 | DOC | User Story | Écran de révocation côté frontend | Full-stack | DONE | P1 | 2 | Intermédiaire | 0.3j | 0.5j | 0.8j | Antigravity | Lead | SPRINT-0003 | 0.5j | Aucun (25/25 tests au vert) | Faible | 2026-07-02 |
| TICKET-0108 | GOV | Chore | Mise à niveau du kit de gouvernance v0.3.8 | Gouvernance | DONE | P0 | 1 | Intermédiaire | 0.1j | 0.2j | 0.4j | Antigravity | Lead | SPRINT-0003 | 0.1j | Aucun | Faible | 2026-07-02 |
| EPIC-0007 | AUDIT | Epic | Traçabilité & Audit Logs | Back-end | DONE | P1 | 5 | Intermédiaire | 1.5j | 2j | 3j | Antigravity | Lead | SPRINT-0003 | 1.35j | Epic entièrement terminée (backend + frontend) | Moyen | 2026-07-02 |
| STORY-0701 | AUDIT | User Story | Enregistrement et API REST des logs d'audit (backend) | Backend | DONE | P1 | 3 | Intermédiaire | 0.7j | 1.0j | 1.8j | Antigravity | Lead | SPRINT-0003 | 0.8j | Backend et tests d'intégration OK | Moyen | 2026-07-02 |
| STORY-0702 | AUDIT | User Story | Écran de visualisation et filtrage des logs d'audit (frontend) | Frontend | DONE | P2 | 2 | Intermédiaire | 0.5j | 0.7j | 1.1j | Antigravity | Lead | SPRINT-0003 | 0.5j | Timeline d'audit patient et tests unitaires OK | Faible | 2026-07-02 |
| EPIC-0008 | PAT_PORTAL | Epic | Portail Patient & Consentement | Full-stack | IN_PROGRESS | P1 | 18 | Senior | 5j | 8j | 12j | Antigravity | Lead | SPRINT-0003 | 3.3j | Epic entièrement terminée | Moyen | 2026-07-02 |
| STORY-0801 | PAT_PORTAL | User Story | Espace patient sécurisé et historique personnel | Full-stack | DONE | P1 | 5 | Intermédiaire | 1.2j | 1.8j | 3j | Antigravity | Lead | SPRINT-0003 | 0.9j | Aucun | Moyen | 2026-07-02 |
| STORY-0802 | PAT_PORTAL | User Story | Téléchargement sécurisé de ses propres ordonnances | Full-stack | DONE | P1 | 2 | Junior | 0.5j | 0.8j | 1.2j | Antigravity | Lead | SPRINT-0003 | 0.3j | Aucun | Faible | 2026-07-02 |
| STORY-0803 | PAT_PORTAL | User Story | Gestion des consentements d'accès du DPU | Full-stack | DONE | P0 | 8 | Senior | 2.5j | 4j | 6j | Antigravity | Lead | SPRINT-0003 | 0.9j | Aucun | Fort | 2026-07-02 |
| STORY-0804 | PAT_PORTAL | User Story | Journal de traçabilité des consultations du DPU | Full-stack | DONE | P2 | 3 | Intermédiaire | 0.8j | 1.4j | 1.8j | Antigravity | Lead | SPRINT-0003 | 1.2j | Aucun (traçabilité intégrée au niveau de l'AuditService) | Faible | 2026-07-02 |
| EPIC-0009 | LAB | Epic | Intégration Laboratoire & Examens Biologiques | Full-stack | BACKLOG | P1 | 7 | Intermédiaire | 1.8j | 2.5j | 4j | À assigner | Lead | À planifier | 0j | Cadrage initial et spécifications fonctionnelles/techniques rédigés | Faible | 2026-07-03 |
| STORY-0901 | LAB | User Story | Demande d'examens biologiques (médecin) | Full-stack | TODO | P1 | 1.5 | Intermédiaire | 0.3j | 0.5j | 0.8j | À assigner | Lead | SPRINT-0004 | 0j | Aucun | Faible | 2026-07-03 |
| STORY-0902 | LAB | User Story | API d'intégration labo externe pour téléversement | Backend | TODO | P1 | 3 | Senior | 0.8j | 1.1j | 1.8j | À assigner | Lead | SPRINT-0004 | 0j | Requiert API Key et validation GUID | Moyen | 2026-07-03 |
| STORY-0903 | LAB | User Story | Écran praticien de visualisation des résultats | Frontend | TODO | P1 | 2.5 | Intermédiaire | 0.7j | 0.9j | 1.4j | À assigner | Lead | SPRINT-0004 | 0j | Tracé de graphes d'évolution Angular | Faible | 2026-07-03 |
| EPIC-0010 | PHARMA | Epic | Dispensation en Pharmacie & Gestion des Prescriptions | Full-stack | BACKLOG | P1 | 7 | Intermédiaire | 1.8j | 2.6j | 4.2j | À assigner | Lead | À planifier | 0j | Cadrage initial et spécifications fonctionnelles/techniques rédigés | Moyen | 2026-07-03 |
| STORY-1001 | PHARMA | User Story | API de récupération sécurisée d'ordonnance | Backend | TODO | P1 | 2 | Intermédiaire | 0.5j | 0.7j | 1.1j | À assigner | Lead | SPRINT-0004 | 0j | Authentification double facteur GUID + PIN | Moyen | 2026-07-03 |
| STORY-1002 | PHARMA | User Story | Enregistrement de dispensation | Full-stack | TODO | P1 | 3 | Senior | 0.8j | 1.2j | 1.9j | À assigner | Lead | SPRINT-0004 | 0j | Coche de délivrance et gestion des génériques | Moyen | 2026-07-03 |
| STORY-1003 | PHARMA | User Story | Traçabilité & Statut ordonnance | Full-stack | TODO | P2 | 2 | Intermédiaire | 0.5j | 0.7j | 1.2j | À assigner | Lead | SPRINT-0004 | 0j | Changements d'état de prescription et logs d'audit | Faible | 2026-07-03 |

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
