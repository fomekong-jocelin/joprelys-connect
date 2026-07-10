# TECHNICAL-DESIGN — Cohérence visuelle des informations médicales patient

## Stack

- Angular 22 standalone, Tailwind CSS v4 CSS-first, i18n JSON FR/EN.
- Spring Boot/Maven uniquement pour la preuve diagnostique du contrat existant.

## Implémentation

- Étendre `UiIconName` et `IconComponent` avec des pictogrammes médicaux SVG réutilisables : protection/allergie, dossier clinique, vaccination et urgence.
- Importer `IconComponent` dans `PatientMedicalInfoComponent` et remplacer tous les emojis du périmètre par `app-ui-icon`.
- Utiliser les variables CSS sémantiques du design system pour les couleurs d'icônes.
- Ajouter les clés `patients.medicalInfo.emergencies.*` dans `fr.json` et `en.json`, puis supprimer les fallbacks codés en dur.
- Ramener les surfaces `rounded-xl` touchées au maximum documenté de `rounded-lg` (8 px).

## Contrat de consultation

`GET /api/visits/{visitId}/consultation` est correctement routé. Après validation de l'accès, `ConsultationService` lève un `404` si la consultation n'existe pas. Le composant Angular ignore ce cas comme prévu. La sémantique HTTP reste inchangée afin de ne pas introduire un changement de contrat silencieux.

## Tests

- Test Angular : présence des icônes partagées et absence des emojis remplacés.
- Test Angular : libellés Urgences résolus par i18n.
- Test Spring ciblé : absence de consultation → `404` documenté.
- Suite Angular et build de production.

## Dette connue

Le composant médical monolithique dépasse 500 lignes. Son découpage en composants Allergies, Antécédents, Vaccinations et Urgences est requis dans une tâche dédiée ; il n'est pas mélangé à ce correctif visuel borné.

## SemVer

PATCH, sans changement API ni base de données.
