# ARCH-20260730 — Architecture Flutter native Joprelys Connect

GitHub issue : #241

## Mode d’intervention

Architecture + Project Management.

## Contexte

Le dépôt contient déjà un projet Flutter sous `mobile/`, mais il est encore au stade squelette généré : `main.dart` affiche `Flutter Demo`, `pubspec.yaml` ne contient que les dépendances Flutter de base et l’Android `applicationId` reste `com.joprelys.joprelys_mobile`.

Le déclencheur métier est P0 : une consultation vocale ne doit jamais donner au médecin l’illusion que l’enregistrement continue alors que le navigateur mobile a été suspendu. L’application native devient donc le canal cible pour les usages cliniques mobiles nécessitant une capture audio longue et contrôlée.

## Objectif

Définir une architecture mobile de référence avant tout développement fonctionnel significatif, compatible avec :

- Android en priorité, iOS sans impasse architecturale ;
- backend Joprelys existant, maître de la vérité métier ;
- design system Joprelys commun avec le web ;
- thèmes `light`, `dark`, `system` ;
- internationalisation française et anglaise ;
- sécurité des données de santé ;
- capture audio clinique native avec état vérifiable ;
- tests, CI/CD et observabilité dès la fondation.

## Action plan

- [x] Lire la gouvernance IA, PM, Flutter, design system, SOLID et documentation-first.
- [x] Inspecter le squelette Flutter existant.
- [x] Inspecter le contrat actuel de consultation IA côté backend.
- [x] Vérifier les contraintes Android officielles sur le foreground service microphone.
- [x] Créer l’issue #241.
- [x] Définir l’architecture feature-first et les responsabilités.
- [x] Définir thème, i18n, configuration et navigation.
- [x] Définir réseau, session, stockage et stratégie offline contrôlée.
- [x] Définir l’architecture native de capture audio clinique.
- [x] Définir tests, observabilité et CI/CD.
- [x] Créer ADR, spécification fonctionnelle, conception technique et plan de tests.
- [x] Créer le découpage Epic → Stories.
- [ ] Faire relire l’ADR par Tech Lead + référent sécurité/mobile + référent clinique.
- [ ] Après validation, lancer la story de fondation Flutter ; ne pas développer tout l’epic en une seule PR.

## Décisions principales

1. Conserver `mobile/` dans le monorepo existant.
2. Android-first, iOS-ready.
3. Architecture feature-first `domain / data / presentation`.
4. Riverpod pour DI/état de présentation ; les widgets restent fins.
5. `go_router` pour la navigation déclarative et les redirections de session.
6. Dio derrière un client réseau central ; aucun appel HTTP direct depuis les widgets.
7. DTO immuables générés (`freezed` / JSON serialization) ; modèles domaine indépendants de Flutter.
8. `flutter_secure_storage` uniquement pour secrets/tokens ; préférences non sensibles séparées.
9. ARB + `gen_l10n`, français et anglais obligatoires.
10. `ThemeData` light/dark mappé depuis `DESIGN.md`, `system` comme mode de sélection possible.
11. Audio clinique isolé dans un plugin local natif `joprelys_clinical_audio`, avec foreground service microphone Android.
12. Le statut UI `Enregistrement en cours` n’est vrai qu’après accusé de réception du moteur natif.
13. Aucun stockage générique offline du DPU ; seuls les brouillons/segments explicitement approuvés peuvent être conservés localement de façon chiffrée et temporaire.
14. Le backend reste l’unique source de vérité pour permissions, décisions cliniques, statuts métier et validation.

## Critères d’acceptation

- [x] Architecture de dossiers documentée.
- [x] Dépendances entre couches documentées.
- [x] Stratégie light/dark/system documentée.
- [x] Stratégie i18n FR/EN documentée.
- [x] Stratégie auth/session/biométrie documentée.
- [x] Stratégie réseau/erreurs/offline documentée.
- [x] Architecture foreground audio Android documentée.
- [x] Stratégie sécurité/PII documentée.
- [x] Stratégie tests/CI documentée.
- [x] Epic mobile découpé en stories <= 8 SP.
- [ ] Validation humaine de l’ADR.

## Estimation de cette tâche d’architecture

- 3 SP ;
- 1 à 1,5 jour senior mobile/architecture ;
- reviewer : Tech Lead + sécurité/mobile + médecin référent pour le flux audio.

## Risques

- Le backend audio actuel accepte des octets audio mais ne fournit pas encore un contrat explicite de segments ordonnés/idempotents pour une reprise robuste après perte réseau ; ce point doit être traité dans une story dédiée avant le pilote audio natif.
- Le package Android définit encore un identifiant temporaire et une signature release debug ; à corriger avant toute distribution externe.
- La conservation locale temporaire de données cliniques nécessite validation DPO/rétention avant pilote.
- iOS nécessitera sa propre validation des contraintes audio/background ; Android est le premier périmètre de réalisation.

## Tests de ce lot

Documentation/architecture uniquement : pas de `flutter test` requis pour cette PR. La première PR de code devra exécuter au minimum :

```bash
flutter pub get
flutter analyze
flutter test
```

## SemVer

Aucun bump applicatif : cadrage et architecture uniquement.
