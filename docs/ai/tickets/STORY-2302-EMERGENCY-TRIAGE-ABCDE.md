# STORY-2302 — Finaliser le triage ABCDE et les réévaluations d’urgence

- GitHub : #42
- Parent : #36
- Statut : QA
- Priorité : P0
- Mode : Engineering + Documentation First
- Branche : `agent/story-2302-abcde-triage`
- Pull request : #53

## Contexte

La PR #48 a déjà livré la création atomique et idempotente du patient URG-TEMP, du dossier d’urgence et du triage initial. Le reliquat de #42 concerne la structuration clinique ABCDE, les réévaluations horodatées et l’historique des orientations recommandées.

## Périmètre retenu

- conserver sans rupture les endpoints et champs existants ;
- créer un journal append-only des évaluations de triage ;
- enregistrer automatiquement une évaluation initiale pour toute nouvelle urgence ;
- reprendre les urgences existantes par migration sans inventer de données cliniques ;
- permettre des réévaluations ABCDE horodatées avant stabilisation ;
- historiser les constantes, le niveau de triage et l’orientation recommandée ;
- exposer une API tenantée, auditée et protégée ;
- ajouter un panneau Angular mobile-first, FR/EN, light/dark et accessible ;
- aligner l’écriture affichée sur l’accès RBAC effectif ;
- couvrir H2, PostgreSQL 16, RBAC, cross-tenant, concurrence et non-régression.

## Hors périmètre

- hospitalisation, documents et finance différée : #46 ;
- recette métier E2E globale : #47 ;
- modification destructive des anciennes colonnes de triage ;
- calcul automatique d’un niveau de triage par le frontend.

## Critères d’acceptation

- [x] Toute urgence possède au moins une évaluation initiale historisée.
- [x] L’évaluation initiale peut conserver `NOT_ASSESSED` pour les axes ABCDE non renseignés.
- [x] Une réévaluation enregistre les cinq axes ABCDE, les constantes disponibles, l’auteur et l’heure clinique.
- [x] Deux réévaluations concurrentes obtiennent des séquences distinctes sans écrasement.
- [x] Aucune réévaluation n’est acceptée après stabilisation.
- [x] Un utilisateur sans `EMERGENCY_WRITE` est refusé et voit une interface en lecture seule.
- [x] Un établissement ne peut ni lire ni écrire le triage d’un autre tenant.
- [x] Les lectures et écritures produisent un audit sans contenu clinique sensible.
- [x] L’historique affiche clairement initial vs réévaluation, ordre chronologique et orientation recommandée.
- [x] L’UI respecte le design system, le mobile-first, le clavier, FR/EN et light/dark.
- [ ] Maven, migrations H2/PostgreSQL, tests Angular et build production sont verts sur le commit final.

## Action plan

- [x] Lire `AGENTS.md`, `SKILL.md`, `DESIGN.md` et les standards obligatoires.
- [x] Auditer #42, la PR #48 et le code d’urgence existant.
- [x] Définir le périmètre résiduel sans créer de doublon.
- [x] Créer la documentation fonctionnelle, technique, API, données et tests.
- [x] Ajouter la migration V64 et le modèle append-only.
- [x] Ajouter le use case et les endpoints de triage.
- [x] Enregistrer automatiquement l’évaluation initiale.
- [x] Ajouter le panneau Angular de réévaluation et d’historique.
- [x] Ajouter les traductions FR/EN et la visibilité RBAC.
- [x] Ajouter les tests backend, frontend, sécurité et concurrence.
- [ ] Exécuter la CI complète sur le commit final et corriger les écarts.
- [ ] Mettre à jour la preuve CI dans `PROJECT-TRACKING.md`, la PR et le ticket #42.
- [ ] Passer la PR prête pour revue après CI verte.

## Architecture et responsabilités

- Controller : validation HTTP, permission et délégation uniquement.
- Use case : verrouillage, règles de séquence, état stabilisé, audit et transaction.
- Infrastructure : persistance JPA tenantée et migration Flyway.
- Angular : collecte, affichage, accès RBAC et orchestration UX ; aucune décision clinique automatique.
- Données : journal append-only, aucun écrasement d’une évaluation antérieure.

## Estimation du reliquat

- 5 SP.
- Senior full-stack santé : 1,5 à 2 jours incluant tests et documentation.
- Reviewer : Tech Lead + médecin urgentiste + QA.

## Impact SemVer

- Bump recommandé : MINOR.
- Motif : nouvel endpoint, nouveau journal clinique et nouveau panneau UI rétrocompatibles.
- Breaking change : non.

## Risques résiduels

- validation visuelle humaine sur appareils réels ;
- validation par un médecin urgentiste de la terminologie et de l’ordre de saisie ;
- cohérence opérationnelle entre orientation recommandée et orientation finale, sans automatisation.

## Definition of Done

Le ticket n’est DONE qu’après CI verte, preuve H2/PostgreSQL 16, contrôle RBAC/cross-tenant, documentation à jour et PR fusionnée.