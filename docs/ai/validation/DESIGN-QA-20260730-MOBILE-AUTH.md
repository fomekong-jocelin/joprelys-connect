# Design QA — authentification mobile Joprelys Connect

> Le rapport précédent sur la surface d'écoute vocale est archivé dans
> `docs/ai/validation/DESIGN-QA-20260728-VOICE-LISTENING-SURFACE.md`.

## Cible et preuves

- Vérité visuelle initiale :
  - `C:\Users\Jocelin FOMEKONG\AppData\Local\Packages\Microsoft.YourPhone_8wekyb3d8bbwe\TempState\medias\Screenshot_20260730_161828.jpg`
  - `C:\Users\Jocelin FOMEKONG\AppData\Local\Packages\Microsoft.YourPhone_8wekyb3d8bbwe\TempState\medias\Screenshot_20260730_162025.jpg`
  - `DESIGN.md` et les tokens visuels du frontend Angular existant.
- Captures de l'implémentation :
  - `mobile/test/goldens/auth_login_dark.png`
  - `mobile/test/goldens/professional_home_dark.png`
- Comparaisons pleine vue :
  - `mobile/test/goldens/auth_login_dark_comparison.png`
  - `mobile/test/goldens/professional_home_dark_comparison.png`
- Viewport Flutter : `393 × 852` pixels logiques, `devicePixelRatio: 1`.
- Sources téléphone : `945 × 2048`, normalisées à `393 × 852`.
- Captures Flutter : `393 × 852`, densité 1:1.
- État : thème sombre, locale française ; connexion déconnectée puis session
  professionnelle authentifiée avec données de test anonymes.

## Findings

Aucun écart P0, P1 ou P2 ne reste ouvert.

- Typographie : la hiérarchie titre, sous-titre, libellés et actions est lisible,
  sans collision ni troncature structurelle. Les poids restent cohérents avec le
  frontend et le thème Flutter.
- Espacement et rythme : le branding, l'introduction et les préférences sont
  sortis de la carte d'action. Les marges, rayons sobres, bordures et ombres
  suivent les tokens centraux.
- Couleurs et tokens : fond bleu nuit, surfaces, cyan primaire, vert de marque et
  rouge destructif sont tous issus du thème central. Le contraste visuel est
  conservé dans l'état sombre contrôlé.
- Images : le logo officiel est rendu avec son ratio natif ; aucun substitut
  dessiné ou placeholder n'est utilisé.
- Contenu : l'identifiant technique `MOB-2805` a disparu de l'interface. L'accueil
  authentifié présente désormais le compte et la sécurité avec un contenu utile.
- Icônes et interactions : les icônes Material restent homogènes. Les contrôles
  langue, thème, biométrie, visibilité du mot de passe et déconnexion disposent de
  cibles tactiles et de tests de widget.

## Open Questions

- Aucune question bloquante. Le nom et l'adresse visibles dans la golden de
  l'accueil sont des fixtures de test, jamais des données de production.

## Comparison History

1. La source de connexion présentait une hiérarchie compacte dans une seule carte,
   un logo écrasé et des champs visuellement irréguliers. Correction : lockup de
   marque partagé, introduction hors carte, libellés externes et champs homogènes.
   Preuve après correction : `auth_login_dark_comparison.png`.
2. La source authentifiée ressemblait à une page de recette avec le badge
   `MOB-2805`, des groupes de réglages dupliqués et un bouton destructif dominant.
   Correction : accueil professionnel structuré en identité et sécurité,
   préférences compactes persistantes et déconnexion secondaire. Preuve après
   correction : `professional_home_dark_comparison.png`.
3. La première capture golden utilisait des glyphes de substitution pour les
   drapeaux emoji et le libellé des boutons. Correction : sélecteur de langue
   textuel `FR/EN` et style de libellé explicite. Les captures finales ne montrent
   plus ces artefacts.

## Focused Region Comparison

Une découpe supplémentaire n'était pas nécessaire : les deux comparaisons
`786 × 852` conservent à taille lisible le header, les champs, les actions, la
carte d'identité et la carte de sécurité.

## Implementation Checklist

- [x] Aligner la composition mobile sur la hiérarchie visuelle du web.
- [x] Préserver le thème central, les rayons sobres, le light/dark et le fr/en.
- [x] Remplacer l'écran de recette authentifié par un accueil professionnel utile.
- [x] Vérifier les interactions principales avec des tests de widgets.
- [x] Figer les deux états sombres avec des goldens 393 × 852.

## Follow-up Polish

- [P3] Bundler ultérieurement Montserrat et Inter dans Flutter si une parité
  typographique stricte avec le web est requise sur tous les appareils.

final result: passed
