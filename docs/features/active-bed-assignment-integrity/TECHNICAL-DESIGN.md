# Conception technique — Intégrité des affectations actives de lit

## Décision

Ajouter `bed_assignments.active_bed_id UUID`, maintenu par le domaine :

- affectation active : `active_bed_id = bed_id` ;
- affectation clôturée : `active_bed_id = NULL`.

Un index unique sur `active_bed_id` interdit deux affectations actives d'un lit. Les valeurs `NULL` multiples conservent l'historique clôturé. Cette forme est compatible avec PostgreSQL 16 et H2 en mode PostgreSQL, contrairement à un index partiel spécifique.

## Migration V76

1. ajouter la colonne nullable ;
2. recopier `bed_id` uniquement sur les lignes actives ;
3. ajouter une contrainte CHECK de cohérence ;
4. créer l'index unique `uq_bed_assignments_active_bed`.

La création de l'index échoue si le préflight révèle des doublons. Aucun `DELETE`, choix arbitraire ou clôture automatique n'est autorisé.

## Domaine et application

`BedAssignmentEntity` synchronise le marqueur à la construction, au changement de lit, à la clôture et via les callbacks JPA. `ActiveBedAssignmentService` centralise la création et utilise `saveAndFlush` pour détecter la collision dans la transaction courante, puis la traduit en `ResponseStatusException(CONFLICT)`.

Admission et transfert conservent le claim atomique existant. La contrainte SQL forme une seconde barrière contre les données désynchronisées et les écritures non nominales.

## Transactions

En cas de conflit lors du flush, l'exception 409 remonte et la transaction appelante annule :

- le passage du lit à `OCCUPIED` ;
- la création/modification du séjour ;
- la clôture éventuelle de l'ancienne affectation lors d'un transfert ;
- la nouvelle affectation.

## Compatibilité

- API : aucun schéma modifié ; nouvelle réponse 409 sur une incohérence auparavant susceptible de produire une erreur 500 ou une donnée invalide.
- DB : migration additive, mais préflight obligatoire.
- Frontend/mobile : aucun changement.
- ADR : pas de nouvel ADR ; cette barrière additive applique l'invariant déjà retenu par ADR-0002 sans figer le modèle cible complet.
