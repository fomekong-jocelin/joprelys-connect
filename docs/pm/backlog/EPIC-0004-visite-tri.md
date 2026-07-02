# EPIC-0004 — Gestion des Visites & Constantes Vitales

## 1. Objectif métier

Permettre le suivi des visites des patients au sein de la clinique, de l'accueil à la fin de la prise en charge, et permettre à l'infirmier (agent de tri) d'enregistrer les constantes vitales nécessaires au diagnostic médical.

## 2. Périmètre

### Inclus

- Ouverture d'une visite pour un patient (choix du motif, du service d'orientation et du médecin traitant).
- Cycle de vie de la visite : `EN_COURS`, `TERMINEE`, `ANNULEE`.
- Formulaire de saisie des constantes vitales (température, poids, taille, pouls, tension artérielle, SpO2, glycémie, fréquence respiratoire).
- Calcul automatique de l'IMC en temps réel (si taille et poids sont renseignés).
- Affichage des constantes vitales dans le profil de la visite active.

### Exclus

- Algorithme de tri d'urgence complexe (ex. Tri de Manchester).
- Alertes de constantes critiques par SMS/E-mail.

## 3. Utilisateurs concernés

- Agent d'accueil
- Infirmier
- Médecin

## 4. User stories

| ID | Titre | Priorité | SP | Statut | Sprint cible |
|---|---|---|---:|---|---|
| [STORY-0401](STORY-0401-ouverture-visite.md) | Ouverture et clôture de visite patient | P0 | 2 | BACKLOG | SPRINT-0002 |
| STORY-0402 | Saisie des constantes vitales et calcul IMC | P0 | 3 | BACKLOG | SPRINT-0003 |

## 5. Dépendances

- EPIC-0001 (Authentification) pour l'accès aux interfaces.
- EPIC-0003 (Patient DPU) pour associer la visite à un patient existant.

## 6. Risques

| Risque | Impact | Mitigation |
|---|---|---|
| Erreur de saisie des unités (ex. taille en cm au lieu de mètres) | Moyen | Validation stricte des données côté client et serveur (ex. taille entre 0.3m et 2.5m). |

## 7. Définition de succès

- [ ] L'infirmier peut enregistrer les constantes vitales en moins de 30 secondes.
- [ ] L'IMC se calcule et s'enregistre automatiquement dès que le poids et la taille sont valides.
- [ ] Une visite ne peut être clôturée que si les étapes médicales obligatoires sont accomplies.

## 8. Estimation globale

| Élément | Valeur |
|---|---|
| Total story points | 5 |
| Effort senior | 1.5j |
| Effort intermédiaire | 2j |
| Effort junior | 3j |
| Nombre de sprints estimé | 1 |

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout de la gestion des visites et de la saisie des constantes vitales de tri |
| Breaking change | Non |
| Release cible | v0.4.0 |
