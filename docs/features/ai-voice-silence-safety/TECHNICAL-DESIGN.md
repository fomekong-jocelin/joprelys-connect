# Sécurité silence et repli vocal IA — Conception technique

## Architecture concernée

- Angular : pont WebRTC, contrôleur Realtime, panneau vocal classique et i18n ;
- Spring Boot : fournisseur OpenAI de transcription, validation de confiance ;
- aucune migration de base de données ;
- aucun changement d'autorisation.

## Décisions

### Cycle de vie Realtime

Le délai du data channel est créé seulement après réception et application de la
réponse SDP. Un échec HTTP ne peut donc plus laisser une promesse rejetée plus tard.

Le contrôleur conserve l'identifiant de la visite pour laquelle une tentative a déjà
été faite. Une nouvelle tentative nécessite un changement de visite ou la
désactivation/réactivation explicite du mode conversationnel.

Les erreurs Realtime attendues conservent leur statut et leur code fonctionnel. Une
exception inattendue limitée aux contrôleurs Realtime est transformée en
`503 AI_REALTIME_UNAVAILABLE` sans exposer son message, au lieu d'un `500` générique.

### Repli classique

La synthèse vocale ne déclenche plus `startRecording`. Le bouton microphone reste
l'unique point d'entrée de la capture classique.

Tous les enregistrements classiques appellent
`POST /transcriptions/audio`. L'ancien `POST /messages/audio` reste disponible pour
compatibilité backend, mais n'est plus consommé par le client Angular courant.

### Détection locale de parole

`ClassicVoiceRecorderService` isole `MediaRecorder`, le flux micro et l'analyseur Web
Audio du composant de présentation. Il compte les trames dont le RMS dépasse un seuil
centralisé. Un minimum de trames actives est requis avant envoi. Si Web Audio est
indisponible, la capture n'est pas bloquée uniquement sur ce signal : la relecture
obligatoire reste le contrôle d'autorité.

### Confiance fournisseur

Pour `gpt-4o-transcribe` et variantes compatibles, la requête demande les `logprobs`.
La confiance est calculée comme moyenne des probabilités de tokens
`exp(logprob)`. `AiConsultationService` refuse une confiance connue inférieure à
`AI_MIN_TRANSCRIPTION_CONFIDENCE`.

La détection vocale du endpoint OpenAI est réglable par
`OPENAI_TRANSCRIBE_VAD_THRESHOLD`. La même valeur durcit le `server_vad` du mode
Realtime de compatibilité. Le prompt demande explicitement de retourner un texte
vide sans parole intelligible et interdit de compléter ou d'inventer un propos. Les
valeurs restent externalisées dans YAML.

## Contrats et compatibilité

- aucun endpoint supprimé ;
- ajout de `POST /transcriptions/realtime` pour déposer le texte Realtime en
  `PENDING_REVIEW` sans lancer l'analyse ;
- aucun DTO HTTP existant modifié ;
- nouvelle erreur possible : `422 AI_TRANSCRIPTION_LOW_CONFIDENCE` ;
- nouvelles variables optionnelles avec valeurs par défaut sûres ;
- comportement Angular corrigé : la dictée classique est toujours contrôlée.

## Sécurité

- backend maître de la décision clinique ;
- aucune donnée audio ou transcription ajoutée aux logs ;
- aucune clé OpenAI exposée au navigateur ;
- aucun affaiblissement des permissions existantes ;
- fail-closed avant analyse clinique.

## Observabilité

Le rejet journalise uniquement le fournisseur et une confiance arrondie, sans texte,
audio, nom de médicament ni donnée patient.

## Impact SemVer

PATCH : correction rétrocompatible de sécurité et de cycle de vie. Aucun contrat
public n'est supprimé.
