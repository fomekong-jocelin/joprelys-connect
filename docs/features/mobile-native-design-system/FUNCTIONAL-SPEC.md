# FUNCTIONAL SPEC — MOB-2802 Design system Flutter Joprelys

## Objectif

Donner à l’application mobile native une identité Joprelys cohérente avec le web, réutilisable par toutes les futures features, sans introduire de parcours métier clinique dans ce lot.

## Source de vérité

`DESIGN.md` reste la source de vérité commune web/mobile.

## Principes UX

- identité médicale sobre, crédible et lisible ;
- mobile-first ;
- contrôles standards carrés ou légèrement arrondis ;
- boutons et inputs : rayon 4 à 6 px ;
- cartes : rayon 6 à 8 px maximum ;
- aucune forme de pilule hors avatars/indicateurs explicitement circulaires ;
- bordures fines et ombres légères ;
- action tactile principale : hauteur minimale 44 px ;
- contraste cohérent en light et dark ;
- aucune palette locale dans les features ;
- aucun faux contenu clinique.

## Thèmes

L’application expose trois modes :

- `system` : suit le thème du système ;
- `light` : force le thème clair ;
- `dark` : force le thème sombre.

Le mode est piloté par un contrôleur Riverpod. La persistance durable de la préférence pourra être raccordée à la couche de préférences non sensibles sans modifier l’API publique du contrôleur.

## Tokens attendus

Le design system centralise :

- couleurs sémantiques : primary, onPrimary, background, surface, text, muted, outline, success, warning, error, info ;
- espacements : 4 / 8 / 16 / 24 / 32 / 48 px ;
- rayons : 0 / 2 / 4 / 6 / 8 px ;
- ombre panneau légère ;
- hauteur tactile minimale 44 px ;
- typographie sémantique alignée sur l’intention Montserrat/Inter de `DESIGN.md`, sans chargement réseau de police.

## Widgets partagés

Le lot fournit les primitives suivantes :

- `AppButton` ;
- `AppTextField` ;
- `AppCard` ;
- `AppBadge` ;
- `AppPageHeader` ;
- `AppLoadingState` ;
- `AppEmptyState` ;
- `AppErrorState` ;
- `AppConfirmDialog`.

Ces composants reçoivent leurs libellés/contenus depuis l’appelant afin de ne pas empiéter sur MOB-2803 (i18n FR/EN).

## Page de fondation

La page de fondation peut présenter les primitives du design system et le sélecteur `system/light/dark`, mais ne doit simuler aucun patient, rendez-vous, consultation, constante, prescription ou résultat clinique.

## Accessibilité

- zones tactiles principales >= 44 px ;
- focus et états disabled visibles ;
- couleurs sémantiques utilisables dans les deux thèmes ;
- libellés fournis par l’appelant ;
- composants compatibles avec `Semantics` et lecteurs d’écran.

## Hors périmètre

- traduction FR/EN complète et formats locale ;
- persistance clinique ;
- réseau/API ;
- authentification ;
- audio natif ;
- écrans métier ;
- release/store/déploiement.

## Critères d’acceptation

- [ ] design tokens centralisés ;
- [ ] thèmes light/dark définis dans `AppTheme` ;
- [ ] mode system/light/dark piloté par Riverpod ;
- [ ] `JoprelysApp` consomme uniquement les thèmes centralisés ;
- [ ] widgets partagés créés ;
- [ ] rayons et tailles tactiles conformes ;
- [ ] aucun faux contenu clinique ;
- [ ] tests Flutter light/dark/system et widgets verts ;
- [ ] analyze + build APK debug verts ;
- [ ] documentation et suivi mis à jour.
