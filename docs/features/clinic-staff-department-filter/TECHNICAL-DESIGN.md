# Spécification Technique — Menu déroulant des services et filtrage des praticiens lors de l'admission

> **SUPERSEDED — HOS-STAFF-001-A / #132**
>
> Cette conception décrit l'ancien modèle `users.department` / filtrage par libellé texte.
> Depuis HOS-STAFF-001-A, `users.department` et `users.specialty` ne sont plus des identités métier :
> les rattachements du personnel reposent sur `staff_organizational_unit_assignments` et
> `staff_specialty_assignments`, avec UUID/codes contrôlés et périodes de validité.
>
> Le champ `visits.service_name` du module Visite reste pour l'instant un snapshot lisible historique.
> La projection Angular `StaffMember.department`, lorsqu'elle est encore utilisée par ce formulaire,
> est dérivée de l'unité active principale et n'est jamais persistée ni acceptée dans un payload staff.
> Toute évolution future du module Visite doit consommer `activeOrganizationalUnits[].id` et ne doit
> pas réintroduire de saisie libre dans le modèle personnel.

## 1. Architecture IHM Angular — historique

Cette section est conservée uniquement pour retracer la conception remplacée.

```typescript
export const DEFAULT_DEPARTMENTS: readonly string[] = [
  'Médecine générale',
  'Pédiatrie',
  'Gynécologie',
  'Urgences',
  'Pharmacie',
  'Laboratoire',
  'Cardiologie'
];
```

### Formulaire Collaborateur — historique, ne plus appliquer

L'ancien formulaire proposait un `<select>` de départements et un champ libre « Autre ».
Cette règle est supprimée du parcours staff par HOS-STAFF-001-A : les unités sont sélectionnées
exclusivement depuis HOS-ORG et les spécialités depuis `medical_specialty_catalog`.

### Formulaire Admission / Visite — dette isolée

Le formulaire historique de visite conserve actuellement `visits.service_name` comme snapshot texte.
Le filtrage personnel ne doit plus dépendre d'une colonne `users.department` : le service Angular staff
expose désormais les unités organisationnelles actives structurées (`id`, `code`, libellés FR/EN,
indicateur principal). Le snapshot de présentation éventuel est dérivé de ces références.

## 2. Impacts Backend & Base de données — état remplacé

L'affirmation historique « aucun impact backend ou base » n'est plus valide.
HOS-STAFF-001-A introduit V92–V95, les tables d'affectations structurées, les contraintes de période
et supprime physiquement `users.department` / `users.specialty` après preflight fail-fast.
