# ADR-0004 — Architecture mobile native Flutter Joprelys Connect

## Statut

Proposé — à valider dans la review de l’issue #241.

## Date

2026-07-30

## Contexte

Joprelys Connect possède déjà un frontend Angular mobile-first et un squelette Flutter sous `mobile/`.

Le frontend web suffit pour de nombreux parcours, mais un risque clinique P0 a été identifié pour la consultation vocale : lorsqu’un navigateur mobile est suspendu ou que le téléphone se verrouille, la capture audio ne peut pas être considérée comme fiable alors que le médecin peut continuer à parler.

Le projet doit donc disposer d’une application mobile native, sans dupliquer le backend ni créer une seconde source de vérité métier.

Contraintes :

- Android prioritaire ;
- iOS doit rester possible ;
- FR/EN obligatoire ;
- light/dark obligatoire ;
- design system commun avec le web ;
- sécurité forte des données de santé ;
- architecture testable ;
- audio clinique natif découplé des écrans ;
- aucune clé fournisseur IA embarquée dans l’application.

## Décision

### 1. Conserver le projet `mobile/` existant

Aucun second dépôt ou second squelette Flutter ne sera créé.

Le squelette généré sera transformé incrémentalement afin de conserver l’historique et la gouvernance du monorepo.

### 2. Architecture feature-first à trois couches

Chaque feature utilise :

```text
domain/
data/
presentation/
```

- `domain` : entities, repository interfaces, use cases ;
- `data` : DTO, mappers, data sources, repository implementations ;
- `presentation` : screens, widgets, controllers/providers, state.

`domain` ne dépend pas de Flutter.

### 3. Backend maître de la vérité métier

Flutter ne porte pas seul :

- autorisation ;
- statut clinique ;
- décision médicale ;
- validation finale ;
- règle de facturation ;
- persistance du dossier.

La validation UI locale reste une aide ergonomique.

### 4. Riverpod pour état/DI

Riverpod est retenu pour :

- providers testables ;
- overrides en tests ;
- état scoped ;
- absence de service locator global ;
- cohérence avec la checklist du dépôt qui exige les tests `ProviderContainer`.

### 5. `go_router` pour navigation

Navigation déclarative avec :

- routes nommées ;
- redirection session ;
- deep links ;
- guards UX.

Les guards n’ont jamais valeur d’autorisation serveur.

### 6. Dio derrière un client réseau central

Aucun widget n’appelle directement le réseau.

Le client centralise :

- Authorization ;
- locale ;
- correlation ID ;
- timeouts ;
- refresh session ;
- mapping d’erreurs ;
- retry uniquement sûr/idempotent.

### 7. DTO immuables

DTO et états structurés utilisent génération immuable (`freezed` / sérialisation JSON) lorsque pertinente.

Les modèles domaine restent indépendants des DTO API.

### 8. Stockage sensible sécurisé

- tokens/secrets : secure storage ;
- thème/langue : préférences non sensibles ;
- aucune donnée clinique dans `SharedPreferences` ;
- aucune réplication locale générale du DPU.

### 9. Design system commun

`DESIGN.md` reste la source de vérité.

Flutter mappe les tokens vers `ThemeData`/tokens Dart centralisés.

Modes :

```text
system | light | dark
```

### 10. i18n Flutter officielle

ARB + `gen_l10n`, minimum :

```text
fr
en
```

Aucun texte visible en dur dans les widgets.

### 11. Audio clinique dans un plugin natif local

Créer un package local :

```text
mobile/packages/joprelys_clinical_audio/
```

Ce package encapsule les détails Android/iOS de capture.

Sous Android, la capture longue utilise un foreground service de type `microphone`, démarré alors que l’application est visible et après accord de `RECORD_AUDIO`.

Le service émet un état natif vérifiable. La UI ne peut afficher `recording` qu’après confirmation du service.

### 12. Segments audio bornés et reprise

La capture longue doit produire des segments temporaires avec séquence et état d’upload.

Le stockage reste privé à l’application et devra être chiffré avant pilote clinique.

Un ACK serveur est requis avant suppression locale sûre.

### 13. Évolution backend requise pour la robustesse audio

Le contrat actuel d’upload audio ne documente pas encore l’idempotence et l’ordre de segments nécessaires à une reprise fiable.

Une story backend séparée devra définir :

- session de capture ;
- séquence ;
- idempotency key ;
- checksum ;
- ACK ;
- déduplication ;
- rejeu ordonné.

Cette règle ne sera pas simulée uniquement côté mobile.

### 14. Online-first

Pas de mode DPU offline complet dans la première architecture.

La résilience locale est ciblée : préférences, session sécurisée, brouillons approuvés et segments audio temporaires.

### 15. Android-first / iOS-ready

Le premier incrément plateforme implémente Android.

L’interface Dart du plugin audio reste indépendante de la plateforme afin d’ajouter une implémentation iOS ultérieure sans refonte des features.

## Raisons

- réduit le risque de perte silencieuse audio ;
- respecte SOLID et la séparation des responsabilités ;
- permet de tester le domaine sans Flutter ;
- conserve les règles métier côté backend ;
- évite un cache clinique local incontrôlé ;
- permet une UI cohérente web/mobile ;
- garde Android natif accessible pour les contraintes système critiques ;
- prépare iOS sans bloquer l’urgence Android.

## Conséquences positives

- architecture maintenable par plusieurs développeurs ;
- sécurité et i18n intégrées dès la fondation ;
- meilleure testabilité ;
- audio natif isolé du domaine clinique ;
- possibilité de développer les features en parallèle après la fondation ;
- cohérence design avec Joprelys Connect web.

## Conséquences négatives / coûts

- plus de structure initiale qu’un prototype Flutter simple ;
- codegen (`freezed`/JSON/i18n) à intégrer à la CI ;
- développement natif Kotlin nécessaire pour l’audio P0 ;
- évolution backend probable pour upload segmenté robuste ;
- recette sur appareils physiques obligatoire ;
- iOS audio nécessitera un incrément dédié.

## Alternatives rejetées

| Alternative | Raison du rejet |
|---|---|
| Continuer uniquement avec le web/PWA | Ne fournit pas la garantie attendue pour la capture clinique lorsque le navigateur est suspendu. |
| Mettre toute la logique dans Flutter sans couches | Régression de maintenabilité/testabilité et violation des standards SOLID du dépôt. |
| Utiliser un unique service Flutter « god object » | Mélange réseau, session, audio, UI et métier ; non testable et trop risqué. |
| Stocker tout le DPU en local | Surface de fuite PHI trop importante et conflits de synchronisation non justifiés pour la v1. |
| Appeler directement OpenAI/Gemini/Claude depuis le mobile | Exposerait des secrets et contournerait contrôles backend, RBAC, audit et politiques cliniques. |
| Dépendre uniquement d’un plugin audio générique sans adapter natif | Ne donne pas assez de contrôle architectural sur le foreground service, les interruptions, l’état réel et la reprise P0. |

## Impact planning

Cette ADR ouvre l’EPIC-0028.

Le chantier complet est multi-sprint. L’implémentation doit commencer par une story de fondation, puis les features indépendantes, et traiter l’audio clinique comme lot P0 dédié.

## Impact version

Aucun bump pour cette ADR.

La première capacité mobile livrable sera évaluée en MINOR.

## Références

- `DESIGN.md`
- `docs/standards/FRONTEND-MOBILE-STANDARDS.md`
- `docs/standards/DESIGN-SYSTEM-STANDARDS.md`
- `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`
- Flutter app architecture : https://docs.flutter.dev/app-architecture/guide
- Flutter internationalization : https://docs.flutter.dev/ui/internationalization
- Android foreground service microphone : https://developer.android.com/develop/background-work/services/fgs/service-types
- Android foreground service declarations : https://developer.android.com/develop/background-work/services/fgs/declare
