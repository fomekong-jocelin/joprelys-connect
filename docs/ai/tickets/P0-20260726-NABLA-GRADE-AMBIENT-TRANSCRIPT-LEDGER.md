# P0 — Nabla-grade Ambient Transcript Ledger

## Contexte

Joprelys ne doit plus considérer la reformulation temps réel comme la source de vérité d'une consultation ambient. Le flux Realtime reste utile pour l'interaction immédiate, mais la génération d'une note clinique ambient doit reposer sur un transcript final, ordonné, diarizé et durable.

Cette fondation applique le principe suivant :

> **L'audio et le transcript final sont les preuves. La note clinique est une projection révisable de ces preuves.**

Référence de cadrage : EPIC #173. Le chantier zéro-perte #167 reste une dépendance directe pour la capture locale durable.

## Contrat cible

```text
Audio ambient
   ↓
Transcription diarizée
   ↓
Transcript Ledger append-only
   ├─ sourceEventId idempotent
   ├─ speaker label brut
   ├─ speaker clinique DOCTOR / PATIENT / UNSPECIFIED
   ├─ startOffsetMs / endOffsetMs
   ├─ séquence serveur
   ├─ statut FINAL / REJECTED
   └─ chaîne de corrections supersedesItemId
   ↓
Transcript clinique effectif
   ↓
Note engine transcript-first (phase suivante)
```

## Ce lot livre

### Persistance

Flyway `V96__create_ai_ambient_transcript_ledger.sql` crée `ai_ambient_transcript_items` avec :

- isolation tenant explicite ;
- rattachement à la visite ;
- séquence serveur unique par visite ;
- idempotence par `source_event_id` ;
- timestamps audio absolus ;
- locuteur brut et type clinique ;
- statut final ;
- auteur de l'écriture ;
- correction append-only via `supersedes_item_id` ;
- contraintes CHECK et index dédiés.

### Diarisation OpenAI dédiée

Le flux ambient utilise un adaptateur séparé du Realtime et de la dictée classique :

- modèle par défaut `gpt-4o-transcribe-diarize` ;
- `response_format=diarized_json` ;
- `chunking_strategy=auto` ;
- normalisation de `fr-FR` vers `fr` pour le fournisseur ;
- aucun `logprobs` ni prompt de transcription demandé au modèle diarizé lorsque non supporté ;
- erreurs upstream transformées en erreurs applicatives contrôlées.

### Fail-safe sur le locuteur

Un label fournisseur `A`, `B`, etc. reste `UNSPECIFIED`. Joprelys ne devine pas qu'un locuteur est médecin ou patient.

Seuls des labels explicitement sémantiques (`doctor`, `clinician`, `provider`, `patient`) sont convertis automatiquement. Sinon une attribution humaine est requise.

### Correction humaine append-only

`POST /api/ai/consultations/{visitId}/ambient/transcript/{itemId}/corrections`

permet au clinicien :

- d'attribuer explicitement `DOCTOR` ou `PATIENT` ;
- de corriger le verbatim ;
- de fournir un `correctionId` idempotent.

L'item d'origine reste intact. La correction crée un nouvel item `MANUAL_CORRECTION` qui référence `supersedes_item_id`.

### Deux vues distinctes

- `GET .../ambient/transcript` : transcript **effectif** FINAL, sans versions supersédées ;
- `GET .../ambient/transcript/audit` : historique append-only complet.

### Séquencement concurrent

L'ajout de chunks verrouille la visite (`PESSIMISTIC_WRITE`) pendant l'attribution des séquences. Deux uploads concurrents de la même consultation ne peuvent donc pas créer silencieusement la même séquence métier.

## Renforcement de la dictée classique

Le calcul de confiance OpenAI batch utilise maintenant le **quintile bas** des probabilités token plutôt qu'une moyenne. Un médicament, un dosage, une négation ou un nombre incertain ne peut plus être masqué par une majorité de mots faciles correctement transcrits.

## Invariants de sûreté

1. Un segment fournisseur ambigu n'est jamais attribué implicitement à un médecin.
2. Une correction n'efface jamais la preuve d'origine.
3. Un retry de chunk ne crée pas de doublon.
4. Le transcript clinique est trié par temps audio puis séquence.
5. Une autre organisation ne peut pas lire ou écrire le ledger de la visite.
6. Le futur moteur de note ne devra consommer que la vue FINAL effective.
7. `UNSPECIFIED` ne pourra pas être la seule preuve d'une prescription, d'une décision médicale ou d'une constante critique.

## Hors périmètre de ce lot

- safety recorder navigateur / IndexedDB / reprise offline (#167) ;
- capture ambient continue côté Angular ;
- reconnaissance automatique fiable de l'identité médecin/patient à partir des labels A/B ;
- known-speaker enrollment ;
- génération de note SOAP/APSO/multi-section ;
- provenance phrase de note → transcript item IDs ;
- corpus clinique de benchmark et métriques WER/factuality.

Ces éléments sont suivis dans #173 et doivent être livrés avant de revendiquer une parité fonctionnelle avec un ambient clinical scribe mature.

## Tests ajoutés

- ordre chronologique des segments ;
- séquence serveur ;
- idempotence d'un chunk rejoué ;
- conservation `A/B` en `UNSPECIFIED` ;
- attribution explicite humaine append-only ;
- vue effective vs audit ;
- contrat HTTP OpenAI diarizé ;
- normalisation de locale ;
- absence de logprobs/prompt sur le modèle diarizé ;
- calcul de confiance batch par bas de distribution.
