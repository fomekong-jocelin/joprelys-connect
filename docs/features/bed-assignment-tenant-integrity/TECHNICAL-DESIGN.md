# Conception technique — Cohérence établissement des affectations de lit

## Migration V78

1. ajouter une contrainte unique `(id, organization_id)` sur `hospitalizations` ;
2. ajouter une contrainte unique `(id, organization_id)` sur `beds` ;
3. ajouter une FK composite `(hospitalization_id, organization_id)` restrictive ;
4. ajouter une FK composite `(bed_id, organization_id)` restrictive.

Les clés candidates sont nécessaires aux FK composites et restent compatibles avec les clés primaires UUID existantes. La migration est additive et échoue sur les incohérences ; elle n'effectue aucun `UPDATE` correctif.

## Application

`ActiveBedAssignmentService` compare l'établissement du lit avec l'établissement transmis par le use case. Une divergence produit un 409 générique avant tout appel repository. Les FK composites couvrent ensuite le séjour, les imports et les écritures SQL directes.

## Suppression

Les deux FK composites utilisent `ON DELETE RESTRICT`, comme les FK simples V44/V77. L'historique doit être archivé, jamais supprimé par cascade.

## Compatibilité

- API : aucun payload modifié ; nouveau motif 409 sur incohérence tenant.
- DB : migration additive, préflight obligatoire.
- UI/mobile : aucun changement.
- ADR : application d'ADR-0002, sans nouvelle décision structurante.
