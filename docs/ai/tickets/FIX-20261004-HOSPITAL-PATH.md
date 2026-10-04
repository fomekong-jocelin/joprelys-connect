# FIX-20261004 — Corrections de la revue hospitalière
## Cadre
Engineering + QA, READY_FOR_REVIEW — implémentation et gates automatisées vertes ; review et recette ouvertes. Demande explicite « vas y corrige ». Parent EPIC-0027 / PATIENT_JOURNEY, constats QA-20261004 HOS-QA-01 à 07.
## EPIC → Stories → Tasks → Subtasks
| Tâche | Objectif / critères d'acceptation | Sous-tâches | Estimation senior | Profil / reviewer | Tests attendus |
|---|---|---|---|---|---|
| HFIX-01 | Admission/transfert accessibles sans droits de configuration, données limitées au tenant | API readonly, projections minimales, clients Angular | 1j / 3 SP | Senior Spring/Angular / Tech Lead sécurité | rôles métier, refus lecture hors tenant, refus écritures config |
| HFIX-02 | Une seule prise en charge ; sauvegardes concurrentes sérialisées | verrou visite partagé, test transactions concurrentes | 0.5–1j / 3 SP | Senior backend / référent clinique | take-charge/release/save, timestamp périmé |
| HFIX-03 | Référence prescription contrôlée côté serveur | validation patient/tenant/statut/médicament, intégration UI selon règle décidée | 0.5–1j / 3 SP | Senior backend/Angular / médecin | référence inconnue/autre patient/tenant/inactive |
| HFIX-04 | Aucun précalcul partiel rendu silencieusement | propagation des erreurs sources, parsing quantité ciblé | 0.5j / 2 SP | Senior backend / DAF | pannes soins/consommation/CRO/prescription, calcul nominal |
| HFIX-05 | Loading/empty/error distincts, une requête d'admission à la fois, FR/EN et composants ≤500 lignes | états et reprise, extraction consent/CRO, tokens, traductions | 1j / 3 SP | Senior Angular / QA UX | erreur chargement, double submit, tests composants, build/i18n |
Chaque tâche est READY après spécifications et tests définis ; DONE = code et documentation à jour, tests verts, reviewer et recette humaine encore explicitement distincts. Aucun engagement de date ni capacité nominative connu.
## Actions
- [x] Analyser le code, les contrats et les risques cross-stack.
- [x] Créer documentation initiale et découpage des corrections.
- [x] HFIX-01 API et Angular.
- [x] HFIX-02 verrouillage et tests.
- [x] HFIX-03 validation serveur et sélection Angular ; décision utilisateur : prescription validée obligatoire.
- [x] HFIX-04 calcul financier.
- [x] HFIX-05 composants, états, i18n, tokens.
- [x] Tests frontend/backend et documentation finale : 604 tests Angular, 86 tests Maven ciblés, build, i18n et diff verts.
- [ ] Recette navigateur multi-profils (permission enregistrée bloque localhost:4201).
## Impacts
Lectures additives : MINOR seules ; lot global MAJOR candidat du fait de prescriptionItemId obligatoire (BREAKING CHANGE). Aucun endpoint existant ni permission d'écriture élargi. Pas de migration DB prévue : verrou pessimiste existant réutilisé. Choix explicite accepté le 2026-10-04 ; ADR-20261004-hospital-medication-prescription-required et contrat API documentent la rupture. Aucun bump, tag ou release préparé. Flutter n'utilise pas le séjour hospitalier à ce jour : vérifier consommateurs avant clôture. Proxy/YAML/Maven/Tailwind v4 conservés. 12-Factor : aucun environnement/secret codé dans les nouveaux services ; état critique en base.
## Hors périmètre
Réservation, handoff, compatibilité clinique du lit et nouveau workflow EPIC-0027 nécessitent validation métier ; aucun statut historique de visite imposé sans décision.

## Résultat d'implémentation
HOS-QA-01 à 07 traités dans ce lot : lectures métier bornées, exclusion par visite, validation médicamenteuse transactionnelle, précalcul complet ou erreur, erreurs/reprise UI, double submit et extraction consent/CRO + FR/EN/tokens. Les tests d'intégration hospitaliers partagent une fixture extraite afin de garder les classes sous 500 lignes.
Les tests concurrents sont de vraies requêtes MockMvc parallèles sur H2. Cela ne constitue ni une recette browser ni une validation PostgreSQL de production.
Review restante : Tech Lead/sécurité, référent clinique, QA ; recette rôles FR/EN light/dark et déploiement coordonné API/clients.

Preuves détaillées : docs/features/hospital-path-fixes/TEST-PLAN.md ; 113 fichiers frontend et 11 classes backend validés. Aucun tag ou déploiement effectué.

## Versionnement des travaux
Demande utilisateur : « commit les travaux ». Le lot de code, tests, revue et documentation est inclus dans un commit local `fix(hospitalization)!: secure and stabilize hospital care flow`. Périmètre contrôlé, artefacts/logs/secrets exclus ; aucune modification applicative supplémentaire pour le commit. Tests existants conservés comme preuves, sans relance de suites inchangées. Review et recettes navigateur/PostgreSQL restent ouvertes ; aucune release ou publication distante incluse dans cette demande.
