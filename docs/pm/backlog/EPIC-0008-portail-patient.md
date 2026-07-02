# EPIC-0008 — Portail Patient & Consentement

## 1. Objectif métier

Permettre aux patients d'accéder de manière sécurisée à leur historique médical (DPU), de télécharger leurs ordonnances au format PDF, et de gérer explicitement le consentement d'accès à leurs données de santé par les différents praticiens ou cliniques tierces du réseau Joprelys Connect.

## 2. Périmètre

### Inclus

- Espace patient sécurisé et accessible via une authentification simplifiée (ex: email + code OTP ou lien magique de connexion).
- Consultation de l'historique personnel des consultations et prescriptions rattachées au DPU du patient.
- Téléchargement sécurisé des ordonnances et documents médicaux au format PDF.
- Gestion du consentement d'accès : le patient peut accorder, révoquer ou limiter temporairement le droit de consultation de son DPU à un praticien ou à un établissement de santé du réseau.
- Journal d'audit personnel : affichage au patient de qui a consulté son DPU et à quel moment.

### Exclus

- Prise de rendez-vous en ligne (out of scope pour le MVP).
- Téléconsultation vidéo ou messagerie instantanée en temps réel avec le médecin.
- Saisie ou modification directe de ses propres données médicales par le patient.

## 3. Utilisateurs concernés

- Patient
- Praticien (Médecin, Infirmier) - impacté par le statut du consentement pour pouvoir consulter

## 4. User stories

| ID | Titre | Priorité | SP | Statut | Sprint cible |
|---|---|---|---:|---|---|
| STORY-0801 | Espace patient sécurisé et historique personnel | P1 | 5 | BACKLOG | SPRINT-0005 |
| STORY-0802 | Téléchargement sécurisé de ses propres ordonnances | P1 | 2 | BACKLOG | SPRINT-0005 |
| STORY-0803 | Gestion des consentements d'accès du DPU | P0 | 8 | BACKLOG | SPRINT-0005 |
| STORY-0804 | Journal de traçabilité des consultations du DPU pour le patient | P2 | 3 | BACKLOG | SPRINT-0006 |

## 5. Dépendances

- EPIC-0003 (Dossier Patient Unique) pour rattacher le compte utilisateur du patient au DPU physique.
- EPIC-0001 (Authentification) pour sécuriser l'accès à l'espace patient.
- EPIC-0006 (Génération PDF) pour réutiliser les documents stockés.

## 6. Risques

| Risque | Impact | Mitigation |
|---|---|---|
| Usurpation d'identité d'un patient accédant à ses données de santé | Critique | Authentification forte (OTP par email ou SMS), session courte, et validation d'identité initiale en clinique lors de l'enregistrement. |
| Blocage d'un médecin en situation d'urgence par manque de consentement | Élevé | Prévoir un mode d'accès "Brise-Glace" (Break the Glass) pour les situations d'urgence avec traçabilité renforcée et alerte immédiate au patient. |

## 7. Définition de succès

- [ ] Un patient peut se connecter à son espace personnel et télécharger son ordonnance sans avoir besoin de contacter la clinique.
- [ ] Si le patient révoque le consentement d'une clinique tierce, aucun médecin de cette clinique ne peut ouvrir son DPU (sauf procédure d'urgence auditée).

## 8. Estimation globale

| Élément | Valeur |
|---|---|
| Total story points | 18 |
| Effort senior | 5j |
| Effort intermédiaire | 8j |
| Effort junior | 12j |
| Nombre de sprints estimé | 1 à 2 |

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout de l'espace patient et du module de gestion des consentements |
| Breaking change | Non |
| Release cible | v0.6.0 |
