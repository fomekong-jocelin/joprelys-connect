# Plan de test — Intégrité séjour et période

## INT-STAY-001 — Séjour inexistant

- Insérer une affectation vers un UUID de séjour absent.
- Attendu : violation de FK ; aucune ligne créée.

## INT-STAY-002 — Double présence active

- Créer deux affectations actives sur deux lits différents pour le même séjour.
- Attendu : la seconde est refusée par l'index unique.

## INT-STAY-003 — Réaffectation après clôture

- Clôturer la première affectation puis en créer une nouvelle pour le même séjour.
- Attendu : succès et historique conservé.

## INT-STAY-004 — Période inversée

- Clôturer avant `assigned_at`, par le domaine puis par SQL direct.
- Attendu : refus dans les deux cas.

## INT-STAY-005 — Suppression restrictive

- Tenter de supprimer un séjour référencé.
- Attendu : refus ; affectation inchangée.

## INT-STAY-006 — Admission, transfert et sortie

- Vérifier les parcours existants et le rollback des collisions.
- Attendu : comportements nominaux inchangés, collisions en 409.

## INT-STAY-007 — PostgreSQL 16

- Vérifier colonne, FK `NO ACTION/RESTRICT`, CHECK, index et collisions sous Testcontainers.
- Attendu : V77 courante et toutes les assertions vertes.

## INT-STAY-008 — Préflight de production

- Exécuter `PRE-MIGRATION-CHECKS.sql` sur une restauration anonymisée représentative.
- Attendu : zéro ligne pour les orphelins, doublons actifs par séjour et périodes inversées ; sinon déploiement bloqué sans correction automatique.

## Non-régression

- tests ciblés spatial/hospitalisation ;
- `mvn clean verify` ;
- frontend non relancé car aucun fichier web n'est modifié par cet incrément.
