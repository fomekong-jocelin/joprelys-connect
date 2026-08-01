# TECHNICAL-DESIGN — Assistant vocal IA de consultation

## 1. Objectif technique

Construire un assistant d’aide à la saisie clinique, désactivé par défaut, qui
transforme un message texte ou audio en brouillon typé. Le système ne prend
aucune décision médicale et n’écrit jamais directement la consultation.

## 2. Stack concernée

- [x] Spring Boot / Maven / YAML
- [x] Angular / Tailwind CSS v4 / proxy relatif
- [ ] Flutter : hors périmètre de la v1
- [ ] Base de données : aucune migration pour les sessions éphémères v1
- [x] CI/CD, sécurité, documentation et observabilité

## 3. Principes d’architecture

```text
Angular mobile-first
  -> API assistant (RBAC + limites + tenant)
  -> AiConsultationUseCase
       -> VisitAccessPort
       -> ConversationSessionPort
       -> AudioTranscriptionPort
       -> ConsultationDraftAssistantPort
       -> ConsultationDraftPolicy

Validation médecin
  -> API de consultation existante
  -> ConsultationService
```

La cible reste la séparation de `AudioTranscriptionPort` et
`ConsultationDraftAssistantPort`. L’incrément de configuration utilise
provisoirement `RoutingAiProvider` pour sélectionner indépendamment le provider
de transcription et celui du brouillon, sans supprimer OpenAI, Gemini ou Claude.
Le remplacement définitif du port combiné `AiProvider` relève de STORY-2501.

## 4. Responsabilités SOLID

| Élément | Responsabilité | Interdit |
|---|---|---|
| `AiConsultationController` | HTTP, validation, délégation | Prompt, parsing ou règle clinique |
| `AiConsultationUseCase` | Orchestration de session | HTTP provider direct |
| `ConsultationDraftPolicy` | Allowlist, tailles, statut brouillon | Persistance JPA |
| `AudioTranscriptionPort` | Contrat audio → texte | Extraction clinique |
| `ConsultationDraftAssistantPort` | Contrat texte → brouillon typé | Sauvegarde consultation |
| Adaptateur provider | Traduction du contrat externe | Décision métier |
| Facade Angular | État UI et appels API | Règle clinique critique |
| Panneau Angular | Présentation et interactions | `HttpClient` direct |

## 5. Flux interactif

1. Le médecin scanne un QR et confirme le patient/visite.
2. Le backend vérifie rôle, permission, tenant et statut `EN_COURS`.
3. Une session éphémère est créée sous feature flag.
4. Chaque audio est borné, transcrit en mémoire puis les octets sont libérés.
5. Seuls le texte et le brouillon courant partent vers l’adaptateur approuvé.
6. Le JSON retourné est parsé strictement, filtré par allowlist et borné.
7. Le médecin voit les modifications proposées et peut dicter une correction.
8. « Appliquer » copie le brouillon dans le formulaire local.
9. La sauvegarde utilise l’API existante et reste une action distincte.

## 6. Modèle de brouillon

```text
symptoms            5000
clinicalExam        5000
diagnosis           5000
conclusion          5000
advice              3000
followUp            1000
```

Un champ inconnu, trop long ou d’un type inattendu est rejeté, jamais tronqué
silencieusement.

## 7. Configuration YAML cible

```yaml
joprelys:
  ai:
    enabled: ${JOPRELYS_AI_ENABLED:false}
    provider: ${AI_PROVIDER:openai}
    speech-provider: ${SPEECH_PROVIDER:openai}
    session-ttl-minutes: ${AI_SESSION_TTL_MINUTES:30}
    max-conversation-turns: ${AI_MAX_CONVERSATION_TURNS:20}
    minimum-transcription-confidence: ${AI_MIN_TRANSCRIPTION_CONFIDENCE:0.35}
    locale: ${AI_LOCALE:fr}
    openai:
      api-key: ${OPENAI_API_KEY:}
      model: ${OPENAI_MODEL:gpt-4.1}
      transcribe-model: ${OPENAI_TRANSCRIBE_MODEL:gpt-4o-mini-transcribe}
      transcribe-vad-threshold: ${OPENAI_TRANSCRIBE_VAD_THRESHOLD:0.8}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
    gemini:
      api-key: ${GEMINI_API_KEY:}
      model: ${GEMINI_MODEL:gemini-2.0-flash}
      base-url: ${GEMINI_BASE_URL:https://generativelanguage.googleapis.com/v1beta}
    claude:
      api-key: ${ANTHROPIC_API_KEY:}
      model: ${CLAUDE_MODEL:claude-haiku-4-5-20251001}
      base-url: ${ANTHROPIC_BASE_URL:https://api.anthropic.com/v1}
```

OpenAI est le choix par défaut pour la transcription et le brouillon clinique.
`AI_PROVIDER` et `SPEECH_PROVIDER` permettent de basculer indépendamment vers
Gemini ou Claude. Les clés restent exclusivement dans l’environnement ou le
vault. L’activation avec une configuration incomplète doit échouer au démarrage.
Une confiance connue sous le seuil est refusée avant analyse. Le seuil VAD plus
élevé limite l'activation sur silence/bruit faible ; il doit être ajusté uniquement
après recette clinique représentative.

## 8. Sécurité et confidentialité

- Deny by default ; rôles `MEDECIN` et `ADMIN_CLINIQUE`, permission `CLINICAL_WRITE`.
- Isolation tenant avant création ou lecture d’une session.
- Feature flag global puis allowlist de cliniques.
- Audio : MIME allowlist, taille bornée, aucun nom de fichier utilisateur.
- Prompt injection : aucun outil disponible au modèle, sortie JSON stricte.
- Pas de nom, DPU, téléphone, e-mail ou date de naissance envoyés au provider.
- Aucun prompt, transcript, brouillon ou réponse clinique dans logs/metrics.
- Rate limiting par utilisateur et tenant.
- Dictée sans TTS. Realtime conversationnel via un canal TTS backend unique,
  sans mute du sender et avec interruption lorsque le médecin reprend la parole.
- Validation DPO/contrat/résidence/rétention obligatoire avant production.

## 9. Sessions et scalabilité

Le store mémoire est limité au pilote mono-instance. Le port `ConversationSessionPort` permet un adaptateur Redis ultérieur. Les sessions expirent et ne constituent pas le dossier médical. La clé logique est `(organizationId, visitId, doctorId)`.

## 10. UI, composant Soft Voice Card et dette préalable

- `DESIGN.md`, thèmes light/dark et i18n FR/EN obligatoires.
- Rayons 4–6 px, 8 px max ; bottom sheet 12 px seulement en partie haute.
- **Isolateur de bloc micro Soft UI (`soft-voice-card`)** :
  - Le bloc micro remplace exclusivement le widget d'enregistrement sans altérer la disposition externe de la page ni des formulaires.
  - Utilisation stricte des variables CSS du thème (`--app-surface`, `--app-border`, `--brand-primary`, `--text-primary`, `--bg-input`).
  - Badge haut-gauche `✦ IA en cours...` / `✦ AI in progress...`, bouton haut-droit `⏹ Arrêter` / `⏹ Stop`.
  - Micro central à halo concentrique et ondes sinusoïdales bleues fluides (*sine wave ribbons*).
  - Consigne centrale **« Écoute en cours... Parlez naturellement »** / **« Listening... Speak naturally »**.
  - Pied de carte conseil **« 💡 Conseil : Vous pouvez dicter vos notes de consultation de façon naturelle. »**.
- **Conteneur de transcription scrollable (stabilité de mise en page anti-layout shift)** :
  - Le flux de transcription en direct s'affiche sous le bloc micro dans une zone scrollable à hauteur fixe/bornée (`max-h-48`, `overflow-y-auto` avec défilement automatique vers le bas).
  - Interdiction absolue d'étirer ou de rétrécir la carte ou l'écran pendant la dictée : la hauteur totale du bloc reste constante (`CLS = 0`).
- `consultation.component.html` (684 lignes) et `dashboard.component.html` (973 lignes) doivent être découpés avant l’intégration UI.

## 11. Observabilité

Mesures autorisées : sessions, durée, résultat, provider, modèle, octets, tokens/coût agrégé et nombre de corrections. Les contenus et identifiants patient sont interdits.

## 12. Tests prévus

| Niveau | Cible |
|---|---|
| Unit | policies, TTL, fusion/correction, parsing strict, clients mockés |
| API | 401/403/404/409/413/415/429/503, tenant A/B |
| Angular | caméra/micro refusés, apply/cancel, FR/EN, light/dark |
| E2E | scan → dictée → correction → application → validation médecin |
| Clinique | cas anonymisés, faux positifs, contradictions, accents/bruit |

## 13. Impact SemVer

MINOR rétrocompatible lorsque l’incrément complet est activable. Aucun bump pour le cadrage seul. Aucune migration DB prévue pour la v1.

## 14. Références vérifiées le 2026-07-17

- Documentation officielle OpenAI Speech-to-text.
- Documentation officielle OpenAI Data controls.
- `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`.
- `DESIGN.md`.

## 15. Historique

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-17 | Codex | Conception initiale sécurisée et séparation STT/extraction |
| 2026-07-17 | ChatGPT | OpenAI par défaut, providers parole/brouillon indépendants, Gemini et Claude conservés |
| 2026-07-28 | Antigravity | Spécification de l'isolation du bloc micro Soft UI et du conteneur de transcription scrollable fixe |
| 2026-07-28 | Codex | Conception d’une surface d’écoute unique pour Dictée/Realtime et Consultation/Constantes |

## 16. Surface d’écoute partagée

### 16.1 Découpage

```text
VoiceListeningSurfaceComponent (présentation pure)
  -> VoiceWaveVisualizerComponent (micro + animation audio)

RealtimeVoiceControllerComponent
  -> état WebRTC Consultation
  -> VoiceListeningSurfaceComponent

AiAssistantInputComponent
  -> état MediaRecorder Consultation
  -> VoiceListeningSurfaceComponent

RealtimeVitalsControllerComponent
  -> état WebRTC Constantes
  -> VoiceListeningSurfaceComponent

SmartVitalsAssistantComponent
  -> état MediaRecorder Constantes
  -> VoiceListeningSurfaceComponent
```

### 16.2 Contrat du composant

La surface partagée reçoit uniquement des données de présentation :

- `active`, `audioLevel`, `statusText` ;
- `badgeText`, `stopText`, `tipText` ;
- `stopDisabled`, `alertText`, `alertTone`.

Elle émet uniquement `stop`. Elle n’injecte aucun service API, ne connaît ni la
visite ni le moteur audio et ne porte aucune règle clinique.

### 16.3 Isolation des moteurs

- Realtime Consultation continue d’utiliser `RealtimeVoiceBridgeService` et le
  pipeline clinique existant.
- Dictée Consultation conserve `ClassicVoiceRecorderService`.
- Realtime Constantes conserve son bridge et ses files durables.
- Dictée Constantes conserve `MediaRecorder` et `AiVitalsApiService`.
- L’intégration ne modifie que la composition des templates et les libellés
  d’état présentés au professionnel.

### 16.4 Stabilité et accessibilité

- La carte d’écoute conserve une hauteur minimale stable.
- Les ondes sont bornées et ne provoquent aucun layout shift.
- L’historique Realtime reste dans un conteneur séparé à hauteur fixe.
- L’action d’arrêt mesure au moins 44 px, possède un libellé accessible et un
  focus visible.
- `prefers-reduced-motion` désactive les pulsations non essentielles.

### 16.5 Impact technique

- Angular uniquement, sans nouvelle dépendance ni Angular Material.
- Tailwind CSS v4 et tokens `DESIGN.md`.
- Aucune API, DB, migration, configuration ou permission impactée.
- PATCH rétrocompatible.

## 17. Pipeline Realtime P0 sans voix automatique ni perte à la fin

### 17.1 Session OpenAI

`OpenAiRealtimeCallService` configure la session avec
`output_modalities=["text"]`, `create_response=false` et aucune sortie
`audio.output`. Les instructions limitent ce modèle au transport de
transcription. Le client ne doit émettre aucun `response.create` et ne doit pas
utiliser `SpeechSynthesis`.

La conversation clinique reste active dans le pipeline Joprelys : le
`assistantMessage` ou la question de clarification validée par le backend est
envoyé une seule fois au service TTS dédié. `ClinicalVoicePlaybackService`
possède l'unique lecteur audio, ne mute jamais le sender et stoppe la lecture
sur reprise de parole (barge-in). Ce service n'est appelé qu'en Realtime.

La transcription Realtime est asynchrone par rapport aux éventuelles réponses
du modèle et ses `logprobs` sont optionnels. Le bridge conserve donc une
confiance absente sous forme `null`; il est interdit de confondre ce cas avec
un transcript vide.

`RealtimeTranscriptHistoryComponent` présente l'historique dans la zone fixe et
donne accès à « Corriger » sur la dernière phrase, quelle que soit la confiance
ASR. La correction remplace immédiatement le texte visible puis
`RealtimeClinicalTurnCoordinator` l'envoie comme un tour conversationnel
explicite (« Je corrige mon dernier énoncé… »). La réponse suit ensuite le même
chemin backend validé, proposition/révision et TTS Realtime que les autres
tours ; aucun traitement métier n'est porté par le composant de présentation.

### 17.2 Ordonnancement

```text
tour ASR non vide
  -> affichage immédiat
  -> intake durable append-only
  -> ACK durable
  -> confiance connue et suffisante ? analyse : revue éditable
  -> proposition/clarification visuelle
  -> décision humaine
  -> fusion sûre dans le formulaire
```

Le backlog signale seulement un retard. Il ne mute jamais le sender. Un état
`durableBlocked` distinct porte le fail-closed de persistance et est le seul
blocage technique autorisé à couper la capture.

### 17.3 Machine de finalisation

Le contrôleur porte un état `finishPending`. Sur l'action utilisateur :

- `finishPending=true` coupe les nouveaux tours ;
- le coordinateur continue à vider les files intake/analyse ;
- une revue de transcript ou une révision en attente maintient le composant
  monté et les contrôles visibles ;
- `endSession` n'est émis que lorsque le pipeline est idle et qu'aucune décision
  humaine n'est en attente ;
- le parent applique alors `applyCurrentDraft()` avant de quitter le Realtime.

`ngOnDestroy()` reste un nettoyage de navigation, jamais le mécanisme normal de
finalisation.

### 17.4 Responsabilités

- `RealtimeVoiceBridgeService` : transport WebRTC et événements ASR uniquement.
- `RealtimeClinicalTurnCoordinator` : durabilité, ordre, déduplication, revue,
  correction conversationnelle et signal d'idle.
- `RealtimeVoiceControllerComponent` : cycle capture/finalisation et
  orchestration de l'historique.
- `RealtimeTranscriptHistoryComponent` : présentation, édition et émission de
  la correction du dernier transcript.
- `VoiceAssistantPanelComponent` : revue humaine, décisions et fusion sûre du
  brouillon.
- `SmartVitalsAssistantComponent` : édition/réanalyse d'une proposition de
  constantes et application explicite.
