# TECHNICAL-DESIGN — Navigation clinique orientée tâches

## Implémentation

`AppShellNavComponent` conserve les routes et permissions existantes, ajoute des séparateurs de rubrique non navigables et utilise des clés i18n dédiées pour les libellés métier.

## Sécurité

Les rubriques sont ajoutées uniquement lorsqu’au moins un lien enfant est accessible. Aucun contrôle RBAC n’est déplacé dans le frontend : les permissions restent calculées par le service d’accès existant.

## Compatibilité

Aucun changement de route ni de contrat API. Le changement est limité à l’architecture d’information, aux textes FR/EN et aux tests de navigation.
