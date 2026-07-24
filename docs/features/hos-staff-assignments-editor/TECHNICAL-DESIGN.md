# TECHNICAL-DESIGN — Architecture et composant `StaffAssignmentEditorComponent`

## 1. Composant Angular `StaffAssignmentEditorComponent`

- **Localisation** : `web/src/app/clinic/staff/staff-assignment-editor.component.ts`
- **Pattern** : Angular Standalone Component avec signals (`signal`, `effect`, `input.required`).
- **Gestion de l'état UI** :
  - `showSpecialtyForm = signal(false)` : contrôle la visibilité du formulaire d'ajout de spécialité.
  - `showUnitForm = signal(false)` : contrôle la visibilité du formulaire d'ajout d'unité.
  - Réinitialisation et fermeture automatique du formulaire lors du succès d'une création (`addSpecialty`, `addUnit`).

## 2. Structure du Template HTML & Design System

- **Design System (`DESIGN.md`)** :
  - Rayon d'arrondi : `rounded-md` (4px à 6px) pour les boutons, formulaires et cartes.
  - Boutons : composant `ButtonComponent` (`app-ui-button`) avec `variant="primary"` et `variant="secondary"`.
  - Contrôle du texte des boutons : `white-space: nowrap` via `button-layout.css`.
  - Couleurs et variables CSS : `bg-[var(--app-surface)]`, `bg-[var(--app-surface-muted)]`, `border-[var(--app-border)]`, `text-[var(--text-primary)]`, `text-[var(--text-muted)]`.
  - Espacements : utilisation de `space-y-4`, `gap-3`, `pt-3`, `mt-4` pour garantir qu'aucun bouton n'est collé aux cartes d'information.

## 3. Dictionnaires i18n FR / EN

- **Localisation** :
  - `web/src/assets/i18n/features/staff-onboarding/fr.json`
  - `web/src/assets/i18n/features/staff-onboarding/en.json`
- **Clés introduites / enrichies** :
  - `staff.assignments.title`
  - `staff.assignments.help`
  - `staff.assignments.specialties`
  - `staff.assignments.specialtiesHelp`
  - `staff.assignments.addSpecialtyToggle`
  - `staff.assignments.chooseSpecialty`
  - `staff.assignments.validFrom`
  - `staff.assignments.validTo`
  - `staff.assignments.primarySpecialty`
  - `staff.assignments.addSpecialty`
  - `staff.assignments.units`
  - `staff.assignments.unitsHelp`
  - `staff.assignments.addUnitToggle`
  - `staff.assignments.unit`
  - `staff.assignments.chooseUnit`
  - `staff.assignments.assignmentRole`
  - `staff.assignments.chooseRole`
  - `staff.assignments.primaryUnit`
  - `staff.assignments.addUnit`
  - `staff.assignments.primary`
  - `staff.assignments.historical`
  - `staff.assignments.close`
  - `staff.assignments.noSpecialty`
  - `staff.assignments.noUnit`
  - `staff.assignments.openEnded`
  - `staff.assignments.loadError`
  - `staff.assignments.saveError`

## 4. Stratégie de Test

- Unit Tests Angular (`staff-assignment-editor.component.spec.ts` / `staff-management.component.spec.ts`).
- Verification de la compilation TypeScript & Lint (`npm run lint`, `npm run build`).
