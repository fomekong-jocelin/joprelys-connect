# Plan de test — Cohérence établissement des affectations de lit

## TENANT-BED-001 — Affectation nominale

- Créer séjour, lit et affectation dans le même établissement.
- Attendu : succès, marqueurs actifs cohérents.

## TENANT-BED-002 — Garde applicative

- Fournir au service un lit appartenant à un autre établissement.
- Attendu : 409 avant appel repository, sans identifiant concurrent exposé.

## TENANT-BED-003 — Écriture SQL incohérente

- Insérer une affectation avec un `organization_id` différent de ses parents.
- Attendu : violation d'une FK composite ; aucune ligne créée.

## TENANT-BED-004 — Schéma

- Vérifier les deux clés candidates, les deux FK composites et leur règle `RESTRICT`.
- Attendu : quatre contraintes présentes sous H2 et PostgreSQL 16.

## TENANT-BED-005 — Parcours

- Rejouer admission, transfert, rollback de collision et sortie.
- Attendu : parcours nominaux inchangés.

## TENANT-BED-006 — Préflight

- Exécuter `PRE-MIGRATION-CHECKS.sql` sur une restauration représentative.
- Attendu : zéro ligne ; sinon déploiement bloqué sans correction automatique.

## Non-régression

- tests ciblés spatial/hospitalisation ;
- test de migration PostgreSQL préparé ;
- `mvn clean verify` ;
- frontend non relancé car cet incrément ne modifie aucun fichier web.
