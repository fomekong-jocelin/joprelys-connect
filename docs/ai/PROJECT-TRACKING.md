# PROJECT TRACKING — Suivi central technique et delivery

> Ce fichier doit être mis à jour après chaque intervention IA ou humaine.

## Statut global

| Champ | Valeur |
|---|---|
| Dernière mise à jour | 2026-07-05 (complétion module 1 TICKET-0112) |
| Responsable mise à jour | Antigravity |
| État global | Sprint 0009 — Audit complet des modules et interopérabilité HL7 FHIR |
| Risques majeurs | Aucun |
| Prochaine priorité | Validation finale et préparation livraison |
| Sprint courant | SPRINT-0009 |
| | |
| Capacité sprint | 15.0j |
| Charge engagée | 7.05j (Est. Senior) |
| Dérive globale | 0.0j |

## Tableau de suivi consolidé

| ID | Epic | Type | Titre | Stack | Statut | Priorité | SP | Profil recommandé | Est. Senior | Est. Intermédiaire | Est. Junior | Assigné | Reviewer | Sprint | Temps passé | Reste à faire | Risque | Dernière MAJ |
|---|---|---|---|---|---|---|---:|---|---:|---|---:|---|---|---|---:|---|---|---|
| TICKET-0001 | GOV | Gouvernance | Mettre en place la documentation IA centralisée | Full-stack | DONE | P0 | 3 | Senior | 0.5j | 0.75j | 1j | Gemini | Lead | SPRINT-0001 | 0.5j | Adapter les tests et créer le backlog | Faible | 2026-07-01 |
| TICKET-0002 | QUAL | Task | Appliquer la checklist de review aux futures PR | Full-stack | READY | P0 | 2 | Intermédiaire | 0.5j | 0.75j | 1j | Lead Developer | Gemini | SPRINT-0006 | 0j | Appliquer la checklist aux PR | Faible | 2026-07-03 |
| TICKET-AUDIT-MODULES-COMPARISON | QUAL | Gouvernance | Audit et analyse d'écart des modules par rapport au CDC | Full-stack | DONE | P1 | 2 | Tech Lead | 0.2j | 0.3j | 0.5j | Antigravity | Lead | SPRINT-0009 | 0.15j | Aucun (Rapport d'audit rédigé) | Faible | 2026-07-05 |
| TICKET-0102 | QUAL | Bug | Configuration de la DataSource PostgreSQL au démarrage | Back-end | DONE | P0 | 1 | Intermédiaire | 0.1j | 0.2j | 0.3j | Gemini | Lead | SPRINT-0002 | 0.1j | Aucun | Faible | 2026-07-02 |
| TICKET-0103 | QUAL | Bug | Configuration du Proxy de Développement Frontend | Front-end | DONE | P0 | 1 | Intermédiaire | 0.05j | 0.1j | 0.2j | Gemini | Lead | SPRINT-0002 | 0.1j | Aucun | Faible | 2026-07-02 |
| TICKET-0104 | QUAL | DevOps | Migration Gradle -> Maven (Backend) & Intégration Tailwind v4 (Frontend) | Full-stack | DONE | P0 | 3 | Senior | 0.2j | 0.35j | 0.6j | Gemini | Lead | SPRINT-0002 | 0.3j | Aucun | Faible | 2026-07-02 |
| TICKET-0105 | QUAL | DevOps | Stylisation UI Tailwind CSS & Mise à jour de la Gouvernance v0.3.4 | Full-stack | DONE | P0 | 2 | Intermédiaire | 0.1j | 0.2j | 0.35j | Gemini | Lead | SPRINT-0002 | 0.2j | Aucun | Faible | 2026-07-02 |
| TICKET-0106 | QUAL | Front-end | Refonte UI Épurée & Intégration de la Charte Graphique | Front-end | DONE | P0 | 2 | Senior | 0.1j | 0.15j | 0.3j | Gemini | Lead | SPRINT-0002 | 0.15j | Aucun | Faible | 2026-07-02 |
| TICKET-0107 | QUAL | Front-end | Correction de l'Accessibilité et des Contrastes Visuels (WCAG) | Front-end | DONE | P0 | 1 | Intermédiaire | 0.03j | 0.05j | 0.1j | Gemini | Lead | SPRINT-0002 | 0.05j | Aucun | Faible | 2026-07-02 |
| TICKET-0108 | UI_UX | Task | Alignement esthétique des inputs, checkboxes, radios, et selects | Frontend | DONE | P1 | 2 | Senior | 0.2j | 0.3j | 0.5j | Antigravity | Lead Developer | SPRINT-0009 | 0.2j | Aucun (harmonisation complète et compilation OK) | Faible | 2026-07-05 |
| TICKET-0109 | QUAL | Bug | Connexion Unifiée, Sélecteur de langue & LazyInit Bug | Full-stack | DONE | P0 | 2 | Intermédiaire | 0.2j | 0.3j | 0.5j | Antigravity | Lead | SPRINT-0003 | 0.25j | Aucun | Faible | 2026-07-02 |
| TICKET-0110 | UI_UX | Task | Conformité Patient et DPU (Vaccinations, Groupe Sanguin, Email, Doublons) | Full-stack | DONE | P1 | 3 | Senior | 0.3j | 0.5j | 0.8j | Antigravity | Lead Developer | SPRINT-0009 | 0.35j | Aucun (implémentation et tests complets) | Faible | 2026-07-05 |
| TICKET-0111 | UI_UX | Task | Résolution de la navigation et responsivité mobile (Mobile First App-like) | Frontend | DONE | P0 | 2 | Senior | 0.3j | 0.35j | 0.6j | Antigravity | Lead Developer | SPRINT-0009 | 0.3j | Aucun (Hamburger, Drawer, Backdrop, factorisation menu, suppression double padding, paddings container/carte/panneaux, boutons empiles, wrap DPU, actualiser mobile icone, polices reduites) | Faible | 2026-07-05 |
| TICKET-0112 | UI_UX | Task | Complétion de la gestion des établissements (Module 1 - type, pays, responsable, clés API) | Full-stack | DONE | P1 | 3 | Senior | 0.4j | 0.6j | 1.0j | Antigravity | Lead Developer | SPRINT-0009 | 0.35j | Aucun (migration DB, entities backend, filtre de sécurité X-API-KEY, IHM formulaire et panneau de clés API Angular en place) | Faible | 2026-07-05 |
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
| TICKET-UI-DPU-PATIENT-TABLE-READABILITY | PAT | UI/UX | Lisibilité du tableau patients DPU | Frontend | DONE | P2 | 0.5 | Intermédiaire | 0.03j | 0.05j | 0.08j | Codex | Lead | SPRINT-0004 | 0.05j | Aucun | Faible | 2026-07-03 |
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
| TICKET-UI-PATIENT-PORTAL-PREMIUM-REDESIGN | PAT_PORTAL | UI/UX | Amélioration premium du portail patient | Frontend | DONE | P1 | 1 | Intermédiaire | 0.1j | 0.15j | 0.25j | Codex | Lead | SPRINT-0004 | 0.15j | Aucun | Faible | 2026-07-03 |
| TICKET-UI-PATIENT-PORTAL-CONSENT-SECURITY-REDESIGN | PAT_PORTAL | UI/UX | Redesign premium des consentements et de la sécurité du portail patient | Frontend | DONE | P1 | 2 | Intermédiaire | 0.1j | 0.2j | 0.35j | Antigravity | Lead | SPRINT-0004 | 0.2j | Aucun (tests et build validés) | Faible | 2026-07-03 |
| TICKET-UI-FIXED-TOPBAR | PAT_PORTAL | UI/UX | Rendre le header topbar fixe et sticky | Frontend | DONE | P2 | 1 | Junior | 0.02j | 0.05j | 0.1j | Antigravity | Lead | SPRINT-0004 | 0.05j | Aucun (tests et build validés) | Faible | 2026-07-03 |
| STORY-0802 | PAT_PORTAL | User Story | Téléchargement sécurisé de ses propres ordonnances | Full-stack | DONE | P1 | 2 | Junior | 0.5j | 0.8j | 1.2j | Antigravity | Lead | SPRINT-0003 | 0.3j | Aucun | Faible | 2026-07-02 |
| STORY-0803 | PAT_PORTAL | User Story | Gestion des consentements d'accès du DPU | Full-stack | DONE | P0 | 8 | Senior | 2.5j | 4j | 6j | Antigravity | Lead | SPRINT-0003 | 0.9j | Aucun | Fort | 2026-07-02 |
| STORY-0804 | PAT_PORTAL | User Story | Journal de traçabilité des consultations du DPU | Full-stack | DONE | P2 | 3 | Intermédiaire | 0.8j | 1.4j | 1.8j | Antigravity | Lead | SPRINT-0003 | 1.2j | Aucun (traçabilité intégrée au niveau de l'AuditService) | Faible | 2026-07-02 |
| EPIC-0009 | LAB | Epic | Intégration Laboratoire & Examens Biologiques + Portail Laboratoire CDC | Full-stack | DONE | P1 | 14 | Intermédiaire | 3.6j | 5.0j | 8.2j | Codex / Antigravity | Lead | SPRINT-0004 | 3.8j | Aucun (Epic entièrement livrée, rôle dédié, filtres et upload validés) | Faible | 2026-07-03 |
| STORY-0901 | LAB | User Story | Demande d'examens biologiques (médecin) | Full-stack | DONE | P1 | 1.5 | Intermédiaire | 0.3j | 0.5j | 0.8j | Antigravity | Lead | SPRINT-0004 | 0.4j | Aucun (tests backend validés, compilation frontend OK) | Faible | 2026-07-03 |
| STORY-0902 | LAB | User Story | API d'intégration labo externe pour téléversement | Backend | DONE | P1 | 3 | Senior | 0.8j | 1.1j | 1.8j | Antigravity | Lead | SPRINT-0004 | 0.8j | Aucun (tests unitaires et d'intégration validés avec succès) | Moyen | 2026-07-03 |
| STORY-0903 | LAB | User Story | Écran praticien de visualisation des résultats | Frontend | DONE | P1 | 2.5 | Intermédiaire | 0.7j | 0.9j | 1.4j | Antigravity | Lead | SPRINT-0004 | 0.9j | Aucun (tests unitaires au vert) | Faible | 2026-07-03 |
| STORY-0904 | LAB | User Story | Portail laboratoire — tableau de bord et demandes reçues | Full-stack | DONE | P1 | 2 | Intermédiaire | 0.5j | 0.7j | 1.2j | Codex / Antigravity | Lead | SPRINT-0004 | 0.5j | Aucun (rôle biologiste dédié et filtres de recherche intégrés) | Faible | 2026-07-03 |
| STORY-0905 | LAB | User Story | Portail laboratoire — détail demande et changement de statut | Full-stack | DONE | P1 | 2 | Intermédiaire | 0.5j | 0.7j | 1.2j | Codex / Antigravity | Lead | SPRINT-0004 | 0.5j | Aucun (détail et transitions de statut validées et testées) | Faible | 2026-07-03 |
| STORY-0906 | LAB | User Story | Portail laboratoire — saisie, validation résultat, PDF et historique | Full-stack | DONE | P1 | 3 | Senior | 0.8j | 1.1j | 1.8j | Codex / Antigravity | Lead | SPRINT-0004 | 0.7j | Aucun (saisie multi-analytes, historique et validation PDF strictes ok) | Faible | 2026-07-03 |
| EPIC-0010 | PHARMA | Epic | Dispensation en Pharmacie & Gestion des Prescriptions + Portail Pharmacie CDC | Full-stack | DONE | P1 | 12 | Intermédiaire | 3.0j | 4.2j | 6.8j | Codex / Antigravity | Lead | SPRINT-0004 | 2.2j | Aucun (Epic entièrement livrée, portail pharmacie opérationnel et tests ok) | Faible | 2026-07-03 |
| STORY-1001 | PHARMA | User Story | API de récupération sécurisée d'ordonnance | Backend | DONE | P1 | 2 | Intermédiaire | 0.4j | 0.6j | 1.0j | Antigravity | Lead | SPRINT-0004 | 0.4j | Aucun (API de vérification prescription validée par tests) | Faible | 2026-07-03 |
| STORY-1002 | PHARMA | User Story | Enregistrement de dispensation | Backend | DONE | P1 | 3 | Senior | 0.8j | 1.1j | 1.8j | Antigravity | Lead | SPRINT-0004 | 0.8j | Aucun (enregistrement et validations de dispensation ok) | Faible | 2026-07-03 |
| STORY-1003 | PHARMA | User Story | Traçabilité & Statut ordonnance | Full-stack | DONE | P1 | 2 | Intermédiaire | 0.3j | 0.5j | 0.8j | Antigravity | Lead | SPRINT-0004 | 0.3j | Aucun (gestion des statuts fully/partially/expired intégrée) | Faible | 2026-07-03 |
| STORY-1004 | PHARMA | User Story | Portail pharmacie — vérification et détail ordonnance | Frontend | DONE | P1 | 2.5 | Intermédiaire | 0.6j | 0.8j | 1.3j | Codex | Lead | SPRINT-0004 | 0.4j | Aucun côté écran ; scan QR physique hors périmètre | Faible | 2026-07-03 |
| STORY-1005 | PHARMA | User Story | Portail pharmacie — délivrance et historique | Full-stack | DONE | P1 | 2.5 | Intermédiaire | 0.6j | 0.8j | 1.3j | Codex / Antigravity | Lead | SPRINT-0004 | 0.7j | Aucun (tests unitaires frontend et d'intégration backend passés avec succès) | Faible | 2026-07-03 |
| TICKET-1010 | PHARMA | Bug | Correction validation Hibernate du schema pharmacie | Back-end | DONE | P0 | 1 | Intermédiaire | 0.1j | 0.15j | 0.25j | Codex | Lead | SPRINT-0004 | 0.1j | Relancer le backend sur PostgreSQL local | Faible | 2026-07-03 |
| TICKET-1011 | LAB | Bug | Correction validation Hibernate du schema lab_results | Back-end | DONE | P0 | 1 | Intermédiaire | 0.05j | 0.1j | 0.2j | Codex | Lead | SPRINT-0004 | 0.05j | Relancer le backend sur PostgreSQL local | Faible | 2026-07-03 |
| TICKET-UI-PHARMACY-LAB-HEADER-FIX | PHARMA/LAB | Bug | Alignement du header pharmacie/labo et résolution du double header | Frontend | DONE | P1 | 2 | Intermédiaire | 0.15j | 0.2j | 0.3j | Antigravity | Lead | SPRINT-0004 | 0.15j | Aucun (enveloppe app-shell, page-header, guards de route configurés) | Faible | 2026-07-03 |
| STORY-1101 | DEVOPS | User Story | CI/CD Automatisation & Compilation strictes | Full-stack | DONE | P0 | 3 | Senior | 0.7j | 1.0j | 1.8j | Antigravity | Lead | SPRINT-0005 | 0.7j | Aucun (pipeline CI créé) | Faible | 2026-07-03 |
| STORY-1102 | PAT_PORTAL | User Story | Audit OWASP & Sécurisation IDOR Portail Patient | Full-stack | DONE | P0 | 5 | Senior | 1.1j | 1.5j | 2.5j | Antigravity | Lead | SPRINT-0005 | 1.1j | Aucun (PatientAccessGuardService créé, tests IDOR au vert) | Fort | 2026-07-03 |
| STORY-1103 | PHARMA | User Story | Gestion réelle des stocks de médicaments | Full-stack | DONE | P1 | 8 | Intermédiaire | 1.8j | 2.3j | 4.0j | Antigravity | Lead | SPRINT-0005 | 1.8j | Tests MockMvc et IHM Angular à implémenter | Moyen | 2026-07-03 |
| STORY-1104 | LAB | User Story | Import de résultats structurés de laboratoire (FHIR/HL7) | Full-stack | DONE | P1 | 5 | Senior | 1.2j | 1.6j | 2.8j | Antigravity | Lead | SPRINT-0005 | 1.2j | Aucun (FhirDiagnosticReportParser Jackson, endpoint FHIR POST, 4 tests unitaires purs) | Fort | 2026-07-03 |
| STORY-1105 | QUAL | User Story | Nettoyage de la dette technique & Application de la checklist QA | Full-stack | DONE | P2 | 2 | Junior | 0.4j | 0.5j | 0.8j | Antigravity | Lead | SPRINT-0005 | 0.4j | Aucun (5 dépendances Maven invalides supprimées, spring-security-test ajouté, application-test.yml complet, repair-on-migrate activé) | Faible | 2026-07-03 |
| STORY-1201 | PAT | User Story | Module Allergies & Antécédents Médicaux | Full-stack | DONE | P1 | 5 | Intermédiaire | 0.8j | 1.2j | 2.0j | Gemini | Lead Developer | SPRINT-0006 | 0.8j | Aucun | Faible | 2026-07-03 |
| STORY-1202 | CLIN | User Story | Module Hospitalisations, lits et notes journalières | Full-stack | DONE | P1 | 8 | Senior | 1.8j | 2.5j | 4.0j | Gemini | Lead Developer | SPRINT-0006 | 1.8j | Aucun | Moyen | 2026-07-04 |
| STORY-1301 | DPU_ACCESS | User Story | Enregistrement de demande d'accès externe (backend) | Backend | DONE | P1 | 5 | Intermédiaire | 0.8j | 1.2j | 2.0j | Gemini | Lead Developer | SPRINT-0007 | 1.2j | Entité, Repository, Service et API REST pour requêtes d'accès temporaires + 5 tests unitaires MockMvc | Moyen | 2026-07-04 |
| STORY-1302 | DPU_ACCESS | User Story | Validation de demande d'accès externe (portail patient) | Full-stack | DONE | P1 | 3 | Intermédiaire | 0.5j | 0.8j | 1.3j | Gemini | Lead Developer | SPRINT-0007 | 0.8j | Aucun (tests backend/frontend validés) | Faible | 2026-07-04 |
| STORY-1303 | DPU_ACCESS | User Story | Contrôle d'accès & Expiration des droits externes | Backend | DONE | P0 | 5 | Senior | 1.0j | 1.5j | 2.5j | Gemini | Lead Developer | SPRINT-0007 | 1.5j | Aucun (tests unitaires et intégration de sécurité au vert) | Fort | 2026-07-04 |
| STORY-1501 | NOTIF | User Story | Socle et service d'envoi de notifications (backend) | Backend | DONE | P1 | 3 | Intermédiaire | 0.6j | 0.9j | 1.5j | Gemini | Lead Developer | SPRINT-0007 | 0.9j | Aucun (table SQL, service et API terminés) | Faible | 2026-07-04 |
| STORY-1502 | NOTIF | User Story | Centre de notifications sur le portail patient (IHM) | Frontend | DONE | P2 | 3 | Junior | 0.4j | 0.6j | 1.0j | Lead Developer | Gemini | SPRINT-0007 | 0.6j | Aucun (IHM Angular, service de liaison et tests terminés) | Faible | 2026-07-04 |
| TICKET-0015 | NOTIF | Task | Complétion du module Notifications (badge, déclencheurs, tests) | Full-stack | DONE | P1 | 4 | Intermédiaire | 0.3j | 0.5j | 0.8j | Antigravity | Lead Developer | SPRINT-0010 | 0.3j | Aucun (NotificationController, badge Angular, déclencheurs, tests unitaires) | Faible | 2026-07-05 |
| TICKET-0016 | API | Task | Module API Joprelys Connect — erreurs normalisées, trace_id, Swagger, rate limiting, webhooks (Sous-tâches 1–5) | Backend | DONE | P0 | 5 | Senior | 0.5j | 0.8j | 1.2j | Antigravity | Lead Developer | SPRINT-0010 | 0.5j | Aucun (GlobalExceptionHandler, TraceIdFilter, RateLimitingFilter, Webhook CRUD, OpenAPI annotations, 21 tests) | Faible | 2026-07-05 |
| STORY-1601 | API_INTEG | User Story | Télétransmission d'ordonnances à AllôPharma (backend) | Backend | DONE | P1 | 5 | Senior | 1.0j | 1.3j | 2.0j | Lead Developer | Gemini | SPRINT-0008 | 1.2j | Aucun (tests unitaires et intégration MockMvc validés) | Faible | 2026-07-04 |
| STORY-1602 | API_INTEG | User Story | Interface de télétransmission (IHM) | Frontend | DONE | P1 | 3 | Intermédiaire | 0.5j | 0.7j | 1.1j | Lead Developer | Gemini | SPRINT-0008 | 0.6j | Aucun (bouton, badge et tests unitaires Angular validés) | Faible | 2026-07-04 |
| TICKET-1603 | GOV | DevOps | Ajout du dépôt remote git | DevOps | DONE | P2 | 1 | Junior | 0.01j | 0.02j | 0.05j | Antigravity | Lead Developer | SPRINT-0008 | 0.02j | Aucun (remote configuré) | Faible | 2026-07-04 |
| EPIC-0011 | FHIR | Epic | Interopérabilité HL7 FHIR | Full-stack | DONE | P1 | 13 | Senior | 4.0j | 5.4j | 8.8j | Antigravity | Lead Developer | SPRINT-0009 | 0.9j | Aucun (Epic entièrement terminée et validée par les tests) | Moyen | 2026-07-05 |
| STORY-1701 | FHIR | User Story | Mapping des entités DPU vers les ressources FHIR (Patient, Encounter, Observation) | Backend | DONE | P1 | 5 | Senior | 1.0j | 1.3j | 2.0j | Antigravity | Lead Developer | SPRINT-0009 | 0.2j | Aucun (DTOs, Mappers et tests unitaires validés) | Moyen | 2026-07-04 |
| STORY-1702 | FHIR | User Story | Endpoints REST FHIR pour les patients et consultations | Backend | DONE | P1 | 5 | Senior | 1.0j | 1.3j | 2.0j | Antigravity | Lead Developer | SPRINT-0009 | 0.4j | Aucun (endpoints GET /fhir/Patient, Encounter et Observation et tests validés) | Moyen | 2026-07-05 |
| STORY-1703 | FHIR | User Story | Portail Développeur & Documentation OpenAPI/Swagger | Documentation | DONE | P2 | 3 | Intermédiaire | 0.7j | 1.0j | 1.7j | Antigravity | Lead Developer | SPRINT-0009 | 0.3j | Aucun (API OpenAPI intégrée et guide utilisateur rédigé) | Faible | 2026-07-05 |
| EPIC-0012 | UI_UX | Epic | Refonte UI/UX Premium back-office | Full-stack | DONE | P1 | 22 | Senior | 2.25j | 3.1j | 5.3j | Antigravity | Lead Developer | SPRINT-0009 | 1.15j | Aucun (Refonte UI/UX premium complète avec navigation contextuelle) | Faible | 2026-07-05 |
| STORY-1801 | UI_UX | User Story | Intégration du sélecteur de Thème (Clair / Sombre) dans l'AppShell | Frontend | DONE | P1 | 2 | Intermédiaire | 0.15j | 0.2j | 0.35j | Antigravity | Lead Developer | SPRINT-0009 | 0.15j | Aucun (Bouton switch et ThemeService intégrés) | Faible | 2026-07-05 |
| STORY-1802 | UI_UX | User Story | Menu Latéral (Sidebar) Rétractable pour le Back-office | Frontend | DONE | P1 | 5 | Senior | 0.5j | 0.8j | 1.3j | Antigravity | Lead Developer | SPRINT-0009 | 0.4j | Aucun (Sidebar collapsible avec rôles en place) | Faible | 2026-07-05 |
| STORY-1803 | UI_UX | User Story | Fil d'Ariane (Breadcrumbs) et Titrage Dynamique | Frontend | DONE | P2 | 2 | Intermédiaire | 0.2j | 0.3j | 0.5j | Antigravity | Lead Developer | SPRINT-0009 | 0.15j | Aucun (Composant breadcrumbs fonctionnel) | Faible | 2026-07-05 |
| STORY-1804 | UI_UX | User Story | Découpage du Dossier Patient Unique (DPU) en Vues Dédiées | Frontend | DONE | P1 | 5 | Senior | 0.6j | 1.0j | 1.7j | Antigravity | Lead Developer | SPRINT-0009 | 0.25j | Aucun (Routage enfant et extraction composants OK) | Faible | 2026-07-05 |
| TICKET-UX-GOOGLE-DESIGN | UI_UX | Refactoring | Refonte UI/UX : Navigation Contextuelle & Pages Dédiées | Frontend | DONE | P0 | 8 | Senior | 0.8j | 1.0j | 1.7j | Antigravity | Lead Developer | SPRINT-0009 | 0.2j | Aucun (Sidebar contextuelle et 5 pages portail patient créées) | Faible | 2026-07-05 |
| EPIC-0013 | PATIENT_COMPLIANCE | Epic | Conformité Module Patient (Cahier des Charges) | Full-stack | DONE | P1 | 22 | Senior | 2.2j | 3.0j | 5.0j | Antigravity | Lead Developer | SPRINT-0009 | 2.2j | Aucun (doublons, fusion, PDF synthèse, scopes granulaires livrés) | Moyen | 2026-07-05 |
| TICKET-1301 | PATIENT_COMPLIANCE | Task | Rendre le téléphone optionnel à la création | Full-stack | DONE | P1 | 1 | Senior | 0.1j | 0.15j | 0.25j | Antigravity | Lead Developer | SPRINT-0009 | 0.1j | Aucun (phone nullable, migration V24) | Faible | 2026-07-05 |
| TICKET-1302 | PATIENT_COMPLIANCE | Task | Service de détection de doublons (Levenshtein) | Backend | DONE | P1 | 5 | Senior | 0.5j | 0.7j | 1.1j | Antigravity | Lead Developer | SPRINT-0009 | 0.5j | Aucun (PatientSimilarityService + PatientDuplicateCandidateEntity + migration V24) | Moyen | 2026-07-05 |
| TICKET-1303 | PATIENT_COMPLIANCE | Task | Logique transactionnelle de fusion de dossiers | Backend | DONE | P1 | 5 | Senior | 0.5j | 0.7j | 1.1j | Antigravity | Lead Developer | SPRINT-0009 | 0.5j | Aucun (mergePatients() avec reassignation des 11 entités, audit, historique) | Moyen | 2026-07-05 |
| TICKET-1304 | PATIENT_COMPLIANCE | Task | IHM d'administration des doublons et assistant de fusion | Frontend | DONE | P1 | 3 | Senior | 0.3j | 0.4j | 0.7j | Antigravity | Lead Developer | SPRINT-0009 | 0.3j | Aucun (DuplicatesPageComponent + modal assistant de fusion côte à côte) | Faible | 2026-07-05 |
| TICKET-1305 | PATIENT_COMPLIANCE | Task | Fiche de synthèse médicale en PDF | Full-stack | DONE | P1 | 3 | Senior | 0.3j | 0.4j | 0.7j | Antigravity | Lead Developer | SPRINT-0009 | 0.3j | Aucun (endpoint GET /summary-pdf, PdfGeneratorService, downloadSummaryPdf Angular) | Faible | 2026-07-05 |
| TICKET-1306 | PATIENT_COMPLIANCE | Task | Scopes granulaires de consentements & canal | Full-stack | DONE | P1 | 3 | Senior | 0.3j | 0.4j | 0.7j | Antigravity | Lead Developer | SPRINT-0009 | 0.3j | Aucun (champs scopes+validation_channel, migration V23, validateAccess()) | Moyen | 2026-07-05 |
| TICKET-1307 | PATIENT_COMPLIANCE | Task | Scopes granulaires pour les demandes d'accès externes | Full-stack | DONE | P1 | 2 | Senior | 0.2j | 0.3j | 0.5j | Antigravity | Lead Developer | SPRINT-0009 | 0.2j | Aucun (champ scopes sur ExternalAccessRequestEntity, migration V23) | Moyen | 2026-07-05 |
| TICKET-CLINIC-EXTERNAL-ACCESS-REQUEST | DPU_ACCESS | Task | Page clinique de demande d'accès externe au DPU | Frontend | DONE | P1 | 2 | Intermédiaire | 0.15j | 0.25j | 0.4j | Antigravity | Lead Developer | SPRINT-0009 | 0.15j | Aucun (route /clinic/access-request, formulaire avec scopes, i18n FR/EN, build OK) | Faible | 2026-07-05 |


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
