# EPIC-0003 — Dossier Patient Unique (DPU) & Recherche

## 1. Objectif métier

Assurer l'identification unique d'un patient à l'aide d'un numéro de Dossier Patient Unique (DPU) standardisé, et permettre aux professionnels autorisés de rechercher et consulter la fiche d'identité d'un patient.

## 2. Périmètre

### Inclus

- Formulaire d'enregistrement patient (nom complet, date de naissance, sexe, ville/quartier, téléphone, contact d'urgence).
- Génération automatique du DPU au format `DPU-JOP-YYYYMMDD-XXXXXX`.
- Génération automatique du numéro patient local `PAT-YYYYMMDD-XXXXXX`.
- Moteur de recherche multicritère (recherche par nom, DPU ou numéro de téléphone).
- Fiche profil du patient centralisant ses données d'identité, allergies et antécédents médicaux.

### Exclus

- Fusion automatique ou manuelle de doublons patients.
- Capture de photo patient directe via webcam.

## 3. Utilisateurs concernés

- Agent d'accueil
- Infirmier
- Médecin
- Administrateur Clinique

## 4. User stories

| ID | Titre | Priorité | SP | Statut | Sprint cible |
|---|---|---|---:|---|---|
| [STORY-0301](STORY-0301-enregistrement-patient.md) | Enregistrement patient et génération du DPU | P0 | 3 | BACKLOG | SPRINT-0002 |
| [STORY-0302](STORY-0302-recherche-patient.md) | Recherche de patients multicritères | P0 | 3 | BACKLOG | SPRINT-0002 |
| STORY-0303 | Saisie des antécédents et allergies | P1 | 2 | BACKLOG | SPRINT-0003 |

## 5. Dépendances

- EPIC-0001 (Authentification) pour restreindre l'accès à la création et à la recherche patient.
- EPIC-0002 (Clinique Pilote) pour lier le patient à la clinique d'origine.

## 6. Risques

| Risque | Impact | Mitigation |
|---|---|---|
| Doublons de patients saisis par inattention | Moyen | Faire une pré-recherche obligatoire par numéro de téléphone avant d'autoriser la création d'un nouveau patient. |

## 7. Définition de succès

- [ ] Tout patient enregistré possède un DPU unique et non modifiable.
- [ ] La recherche par numéro de téléphone ou DPU affiche instantanément le patient concerné.
- [ ] Un agent non connecté ne peut effectuer de recherche.

## 8. Estimation globale

| Élément | Valeur |
|---|---|
| Total story points | 8 |
| Effort senior | 2j |
| Effort intermédiaire | 2.6j |
| Effort junior | 4j |
| Nombre de sprints estimé | 1 |

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout de la gestion d'identité patient et de la génération des numéros DPU |
| Breaking change | Non |
| Release cible | v0.4.0 |
