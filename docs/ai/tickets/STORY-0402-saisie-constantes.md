# STORY-0402 — Saisie des Constantes Vitales & Calcul IMC

## 1. Objectif

Cette user story consiste à permettre à l'infirmier (ou au médecin) d'enregistrer les constantes vitales d'un patient lors d'une visite active (`EN_COURS`), de calculer automatiquement son IMC en temps réel et d'afficher l'historique de ces constantes.
1. **Modèle de données** : Création de la table `vitals` liée à une visite.
2. **Calcul IMC** : Calcul en temps réel côté client et persistance en base de données.
3. **API REST** : Endpoint pour enregistrer les constantes d'une visite, et pour les récupérer.
4. **Interface Angular** : Formulaire de saisie des constantes intégré dans le tableau de bord de la file d'attente (ou vue détaillée de la visite) et affichage des constantes saisies.

## 2. Critères d'acceptation

- [ ] L'utilisateur (infirmier/médecin) peut ouvrir le formulaire de constantes pour un patient ayant une visite `EN_COURS`.
- [ ] Les constantes suivantes peuvent être saisies avec validations de limites :
  * **Température** (°C) : entre 30.0 et 45.0
  * **Poids** (kg) : entre 1.0 et 500.0
  * **Taille** (cm) : entre 30 et 250
  * **Pouls** (bpm) : entre 20 et 250
  * **Tension artérielle** (Systolique/Diastolique mmHg) : Systolique (40 à 250), Diastolique (30 à 150)
  * **SpO2** (%) : entre 50 et 100
  * **Glycémie** (g/L) : entre 0.1 et 10.0
  * **Fréquence respiratoire** (cycles/min) : entre 5 et 100
- [ ] **Calcul de l'IMC** : Si le poids (kg) et la taille (cm) sont valides, l'IMC (Indice de Masse Corporelle) est calculé automatiquement à la volée sur l'écran : `IMC = Poids / (Taille_en_mètres ^ 2)`. Il est affiché avec 2 décimales.
- [ ] Les constantes sont sauvegardées et rattachées à la visite courante.
- [ ] Une fois saisies, les constantes s'affichent instantanément dans le profil de la visite et sur le tableau de bord.
- [ ] L'isolation multi-tenant est strictement respectée (les constantes d'une clinique ne sont pas lisibles par une autre clinique).

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0004 (Gestion des Visites & Constantes Vitales) |
| User story parent | STORY-0402 |
| Sprint cible | SPRINT-0003 |
| Priorité business | P0 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Intermédiaire |
| Effort senior | 0.8j |
| Effort intermédiaire | 1.1j |
| Effort junior | 1.8j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | STORY-0401 |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [ ] `AGENTS.md` lu
- [ ] `SKILL.md` lu
- [ ] `PROJECT-MANAGER-SKILL.md` lu si nécessaire
- [ ] `README-IA.md` lu
- [ ] `WORKFLOW-IA.md` lu
- [ ] `PROJECT-TRACKING.md` lu
- [ ] `CHANGELOG.md` lu
- [ ] `review-checklist.md` lu
- [ ] Code existant et entités de visite analysés
- [ ] Backend Maven uniquement vérifié
- [ ] Frontend Tailwind CSS vérifié
- [ ] Absence d'Angular Material vérifiée

## 5. Hypothèses

- Les constantes vitales sont facultatives au niveau de la base de données, mais le formulaire effectue des validations de fourchettes réalistes si les champs sont saisis.
- Une visite ne peut avoir qu'un seul enregistrement de constantes vitales de tri initial.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Saisie de valeurs aberrantes (Ex: Taille en mètres au lieu de cm) | Moyen | Contrôle strict des bornes de saisie côté frontend et backend avec messages d'erreurs clairs. |

## 7. Action plan

- [ ] **Documentation** : Mettre à jour les spécifications fonctionnelles et techniques dans `docs/features/visite/`.
- [ ] **DB/Migration** (TASK-0402-01) : Créer la table `vitals` via Flyway.
- [ ] **Backend/API** (TASK-0402-02) : Créer l'entité, le repository, le service et les controllers de constantes.
- [ ] **Frontend/API** : Mettre à jour `VisitApiService.ts` pour gérer les constantes.
- [ ] **Frontend/UI** (TASK-0402-03) : Créer le formulaire de constantes et l'intégrer sur le dashboard de file d'attente.
- [ ] **Tests/QA** : Écrire les tests unitaires et d'intégration backend et frontend.
- [ ] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## 8. Implémentation réalisée

*(Sera complété pendant le développement)*

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-02 | Antigravity | 0.02j | 10% | Spécifications et implémentation | Aucun | Initialisation du ticket STORY-0402 |

## 10. Tests et vérifications

*(Sera complété à la fin)*

## 11. Documentation

- [ ] Spécification fonctionnelle mise à jour : `docs/features/visite/FUNCTIONAL-SPEC.md`
- [ ] Spécification technique mise à jour : `docs/features/visite/TECHNICAL-DESIGN.md`

## 12. Reste à faire

- [ ] Rédaction des spécifications fonctionnelles et techniques
- [ ] Implémentation du code et des tests

## 13. Statut final

Statut : IN_PROGRESS
