# STORY-2302 — Finaliser le triage ABCDE et les réévaluations d’urgence

- GitHub : #42
- Parent : #36
- Statut : IN_PROGRESS
- Priorité : P0
- Mode : Engineering + Documentation First
- Branche : `agent/story-2302-abcde-triage`

## Contexte

La PR #48 a déjà livré la création atomique et idempotente du patient URG-TEMP, du dossier d’urgence et du triage initial. Le reliquat de #42 concerne la structuration clinique ABCDE, les réévaluations horodatées et l’historique des orientations recommandées.

## Périmètre retenu

- conserver sans rupture les endpoints et champs existants ;
- créer un journal append-only des évaluations de triage ;
- enregistrer automatiquement une évaluation initiale pour toute nouvelle urgence ;
- reprendre les urgences existantes par migration sans inventer de données cliniques ;
- permettre des réévaluations ABCDE horodatées avant stabilisation ;
- historiser les constantes, le niveau de triage et l’orientation recommandée ;
- exposer une API tenantée et protégée ;
- ajouter un panneau Angular mobile-first, FR/EN, light/dark et accessible ;
- couvrir H2, PostgreSQL 16, RBAC, cross-tenant, concurrence et non-régression.

## Hors périmètre

- hospitalisation, documents et finance différée : #46 ;
- recette métier E2E globale : #47 ;
- modification destructive des anciennes colonnes de triage ;
- calcul automatique d’un niveau de triage par le frontend.

## Critères d’acceptation

- [ ] Toute urgence possède au moins une évaluation initiale historisée.
- [ ] L’évaluation initiale peut conserver `NOT_ASSESSED` pour les axes ABCDE non renseignés.
- [ ] Une réévaluation enregistre les cinq axes ABCDE, les constantes disponibles, l’auteur et l’heure clinique.
- [ ] Deux réévaluations concurrentes obtiennent des séquences distinctes sans écrasement.
- [ ] Aucune réévaluation n’est acceptée après stabilisation.
- [ ] Un utilisateur sans `EMERGENCY_WRITE` est refusé.
- [ ] Un établissement ne peut ni lire ni écrire le triage d’un autre tenant.
- [ ] L’historique affiche clairement initial vs réévaluation, ordre chronologique et orientation recommandée.
- [ ] L’UI fonctionne sur mobile, au clavier, en FR/EN et thèmes light/dark.
- [ ] Maven, migrations H2/PostgreSQL, tests Angular et build production sont verts.

## Action plan

- [x] Lire `AGENTS.md`, `SKILL.md`, `DESIGN.md` et les standards obligatoires.
- [x] Auditer #42, la PR #48 et le code d’urgence existant.
- [x] Définir le périmètre résiduel sans créer de doublon.
- [x] Créer la documentation fonctionnelle, technique, API, données et tests.
- [ ] Ajouter la migration V64 et le modèle append-only.
- [ ] Ajouter le use case et les endpoints de triage.
- [ ] Enregistrer automatiquement l’évaluation initiale.
- [ ] Ajouter le panneau Angular de réévaluation et d’historique.
- [ ] Ajouter les traductions FR/EN.
- [ ] Ajouter les tests backend et frontend.
- [ ] Exécuter la CI complète et corriger les écarts.
- [ ] Mettre à jour `PROJECT-TRACKING.md`, `CHANGELOG.md` et le ticket #42.
- [ ] Ouvrir une PR prête pour revue après CI verte.

## Architecture et responsabilités

- Controller : validation HTTP, permission et délégation uniquement.
- Use case : verrouillage, règles de séquence, état stabilisé et transaction.
- Infrastructure : persistance JPA tenantée et migration Flyway.
- Angular : collecte, affichage et orchestration UX ; aucune décision clinique automatique.
- Données : journal append-only, aucun écrasement d’une évaluation antérieure.

## Estimation du reliquat

- 5 SP.
- Senior full-stack santé : 1,5 à 2 jours incluant tests et documentation.
- Reviewer : Tech Lead + médecin urgentiste + QA.

## Impact SemVer

- Bump recommandé : MINOR.
- Motif : nouvel endpoint, nouveau journal clinique et nouveau panneau UI rétrocompatibles.
- Breaking change : non.

## Risques

- migration des urgences historiques sans fausse donnée clinique ;
- concurrence sur le numéro de séquence ;
- exposition de données cliniques inter-tenant ;
- surcharge cognitive sur mobile ;
- incohérence entre orientation recommandée et orientation finale.

## Definition of Done

Le ticket n’est DONE qu’après CI verte, preuve H2/PostgreSQL 16, contrôle RBAC/cross-tenant, documentation à jour et PR fusionnée.