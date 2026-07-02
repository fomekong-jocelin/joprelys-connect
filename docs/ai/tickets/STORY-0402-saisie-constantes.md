# STORY-0402 — Saisie des Constantes Vitales & Calcul IMC

## 1. Objectif

Cette user story consiste à permettre à l'infirmier (ou au médecin) d'enregistrer les constantes vitales d'un patient lors d'une visite active (`EN_COURS`), de calculer automatiquement son IMC en temps réel et d'afficher l'historique de ces constantes.
1. **Modèle de données** : Création de la table `vitals` liée à une visite.
2. **Calcul IMC** : Calcul en temps réel côté client et persistance en base de données.
3. **API REST** : Endpoint pour enregistrer les constantes d'une visite, et pour les récupérer.
4. **Interface Angular** : Formulaire de saisie des constantes intégré dans le tableau de bord de la file d'attente (ou vue détaillée de la visite) et affichage des constantes saisies.

## 2. Critères d'acceptation

- [x] L'utilisateur (infirmier/médecin) peut ouvrir le formulaire de constantes pour un patient ayant une visite `EN_COURS`.
- [x] Les constantes suivantes peuvent être saisies avec validations de limites :
  * **Température** (°C) : entre 30.0 et 45.0
  * **Poids** (kg) : entre 1.0 et 500.0
  * **Taille** (cm) : entre 30 et 250
  * **Pouls** (bpm) : entre 20 et 250
  * **Tension artérielle** (Systolique/Diastolique mmHg) : Systolique (40 à 250), Diastolique (30 à 150)
  * **SpO2** (%) : entre 50 et 100
  * **Glycémie** (g/L) : entre 0.1 et 10.0
  * **Fréquence respiratoire** (cycles/min) : entre 5 et 100
- [x] **Calcul de l'IMC** : Si le poids (kg) et la taille (cm) sont valides, l'IMC (Indice de Masse Corporelle) est calculé automatiquement à la volée sur l'écran : `IMC = Poids / (Taille_en_mètres ^ 2)`. Il est affiché avec 2 décimales.
- [x] Les constantes sont sauvegardées et rattachées à la visite courante.
- [x] Une fois saisies, les constantes s'affichent instantanément dans le profil de la visite et sur le tableau de bord.
- [x] L'isolation multi-tenant est strictement respectée (les constantes d'une clinique ne sont pas lisibles par une autre clinique).

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0004 (Gestion des Visites & Constantes Vitales) |
| User story parent | STORY-0402 |
| Sprint cible | SPRINT-0002 |
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

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu si nécessaire
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant et entités de visite analysés
- [x] Backend Maven uniquement vérifié
- [x] Frontend Tailwind CSS vérifié
- [x] Absence d'Angular Material vérifiée

## 5. Hypothèses

- Les constantes vitales sont facultatives au niveau de la base de données, mais le formulaire effectue des validations de fourchettes réalistes si les champs sont saisis.
- Une visite ne peut avoir qu'un seul enregistrement de constantes vitales de tri initial.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Saisie de valeurs aberrantes (Ex: Taille en mètres au lieu de cm) | Moyen | Contrôle strict des bornes de saisie côté frontend et backend avec messages d'erreurs clairs. |
| Incohérence des types de colonnes H2 vs PostgreSQL (Double vs DECIMAL) | Élevé | Conversion de Double en BigDecimal pour garantir l'indépendance de validation de Hibernate sur tous les SGBD. |

## 7. Action plan

- [x] **Documentation** : Mettre à jour les spécifications fonctionnelles et techniques dans `docs/features/visite/`.
- [x] **DB/Migration** (TASK-0402-01) : Créer la table `vitals` via Flyway.
- [x] **Backend/API** (TASK-0402-02) : Créer l'entité, le repository, le service et les controllers de constantes.
- [x] **Frontend/API** : Mettre à jour `VisitApiService.ts` pour gérer les constantes.
- [x] **Frontend/UI** (TASK-0402-03) : Créer le formulaire de constantes et l'intégrer sur le dashboard de file d'attente.
- [x] **Tests/QA** : Écrire les tests unitaires et d'intégration backend et frontend.
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## 8. Implémentation réalisée

- **Migration Flyway** : Ajout de `V5__create_vitals_table.sql` pour modéliser la table `vitals` avec intégrité référentielle en cascade.
- **Entité JPA & BigDecimal** : Implémentation de `VitalsEntity` et utilisation de `BigDecimal` pour les champs physiques afin de concilier la validation de schéma Hibernate sous PostgreSQL et H2.
- **REST Endpoints** : Ajout de la gestion des constantes de visite via les routes `POST /api/visits/{id}/vitals` (avec validations de contraintes jakarta) et `GET /api/visits/{id}/vitals`.
- **Automatisation IMC** : Calcul automatique d'IMC à la volée côté serveur (via `VisitService`) et côté client Angular.
- **Interface Utilisateur** : Boîte de dialogue modale de saisie réactive et affichage de la file d'attente sous forme de badges horizontaux compacts (avec icônes 🌡️, 🩺, ❤️, 🫁) pour éviter l'empilement vertical excessif.
- **Correction File d'attente** : Conversion de l'état de file d'attente Angular en `signal()` pour éviter le blocage durable sur "Chargement de la file d'attente...".
- **Correction API active** : Chargement explicite des constantes via `LEFT JOIN FETCH v.vitals` dans `VisitRepository.findActiveVisits()` afin que `/api/visits/active` retourne les visites avec constantes sans accès lazy hors transaction.
- **Clarification parcours** : Ajout d'un message d'aide dans le dashboard indiquant que la file d'attente se remplit après ouverture d'une visite depuis le dossier patient, puis que les constantes se saisissent dans cette file avant consultation.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-02 | Antigravity | 0.02j | 10% | Spécifications et implémentation | Aucun | Initialisation du ticket STORY-0402 |
| 2026-07-02 | Antigravity | 0.85j | 100% | Aucun | Aucun | Finalisation de l'implémentation, résolution des lancements Hibernate et tests OK |
| 2026-07-02 | Codex | 0.05j | 100% | Aucun | Aucun | Correction du blocage de chargement de la file d'attente, fetch des constantes avec les visites actives et clarification du moment de saisie des constantes. |
| 2026-07-02 | Antigravity | 0.10j | 100% | Aucun | Aucun | Conception et implémentation d'un volet latéral responsive (Drawer/Bottom Sheet) pour afficher le détail de l'admission et les constantes, simplifiant la file d'attente. |

## 10. Tests et vérifications

- **Tests unitaires & d'intégration backend ciblés** : `./mvnw.cmd -Dtest=VisitControllerTest test` -> BUILD SUCCESS, 9/9 tests OK.
- **Tests backend complets** : `./mvnw.cmd test` -> BUILD SUCCESS, 32/32 tests OK.
- **Tests unitaires frontend** : `npm run test -- --watch=false` -> 17/17 tests OK.
- **Build frontend production** : `npm run build -- --progress=false` -> Application bundle generation complete.
- **Packaging Maven initial** : `clean package` réussi sous JDK 21.

## 11. Documentation

- [x] Spécification fonctionnelle mise à jour : `docs/features/visite/FUNCTIONAL-SPEC.md`
- [x] Spécification technique mise à jour : `docs/features/visite/TECHNICAL-DESIGN.md`

## 12. Reste à faire

Aucun.

## 13. Statut final

Statut : DONE
