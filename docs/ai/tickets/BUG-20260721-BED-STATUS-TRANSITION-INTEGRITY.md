# HOS-BED-002-B — Sécuriser les transitions manuelles de statut d’un lit

> **État actuel** : ce ticket documente le garde-fou livré avant la séparation RBAC complète. Son risque résiduel historique sur `HOSPITALIZATION_MANAGE` a été traité par HOS-RBAC-001-A/B/C puis supprimé définitivement par HOS-RBAC-001-D / #121 avec V86. Les références ci-dessous à cette permission décrivent l'état antérieur au durcissement RBAC.

## Métadonnées

- **Type** : Bug / garde-fou P0
- **Epic** : EPIC-0027 — Organisation hospitalière, capacité et parcours patient
- **Story** : HOS-BED-002 — Séparer capacité, disponibilité et remise en état
- **Audit** : AUDIT-20260721
- **Écarts couverts** : GAP-006 ; réduction partielle de GAP-008 et GAP-016
- **Priorité** : Critique
- **Statut** : QA CI VERTE / REVUE MÉTIER REQUISE
- **Estimation** : 2 SP / 1 à 2 jours
- **SemVer indicatif** : PATCH dans le modèle legacy, inclus dans la cible MINOR globale de l’epic

## Contexte

À l'origine, l’endpoint legacy `POST /api/spatial/beds/{id}/status` permettait à tout utilisateur disposant de `HOSPITALIZATION_MANAGE` de modifier directement un lit vers `OCCUPIED`, `CLEANING`, `MAINTENANCE` ou `FREE`.

Cette mutation générique pouvait désynchroniser trois sources de vérité :

- le statut du lit ;
- l’affectation active `BedAssignment` ;
- le séjour `Hospitalization`.

Le cas le plus dangereux était la transition manuelle vers `FREE` : le service clôturait l’affectation active sans mettre à jour le séjour et sans confirmer le départ physique du patient.

## Objectif

Transformer l’endpoint legacy en commande opérationnelle limitée aux lits non affectés, sans lui permettre de créer ou de clôturer une occupation.

## Règles métier

1. `OCCUPIED` ne peut jamais être demandé par l’endpoint générique ; seule une admission ou un transfert peut créer une occupation.
2. Tant qu’une affectation active existe, aucune transition manuelle du statut du lit n’est autorisée.
3. Un lit marqué `OCCUPIED` sans affectation active est une incohérence à réconcilier ; l’endpoint ne doit pas la masquer.
4. Un lit non affecté peut encore passer entre les états opérationnels legacy `FREE`, `CLEANING` et `MAINTENANCE`.
5. Une requête idempotente conservant le même état d’un lit non affecté ne produit ni écriture ni audit inutile.
6. La libération d’une occupation doit être réalisée uniquement par les workflows de transfert, sortie physique et remise en état.

## Critères d’acceptation

- Étant donné un lit libre, lorsque l’API demande `OCCUPIED`, alors elle répond `409` et ne crée aucune affectation.
- Étant donné un lit avec une affectation active, lorsque l’API demande `FREE`, `CLEANING` ou `MAINTENANCE`, alors elle répond `409`, conserve l’affectation et ne modifie pas le lit.
- Étant donné un lit marqué `OCCUPIED` sans affectation, lorsque l’API demande un autre statut, alors elle répond `409` avec une indication de réconciliation nécessaire.
- Étant donné un lit libre sans affectation, lorsque l’API demande `MAINTENANCE`, alors la transition réussit.
- Étant donné un lit en nettoyage sans affectation, lorsque l’API demande `FREE`, alors la transition réussit.
- Aucun chemin de ce ticket ne clôt directement `BedAssignment.releasedAt`.

## Implémentation

- Ajouter une politique dédiée `BedStatusTransitionPolicy`.
- Faire valider toute commande manuelle par cette politique dans `SpatialService.updateBedStatus`.
- Supprimer la clôture automatique d’affectation lors du passage manuel à `FREE`.
- Conserver les commandes transactionnelles d’admission et de transfert comme seules sources de `OCCUPIED`.
- Corriger le binding JDBC des `Instant` dans les assertions PostgreSQL V76–V78 afin que leur validation s’exécute réellement en CI.

## Tests

Tests ciblés couverts :

- refus de `FREE → OCCUPIED` ;
- refus de toute transition avec affectation active ;
- absence de clôture silencieuse de l’affectation ;
- refus d’un lit `OCCUPIED` orphelin ;
- succès d’une transition opérationnelle sur lit non affecté.

Validation CI :

- workflow `Joprelys Connect — CI Pipeline`, run 914 ;
- backend `Maven Build & Tests` : succès avec `verify` strict ;
- assertions PostgreSQL V76–V78 : exécutées avec succès ;
- tests Angular : succès ;
- build Angular production : succès.

## Hors périmètre

- remplacement des quatre statuts legacy par les axes existence/ouverture/hygiène/usage ;
- workflow de sortie médicale, administrative et physique ;
- tâche de turnover et validation du bionettoyage ;
- correction automatique des incohérences historiques ;
- contraintes PostgreSQL de chevauchement HOS-BED-001-D.

La séparation complète des permissions hygiène, maintenance, transfert, sortie et écritures cliniques a été prise en charge par HOS-RBAC-001-A/B/C/D et n'est plus un hors-périmètre ouvert de ce ticket.

## Risques résiduels actuels

- `HOSPITALIZATION_MANAGE` n'est plus un risque accepté : HOS-RBAC-001-D le retire définitivement.
- les transitions `FREE`, `CLEANING` et `MAINTENANCE` ne portent pas encore toutes les preuves opérationnelles attendues ;
- le statut monolithique reste un modèle transitoire ;
- une procédure de réconciliation doit être définie pour les lits `OCCUPIED` sans affectation active.

## DoD

- [x] politique de transition isolée ;
- [x] aucune clôture d’affectation depuis l’endpoint générique ;
- [x] tests unitaires ciblés ajoutés ;
- [x] backend Maven `verify` strict exécuté en CI ;
- [x] validation PostgreSQL V76–V78 exécutée en CI ;
- [x] tests et build Angular verts ;
- [ ] recette API avec profils autorisés et non autorisés ;
- [ ] validation cadre/bed manager ;
- [ ] suivi EPIC, changelog et project tracking finalisé après fusion.
