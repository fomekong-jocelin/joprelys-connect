# FUNCTIONAL SPEC — MOB-2803 Internationalisation mobile

## Objectif

Permettre à Joprelys Connect mobile de fonctionner nativement en français et en anglais avant l’introduction des écrans métier, avec une langue initiale cohérente, une bascule immédiate et des formats locaux fiables.

## Langues supportées

- Français (`fr`) ;
- Anglais (`en`).

Le français est la langue de repli du produit mobile.

## Comportement au démarrage

1. Lire la langue du système.
2. Si la langue est française, utiliser `fr`.
3. Si elle est anglaise, utiliser `en`.
4. Pour toute autre langue, utiliser `fr`.

## Changement de langue

La langue peut être modifiée en cours d’exécution sans redémarrer l’application. Les widgets consommant `AppLocalizations` doivent être reconstruits automatiquement par Flutter.

La persistance durable de la préférence n’est pas dans ce lot. Elle sera raccordée à une future couche de préférences non sensibles sans stocker de donnée clinique ni de secret.

## Formats locaux

Le produit doit fournir des helpers cohérents pour :

- date ;
- heure ;
- date + heure ;
- nombre décimal.

Les valeurs métier restent des valeurs typées ; seule leur présentation dépend de la locale.

## Organisation des traductions

Les chaînes sont définies dans des fichiers ARB et générées par l’outillage Flutter officiel. Les futures features doivent ajouter leurs clés de traduction au mécanisme de localisation, sans recréer un service HTTP/dictionnaire parallèle.

## Frontière avec le thème

L’i18n et le thème sont deux préoccupations distinctes.

MOB-2803 ne modifie pas :

- `AppTheme` ;
- `AppDesignTokens` ;
- les choix visuels métier des futures features.

La centralisation visuelle reste volontairement limitée, comme sur le web : le thème fournit les bases transversales, les features gardent leur composition et leurs décisions UX locales.

## Accessibilité

- les libellés visibles suivent la locale active ;
- le changement de langue ne doit pas casser les zones tactiles ;
- aucune chaîne brute de clé de traduction ne doit être affichée sur la page de fondation ;
- l’application garde les composants et contrastes du design system existant sans les redéfinir.

## Hors périmètre

- traduction de tous les futurs écrans métier non encore développés ;
- persistance durable de la préférence de langue ;
- traduction venant du backend ;
- formats monétaires métier ;
- modification du design system ;
- réseau/auth/audio natif ;
- publication store.

## Critères d’acceptation

- [ ] ARB FR/EN générés correctement ;
- [ ] locale initiale résolue depuis le système ;
- [ ] fallback français ;
- [ ] bascule FR/EN immédiate ;
- [ ] formats date/heure/nombre cohérents ;
- [ ] page de fondation intégralement localisée ;
- [ ] aucun élargissement du thème ;
- [ ] tests et APK debug verts.
