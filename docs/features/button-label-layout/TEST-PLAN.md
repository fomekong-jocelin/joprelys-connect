# Button label layout — Test plan

## Objectif

Prouver que les libellés des boutons standard restent sur une seule ligne sans créer de régression responsive, visuelle ou d'accessibilité.

## Vérifications automatisées

### Angular

```bash
cd web
npm ci
npm run lint
npm run test -- --watch=false
npm run build
```

Résultat attendu : toutes les commandes réussissent sans warning bloquant.

## Recette visuelle

Tester les largeurs suivantes :

- 320 px ;
- 375 px ;
- 768 px ;
- 1440 px.

Tester au minimum :

- en-tête de page avec action primaire ;
- groupe de trois actions ;
- modale avec Annuler / Enregistrer ;
- bouton icône + texte ;
- bouton texte seul ;
- bouton icône seule ;
- bouton disabled ;
- variantes primaire, secondaire et danger.

## Matrice

| Scénario | Light FR | Dark FR | Light EN | Dark EN |
|---|---:|---:|---:|---:|
| Libellé court | À tester | À tester | À tester | À tester |
| Libellé long | À tester | À tester | À tester | À tester |
| Groupe avec retour à la ligne | À tester | À tester | À tester | À tester |
| Bouton disabled | À tester | À tester | À tester | À tester |
| Icône + texte | À tester | À tester | À tester | À tester |

## Assertions

- le texte ne passe pas sur une seconde ligne ;
- le bouton conserve une hauteur stable ;
- le conteneur peut revenir à la ligne entre deux boutons ;
- l'icône et le texte restent centrés ;
- aucun scroll horizontal global ;
- focus visible conservé ;
- aucune différence fonctionnelle entre light et dark.

## Non-régression

Vérifier en priorité :

- configuration de la structure hospitalière ;
- facturation et devis ;
- bordereaux assurance ;
- rendez-vous patient ;
- fenêtres de confirmation.
