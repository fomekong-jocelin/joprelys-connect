# Joprelys AI cost routing — 2026-07-28

## Decision

Joprelys uses a cost-aware model routing policy for the clinical voice assistant.

| Workload | Default model | Rationale |
|---|---|---|
| Classic dictation / uploaded audio | `gpt-4o-mini-transcribe` | Accurate low-cost medical transcription |
| Continuous Realtime transport | `gpt-realtime-2.1-mini` | Lower-cost Realtime tier; server-side automatic responses remain disabled |
| Realtime input transcription | `gpt-4o-mini-transcribe` | Avoid paying the full transcription model on every live turn |
| Normal consultation turns / structured extraction | `gpt-4o-mini` | High-volume fast path with Structured Outputs support |
| TTS playback | `gpt-4o-mini-tts` | Existing low-cost speech playback path |
| Durable ambient recovery / diarization | `gpt-4o-transcribe-diarize` | Safety/recovery path; preserved intentionally |
| Deep final clinical review | `gpt-5.6-terra` | Reserved for an explicit clinician-triggered final review, not the continuous loop |

The deep final review is deliberately not auto-wired in this change. The current consultation workflow continuously builds a clinician-reviewable draft and applies explicit safety guards. Introducing an extra final model call that can rewrite the draft requires a separate contract, acceptance flow and regression suite before it is allowed to alter clinical content.

## Cost-control principles

1. Never use the flagship Realtime model as a passive transcription transport.
2. Keep the continuous loop on the cheapest model that satisfies the workflow.
3. Keep expensive reasoning out of every speech turn.
4. Do not compromise the durable ambient safety path merely to reduce cost.
5. Do not log transcript text, patient data, prompts or model responses in cost telemetry.

## Measurement framework

### Primary KPIs

**AI cost per completed consultation**

- Grain: one consultation/visit.
- Formula: sum of estimated provider cost for transcription, realtime, text generation and TTS calls attached to the visit.
- Decision: detect model-routing regressions and cost spikes.
- Provisional target: normal 60-minute consultation at or below **US$0.80**; investigate sessions above **US$1.25** until production baselines are available.

**Realtime cost share**

- Formula: Realtime estimated cost / total AI estimated cost.
- Decision: detect accidental long-running or expensive Realtime usage.
- Guardrail: investigate if Realtime consistently becomes the dominant cost while assistant output remains low.

**Clinician acceptance rate of AI proposals**

- Formula: accepted proposals / decided proposals.
- Decision: ensure cost reductions do not silently degrade clinical usefulness.
- Segment by model and workflow, never by patient identity in analytics exports.

### Driver metrics

- Realtime connected duration per consultation.
- Number of Realtime reconnects and fallback-model activations.
- Total text tokens per consultation and model.
- Number and bytes of classic transcription requests.
- Low-confidence transcription rejection rate.
- P50/P95 response latency by operation.

### Quality and safety guardrails

- Low-confidence transcript rate must not worsen materially after routing changes.
- Medication safety / factuality guard rejection rates must be monitored by release.
- No reduction in the explicit clinician validation requirement.
- No PHI in `AI_USAGE` telemetry.

## Telemetry introduced in this change

`OpenAiProvider` emits privacy-safe structured log lines:

```text
AI_USAGE provider=openai operation=transcription model=<model> audioBytes=<bytes> locale=<locale>
AI_USAGE provider=openai operation=chat model=<model> totalTokens=<tokens>
AI_USAGE provider=openai operation=structured_chat model=<model> totalTokens=<tokens>
```

Existing Realtime call logs already expose the selected model, compatibility mode and purpose without transcript content. These logs are sufficient for the first operational baseline; a durable per-visit cost ledger can be added after the baseline proves which dimensions are actually needed.

## Current model price references

Pricing must be refreshed from OpenAI before changing hard cost thresholds:

- `gpt-realtime-2.1-mini`: https://developers.openai.com/api/docs/models/gpt-realtime-2.1-mini
- `gpt-4o-mini-transcribe`: https://developers.openai.com/api/docs/models/gpt-4o-mini-transcribe
- `gpt-4o-mini`: https://developers.openai.com/api/docs/models/gpt-4o-mini
- `gpt-5.6-terra`: https://developers.openai.com/api/docs/models/gpt-5.6-terra

As of 2026-07-28, the verified published prices are:

- `gpt-realtime-2.1-mini`: US$10 / 1M audio input tokens, US$20 / 1M audio output tokens; US$0.60 / 1M text input tokens, US$2.40 / 1M text output tokens.
- `gpt-4o-mini-transcribe`: US$1.25 / 1M audio input tokens, US$5 / 1M output tokens.
- `gpt-4o-mini`: US$0.15 / 1M text input tokens, US$0.60 / 1M text output tokens.
- `gpt-5.6-terra`: US$2.50 / 1M text input tokens, US$15 / 1M text output tokens.

## Rollout validation

Before merging to production:

- Run backend AI/OpenAI tests.
- Run voice/realtime frontend tests.
- Validate a French consultation with silence, negations, drug names, numbers and vital signs.
- Confirm Realtime creates sessions with `gpt-realtime-2.1-mini` and input transcription uses `gpt-4o-mini-transcribe`.
- Confirm normal chat calls report `gpt-4o-mini` in `AI_USAGE`.
- Compare transcript confidence and clinician proposal acceptance against the previous release.
