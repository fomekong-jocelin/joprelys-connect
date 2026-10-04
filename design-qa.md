# Design QA — entrée en consultation

final result: blocked

Le rapport mobile précédent est conservé dans `docs/ai/validation/DESIGN-QA-20260730-MOBILE-AUTH.md`.

## Cible
- Maquette acceptée par l'utilisateur le 2026-10-04 : `C:/Users/Jocelin FOMEKONG/.codex/generated_images/01a10452-2660-7ab3-835e-6a6316484fb3/exec-0645e321-75bd-458b-ba80-e3c3196858ec.png` (1732 × 908 px, dimensions lues dans le fichier). Image ouverte pour ancrer l'implémentation.
- Capture d'anomalie fournie par l'utilisateur : message de formulaire prêt, champs absents.
- Implémentation : `web/src/app/consultation/consultation-entry-mode.component.*`, page et panneau vocal de consultation. Application locale utilisée dans la session : `http://localhost:4201`.
- Sources de charte : `DESIGN.md`, styles/tokens centraux et icônes UI existantes.

## Blocage de comparaison
L'accès au navigateur a été refusé précédemment dans cette session. Aucun contournement par un autre canal ou une nouvelle session automatisée n'a été utilisé. Il n'existe donc pas de capture de l'implémentation ni de comparaison combinée source/rendu au même viewport. Aucun score de fidélité, aucune validation visuelle responsive/thème/clavier et aucune recette microphone réelle ne sont attestés.

Le skill Product Design `design-qa/SKILL.md` impose : “If either artifact cannot be opened, captured, or compared, write `design-qa.md` with `final result: blocked` and name the blocker.” Ce rapport applique cette règle ; les tests DOM et le build ne la remplacent pas.

## Éléments vérifiés séparément
- Tests DOM de sélection sans capture, CTA par mode, champs présents en manuel pristine et consultation chargée, rapport accepté, reprise sans perte, échec realtime, refus/attente microphone et retour manuel.
- 115 fichiers / 619 tests Angular réussis ; build production et contrôle shell i18n réussis.
- 37 nouvelles clés FR/EN complètes ; 12 tokens visuels existants contrôlés.
- Aucun asset raster requis dans la zone refondue : logo fourni et navigation conservés ; icônes du composant partagé, aucun asset de remplacement créé.

## Comparaison à reprendre
Capturer l'écran initial et le formulaire ouvert en desktop, puis vérifier mobile, FR/EN, light/dark et clavier. Comparer la maquette et la capture ensemble, en normalisant densité/viewport et état patient synthétique. Vérifier les cinq surfaces : typographies, rythme/espacement, couleurs/tokens, qualité des assets existants et libellés. Ajouter une comparaison focalisée sur les choix/CTA et une sur les champs/bandeau. Corriger les écarts P0/P1/P2, recapturer, puis seulement déclarer la QA visuelle passée.
