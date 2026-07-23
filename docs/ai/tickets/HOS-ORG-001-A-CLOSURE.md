# HOS-ORG-001-A — Clôture gouvernée

## Résultat

HOS-ORG-001-A est livré dans `main` par la PR #133 au commit `72b5e139592b20a9ea14ae366d3fcbab99c46cf1`.

## Preuves

- backend Maven strict : vert ;
- frontend tests et build production : verts ;
- PostgreSQL 16 V1 → V87 : vert ;
- isolation tenant : testée ;
- RBAC : testé ;
- mobile-first, FR/EN, light/dark : implémentés et testés ;
- aucun développement serveur ;
- aucune action PROD/RECETTE.

## Décision

Le ticket #130 peut être clôturé comme terminé. HOS-LOC-001-A (#131) devient le prochain incrément actif et doit repartir du `main` contenant #133.

## Limites connues et assumées

- le modèle `Ward/Room/Bed` reste utilisé par l'hospitalisation jusqu'à HOS-LOC-001-A ;
- les champs historiques `users.department/users.specialty` restent présents jusqu'à HOS-STAFF-001-A ;
- aucune donnée historique ambiguë n'est convertie automatiquement.
