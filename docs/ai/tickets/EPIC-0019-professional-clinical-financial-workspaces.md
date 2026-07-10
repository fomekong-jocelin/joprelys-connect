# EPIC-0019 — Postes métier professionnels : hospitalisation et caisse

| Champ | Valeur |
|---|---|
| Type | Diagnostic + Architecture produit + Engineering + Project Manager |
| Statut | IN_PROGRESS |
| Priorité | P0 |
| Estimation | 10,5 j senior / 14,0 j intermédiaire / 23,0 j junior encadré |
| Profil recommandé | Senior full-stack, UX designer santé, Médecin Chef, Cadre infirmier et DAF |
| Reviewer | Lead Developer + Médecin Chef + DAF |
| Dépendances | EPIC-0017, EPIC-0018 et validation métier |
| Impact SemVer prévisionnel | MINOR |
| Dernière mise à jour | 2026-07-10 |

## Contexte et diagnostic

La demande porte sur une remise à niveau professionnelle des parcours d'hospitalisation et de caisse, conformément au CDC V2.1 / TC2CDK et, lorsque le CDC ne précise pas un comportement, selon un socle opérationnel hospitalier prudent à faire valider par la gouvernance clinique et financière.

Écarts constatés dans l'existant :

- `patient-hospitalization.component.ts` compte 640 lignes et son template 797 lignes : les limites de responsabilité et de taille sont dépassées ;
- le même écran mélange admission, transfert, sortie, notes, consentements, soins, administrations médicamenteuses, consommables et bloc opératoire ;
- plusieurs textes visibles restent codés en français, et les erreurs sont rendues par `alert()` ;
- la caisse a désormais un socle métier plus complet dans EPIC-0018, mais doit être contrôlée comme un poste de travail autonome : recherche, encaissement, reçu, session, exception et clôture, sans ambiguïté avec les créances ;
- le suivi documentaire n'est pas cohérent : EPIC-0018, son backlog et `PROJECT-TRACKING.md` divergent sur certains statuts. Cette divergence interdit de déclarer une livraison stable sans revue de consolidation.

## Objectif

Fournir des espaces de travail distincts, sûrs et rapides pour l'admission, les soins, le médecin, le bloc, le caissier, le recouvrement et la DAF. Le système doit guider l'utilisateur, afficher l'état clinique ou financier compréhensible, empêcher les actions incohérentes côté backend et conserver une trace complète des décisions.

## Règles produit de référence quand le CDC est silencieux

- Une action clinique ou financière porte son auteur, sa date, son statut et son historique ; aucune correction ne doit écraser silencieusement une donnée validée.
- Le contexte patient est visible avant une action sensible : identité, numéro de séjour, service/lit, alertes et statut du séjour. Les règles médicales exactes restent validées par le Médecin Chef.
- Les étapes cliniques sont séparées par rôle et par moment de travail : admission, transmissions/soins, prescription-administration, bloc, sortie.
- Le caissier ne voit que les montants réellement exigibles et les opérations de sa session ; la DAF supervise, traite les exceptions et exporte.
- Les statuts explicites remplacent les couleurs ou termes ambigus : par exemple « part patient réglée — assurance à recouvrer » plutôt que « payée ».
- Chaque écran reste utilisable en FR/EN, clair/sombre, clavier et à partir de 1280 px ; les actions destructives ou financières demandent confirmation et retour d'état accessible.

## Découpage EPIC → stories → tâches

| Story | Objectif | SP | Est. senior | Profil | Statut |
|---|---|---:|---:|---|---|
| STORY-2120 | Fondations UX et navigation par poste métier | 3 | 1,0 j | Senior Frontend + UX | READY après validation maquette |
| STORY-2121 | Poste hospitalisation : synthèse de séjour, admission et transfert | 8 | 2,5 j | Senior Full-stack | BACKLOG |
| STORY-2122 | Poste soignant : transmissions, soins et administration sécurisée | 8 | 2,5 j | Senior Full-stack + Cadre infirmier | BACKLOG |
| STORY-2123 | Poste médecin/bloc/sortie et documents cliniques | 5 | 1,5 j | Senior Full-stack + Médecin Chef | BACKLOG |
| STORY-2124 | Poste caissier et exceptions financières cohérentes | 5 | 1,5 j | Senior Full-stack + DAF | BACKLOG |
| STORY-2125 | Validation de rôle, accessibilité et non-régression de parcours | 5 | 1,5 j | Senior QA / Full-stack | BACKLOG |

### Tâches de la première story prête (STORY-2120)

| Tâche | Objectif | Critères d'acceptation | Est. senior | Reviewer | Tests attendus |
|---|---|---|---:|---|---|
| TASK-2120-01 | Cartographier les parcours par rôle | Sept postes métier et les droits/actions sont validés par le Médecin Chef, le Cadre infirmier et la DAF | 0,25 j | Médecin Chef + DAF | Revue de maquette/scénarios |
| TASK-2120-02 | Définir le shell hospitalier et financier | Bandeau patient, barre d'état, hiérarchie d'actions, états vide/chargement/erreur et navigation documentés | 0,25 j | Lead Frontend | Revue DESIGN.md / a11y |
| TASK-2120-03 | Définir les composants partagés | `patient-context`, `worklist`, `timeline`, `status`, `confirmation` et `feedback` réutilisables, i18n FR/EN et thèmes inclus | 0,25 j | Lead Frontend | Tests unitaires de composants |
| TASK-2120-04 | Valider les critères de prêt des stories suivantes | API, données, RBAC, scénarios de test et maquettes disponibles | 0,25 j | Lead Developer | Revue DoR |

## Critères d'acceptation globaux

- [ ] Aucun composant Angular métier ne dépasse 500 lignes ; alerte et extraction dès 300 lignes.
- [ ] Tous les libellés visibles hospitalisation/caisse sont servis par les ressources `fr` et `en`.
- [ ] Aucune règle clinique, autorisation ou règle de caisse critique n'est déplacée dans le frontend.
- [ ] Les actions de soin, médicament, sortie, encaissement, dépense, clôture et résolution d'écart possèdent une preuve d'autorisation, d'audit et de test.
- [ ] Les états cliniques et financiers sont explicites, accessibles et testés sur les rôles autorisés et non autorisés.
- [ ] Les parcours admission → séjour → sortie et facture → encaissement → reçu → clôture sont couverts par des tests E2E.

## Action plan

- [x] Lire les règles de gouvernance, design, documentation et PM.
- [x] Analyser le CDC synthétisé dans EPIC-0017 et les spécifications existantes.
- [x] Auditer les composants hospitalisation et caisse sans modifier le travail non versionné en cours.
- [x] Créer le cadrage fonctionnel, la conception technique et le plan de test initial.
- [x] Créer l'epic, le backlog et le suivi de risque.
- [x] Implémenter le shell initial du séjour : en-tête clinique, actions regroupées, navigation accessible et i18n FR/EN.
- [x] Ajouter les tests unitaires du nouvel en-tête de séjour et exécuter les tests Angular.
- [x] Extraire les transmissions, soins, administrations médicamenteuses et consommables dans des composants autonomes testés.
- [ ] Organiser la revue de validation Médecin Chef / Cadre infirmier / DAF.
- [ ] Produire les maquettes validées de STORY-2120.
- [ ] Démarrer uniquement les stories dont la Definition of Ready est complète.
- [ ] Consolider les statuts contradictoires d'EPIC-0018 avant release.

## Suivi d'exécution — STORY-2120

| Date | Réalisé | Preuve | Reste à faire |
|---|---|---|---|
| 2026-07-10 | Nouveau composant `HospitalizationStayHeaderComponent`, contexte clinique et actions de séjour séparés du contenu de dossier ; navigation des activités rendue accessible et internationalisée | `npm run build` OK ; `npm run test -- --watch=false` OK (103 tests) | Extraire les panneaux cliniques et valider la maquette avec les référents métier |
| 2026-07-10 | Extraction des panneaux `notes`, `daily-care`, `medication` et `consumption` ; suppression des `alert()` associés du composant parent | `npm run build` OK ; `npm run test -- --watch=false` OK (111 tests) | Extraire consentements et bloc/CRO ; valider la maquette avec les référents métier |

## Risques

| Risque | Impact | Mitigation |
|---|---|---|
| Règles cliniques implicites non validées | P0 patient/sécurité | Validation Médecin Chef et Cadre infirmier avant implémentation |
| Refonte visuelle qui modifie des flux critiques | P0 financier/soin | Migration incrémentale, feature flags si disponibles, E2E et RBAC |
| Travail non versionné déjà présent | Écrasement/régression | Aucun fichier source existant modifié dans ce cadrage ; revue Git obligatoire |
| Incohérence de suivi EPIC-0018 | Livraison mal qualifiée | Réconcilier ticket, backlog et tracking avant release |

## Definition of Ready / Done

**Ready :** maquette de rôle validée, données/API connues, droits RBAC définis, règles cliniques ou DAF approuvées, critères d'acceptation et tests identifiés.

**Done :** critères validés, tests backend/Angular/E2E et sécurité exécutés, documentation et traductions mises à jour, revue métier obtenue, aucun risque P0/P1 ouvert.

## Vérifications de cadrage

- [x] Lecture des standards projet, du CDC synthétisé et des tickets EPIC-0017 / EPIC-0018.
- [x] Mesure des composants cibles : hospitalisation 640 lignes TypeScript et 797 lignes HTML.
- [x] Vérification de la propreté documentaire des nouveaux fichiers.
- [x] `web/npm run build` : compilation de production réussie.
- [x] `web/npm run test -- --watch=false` : 23 fichiers et 111 tests réussis.
- [ ] Les tests Maven ne sont pas exécutés : aucun code backend ni contrat API n'est modifié.
- [ ] `git diff --check` global reste en échec sur des espaces de fin de ligne préexistants dans le travail non versionné (notamment `ReceivableResponse`, `CashRegisterControllerTest` et `en.json`) ; hors périmètre de ce cadrage.

## Impact version / SemVer

MINOR prévisionnel : évolution rétrocompatible de parcours et d'API potentiellement nécessaires. Aucun bump ni release ne sont préparés à ce stade, car aucune modification applicative n'est livrée.
