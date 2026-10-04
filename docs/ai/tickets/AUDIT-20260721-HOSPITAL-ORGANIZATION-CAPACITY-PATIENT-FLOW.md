# AUDIT-20260721 — Organisation hospitalière, capacités et parcours patient

## Mode d'intervention

Audit fonctionnel et métier + Engineering + Project Manager + Architecture proposée.

## Statut

DONE — audit documentaire et technique terminé ; validation pluridisciplinaire et implémentation hors périmètre.

Addendum praticien du 2026-08-09 : le diagnostic ciblé du chemin urgence/admission jusqu’au lit est consigné dans [DIAG-20260809-PRACTITIONER-HOSPITAL-PATH-BED-ASSIGNMENT](DIAG-20260809-PRACTITIONER-HOSPITAL-PATH-BED-ASSIGNMENT.md) et [PRACTITIONER-PATIENT-FLOW-ADDENDUM](../../features/hospital-organization-capacity-audit/PRACTITIONER-PATIENT-FLOW-ADDENDUM.md). Les gaps P0 confirment que HOS-ADM, HOS-MOV et HOS-PATH restent à cadrer avant implémentation.

## Objectif

Évaluer sans présupposé la manière dont Joprelys modélise et opère les structures de santé, les services, les espaces, les lits, les hospitalisations, les affectations de personnel et le parcours patient, puis proposer un modèle cible adaptable aux petites structures comme aux réseaux multi-établissements.

## Périmètre

- Spring Boot, Flyway/PostgreSQL, API et RBAC ;
- Angular, routes, menus, formulaires et parcours ;
- documentation, tickets, scénarios de tests et jeux de données accessibles ;
- organisation, capacité, hospitalisation, urgences, bloc, laboratoire, imagerie, pharmacie, équipements et personnel.

## Hypothèses de travail

- La branche `main` au commit audité est la preuve primaire de l'implémentation réellement disponible.
- Un ticket déclaré DONE ne prouve pas l'existence d'un comportement sans correspondance dans le code, les migrations ou les tests.
- Une fonction non retrouvée après recherche croisée code/API/UI/DB/docs est qualifiée d'absente, pas d'implicite.
- Les recommandations cliniques et réglementaires devront être validées localement par les professionnels et autorités compétents avant implémentation.

## Actions

- [x] Lire les règles de gouvernance, standards techniques, design et release.
- [x] Identifier les tickets et spécifications existants liés au périmètre.
- [x] Reconstituer le modèle organisationnel, spatial et clinique réellement implémenté.
- [x] Auditer les entités, migrations, contraintes, APIs, rôles et permissions.
- [x] Auditer les écrans, formulaires, menus et parcours existants.
- [x] Auditer les tests et preuves de concurrence, sécurité et historisation.
- [x] Classer les écarts et risques avec priorité et complexité.
- [x] Définir le modèle cible, les workflows, statuts, règles métier, écrans et KPI.
- [x] Rédiger les scénarios de test fonctionnels.
- [x] Produire le backlog `EPIC-0027` avec stories, tâches, estimations et reviewers.
- [x] Mettre à jour le suivi projet et le changelog documentaire.
- [x] Effectuer la vérification finale des livrables.

## Livrables prévus

- `docs/features/hospital-organization-capacity-audit/AUDIT-REPORT.md` ;
- `docs/features/hospital-organization-capacity-audit/FUNCTIONAL-SPEC.md` ;
- `docs/features/hospital-organization-capacity-audit/TECHNICAL-DESIGN.md` ;
- `docs/features/hospital-organization-capacity-audit/DATA-MODEL.md` ;
- `docs/features/hospital-organization-capacity-audit/TEST-PLAN.md` ;
- `docs/features/hospital-organization-capacity-audit/API-CONTRACT.md` ;
- `docs/pm/backlog/EPIC-0027-hospital-organization-capacity-patient-flow.md` ;
- ADR proposé pour le modèle organisationnel et spatial cible.

## Critères d'acceptation

- [x] Le rapport distingue explicitement existant, partiel, absent et défectueux.
- [x] Chaque constat majeur est relié à une preuve du dépôt.
- [x] Les vingt-deux sections demandées sont couvertes.
- [x] Les écarts sont classés par catégorie, priorité et complexité.
- [x] Le backlog suit le format fonctionnel demandé et le découpage EPIC → stories → tâches.
- [x] Les validations métier, soignantes, administratives, sécurité et réglementaires nécessaires sont identifiées.

## Estimation

- Audit documentaire et technique : 5 jours senior.
- Validation métier pluridisciplinaire : 2 à 3 jours cumulés.
- Cadrage cible et backlog : 3 jours senior.
- Total de cadrage : 8 jours senior hors ateliers de validation.

## Profils et reviewers

- Responsable recommandé : architecte fonctionnel santé / senior full-stack.
- Reviewers : médecin responsable, cadre infirmier, responsable admissions, responsable biomédical, pharmacien, biologiste, DPO/RSSI, architecte données et QA senior.

## Impact version / SemVer

Aucun bump applicatif pour l'audit documentaire. L'implémentation du modèle cible est susceptible d'exiger un MAJOR en raison des contrats API et migrations structurantes.

## Tests et vérifications réalisés

- [x] Présence des 22 sections obligatoires dans le rapport principal.
- [x] Présence de 40 écarts classés et priorisés.
- [x] Présence de 34 scénarios fonctionnels structurés dans le plan de test.
- [x] Présence de 13 user stories dans `EPIC-0027`, avec tâches et sous-tâches.
- [x] `git diff --check` sans erreur de contenu ; seuls des avertissements de conversion LF/CRLF ont été émis sur des documents existants modifiés.
- [x] Tests Angular ciblés : 2 fichiers et 6 tests réussis.
- [ ] Tests backend non exécutables dans l'environnement courant : le wrapper PowerShell Maven échoue sur la résolution de `MAVEN_M2_PATH`, puis Maven système ne peut pas télécharger le parent Spring Boot 4.1.0 à cause de l'accès réseau restreint. Cette limite environnementale n'est pas assimilée à une validation backend.
- [x] Aucun code applicatif, contrat API, schéma SQL ou comportement d'exécution modifié par cet audit.

## Reste à faire

- Faire valider ADR-0002 et les workflows par médecin, cadre infirmier, admissions, biomédical, laboratoire, pharmacie, DPO/RSSI et DBA.
- Arbitrer la phase 0 critique sans l'engager silencieusement dans SPRINT-0014.
- Découper chaque story supérieure à 5 SP avant passage READY.
- Préparer une copie anonymisée et le rapport de réconciliation avant toute migration.
