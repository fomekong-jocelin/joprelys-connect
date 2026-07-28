# Joprelys AI cost routing — 2026-07-28

## Decision

Joprelys uses a cost-aware model routing policy for the clinical voice assistant.

| Workload | Default model | Rationale |
|---|---|---|
| Classic dictation / uploaded audio | `gpt-4o-mini-transcribe` | Accurate low-cost medical transcription |
| Continuous Realtime transport | `gpt-realtime-2.1-mini` | Lower-cost Realtime tier; server-side automatic responses remain disabled |
| Realtime input transcription | `gpt-4o-mini-transcribe` | Avoid paying the full transcription model on every live turn |
| Normal consultation turns / structured extraction | `gpt-4o-mini` | High-volume fast path with Structured Outputs support |
| TTS playback | `tts-1` | Supported low-latency speech model; replaces deprecated `gpt-4o-mini-tts` |
| Durable ambient recovery / diarization | `gpt-4o-transcribe-diarize` | Safety/recovery path; preserved intentionally |
| Deep final clinical review | `gpt-5.6-terra` | Explicit clinician-triggered second pass only; never part of the continuous loop |

The final review is wired as a governed, optional second pass. It receives only the accepted draft, uses a strict Structured Output contract, passes proposed changes through the existing factuality/grounding/medication guards, and returns proposals only. It never writes clinical data directly. The clinician must explicitly accept proposals before the frontend emits an accepted patch.

Final-review changes are restricted twice: the provider schema only permits narrative clinical fields, and the server independently filters proposals to those same narrative fields. Prescription, laboratory orders and vital signs are read-only context for this final review.

A final review becomes stale if the underlying accepted draft changes before the clinician decides. Stale reviews are rejected rather than being applied to a newer consultation state. The action is also disabled while Realtime listening or classic dictation capture is active. Accepted final-review patches pass through the existing frontend safe-merge logic so a more recent clinician edit always wins.

## Cost-control principles

1. Never use the flagship Realtime model as a passive transcription transport.
2. Keep the continuous loop on the cheapest model that satisfies the workflow.
3. Keep expensive reasoning out of every speech turn.
4. Run `gpt-5.6-terra` only when the clinician explicitly asks for the final review.
5. Do not compromise the durable ambient safety path merely to reduce cost.
6. Do not log transcript text, patient data, prompts or model responses in cost telemetry.
7. Never silently escalate a Realtime fallback to a more expensive or deprecated model.
8. Do not keep a deprecated speech-generation model in the production defaults.

## Measurement framework

### Primary KPIs

**AI cost per completed consultation**

- Grain: one consultation/visit.
- Formula: sum of estimated provider cost for transcription, realtime, text generation, final review and TTS calls attached to the visit.
- Decision: detect model-routing regressions and cost spikes.
- Provisional target: normal 60-minute consultation at or below **US$0.80**; investigate sessions above **US$1.25** until production baselines are available.

**Realtime cost share**

- Formula: Realtime estimated cost / total AI estimated cost.
- Decision: detect accidental long-running or expensive Realtime usage.
- Guardrail: investigate if Realtime consistently becomes the dominant cost while assistant output remains low.

**Clinician acceptance rate of AI proposals**

- Formula: accepted proposals / decided proposals.
- Decision: ensure cost reductions do not silently degrade clinical usefulness.
- Segment normal-turn proposals and final-review proposals separately.
- Never segment by patient identity in analytics exports.

### Driver metrics

- Realtime connected duration per consultation.
- Number of Realtime reconnects and fallback attempts.
- Total text tokens per consultation and model.
- Final-review invocation rate and total tokens per review.
- Number and bytes of classic transcription requests.
- TTS request count and input characters.
- Low-confidence transcription rejection rate.
- P50/P95 response latency by operation.

### Quality and safety guardrails

- Low-confidence transcript rate must not worsen materially after routing changes.
- Medication safety / factuality guard rejection rates must be monitored by release.
- Final-review hallucinated facts, unsupported diagnosis and medication substitutions must remain fail-closed.
- Final review must never propose prescription, lab-order or vital-sign mutations.
- Stale final-review decisions must be rejected.
- More recent clinician edits must win over final-review patches.
- No reduction in the explicit clinician validation requirement.
- No PHI in `AI_USAGE` telemetry.

## Telemetry introduced in this change

Privacy-safe structured log lines:

```text
AI_USAGE provider=openai operation=transcription model=<model> audioBytes=<bytes> locale=<locale>
AI_USAGE provider=openai operation=chat model=<model> totalTokens=<tokens>
AI_USAGE provider=openai operation=structured_chat model=<model> totalTokens=<tokens>
AI_USAGE provider=openai operation=final_review model=<model> totalTokens=<tokens>
AI_USAGE provider=openai operation=tts model=<model> inputChars=<chars> outputBytes=<bytes>
```

No prompt, response, transcript, medication, diagnosis, patient identifier or other clinical content is written to these usage logs.

Existing Realtime call logs expose the selected model, compatibility mode and purpose without transcript content. These logs are sufficient for the first operational baseline; a durable per-visit cost ledger can be added after the baseline proves which dimensions are actually needed.

## Current model price references

Pricing must be refreshed from OpenAI before changing hard cost thresholds:

- `gpt-realtime-2.1-mini`: https://developers.openai.com/api/docs/models/gpt-realtime-2.1-mini
- `gpt-4o-mini-transcribe`: https://developers.openai.com/api/docs/models/gpt-4o-mini-transcribe
- `gpt-4o-mini`: https://developers.openai.com/api/docs/models/gpt-4o-mini
- `gpt-5.6-terra`: https://developers.openai.com/api/docs/models/gpt-5.6-terra
- `tts-1`: https://developers.openai.com/api/docs/models/tts-1

As of 2026-07-28, the verified published prices are:

- `gpt-realtime-2.1-mini`: US$10 / 1M audio input tokens, US$20 / 1M audio output tokens; US$0.60 / 1M text input tokens, US$2.40 / 1M text output tokens.
- `gpt-4o-mini-transcribe`: US$1.25 / 1M audio input tokens, US$5 / 1M output tokens.
- `gpt-4o-mini`: US$0.15 / 1M text input tokens, US$0.60 / 1M text output tokens.
- `gpt-5.6-terra`: US$2.50 / 1M text input tokens, US$15 / 1M text output tokens.
- `tts-1`: US$15 / 1M input characters for speech generation.

## Final-review safety contract

The deep final review must satisfy all of these conditions:

- It is explicitly initiated by the clinician.
- It runs only on an accepted, stable draft.
- It proposes changes only to narrative clinical fields.
- Prescription, laboratory orders and vital signs are immutable in this review path.
- It cannot introduce a new diagnosis, medication, dose, route, frequency, duration, number, unit, laterality or negation.
- Evidence must quote the accepted draft exactly.
- Only `SET` proposals are accepted from Terra; no autonomous delete/clear operation is allowed.
- Every output passes deterministic factuality, grounding and medication-safety guards.
- A draft change after review creation invalidates the review.
- Accept/reject decisions are explicit.
- Accepted output is passed through the existing safe merge and then emitted to the clinician-controlled form flow, never written directly to clinical persistence.

## Regression coverage added

`FinalClinicalReviewServiceTest` covers at minimum:

- explicit accept before an accepted patch exists;
- preservation of the number `39` and the negation `sans` in a safe narrative reordering;
- rejection of stale reviews after the draft changes;
- rejection of an unsupported invented diagnosis;
- rejection of medication substitution;
- immutability of vital signs and laboratory orders during the final review.

`OpenAiRealtimeCallServiceTest` now asserts the low-cost Realtime model and `gpt-4o-mini-transcribe` live transcription configuration while preserving VAD and silent-response safety behavior.

## CI gate

The repository intentionally skips the heavy PR jobs while the PR is Draft. Once the final HEAD is ready, changing the PR to Ready triggers the complete change-detection gate and then Maven/Angular validation. Any later code commit makes that gate stale and requires Draft -> Ready again.

## Rollout validation

The PR gate must complete successfully before merge. Production smoke validation should then confirm the selected provider models, voice output and medical transcription behavior without bypassing clinician validation.
