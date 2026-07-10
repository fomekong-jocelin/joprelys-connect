# PROJECT TRACKING — Suivi central technique et delivery

> Ce fichier doit être mis à jour après chaque intervention IA ou humaine.

## Statut global

| Champ | Valeur |
|---|---|
| Dernière mise à jour | 2026-07-10 (STORY-2201 implémentée et validée : contrat financier patient/assurance unifié, 282 tests backend, PostgreSQL 16, tests Angular et build verts) |
| Responsable mise à jour | Codex |
| État global | EPIC-0019 en cours ; EPIC-0020 en cours avec contrat d’état financier unifié prêt pour validation DAF/Product |
| Risques majeurs | Validation métier des définitions `PAID` / `SETTLED` ; DTO financiers résiduels en `Double` ; `PatientMedicalInfoComponent` monolithique au-dessus de 500 lignes |
| Prochaine priorité | Faire valider STORY-2201 par la DAF/Product, poursuivre STORY-2202 puis débloquer STORY-2203 |
| Sprint courant | SPRINT-0014 |
| | |
| Capacité sprint | À planifier |
| Charge engagée | 22.10j (Est. Senior) |
| Dérive globale | 0.0j |

## Tableau de suivi consolidé

| ID | Epic | Type | Titre | Stack | Statut | Priorité | SP | Profil recommandé | Est. Senior | Est. Intermédiaire | Est. Junior | Assigné | Reviewer | Sprint | Temps passé | Reste à faire | Risque | Dernière MAJ |
|---|---|---|---|---|---|---|---:|---|---:|---|---:|---|---|---|---:|---|---|---|
| TASK-20260710-P0-BASELINE-CLOSURE | TECHNICAL_BASELINE | Documentation | Clôture documentaire de la baseline P0 | Documentation / QA | DONE | P0 | 1 | Tech Lead / QA | 0.1j | 0.15j | 0.25j | Codex | Lead Developer | SPRINT-0014 | 0.1j | Aucun | Faible | 2026-07-10 |
| BUG-20260710-CI-BASELINE-EXECUTION | TECHNICAL_BASELINE | Bug CI/CD | Déblocage réel des tests Maven et Angular | CI/CD | DONE | P0 | 1 | DevOps / full-stack intermédiaire | 0.15j | 0.25j | 0.5j | Codex | Lead Developer | SPRINT-0014 | 0.15j | Aucun | Faible | 2026-07-10 |
| BUG-20260710-V55-H2-COMPATIBILITY | TECHNICAL_BASELINE | Bug DB | Migration V55 compatible H2 et PostgreSQL 16 | Backend / SQL | DONE | P0 | 2 | Backend Java / SQL senior | 0.4j | 0.7j | 1.2j | Codex | Lead Backend + référent données | SPRINT-0014 | 0.4j | Vérifier `flyway_schema_history` avant déploiement partagé | Faible | 2026-07-10 |
| BUG-20260710-BACKEND-TESTS-BIGDECIMAL | TECHNICAL_BASELINE | Bug Backend | Alignement BigDecimal des tests et quantités | Backend / QA | DONE | P0 | 2 | Backend Java intermédiaire / senior | 0.3j | 0.5j | 0.8j | Codex | Lead Backend + QA finance | SPRINT-0014 | 0.3j | DTO financiers résiduels en `Double` à traiter séparément | Faible | 2026-07-10 |
| EPIC-0019 | PROFESSIONAL_WORKSPACES | Epic | Postes métier professionnels hospitalisation et caisse | Full-stack | IN_PROGRESS | P0 | 34 | Senior full-stack + UX santé + Médecin Chef + DAF | 10.5j | 14.0j | 23.0j | Codex | Lead Developer + Médecin Chef + DAF | À planifier | 1.6j | Extraction consentements/bloc-CRO, validation métier et consolidation EPIC-0018 | Élevé | 2026-07-10 |
| EPIC-0018 | FINANCE_OPERATIONS | Epic | Intégrité financière et poste facturation/caisse | Full-stack | IN_PROGRESS | P0 | 34 | Senior full-stack + DAF | 7.5j | 10.0j | 15.0j | Codex | Lead Developer + DAF | À planifier | 3.0j | Validation DAF finale des règles financières | Moyen | 2026-07-10 |
| EPIC-0020 | FINANCE_UX | Epic | Refonte du workspace Facturation & Caisse orienté tâche | Full-stack + Product Design | IN_PROGRESS | P0 | 36 | Senior full-stack + UX santé + DAF + QA | 11.0j | 14.3j | 20.0j | Codex | Lead Developer + Product/DAF | SPRINT-0014 | 2.1j | STORY-2201 en QA ; workspace factures, caisse/assurance et QA globale restantes | Élevé | 2026-07-10 |
| STORY-2201 | FINANCE_UX | User Story | Contrat d'état financier unique patient/assurance | Backend / Full-stack | QA | P0 | 5 | Senior | 1.5j | 2.0j | 3.0j | Codex | Lead Developer + DAF | SPRINT-0014 | 1.2j | Validation DAF/Product des définitions `PAID` / `SETTLED`, revue et fusion PR #14 | Moyen | 2026-07-10 |
| STORY-2202 | FINANCE_UX | User Story | Workspace Factures orienté tâche | Frontend / Product Design | IN_PROGRESS | P0 | 8 | Senior | 2.5j | 3.3j | 4.5j | Codex | Lead Developer + Product/DAF | SPRINT-0014 | 0.8j | Lot 1 livré ; reste à traiter la densité de l'historique et les états de détail/validation | Élevé | 2026-07-10 |
| STORY-2203 | FINANCE_UX | User Story | Poste caissier simplifié et file d'encaissement | Full-stack | READY | P0 | 8 | Senior | 2.5j | 3.3j | 4.5j | À assigner | Lead Developer + DAF | À planifier | 0j | Dépend de la validation/fusion de STORY-2201 | Élevé | 2026-07-10 |
| STORY-2204 | FINANCE_UX | User Story | Poste assurance et progression des bordereaux | Full-stack | IN_PROGRESS | P1 | 5 | Senior + intermédiaire | 1.5j | 2.0j | 3.0j | Codex | DAF + Lead Developer | SPRINT-0014 | 0.1j | En-tête de liste corrigé ; parcours complet dépend de STORY-2201 | Moyen | 2026-07-10 |
| BUG-20260710-BORDEREAUX-HEADER | FINANCE_UX | Bug | En-tête de liste des bordereaux d'assurance comprimé | Frontend | QA | P2 | 1 | Frontend intermédiaire | 0.1j | 0.15j | 0.25j | Codex | Lead Frontend + DAF | SPRINT-0014 | 0.1j | QA visuelle manuelle light/dark et responsive | Faible | 2026-07-10 |
| BUG-20260710-PATIENT-MEDICAL-ICONS-I18N | UI_UX | Bug | Icônes et libellés Urgences incohérents dans le dossier médical patient | Frontend + diagnostic backend | QA | P1 | 2 | Frontend intermédiaire + reviewer backend | 0.3j | 0.5j | 0.8j | Codex | Lead Frontend + Lead Backend | SPRINT-0014 | 0.3j | QA visuelle ; refactor composant >500 lignes | Moyen | 2026-07-10 |
| STORY-2205 | FINANCE_UX | User Story | Détail facture, documents et actions exceptionnelles | Frontend | IN_PROGRESS | P1 | 5 | Senior | 1.5j | 2.0j | 3.0j | Codex | Lead Developer + DAF | SPRINT-0014 | 0.8j | Correction responsive compilée et testée ; QA visuelle navigateur restante | Moyen | 2026-07-10 |
| TASK-2207 | FINANCE_UX | Correctif UI/UX + tests | Modale annulation, visite devis, feedback et deep-link | Frontend | QA | P2 | 3 | Senior Frontend | 0.8j | 1.2j | 2.0j | Codex | Lead Developer + DAF | SPRINT-0014 | 0.8j | Exécuter Vitest/build puis QA light/dark, clavier et mobile | Moyen | 2026-07-10 |
| STORY-2206 | FINANCE_UX | User Story | QA UX, accessibilité et régression financière | QA / Full-stack | READY | P0 | 5 | Senior QA/full-stack | 1.5j | 2.0j | 3.0j | À assigner | Lead Developer + DAF | À planifier | 0j | Dépend de STORY-2201 à 2205 | Élevé | 2026-07-10 |
| STORY-2112 | FINANCE_OPERATIONS | Feature | Synthèse de règlement patient / assurance | Full-stack | DONE | P0 | 5 | Senior full-stack | 1.2j | 1.6j | 2.4j | Codex | Lead Developer + DAF | SPRINT-0012 | 1.2j | Validation DAF des libellés métier | Faible | 2026-07-09 |
| STORY-2113 | FINANCE_OPERATIONS | Feature | Poste caissier: encaissement, reçu, clôture, écarts | Full-stack | DONE | P0 | 8 | Senior full-stack | 2.0j | 2.6j | 4.0j | Codex | Lead Developer + DAF | SPRINT-0013 | 0.4j | Validé par test E2E complet (facture->patient->assurance->banque->clôture) | Faible | 2026-07-09 |
| STORY-2114 | FINANCE_OPERATIONS | Feature | Poste recouvrement: balance âgée, actions de relance | Full-stack | DONE | P1 | 8 | Senior full-stack | 2.0j | 2.6j | 4.0j | Codex | Lead Developer + DAF | SPRINT-0013 | 2.0j | Réalisé (V53 table, REST, DTO aging slice, modal timeline relances Angular) | Faible | 2026-07-09 |
| STORY-2115 | FINANCE_OPERATIONS | Feature | Pilotage DAF: sessions, exports comptables OHADA | Full-stack | DONE | P1 | 5 | Senior full-stack | 1.0j | 1.3j | 2.0j | Codex | Lead Developer + DAF | SPRINT-0013 | 1.0j | Réalisé (Flyway V54, DAF dashboard resolution écarts, CSV export Sage 100, tests ok) | Faible | 2026-07-09 |
| STORY-2116 | FINANCE_OPERATIONS | Feature | Tests E2E, RBAC, accessibilité, non-régression | Full-stack | DONE | P0 | 5 | Senior full-stack | 0.5j | 0.6j | 1.0j | Codex | Lead Developer + DAF | SPRINT-0013 | 0.5j | Terminé — tests backend E2E + Vitest Angular + build prod OK | Faible | 2026-07-09 |
| STORY-2111 | FINANCE_OPERATIONS | Correctif | Synchronisation règlement facture / créance patient | Full-stack | DONE | P0 | 3 | Senior full-stack | 0.8j | 1.1j | 1.6j | Codex | Lead Developer + DAF | SPRINT-0012 | 0.8j | Remplacé par le contrat unifié STORY-2201 | Faible | 2026-07-10 |
