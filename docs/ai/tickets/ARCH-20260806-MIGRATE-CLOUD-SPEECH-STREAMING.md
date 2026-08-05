# ARCH-20260806 — Migration vers transcription streaming cloud

## 1. Constat critique

L'architecture actuelle utilise `speech_to_text: ^7.3.0`, un plugin Flutter qui s'appuie sur les moteurs de reconnaissance vocale **on-device** natifs Android/iOS.

### Problèmes constatés

1. **Fiabilité catastrophique** : le moteur démarre ou pas, sans raison claire
2. **Qualité médiocre** : vocabulaire médical non reconnu, erreurs fréquentes
3. **Perte d'audio** : chunks perdus entre les restarts
4. **Pas de contrôle** : impossible de déboguer ou optimiser le moteur natif
5. **Expérience incohérente** : comportement différent selon le fabricant Android

### Symptômes utilisateur

- Active le micro → "ça écoute mais ne fait rien"
- Quand ça marche → "ça saute, ça n'écoute que des conneries"
- Impossibilité de dicter du vocabulaire médical complexe
- Perte de confiance dans l'outil

## 2. Architecture cible : Streaming cloud

### Modèle Nabla et applications médicales professionnelles

Toutes les applications de dictée médicale de qualité production utilisent une API cloud :

| Solution | Latence | Qualité | Vocabulaire médical | Prix |
|----------|---------|---------|---------------------|------|
| **OpenAI Whisper API** | 200-500ms | Excellente | Oui (fine-tunable) | $0.006/min |
| **Google Cloud Speech-to-Text** | 100-300ms | Excellente | Oui (modèle médical) | $0.024/min |
| **Azure Speech Services** | 150-400ms | Excellente | Oui | $1/heure |
| **Deepgram** | 50-200ms | Excellente | Oui | $0.0043/min |

**Recommandation : OpenAI Whisper API**
- Meilleur rapport qualité/prix
- Excellente reconnaissance du français médical
- API simple et stable
- Fine-tuning possible pour vocabulaire spécifique

### Architecture proposée

```
┌─────────────┐
│   Flutter   │
│   Mobile    │
└──────┬──────┘
       │ 1. Capture audio chunks (record package)
       │    → PCM 16-bit, 16kHz, mono
       │    → Buffer 2-3 secondes
       │
       ▼
┌─────────────┐
│  Joprelys   │
│   Backend   │
│  (Spring)   │
└──────┬──────┘
       │ 2. Forward audio stream
       │
       ▼
┌─────────────┐
│  OpenAI     │
│  Whisper    │
│  API        │
└──────┬──────┘
       │ 3. Transcription streaming
       │
       ▼
┌─────────────┐
│  Backend    │
│  WebSocket  │
└──────┬──────┘
       │ 4. Transcript chunks
       │
       ▼
┌─────────────┐
│  Flutter    │
│    UI       │
└─────────────┘
```

### Pourquoi passer par le backend ?

1. **Sécurité** : La clé API OpenAI ne doit jamais être dans l'app mobile
2. **Monitoring** : Logs, métriques, quotas centralisés
3. **Fallback** : Possibilité de basculer sur un autre provider sans update app
4. **Cache** : Possibilité de cacher des transcriptions identiques
5. **Vocabulaire custom** : Fine-tuning côté backend sans impacter le mobile

## 3. Plan de migration

### Phase 1 : Backend — Endpoint streaming transcription

**Nouveau service Spring Boot** : `VoiceTranscriptionStreamingService`

```java
@Service
public class VoiceTranscriptionStreamingService {
    private final OpenAiClient openAiClient;
    private final VoiceSessionRepository sessionRepo;
    
    public Flux<TranscriptChunk> transcribeStream(
        String visitId,
        Flux<AudioChunk> audioStream
    ) {
        // 1. Valider session
        // 2. Forward à Whisper API
        // 3. Stream back les résultats
        // 4. Log et métriques
    }
}
```

**Nouveau endpoint WebSocket** : `/api/voice/stream/{visitId}`

- Input : Chunks audio base64 ou binaire
- Output : Chunks de transcription JSON temps réel

**Dépendances Maven** :
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### Phase 2 : Mobile — Capture et streaming audio

**Remplacer `speech_to_text` par `record` + WebSocket**

1. Utiliser `record: ^5.1.2` pour capturer l'audio brut
2. Établir WebSocket vers backend
3. Streamer les chunks audio (buffer 2s)
4. Recevoir et afficher les transcriptions temps réel

**Nouveau service Flutter** : `CloudSpeechStreamingService`

```dart
class CloudSpeechStreamingService extends ValueNotifier<RealtimeSpeechState> {
  final AudioRecorder _recorder = AudioRecorder();
  final WebSocketChannel _wsChannel;
  final String _visitId;
  
  Future<void> startStreaming() async {
    // 1. Démarrer capture audio
    final stream = await _recorder.startStream(
      const RecordConfig(
        encoder: AudioEncoder.pcm16bits,
        sampleRate: 16000,
        numChannels: 1,
      ),
    );
    
    // 2. Buffer et envoyer chunks
    stream.listen((chunk) {
      _wsChannel.sink.add(base64Encode(chunk));
    });
    
    // 3. Recevoir transcriptions
    _wsChannel.stream.listen((message) {
      final chunk = TranscriptChunk.fromJson(message);
      _ingestTranscript(chunk.text, finalResult: chunk.isFinal);
    });
  }
}
```

### Phase 3 : Tests et optimisation

1. **Tester latence** : objectif < 500ms end-to-end
2. **Tester qualité** : vocabulaire médical français
3. **Tester robustesse** : connexion instable, reconnexion auto
4. **Optimiser buffer** : trouver le bon équilibre latence/qualité

## 4. Avantages immédiats

| Avant (on-device) | Après (cloud streaming) |
|-------------------|-------------------------|
| Démarre quand il veut | Démarre instantanément |
| Qualité médiocre | Qualité professionnelle |
| Vocabulaire limité | Vocabulaire médical |
| Perte de chunks | Aucune perte |
| Impossible à déboguer | Logs complets backend |
| Dépend du fabricant | Comportement uniforme |

## 5. Coûts estimés

**OpenAI Whisper API** : $0.006 par minute

- Consultation moyenne : 10 minutes de dictée
- Coût par consultation : $0.06
- 1000 consultations/mois : $60/mois
- **Négligeable comparé au coût d'une app qui ne fonctionne pas**

**Alternative** : Deepgram ($0.0043/min) encore moins cher si budget critique

## 6. Risques et mitigations

| Risque | Impact | Mitigation |
|--------|--------|------------|
| Latence réseau | Délai perceptible | Buffer optimisé, feedback UI |
| Coût cloud | Récurrent | Monitoring quotas, fallback on-device |
| Dépendance externe | Service down | Circuit breaker, retry, fallback |
| Confidentialité | Données médicales cloud | OpenAI Business (HIPAA compliant) |

## 7. Statut

**PROPOSITION — VALIDATION_REQUIRED**

Cette migration est la **seule solution professionnelle** pour corriger définitivement les problèmes de dictée vocale.

Les correctifs `pauseFor: 30s` et mécanismes de restart sont des **pansements sur une jambe de bois** — le moteur on-device Android est intrinsèquement non fiable pour un usage médical professionnel.

## 8. Prochaines étapes

1. **Décision** : Valider l'approche cloud streaming
2. **Choix provider** : OpenAI Whisper (recommandé) ou alternative
3. **POC backend** : Endpoint WebSocket + Whisper API (2j)
4. **POC mobile** : Capture audio + WebSocket (2j)
5. **Tests qualité** : Vocabulaire médical français (1j)
6. **Migration complète** : Remplacer `ClinicalSpeechService` (3j)
7. **Recette** : Tests terrain avec praticiens (1 semaine)

**Estimation totale : 2 semaines**

## 9. Alternative temporaire

Si la migration cloud est refusée, la **seule alternative acceptable** est :

**Utiliser `record` pour capturer l'audio localement, puis POST vers votre backend IA existant par chunks de 10-15 secondes** (mode quasi-temps réel).

Cela évite la dépendance au moteur Android on-device tout en gardant le traitement côté serveur.

## 10. Références

- [Nabla Copilot](https://www.nabla.com) : utilise transcription cloud
- [Nuance Dragon Medical](https://www.nuance.com/healthcare/dragon-medical.html) : cloud streaming
- [OpenAI Whisper API](https://platform.openai.com/docs/guides/speech-to-text)
- [Deepgram Medical](https://deepgram.com/solutions/healthcare)
