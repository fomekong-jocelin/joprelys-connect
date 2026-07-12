# TECHNICAL-DESIGN — Shell de régularisation patient

## Conception

`PatientReconciliationPageComponent` importe et projette son contenu dans `AppShellComponent`, déjà utilisé par les autres routes cliniques. Aucun routeur, guard, contrat API ou token de design n’est dupliqué.

## Vérification

Le test du composant remplace le shell par un stub de projection afin de vérifier l’intégration sans dépendre des services du shell. Les tests et le build Angular valident ensuite le template complet.
