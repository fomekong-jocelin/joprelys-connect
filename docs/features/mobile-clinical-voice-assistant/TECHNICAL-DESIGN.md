# Design Technique — Assistant Vocal Clinique & Extraction Intelligente (MOB-2816)

## 1. Architecture

```text
[ ClinicalVoiceAssistantSheet ] ──(Dictée brute)──> [ ClinicalDictationParser ]
                                                             │
                                                     (Extraction Regex/NLP)
                                                             │
                                                  ┌──────────┴──────────┐
                                                  ▼                     ▼
                                           [ PatientVitals ]   [ ConsultationNote ]
```

## 2. Extraction des Constantes Médicales (`ClinicalDictationParser`)

Le parser autonome `ClinicalDictationParser` analyse les motifs médicaux courants :
- **Température** : Regex `(?:température|temp|t°?)\s*:?\s*(\d{2}(?:[\.,]\d)?)` -> °C
- **Tension Arterielle** : Regex `(?:tension|ta|bp)\s*:?\s*(\d{2,3})[\s\/]+(\d{2,3})` -> Systolique / Diastolique
- **Pouls / Fréquence cardiaque** : Regex `(?:pouls|fc|pulse|bpm)\s*:?\s*(\d{2,3})` -> bpm
- **Saturation SpO2** : Regex `(?:spo2|sat|saturation)\s*:?\s*(\d{2,3})` -> %
- **Poids** : Regex `(?:poids|weight)\s*:?\s*(\d{2,3}(?:[\.,]\d)?)` -> kg
- **Taille** : Regex `(?:taille|height)\s*:?\s*(\d{2,3})` -> cm
- **Glycémie** : Regex `(?:glycémie|glycemie|dextro)\s*:?\s*(\d+(?:[\.,]\d+)?)` -> g/L
- **Fréquence respiratoire** : Regex `(?:fréquence respiratoire|freq resp|fr)\s*:?\s*(\d{1,2})` -> c/min
- **Douleur EVA** : Regex `(?:douleur|eva)\s*:?\s*(\d{1,2})` -> 0-10

## 3. Composants UI & Intégration

- Modale dédiée `ClinicalVoiceAssistantSheet` avec enregistreur / zone de dictée.
- Bouton de bascule `[🎙️ Assistant vocal]` intégré dans `PatientVitalsSheet` et `ConsultationNotesSheet`.
- Provider Riverpod `clinicalDictationParserProvider`.
