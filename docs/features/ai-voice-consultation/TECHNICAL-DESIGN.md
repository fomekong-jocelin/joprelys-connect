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
suspectedDiagnosis  5000
diagnosis           5000
finalDiagnosis      5000
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
- TTS désactivé par défaut pour éviter une divulgation sonore.
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
