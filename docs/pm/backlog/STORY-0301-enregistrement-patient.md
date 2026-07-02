# STORY-0301 — Enregistrement Patient & Génération du DPU

## 1. User story

En tant qu'**agent d'accueil**, je veux **pouvoir enregistrer un nouveau patient dans le système et lui générer son identifiant DPU**, afin d'**initialiser son dossier médical partagé**.

## 2. Critères d'acceptation

- [ ] L'agent d'accueil peut renseigner l'identité du patient : Nom complet, Sexe, Date de naissance, Numéro de téléphone, Ville/Quartier et Contact d'urgence (Nom + Téléphone).
- [ ] Le système génère automatiquement un Dossier Patient Unique au format `DPU-JOP-YYYYMMDD-XXXXXX` (où XXXXXX est un compteur séquentiel ou aléatoire unique sur la journée).
- [ ] Le système génère aussi un numéro patient interne local à la clinique au format `PAT-YYYYMMDD-XXXXXX`.
- [ ] Le système effectue un contrôle d'unicité sur le numéro de téléphone et le couple Nom + Date de naissance avant d'autoriser la création (pour prévenir les doublons).
- [ ] Une fois créé, le dossier patient affiche un statut actif et est prêt à recevoir des visites.

## 3. Périmètre

### Inclus

- Schéma de base de données pour `patients`.
- Logique backend de génération de numéros séquentiels uniques DPU / PAT.
- API REST POST de création de patient.
- Formulaire Web d'enregistrement responsive.

### Exclus

- Capture de photo d'identité du patient.
- Consentement électronique signé par biométrie ou signature tactile (reporté).

## 4. Tâches

| ID | Titre | Stack | Profil recommandé | SP | Estimation | Statut |
|---|---|---|---|---:|---:|---|
| TASK-0301-01 | Script de création de la table `patients` et contraintes d'unicité | DB / SQL | Junior | 1 | 0.25j | TODO |
| TASK-0301-02 | Service backend de génération de DPU / PAT unique sans collision | Backend | Senior | 2 | 0.5j | TODO |
| TASK-0301-03 | API REST de création de patient avec validations | Backend | Intermédiaire | 1 | 0.5j | TODO |
| TASK-0301-04 | Interface Frontend de saisie patient avec contrôles de saisie | Frontend | Junior | 1 | 0.75j | TODO |
| TASK-0301-05 | Tests unitaires de la logique de détection de doublons | Test | Intermédiaire | 1 | 0.25j | TODO |

## 5. Estimation

| Champ | Valeur |
|---|---|
| Story points | 3 |
| Complexité | M |
| Profil recommandé | Intermédiaire |
| Effort senior | 1j |
| Effort intermédiaire | 1.3j |
| Effort junior | 2.2j |
| Risque | Moyen |

## 6. Definition of Ready

- [x] Critères d'acceptation clairs
- [x] Dépendances connues (Multi-tenant)
- [x] Profil recommandé identifié
- [x] Estimation faite
- [x] Reviewer identifié (Lead Developer)

## 7. Definition of Done

- [ ] Table SQL initialisée
- [ ] Formulaire et API opérationnels avec génération sans collision
- [ ] Taux de couverture de tests unitaires > 80 %
- [ ] Review de code validée

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout du module d'enregistrement patient et génération de l'identifiant DPU |
| Breaking change | Non |
| Release cible | v0.4.0 |
