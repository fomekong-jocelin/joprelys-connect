# EPIC-0005 — Consultation Médicale & Prescription

## 1. Objectif métier

Fournir au médecin une interface fluide pour saisir le compte-rendu clinique de la consultation (symptômes, diagnostic, conclusion) et rédiger des prescriptions médicamenteuses (ordonnances) de manière structurée.

## 2. Périmètre

### Inclus

- Formulaire de saisie de consultation (symptômes, examen clinique, diagnostic codifié/texte, conclusion et conseils).
- Rapprochement avec les constantes vitales saisies lors du tri.
- Module de prescription d'ordonnance (médicament, forme/dosage, posologie, durée de traitement, quantité et instructions spéciales).
- Liaison stricte : une consultation appartient à une visite, une prescription appartient à une consultation.

### Exclus

- Intégration d'une base de données médicamenteuse commerciale externe (ex. Vidal). Les médicaments sont saisis en texte libre.
- Module de détection automatique des interactions médicamenteuses et contre-indications.

## 3. Utilisateurs concernés

- Médecin

## 4. User stories

| ID | Titre | Priorité | SP | Statut | Sprint cible |
|---|---|---|---:|---|---|
| STORY-0501 | Saisie du compte-rendu de consultation | P0 | 3 | BACKLOG | SPRINT-0003 |
| STORY-0502 | Rédaction d'une prescription (ordonnance) | P0 | 3 | BACKLOG | SPRINT-0003 |
| STORY-0503 | Consultation de l'historique médical lors du diagnostic | P1 | 2 | BACKLOG | SPRINT-0004 |

## 5. Dépendances

- EPIC-0001 (Authentification) pour restreindre l'accès de saisie aux seuls médecins.
- EPIC-0004 (Visite & Constantes) car une consultation doit s'appuyer sur une visite active et ses constantes vitales.

## 6. Risques

| Risque | Impact | Mitigation |
|---|---|---|
| Perte de données en cours de saisie de consultation | Moyen | Implémenter une sauvegarde automatique locale (autosave) temporaire. |

## 7. Définition de succès

- [ ] Le médecin peut consulter les constantes vitales du tri sur le même écran de saisie de la consultation.
- [ ] L'ordonnance ne contient pas de répétitions de termes (ex. "10 jours jours").
- [ ] Une consultation enregistrée ne peut plus être modifiée une fois la visite clôturée.

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
| Justification | Ajout du module clinique médecin de consultation et d'ordonnance |
| Breaking change | Non |
| Release cible | v0.5.0 |
