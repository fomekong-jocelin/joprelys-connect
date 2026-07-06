# Spécification Technique — Menu déroulant des services et filtrage des praticiens lors de l'admission

## 1. Architecture IHM Angular
Nous allons centraliser la liste des départements/services cliniques sous forme de constante réutilisable.

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

### Formulaire Collaborateur (`StaffManagementComponent` & `ProfileComponent`)
1. Dans le template HTML, remplacer le champ de texte `department` par :
   - Un élément `<select>` lié à un état temporaire `selectedDepartment`.
   - Si `selectedDepartment === 'Autre'`, afficher un champ texte libre pour `customDepartment`.
   - Lors de la soumission du formulaire, concaténer ou assigner la valeur finale : si "Autre", utiliser `customDepartment`, sinon utiliser `selectedDepartment`.

### Formulaire Admission (`PatientDetailComponent`)
1. Remplacer l'élément `<input>` de `visitService` par un `<select>`.
2. Générer dynamiquement les options de ce `<select>` en combinant `DEFAULT_DEPARTMENTS` et tous les départements uniques non vides présents dans la liste des collaborateurs (`staffList()`).
3. Mettre à jour `getFilteredPractitioners()` pour filtrer les praticiens :
   - Si un service clinique est spécifié : filtrer les praticiens dont le département correspond (comparaison insensible à la casse).
   - Si aucun praticien n'est trouvé pour ce service, utiliser le filtrage d'orientation classique en secours (fallback).
4. Lorsque `visitOrientation` change, pré-sélectionner le service correspondant s'il y a correspondance.
5. Lorsque `visitService` change, réinitialiser `visitMainPractitionerId` si le médecin précédemment choisi ne fait pas partie de la liste filtrée.

## 2. Impacts Backend & Base de données
- Aucun impact backend ou base de données. Le champ `department` (users) et `service` (visits) restent des chaînes de caractères standards au niveau de l'API REST.
