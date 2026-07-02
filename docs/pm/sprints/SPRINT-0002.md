# SPRINT-0002 — Socle Technique, Authentification et Patient

## 1. Informations générales

| Champ | Valeur |
|---|---|
| Sprint | SPRINT-0002 |
| Période | du 2026-07-16 au 2026-07-30 |
| Objectif | Initialiser l'architecture technique, implémenter l'authentification sécurisée, la structure de clinique pilote multi-tenant et la gestion d'identité patient de base (DPU et ouverture de visite) |
| Responsable | Lead Developer / Scrum Master |

## 2. Capacité

| Développeur | Profil | Jours ouvrés | Absences | Réunions/support | Capacité planifiable |
|---|---|---:|---:|---:|---:|
| Lead Developer | Senior | 10 | 0 | 2.5 | 7.5j |
| Gemini (Antigravity) | Senior | 10 | 0 | 2.5 | 7.5j |

## 3. Charge sélectionnée

| Ticket | Titre | SP | Estimation | Assigné | Reviewer | Statut |
|---|---|---:|---:|---|---|---|
| [STORY-0101](../backlog/STORY-0101-connexion.md) | Connexion & Déconnexion Sécurisée | 3 | 0.5j | Codex | Lead Developer | REVIEW |
| [STORY-0102](../backlog/STORY-0102-roles.md) | Contrôle d'Accès Basé sur les Rôles (RBAC) | 3 | 0.4j | Gemini | Lead Developer | REVIEW |
| [STORY-0201](../backlog/STORY-0201-enregistrement-clinique.md) | Enregistrement de la Clinique Pilote & Multi-tenant | 3 | 1.0j | Gemini / Codex | Lead Developer | REVIEW |
| [STORY-0301](../backlog/STORY-0301-enregistrement-patient.md) | Enregistrement Patient & Génération du DPU | 3 | 1.0j | Gemini | Lead Developer | TODO |
| [STORY-0302](../backlog/STORY-0302-recherche-patient.md) | Recherche de Patients Multicritères | 3 | 0.5j | Gemini | Lead Developer | TODO |
| [STORY-0401](../backlog/STORY-0401-ouverture-visite.md) | Ouverture & Clôture de Visite Patient | 2 | 0.5j | Gemini | Lead Developer | TODO |

## 4. Synthèse capacité

| Élément | Valeur |
|---|---:|
| Capacité planifiable totale | 15.0j |
| Charge engagée | 5.70j |
| Marge restante | 9.30j |
| Taux de charge | 38.0 % |

> [!NOTE]
> La charge est calibrée à 38.0 % car ce sprint comprend l'effort technique initial d'échafaudage de l'architecture des dépôts (Spring Boot, Angular et/ou Flutter), la base de données, l'authentification, RBAC et la reprise UI mobile-first de STORY-0201.

## 5. Risques sprint

| Risque | Impact | Mitigation |
|---|---|---|
| Retards liés à l'initialisation de l'infrastructure/base de données | Moyen | Démarrer par les tâches SQL de structure (TASK-0201-01 et TASK-0301-01) dès le premier jour |
| Problème d'isolation tenant (fuite inter-clinique) | Fort | Code reviews strictes par le Lead Developer des aspects de filtrage Spring Boot |
| Vérification backend locale bloquée par téléchargement Gradle interdit | Moyen | Installer/cache Gradle 9.5.1 localement puis exécuter `cd backend && ./gradlew test` |
| Build Angular production muet sous Node.js 25.9.0 | Moyen | Relancer la validation `npm run build` sous Node pair/LTS (Node 24 ou 22) avant validation Lead |

## 6. Definition of Success

- [x] L'écran de connexion frontend est connecté aux API sécurisées par JWT.
- [x] Les pages s'affichent uniquement selon le rôle de l'utilisateur connecté.
- [ ] L'enregistrement patient génère des DPU uniques sans collision.
- [ ] L'agent d'accueil peut ouvrir une visite pour un patient et la voir apparaître dans la file d'attente.

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout des modules fonctionnels fondamentaux du MVP (authentification, clinique, patient, visites) |
| Breaking change | Non |
| Release cible | v0.4.0 |
