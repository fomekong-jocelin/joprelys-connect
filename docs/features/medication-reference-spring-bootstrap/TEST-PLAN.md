# Démarrage conditionnel du référentiel médicament — Plan de test

## Automatisé

- `ApplicationContextRunner` avec les deux propriétés à `true` :
  un bean `RxNormMedicationReferencePort` est instancié ;
- propriétés à `false` : aucun bean RxNorm ;
- tests unitaires RxNorm existants conservés ;
- `./mvnw clean verify`.

## Recette

1. Déployer le correctif avec les flags encore désactivés.
2. Vérifier la stabilité du service.
3. Activer `JOPRELYS_MEDICATION_REFERENCE_ENABLED=true`.
4. Activer `JOPRELYS_RXNORM_ENABLED=true`.
5. Redémarrer une fois le service et vérifier `active (running)`.
6. Tester une résolution RxNorm contrôlée.
7. Contrôler l'absence de boucle de redémarrage et d'appel réseau au bootstrap.

## Résultat local du 26 juillet 2026

Le test de contexte a été ajouté mais Maven n'a pas pu construire le projet :
`spring-boot-starter-parent:4.1.0` est absent du cache local et le réseau est
indisponible. Ce test et `clean verify` doivent être exécutés par la CI avant
réactivation des flags en Recette.
