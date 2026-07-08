# Spécification Fonctionnelle — Responsivité de la Vérification d'Ordonnance (Portail Pharmacie)

## 1. Problème métier à résoudre
Sur l'écran de vérification d'ordonnance du portail pharmacie, lorsque l'application est visualisée sur un écran mobile ou étroit (largeur inférieure à 1024px ou 768px), le contenu de la carte de recherche d'ordonnance et de la carte de délivrance d'ordonnance déborde horizontalement. 
Cela provoque une coupure visuelle majeure :
- Le bouton de validation de la recherche (dont le texte est "Vérifier") est coupé à "Véri".
- La phrase d'aide et les champs de saisie (Numéro d'ordonnance et Code PIN) sont tronqués sur leur bordure droite.
- L'utilisateur n'a pas accès à la totalité de l'interface et l'esthétique générale de l'application est dégradée.

## 2. Utilisateurs concernés
- Les pharmaciens et préparateurs en pharmacie accédant au portail pharmacie depuis un appareil mobile ou une tablette.

## 3. Objectif de la fonctionnalité
Rendre l'interface de recherche et de délivrance d'ordonnances entièrement adaptative et fluide sur toutes les tailles d'écrans, en particulier sur mobile (Mobile-First / Responsive). Le contenu doit s'ajuster automatiquement à la largeur du viewport disponible sans déborder ni être tronqué, et les tableaux de dispensation volumineux doivent pouvoir défiler horizontalement de manière isolée au sein de leur carte sans étirer l'ensemble de la page.

## 4. Périmètre inclus / exclu
- **Inclus** :
  - L'écran de vérification d'ordonnance du portail pharmacie (`app-pharmacy-prescription-verify-page`).
  - Le panneau de dispensation et d'historique de délivrance (`app-pharmacy-dispensation-panel`).
  - L'ajustement du comportement de rétrécissement (shrink) des cartes Grid et Flex.
- **Exclu** :
  - Modification des flux métiers ou des API de validation et délivrance d'ordonnances.
  - Refonte des thèmes de couleurs ou typographies.

## 5. Parcours utilisateur attendu
1. Le pharmacien accède à la page de vérification d'ordonnance sur son mobile.
2. Il voit la boîte de recherche d'ordonnance s'adaptateur parfaitement à la largeur de son écran.
3. Le texte d'explication, les labels, les inputs (Numéro d'ordonnance et PIN) et le bouton de validation "Vérifier" s'affichent en entier et de manière lisible.
4. Une fois l'ordonnance chargée, les détails et le tableau des médicaments s'affichent.
5. Sur mobile, le tableau de dispensation permet un défilement horizontal local sans altérer la mise en page de la carte et du reste de l'écran.

## 6. Règles métier connues
- L'utilisateur doit pouvoir saisir le numéro d'ordonnance et le code PIN complets et les soumettre.
- La mise en page doit respecter la charte graphique et la règle des arrondis sobres (4px à 6px recommandés, 8px max) définis dans `DESIGN.md`.

## 7. Critères d'acceptation
- Le formulaire de recherche ("Rechercher une ordonnance") ne déborde pas de l'écran sur mobile.
- Le bouton "Vérifier" s'affiche entièrement sans être tronqué.
- Le texte d'explication ne déborde pas et revient à la ligne normalement.
- Le panneau de dispensation (`app-pharmacy-dispensation-panel`) s'adapte sans provoquer de défilement horizontal de la page entière.
- La compilation et les tests de l'application Angular se déroulent sans erreur.

## 8. Évolution Desktop (Amélioration ergonomique du tableau)
Afin d'éviter que le tableau de délivrance des médicaments ne soit trop contracté sur les écrans d'ordinateurs classiques et n'affiche une barre de défilement horizontal inconfortable, l'interface a été restructurée :
- **Disposition verticale** : Le formulaire de dispensation active et l'historique des délivrances enregistrées ne sont plus affichés côte à côte en Grid, mais l'un après l'autre verticalement (Flex-Col).
- **Aération visuelle** : Le tableau de délivrance bénéficie de 100% de la largeur du conteneur de droite, lui offrant un confort de lecture optimal et éliminant tout défilement horizontal sur écran desktop.
- **Largeur minimale fluide** : Le tableau a une largeur minimale redimensionnée à `650px` au lieu de `780px`, assurant une flexibilité idéale sur les écrans plus étroits.

