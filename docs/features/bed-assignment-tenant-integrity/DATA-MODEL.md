# Modèle de données — Cohérence établissement des affectations de lit

## Clés candidates

- `uq_hospitalizations_id_organization (id, organization_id)` ;
- `uq_beds_id_organization (id, organization_id)`.

## Clés étrangères composites

- `fk_bed_assignments_hospitalization_organization` :
  `(hospitalization_id, organization_id) → hospitalizations(id, organization_id)` ;
- `fk_bed_assignments_bed_organization` :
  `(bed_id, organization_id) → beds(id, organization_id)`.

Les deux relations utilisent `ON DELETE RESTRICT`.

## Cardinalités

- un séjour possède zéro à plusieurs affectations historiques du même établissement ;
- un lit possède zéro à plusieurs affectations historiques du même établissement ;
- une affectation appartient exactement à l'établissement commun à ses deux parents.

## Limites restantes

Les intervalles clôturés peuvent encore se chevaucher. Les relations `Ward → Room → Bed` ne possèdent pas encore toutes une cohérence tenant composite ; elles relèvent du futur référentiel géographique.
