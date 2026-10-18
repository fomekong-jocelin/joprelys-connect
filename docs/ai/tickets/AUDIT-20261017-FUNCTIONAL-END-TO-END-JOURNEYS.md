# AUDIT-20261017 — Audit fonctionnel de bout en bout : configuration, intervenants et circuits patient

## Mode d'intervention

QA fonctionnelle et diagnostic métier (audit), avec un volet Project Manager pour le découpage du backlog de correction. Aucun code applicatif modifié.

## Statut

DONE pour l'audit. Les corrections font l'objet de l'[EPIC-0029](../../pm/backlog/EPIC-0029-functional-journey-coherence.md), à arbitrer avec le PO et les référents cliniques.

## Objectif

Vérifier la cohérence fonctionnelle de Joprelys de la configuration d'un établissement jusqu'à la clôture complète d'une prise en charge. L'audit suit chaque intervenant et chaque transmission, puis propose des corrections applicables.

## Livrables

- [Rapport d'audit](../../features/functional-e2e-audit-20261017/AUDIT-REPORT.md) : synthèse, couverture, cartographie, configuration, responsabilités, registre des constats, parcours cibles et questions bloquantes.
- [Plan de recette de bout en bout](../../features/functional-e2e-audit-20261017/TEST-PLAN.md).
- [Preuves d'exécution](../../features/functional-e2e-audit-20261017/RUNTIME-EVIDENCE.md) et [scripts de scénarios](../../features/functional-e2e-audit-20261017/scenarios/).
- [Backlog de correction EPIC-0029](../../pm/backlog/EPIC-0029-functional-journey-coherence.md).

## Actions

- [x] Lire la gouvernance du dépôt, les audits précédents (AUDIT-20260721, TICKET-20261003, DIAG-20260809) et le changelog actif.
- [x] Cartographier le périmètre réel : 24 modules backend, 324 mappings REST, 55 entrées de routes Angular, 17 rôles système et 85 permissions.
- [x] Analyser le code des parcours : visite, prise en charge, consultation, laboratoire, prescription, pharmacie, hospitalisation, lits, urgences, facturation, caisse, rendez-vous et identité.
- [x] Monter un environnement de test isolé : base PostgreSQL dédiée `joprelys_audit_e2e_20261017`, backend sur le port 8095, IA désactivée et données fictives uniquement.
- [x] Exécuter 8 scénarios multi-rôles par l'API : 123 vérifications, dont 33 en écart (4 de ces écarts sont des artefacts du harnais, documentés comme tels).
- [x] Rédiger le registre des constats (45 constats, FA-01 à FA-45), relié aux preuves.
- [x] Proposer les parcours cibles, le backlog, le plan de recette et les questions métier.
- [x] Mettre à jour PROJECT-TRACKING et CHANGELOG (entrée documentaire).
- [ ] Faire valider les questions métier Q-01 à Q-10 par le PO, un médecin référent, un cadre infirmier, un pharmacien, la DAF et le DPO.
- [ ] Recette visuelle navigateur des parcours, en clair et en sombre, FR/EN et mobile. Non réalisée dans cette intervention ; voir les limites de couverture.

## Critères d'acceptation de l'audit

- [x] Chaque constat a un identifiant stable, une preuve et un degré de certitude (observé, déduit ou hypothèse).
- [x] La gravité est séparée de la certitude.
- [x] Chaque constat structurant a une solution, des règles de gestion, des critères d'acceptation et ses dépendances.
- [x] Les fonctions inaccessibles ou non vérifiées apparaissent comme limites de couverture.
- [x] Aucun code applicatif ni aucune donnée réelle n'ont été modifiés.

## Estimation et capacité

- Audit réalisé : environ 3 jours senior en équivalent.
- Backlog proposé : 117 SP (voir EPIC-0029), soit environ 3 à 4 sprints à capacité nominale selon `docs/pm/CAPACITY-PLANNING.md`. Aucune capacité n'a été engagée ; arbitrage PO requis.

## SemVer

Audit documentaire : aucun bump. Les corrections P0 et P1 introduiront des statuts et des contrats nouveaux : bump MINOR minimum. Le passage de `service` (texte libre) à `serviceUnitId` sur la visite et la séparation statut d'ordonnance / statut de délivrance sont des candidats MAJOR (contrat API).

## Environnement de test laissé en place

- Base `joprelys_audit_e2e_20261017` sur le PostgreSQL local, contenant uniquement des données fictives. Pour la supprimer : `DROP DATABASE joprelys_audit_e2e_20261017;`.
- L'instance de développement (port 8080, base `joprelys`) n'a pas été modifiée.
