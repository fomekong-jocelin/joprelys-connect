# TECHNICAL DESIGN — Fondation mobile native Joprelys Connect

## 1. État initial constaté

Le projet Flutter existe déjà sous `mobile/` mais reste un squelette généré :

- `mobile/lib/main.dart` contient encore l’application compteur `Flutter Demo` ;
- `mobile/pubspec.yaml` ne contient que Flutter, Cupertino Icons et les lints de base ;
- l’Android `applicationId` est encore `com.joprelys.joprelys_mobile` ;
- la configuration release Android utilise encore la signature debug ;
- aucun thème Joprelys, aucune i18n métier, aucun client API, aucun stockage sécurisé et aucune architecture fonctionnelle ne sont présents.

Cette fondation doit être transformée progressivement, sans recréer un second projet Flutter à côté.

## 2. Stack cible

### Application

- Flutter / Dart du projet existant ;
- Android prioritaire ;
- iOS préparé mais non bloquant pour le premier incrément ;
- architecture feature-first et couches `domain / data / presentation`.

### Bibliothèques structurantes retenues

Les versions seront figées dans `pubspec.lock` lors de la story d’implémentation et mises à jour volontairement, jamais automatiquement en bloc.

- `flutter_riverpod` : injection de dépendances et état de présentation ;
- `go_router` : navigation déclarative ;
- `dio` : transport HTTP centralisé, interceptors, annulation et contrôle des timeouts ;
- `freezed` + `json_serializable` : DTO immuables et sérialisation ;
- `flutter_secure_storage` : tokens/secrets locaux ;
- `shared_preferences` : uniquement préférences non sensibles (thème, langue, onboarding) ;
- `flutter_localizations` + `intl` : i18n et formats locaux ;
- `local_auth` : biométrie locale optionnelle.

Aucune librairie ne devient la source de vérité métier : le backend reste maître.

## 3. Architecture globale

```text
┌─────────────────────────────────────────────────────────────┐
│                         Flutter UI                          │
│ Screens / Shared Widgets / Controllers (Riverpod)          │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               v
┌─────────────────────────────────────────────────────────────┐
│                         DOMAIN                              │
│ Entities / Value Objects / Repository Ports / Use Cases    │
│                  Aucune dépendance Flutter                 │
└──────────────────────────────┬──────────────────────────────┘
                               │ abstractions
                               v
┌─────────────────────────────────────────────────────────────┐
│                          DATA                               │
│ DTO / Mappers / Repository impl / Remote + Local sources   │
└──────────────────────┬───────────────────────┬──────────────┘
                       │                       │
                       v                       v
               Joprelys Backend       Platform / Secure Store
                                       Android / iOS plugins
```

### Règle de dépendance

- `presentation` dépend de `domain` ;
- `data` implémente les contrats définis dans `domain` ;
- `domain` ne dépend ni de Flutter, ni de Dio, ni du stockage, ni des plugins ;
- les écrans ne connaissent pas Dio ;
- les widgets ne connaissent pas les DTO API ;
- les décisions métier critiques ne sont jamais calculées uniquement côté mobile.

## 4. Arborescence cible

```text
mobile/
  lib/
    main.dart
    bootstrap.dart

    app/
      app.dart
      router/
        app_router.dart
        route_names.dart
        route_guards.dart

    core/
      config/
        app_config.dart
        app_environment.dart
      theme/
        app_design_tokens.dart
        app_theme.dart
        theme_controller.dart
      l10n/
        localization_extensions.dart
      network/
        api_client.dart
        auth_interceptor.dart
        locale_interceptor.dart
        correlation_interceptor.dart
        api_error_mapper.dart
        network_status.dart
      security/
        secure_session_store.dart
        biometric_gate.dart
        sensitive_data_policy.dart
      storage/
        preferences_store.dart
        clinical_draft_store.dart
      observability/
        app_logger.dart
        telemetry.dart
      errors/
        app_failure.dart
        failure_mapper.dart
      platform/
        lifecycle_service.dart

    shared/
      widgets/
        app_button.dart
        app_text_field.dart
        app_card.dart
        app_badge.dart
        app_page_header.dart
        app_loading_state.dart
        app_empty_state.dart
        app_error_state.dart
        app_confirm_dialog.dart
        patient_context_header.dart
      formatting/
      accessibility/

    features/
      auth/
        domain/
        data/
        presentation/
      dashboard/
        domain/
        data/
        presentation/
      active_queue/
        domain/
        data/
        presentation/
      planning/
        domain/
        data/
        presentation/
      patients/
        domain/
        data/
        presentation/
      patient_record/
        domain/
        data/
        presentation/
      vitals/
        domain/
        data/
        presentation/
      clinical_audio/
        domain/
        data/
        presentation/
      ai_consultation/
        domain/
        data/
        presentation/
      prescriptions/
        domain/
        data/
        presentation/
      laboratory/
        domain/
        data/
        presentation/

    l10n/
      app_fr.arb
      app_en.arb

  packages/
    joprelys_clinical_audio/
      lib/
      android/
      ios/
      test/

  l10n.yaml
```

## 5. Structure d’une feature

Exemple `features/vitals/` :

```text
vitals/
  domain/
    entities/
      vital_measurement.dart
    repositories/
      vitals_repository.dart
    use_cases/
      load_visit_vitals.dart
      save_visit_vitals.dart

  data/
    dto/
      vital_measurement_dto.dart
    mappers/
      vital_measurement_mapper.dart
    data_sources/
      vitals_remote_data_source.dart
    repositories/
      default_vitals_repository.dart

  presentation/
    screens/
      vitals_screen.dart
    widgets/
      vital_input_field.dart
      vital_status_hint.dart
    controllers/
      vitals_controller.dart
    state/
      vitals_state.dart
```

### Limites

- widget : alerte dès ~200–250 lignes, limite dure 500 ;
- méthode : cible <= 25 lignes, limite 40 ;
- préférer plusieurs widgets nommés à des méthodes `buildXxx()` massives ;
- un controller/provider ne devient pas un service métier fourre-tout.

## 6. Bootstrap et configuration

### `main.dart`

Doit rester minimal :

1. initialiser Flutter ;
2. charger `AppConfig` ;
3. initialiser stockage non sensible et sécurisé ;
4. initialiser observabilité sans PII ;
5. lancer un `ProviderScope` ;
6. déléguer à `JoprelysApp`.

### `AppConfig`

Contient les informations publiques centralisées :

- app name ;
- short name ;
- publisher ;
- logo light/dark ;
- locale par défaut ;
- locales supportées ;
- thème par défaut ;
- URLs publiques (support, confidentialité, CGU) ;
- environnement courant ;
- base URL API issue du build.

### Environnements

Minimum :

- `dev` ;
- `recette` ;
- `prod`.

Injection par `--dart-define` ou mécanisme build validé.

Important : `--dart-define` n’est pas un coffre-fort. Aucun secret, token, clé privée ou clé fournisseur IA ne doit y être stocké.

## 7. Identifiants applicatifs

L’identifiant actuel `com.joprelys.joprelys_mobile` est considéré temporaire.

Cible proposée avant toute première distribution externe :

```text
Production : com.joprelys.connect
Recette    : com.joprelys.connect.recette
Dev        : com.joprelys.connect.dev
```

Même stratégie côté iOS bundle identifier.

Le choix définitif doit être validé avant la première publication car l’identifiant de production devient un contrat de distribution difficile à changer après mise en store.

## 8. Design system Flutter

`DESIGN.md` reste la source de vérité commune web/mobile.

### Tokens Dart

`AppDesignTokens` mappe :

- couleurs sémantiques ;
- typography ;
- spacing ;
- radius ;
- elevation/shadows ;
- tailles tactiles ;
- états success/warning/error/info.

Interdits :

- `Color(0x...)` dispersé dans les features ;
- `TextStyle(...)` répété dans les écrans ;
- radius arbitraires ;
- composants standard en forme de pilule.

### Rayons

- inputs : 4 px ;
- boutons : 4–6 px ;
- cards : 6–8 px maximum ;
- avatars et indicateurs circulaires : cercle autorisé ;
- bottom sheets : exception limitée à la partie supérieure.

### Thèmes

`AppTheme` expose :

```text
ThemeData light
ThemeData dark
```

`ThemeController` gère :

```text
system | light | dark
```

La préférence est persistée dans un stockage non sensible.

### Polices

L’intention design reste :

- Montserrat pour titres ;
- Inter pour contenu.

Les assets doivent être embarqués localement après vérification licence/asset. Aucun chargement dynamique de police depuis un réseau tiers dans le parcours clinique.

## 9. Internationalisation

Approche Flutter officielle `gen_l10n` :

```text
mobile/l10n.yaml
mobile/lib/l10n/app_fr.arb
mobile/lib/l10n/app_en.arb
```

Règles :

- aucun texte utilisateur dans les widgets ;
- clés groupées par domaine (`auth.*`, `dashboard.*`, `patients.*`, etc.) ;
- placeholders/pluriels déclarés dans ARB ;
- dates/heures/nombres via locale active ;
- français par défaut si aucune préférence n’existe ;
- `Accept-Language` renseigné par le client réseau ;
- changement de langue sans redémarrage complet de l’app.

## 10. État et injection de dépendances

Riverpod est retenu car le standard de review du dépôt attend déjà des tests `ProviderContainer`.

### Principes

- providers d’infrastructure en racine ;
- repository providers exposent les interfaces domaine ;
- use cases injectés ;
- controllers de présentation par écran/feature ;
- état immutable autant que possible ;
- pas de service locator global caché ;
- pas de singleton mutable métier.

### Flux

```text
Widget
 -> Controller/Notifier
 -> UseCase
 -> Repository interface
 -> Repository implementation
 -> DataSource / API / Platform
```

## 11. Navigation

`go_router` avec routes nommées.

### Zones

```text
/auth
/dashboard
/queue
/planning
/patients
/patients/:patientId
/visits/:visitId/vitals
/visits/:visitId/consultation
/visits/:visitId/prescriptions
/laboratory
/settings
```

### Guards/redirections

Le router peut :

- rediriger vers login si session absente ;
- empêcher l’accès UI à une route sans permission connue ;
- restaurer une destination après re-authentification.

Mais toute autorisation réelle reste vérifiée par le backend. Une route masquée n’est jamais considérée comme sécurité suffisante.

## 12. Client API et contrats

### Client central

`ApiClient` encapsule Dio.

Interceptors :

1. Authorization ;
2. locale (`Accept-Language`) ;
3. correlation ID ;
4. mapping 401/403/409/422/429/5xx ;
5. refresh session contrôlé ;
6. journalisation technique sans corps clinique.

### Timeouts

Centralisés par environnement. Aucun timeout arbitraire par écran.

### Retry

Automatique uniquement pour :

- erreurs réseau temporaires ;
- requêtes sûres/idempotentes ;
- opérations disposant d’une vraie clé d’idempotence côté serveur.

Aucun retry aveugle de création/validation clinique.

### DTO

- `@freezed` pour immutabilité ;
- `json_serializable` ;
- mapping DTO -> domaine explicite ;
- API errors -> `AppFailure` typé.

## 13. Authentification, session et biométrie

### Stockage

`flutter_secure_storage` :

- access token si nécessaire au modèle actuel ;
- refresh token ;
- éventuel secret local biométrique.

`SharedPreferences` interdit pour :

- JWT ;
- refresh token ;
- données patients ;
- transcript ;
- audio ;
- secrets.

### Biométrie

`local_auth` sert à déverrouiller une session locale préalablement établie.

Elle ne :

- crée pas une identité serveur ;
- contourne pas l’expiration ;
- remplace pas le backend ;
- accorde aucune permission métier.

### Frontière de session

À logout / changement de compte / refresh invalide :

- purge des providers sensibles ;
- suppression des caches cliniques non nécessaires ;
- purge des brouillons selon politique ;
- arrêt de toute capture audio ;
- retour auth.

## 14. Stratégie offline

### Position

Online-first, avec résilience ciblée, pas de dossier médical offline complet.

### Local autorisé

- préférences UI ;
- session chiffrée ;
- brouillons explicitement prévus ;
- file de segments audio chiffrés temporairement en attente d’ACK serveur.

### Local interdit par défaut

- liste complète de patients ;
- historique clinique complet ;
- PDF cliniques permanents ;
- cache indéfini ;
- synchronisation bidirectionnelle générique.

Toute nouvelle persistance de PHI doit passer par une revue sécurité/DPO.

## 15. Architecture audio clinique native — P0

### 15.1 Pourquoi un plugin natif local

Le cycle de vie du microphone et du foreground service Android ne doit pas être mélangé à l’écran Consultation.

Créer :

```text
mobile/packages/joprelys_clinical_audio/
```

Ce plugin local porte uniquement la capacité technique de capture.

Il ne connaît :

- ni diagnostic ;
- ni patient métier ;
- ni prescription ;
- ni prompt IA.

### 15.2 Contrat Dart

```text
ClinicalAudioPlatform
  start(config) -> AudioSession
  pause()
  resume()
  stop()
  stream states/events
  stream segment metadata
```

États :

```text
idle
requestingPermission
starting
recording
paused
interrupted
recovering
stopping
finalizing
stopped
failed
```

### 15.3 Règle de vérité UI

La présentation ne passe à `recording` qu’après confirmation native.

Un simple clic sur « Démarrer » ne suffit pas à considérer que le microphone enregistre.

Si le service natif disparaît ou remonte une interruption :

- l’état UI change immédiatement ;
- une interruption est enregistrée dans le journal de session ;
- le médecin doit pouvoir reprendre ou redicter ;
- aucune période non capturée n’est présentée comme enregistrée.

### 15.4 Android foreground service

Le service Android doit :

- être déclaré avec `foregroundServiceType="microphone"` ;
- demander les permissions requises par le niveau Android ciblé ;
- être démarré pendant que l’application est visible et après permission `RECORD_AUDIO` accordée ;
- afficher une notification persistante pendant la capture ;
- continuer à remonter son état au Flutter app ;
- arrêter proprement la capture sur action explicite/logout/erreur fatale.

Pour les cibles Android modernes, la permission foreground microphone spécifique doit être prise en compte selon les exigences du SDK ciblé.

### 15.5 Segmentation et résilience

La capture produit des segments bornés plutôt qu’un unique fichier de plusieurs dizaines de minutes.

Chaque segment possède au minimum :

- session locale ;
- séquence ;
- horodatage monotone ;
- durée ;
- taille ;
- empreinte ;
- état `pending/uploading/acknowledged/failed`.

Les fichiers restent dans le stockage privé de l’application.

Avant pilote clinique, la persistance temporaire doit être chiffrée avec une clé protégée par le Keystore Android / Keychain iOS ou mécanisme plateforme équivalent.

### 15.6 Contrat backend — gap identifié

Le backend expose déjà notamment :

```text
POST /api/ai/consultations/{visitId}/transcriptions/audio
POST /api/ai/consultations/{visitId}/messages/audio
POST /api/ai/consultations/{visitId}/messages/realtime
```

Le contrat `transcriptions/audio` accepte actuellement un bloc binaire audio et un MIME autorisé.

Pour une reprise fiable de segments mobiles, il manque encore un contrat explicitement documenté pour :

- `sessionId` mobile ;
- `segmentSequence` ;
- idempotency key ;
- checksum ;
- ACK persistant ;
- déduplication ;
- ordre/rejeu après reconnexion.

**Décision :** ne pas bricoler ces garanties uniquement dans Flutter. Une story backend dédiée doit définir ce contrat avant le pilote longue durée.

### 15.7 Rétention

Aucune durée de conservation arbitraire n’est fixée dans ce document.

Politique cible :

- suppression locale après ACK serveur et finalisation sûre ;
- récupération des orphelins au prochain démarrage ;
- TTL configurable uniquement après validation DPO/exploitation ;
- aucune sauvegarde dans galerie, dossier Téléchargements ou stockage partagé.

## 16. Consultation IA mobile

L’application réutilise les principes actuels :

- transcript = preuve factuelle ;
- propositions IA = brouillon ;
- médecin décide ;
- pas d’écriture automatique de diagnostic/prescription ;
- correction explicite ;
- aucune perte silencieuse.

Le flux mobile cible :

```text
Native audio service
 -> segments fiables
 -> uploader
 -> backend transcription/session
 -> transcript durable
 -> extraction/propositions backend
 -> review médecin
 -> API consultation existante
```

Le mobile ne doit pas appeler directement un fournisseur IA avec une clé embarquée.

## 17. Données sensibles et logs

### Interdit dans les logs

- nom patient ;
- DPU ;
- téléphone ;
- email ;
- transcript ;
- diagnostic ;
- prescription ;
- audio ;
- token.

### Autorisé

- correlation ID ;
- type d’opération ;
- durée ;
- code de résultat ;
- nombre de segments ;
- taille agrégée ;
- version app ;
- modèle appareil/OS si nécessaire au diagnostic technique.

Les logs release doivent rester minimaux.

## 18. Observabilité

Créer un port `Telemetry` pour ne pas coupler les features à un fournisseur.

Événements techniques envisageables :

- app_start ;
- auth_success/auth_failure sans identité ;
- api_failure par code ;
- audio_started ;
- audio_interrupted ;
- audio_resumed ;
- audio_segment_ack ;
- audio_finalized ;
- crash technique.

Aucun contenu clinique dans les événements.

## 19. Accessibilité et ergonomie

- cible tactile >= 44 px ;
- contraste WCAG AA ;
- labels sémantiques pour lecteurs d’écran ;
- support de text scaling raisonnable ;
- aucun sens porté uniquement par une couleur ;
- états loading/empty/error/disabled standardisés ;
- actions critiques distinctes ;
- mode consultation réduit la charge cognitive et garde l’état audio toujours visible.

## 20. Tests

### Domain

- use cases purs ;
- mapping erreurs ;
- transitions de state machines ;
- aucun I/O réel.

### Data

- repositories avec clients mockés ;
- mapping DTO ;
- refresh 401 ;
- cancellation ;
- erreurs 403/409/422/429/5xx.

### Riverpod

- `ProviderContainer` ;
- overrides des repositories ;
- états success/loading/error.

### Widgets

- interactions ;
- accessibility labels ;
- FR/EN ;
- light/dark ;
- tailles compactes ;
- golden tests du design system partagé.

### Audio Android

- tests unitaires Kotlin du service ;
- tests du bridge Flutter/native ;
- tests instrumentation sur appareil/émulateur compatible ;
- permission refusée ;
- écran éteint ;
- app background ;
- interruption audio ;
- perte réseau ;
- process/app kill ;
- reprise et nettoyage.

## 21. CI/CD mobile

À ajouter dans une story dédiée :

### Gate PR mobile

```bash
flutter pub get
dart format --output=none --set-exit-if-changed .
flutter analyze
flutter test
```

Puis selon l’incrément :

```bash
flutter build apk --debug
```

Avant release :

- build signé ;
- obfuscation et `split-debug-info` ;
- symboles conservés côté CI sécurisé ;
- scan dépendances ;
- secrets uniquement GitHub/secret store ;
- aucun keystore versionné.

### Trigger

Le pipeline doit détecter les changements dans :

```text
mobile/**
```

et ne pas lancer inutilement les builds Flutter sur une PR pure backend/web.

## 22. Sécurité release

Avant premier pilote externe :

- applicationId/bundleId définitif ;
- signing release propre ;
- secure storage validé ;
- aucun secret dans APK/IPA ;
- TLS strict ;
- stratégie de certificate pinning revue et testée avant activation ;
- logs release assainis ;
- R8/obfuscation Android ;
- root/jailbreak : décision risque/UX documentée avant blocage éventuel ;
- politique de rétention audio/brouillons validée.

## 23. Dépendances inter-features

```text
Foundation
  ├─ Auth/Session
  ├─ Theme/i18n/shared UI
  ├─ Network/errors
  └─ Router
       ├─ Dashboard/Queue
       ├─ Planning
       ├─ Patients/Patient Record
       ├─ Vitals
       ├─ Prescriptions/Lab
       └─ Clinical Audio
             └─ AI Consultation
```

La consultation audio ne doit pas être démarrée avant la fondation session/network et le plugin audio P0.

## 24. Références

Internes :

- `AGENTS.md` ;
- `SKILL.md` ;
- `PROJECT-MANAGER-SKILL.md` ;
- `DESIGN.md` ;
- `docs/standards/FRONTEND-MOBILE-STANDARDS.md` ;
- `docs/standards/DESIGN-SYSTEM-STANDARDS.md` ;
- `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md` ;
- `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md` ;
- `docs/features/ai-voice-consultation/TECHNICAL-DESIGN.md`.

Officielles vérifiées lors du cadrage :

- Flutter — Guide to app architecture : `https://docs.flutter.dev/app-architecture/guide` ;
- Flutter — Internationalization : `https://docs.flutter.dev/ui/internationalization` ;
- Android — Foreground service types / microphone : `https://developer.android.com/develop/background-work/services/fgs/service-types` ;
- Android — Declare foreground services : `https://developer.android.com/develop/background-work/services/fgs/declare`.

## 25. Impact SemVer

Architecture et documentation uniquement : aucun bump applicatif.

La première fondation mobile fonctionnelle sera une capacité nouvelle et sera évaluée comme incrément MINOR du produit lorsqu’elle deviendra livrable.
