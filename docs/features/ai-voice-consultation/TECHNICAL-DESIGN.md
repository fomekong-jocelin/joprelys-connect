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

Le port initial `AiProvider` combinant STT et chat doit être remplacé. Cette
séparation applique ISP et permet un moteur STT distinct du moteur d’extraction
sans présenter cela comme un fournisseur unique.

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
  web:
    base-url: ${JOPRELYS_WEB_BASE_URL:https://joprelys.com}
  ai:
    enabled: ${JOPRELYS_AI_ENABLED:false}
    provider: ${JOPRELYS_AI_PROVIDER:disabled}
    session-ttl: ${JOPRELYS_AI_SESSION_TTL:PT30M}
    max-conversation-turns: ${JOPRELYS_AI_MAX_TURNS:20}
    max-audio-bytes: ${JOPRELYS_AI_MAX_AUDIO_BYTES:10485760}
```

Les clés restent exclusivement dans l’environnement/vault. L’activation sans
configuration valide doit échouer au démarrage.

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

Le store mémoire est limité au pilote mono-instance. Le port
`ConversationSessionPort` permet un adaptateur Redis ultérieur. Les sessions
expirent et ne constituent pas le dossier médical. La clé logique est
`(organizationId, visitId, doctorId)`.

## 10. UI et dette préalable

- `DESIGN.md`, thèmes light/dark et i18n FR/EN obligatoires.
- Rayons 4–6 px, 8 px max ; bottom sheet 12 px seulement en partie haute.
- Scanner QR, panneau vocal et liste de changements sont partagés.
- `consultation.component.html` (684 lignes) et `dashboard.component.html`
  (973 lignes) doivent être découpés avant l’intégration UI.

## 11. Observabilité

Mesures autorisées : sessions, durée, résultat, provider, modèle, octets,
tokens/coût agrégé et nombre de corrections. Les contenus et identifiants
patient sont interdits.

## 12. Tests prévus

| Niveau | Cible |
|---|---|
| Unit | policies, TTL, fusion/correction, parsing strict, clients mockés |
| API | 401/403/404/409/413/415/429/503, tenant A/B |
| Angular | caméra/micro refusés, apply/cancel, FR/EN, light/dark |
| E2E | scan → dictée → correction → application → validation médecin |
| Clinique | cas anonymisés, faux positifs, contradictions, accents/bruit |

## 13. Impact SemVer

MINOR rétrocompatible lorsque l’incrément complet est activable. Aucun bump pour
le cadrage seul. Aucune migration DB prévue pour la v1.

## 14. Références vérifiées le 2026-07-17

- Documentation officielle OpenAI Speech-to-text.
- Documentation officielle OpenAI Data controls.
- `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`.
- `DESIGN.md`.

## 15. Historique

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-17 | Codex | Conception initiale sécurisée et séparation STT/extraction |
