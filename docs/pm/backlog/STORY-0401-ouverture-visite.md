# STORY-0401 — Ouverture & Clôture de Visite Patient

## 1. User story

En tant qu'**agent d'accueil**, je veux **ouvrir une visite clinique pour un patient et l'orienter**, afin d'**enregistrer sa présence dans la file d'attente active et de tracer son parcours de soins**.

## 2. Critères d'acceptation

- [ ] L'agent d'accueil peut ouvrir une visite à partir de la fiche profil d'un patient.
- [ ] Il doit renseigner le motif de visite (texte libre) et le médecin traitant ou le service de destination (ex. Médecine générale, Tri, Pédiatrie).
- [ ] Le système génère un numéro de visite au format `VIS-YYYYMMDD-XXXXXX` et enregistre la date et l'heure d'arrivée.
- [ ] Le statut initial est `EN_COURS`.
- [ ] L'interface du tableau de bord de la clinique liste toutes les visites `EN_COURS` (file d'attente).
- [ ] Le médecin peut clôturer la visite, ce qui passe le statut à `TERMINEE` et horodate la fin de prise en charge (`closed_at`).
- [ ] Une visite clôturée ne peut pas repasser à l'état `EN_COURS` sans laisser de trace d'audit.
- [ ] La relation JPA `Visit -> Patient` est configurée en `FetchType.LAZY` pour éviter la surcharge mémoire lors du listage de la file d'attente (voir [ADR-0002](docs/ai/adr/ADR-0002-pagination-lazy-loading.md)).

## 3. Périmètre

### Inclus

- Modèle de données `visits` en base de données.
- API REST de gestion des visites (ouverture, clôture, liste active).
- Formulaire d'ouverture de visite dans le Frontend.
- Tableau de bord simple de file d'attente active (dashboard).

### Exclus

- Routage automatique intelligent des patients en file d'attente (gestion de tickets d'appel).

## 4. Tâches

| ID | Titre | Stack | Profil recommandé | SP | Estimation | Statut |
|---|---|---|---|---:|---:|---|
| TASK-0401-01 | Script de migration DB pour la table `visits` | DB / SQL | Junior | 1 | 0.25j | TODO |
| TASK-0401-02 | API REST backend pour les visites avec fetch LAZY (Spring Boot) | Backend | Intermédiaire | 1 | 0.5j | TODO |
| TASK-0401-03 | Écran d'ouverture de visite sur le profil patient | Frontend | Junior | 1 | 0.5j | TODO |
| TASK-0401-04 | Tableau de bord de file d'attente des visites actives | Frontend | Junior | 1 | 0.5j | TODO |
| TASK-0401-05 | Tests unitaires et d'intégration du cycle de vie de la visite | Test | Intermédiaire | 1 | 0.25j | TODO |

## 5. Estimation

| Champ | Valeur |
|---|---|
| Story points | 2 |
| Complexité | S |
| Profil recommandé | Junior autonome |
| Effort senior | 0.5j |
| Effort intermédiaire | 0.65j |
| Effort junior | 1.1j |
| Risque | Faible |

## 6. Definition of Ready

- [x] Critères d'acceptation clairs
- [x] Dépendances connues (Patient DPU)
- [x] Profil recommandé identifié
- [x] Estimation faite
- [x] Reviewer identifié (Lead Developer)

## 7. Definition of Done

- [ ] Table SQL initialisée
- [ ] API REST fonctionnelle (ouverture, clôture)
- [ ] Tableau de file d'attente opérationnel et synchronisé
- [ ] Review validée

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout de la gestion des visites patients et de la file d'attente active |
| Breaking change | Non |
| Release cible | v0.4.0 |
