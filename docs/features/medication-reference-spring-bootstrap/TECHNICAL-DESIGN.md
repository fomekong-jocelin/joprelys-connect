# Démarrage conditionnel du référentiel médicament — Conception technique

## Cause

Spring ne peut pas sélectionner implicitement un constructeur lorsqu'un composant en
déclare plusieurs. `RxNormMedicationReferencePort` en possède un pour la production
(`MedicationReferenceProperties`) et un pour les tests (`RestClient`).

## Correction

Le constructeur de production est marqué `@Autowired`. Le constructeur de test reste
package-private et non injectable.

Cette correction :

- conserve l'injection par constructeur ;
- ne crée aucun constructeur vide ;
- ne transforme pas `RestClient` en dépendance globale ambiguë ;
- ne modifie ni l'interface `MedicationReferencePort` ni le contrat clinique ;
- respecte l'activation par `@ConditionalOnProperty`.

## Alternative rejetée

Ajouter un constructeur sans argument masquerait la configuration et produirait un
client RxNorm invalide. Supprimer le constructeur de test alourdirait les tests
réseau. L'annotation explicite est la correction Spring minimale.

## Sécurité et exploitation

Aucun secret n'est ajouté. Les URL et timeouts restent externalisés. Après livraison,
la Recette pourra réactiver progressivement les deux feature flags et revenir
immédiatement au contournement en cas d'incident RxNav.

## Impact SemVer

PATCH, sans breaking change.
