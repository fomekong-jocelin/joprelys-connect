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

## 4. Coordination de la reconnaissance continue

`ClinicalSpeechService` garantit un seul appel asynchrone à `listen()` à la fois.
Un numéro de cycle invalide les résultats tardifs après arrêt explicite. La reprise
sur `done/notListening` est suspendue pendant `_recoverSpeechRecognizer()` afin
que le `cancel()` Android ne déclenche pas un second démarrage concurrent.

La fusion applique successivement :

1. égalité / hypothèse cumulative ;
2. chevauchement exact suffixe → préfixe ;
3. chevauchement récent quasi exact avec au moins quatre tokens et une similarité forte ;
4. nouveau segment uniquement après finalisation réelle.

La comparaison quasi exacte reste limitée à la bordure récente pour ne pas
supprimer une répétition volontaire prononcée plus tard dans la consultation.

## 5. Restauration et source de vérité

- Le brouillon chiffré local est indexé par visite et prioritaire lorsqu'il a du contenu.
- `explicitlyCleared=true` est une barrière de restauration : aucun contenu local
  ou serveur antérieur n'est réinjecté.
- Seul le statut serveur `PENDING_REVIEW` autorise la restauration du transcript.
- `ANALYZED` et `NONE` conservent les résultats structurés de session, mais ne
  préremplissent pas la timeline de capture.
- Le backend mémorise les réponses realtime par `eventId` pour la durée de la
  session et ne rappelle pas le modèle lors d'un retry identique.

## 6. Responsabilités

- Flutter : capture, fusion d'hypothèses, brouillon de revue, état UI.
- Spring Boot : idempotence de l'analyse, validation, règles cliniques et session.
- Le pipeline Ambient durable reste séparé ; ce correctif ne prétend pas livrer
  une file audio chiffrée ou un VAD applicatif.
