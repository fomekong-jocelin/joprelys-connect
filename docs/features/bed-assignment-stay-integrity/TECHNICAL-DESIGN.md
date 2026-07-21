# Conception technique — Intégrité séjour et période des affectations de lit

## Migration V77

1. ajouter `active_hospitalization_id UUID` ;
2. le renseigner avec `hospitalization_id` pour les lignes actives ;
3. ajouter `fk_bed_assignments_hospitalization` avec `ON DELETE RESTRICT` ;
4. ajouter `ck_bed_assignments_active_hospitalization_consistency` ;
5. ajouter `ck_bed_assignments_assignment_period` ;
6. créer `uq_bed_assignments_active_hospitalization`.

Les étapes échouent sur les données incompatibles. PostgreSQL exécute la migration transactionnellement ; aucune suppression ou réparation automatique n'est incluse.

Avant tout déploiement, `PRE-MIGRATION-CHECKS.sql` doit être exécuté sur une copie restaurée puis sur la cible pendant la fenêtre de changement. Ses trois requêtes sont en lecture seule et doivent chacune retourner zéro ligne. Tout résultat bloque V77 jusqu'à qualification et plan de correction validé par le DBA, le responsable hospitalisation et, pour les règles de conservation, le DPO.

## Domaine

`BedAssignmentEntity` maintient simultanément :

- `active_bed_id` ;
- `active_hospitalization_id`.

La méthode `releaseAt(Instant)` remplace le setter générique de fin, exige une date non nulle et refuse toute date antérieure à `assignedAt`. Les callbacks JPA vérifient la période et resynchronisent les marqueurs.

Lors d'un transfert, la clôture de l'affectation source est explicitement flushée avant l'insertion de l'affectation cible. Cet ordre rend visible la libération du marqueur `active_hospitalization_id` à la contrainte unique, tout en restant dans la même transaction : toute erreur sur la cible annule également la clôture et le changement de statut des lits.

## Suppression

`ON DELETE RESTRICT` est retenu : une hospitalisation référencée ne peut plus être supprimée par une cascade patient. Le futur workflow de conservation devra archiver/anonymiser selon la politique réglementaire sans supprimer les faits de mouvement.

## Erreurs

Le service de création traduit les violations tardives en 409 avec un message générique ne révélant ni lit ni séjour concurrent. Les erreurs de programmation impossibles dans le chemin nominal restent couvertes par les contraintes SQL.

## Compatibilité

- API : aucun payload modifié ; message 409 généralisé.
- DB : additive, préflight obligatoire.
- UI/mobile : aucun changement.
- ADR : pas de nouvel ADR ; application de l'historisation et des invariants d'ADR-0002.
