# Diagnostic P0 — Assistant vocal clinique Flutter / Spring Boot

## Symptômes reproduits par le code et la capture

- une même séquence vocale apparaît trois ou quatre fois ;
- des transcripts d'une analyse précédente reviennent à l'ouverture ;
- des mots disparaissent ou sont dupliqués lors d'une reprise Android ;
- l'écran peut présenter une capture active et une erreur terminale ;
- le transcript visible se termine au milieu d'une phrase.

## Chaîne analysée

```text
Micro Android
  → speech_to_text (résultats partiels/finals)
  → ClinicalSpeechService
  → fusion ClinicalSpeechHypothesis
  → brouillon chiffré indexé par visitId
  → session IA Spring Boot
  → analyse / révisions
  → application explicite aux constantes et à la note SOAP
```

Le paramètre `AudioRecorder` accepté par `ClinicalSpeechService` n'est pas utilisé :
le flux mobile courant dépend du moteur STT natif, sans journal de chunks audio ni
VAD applicatif. Le pipeline Ambient backend existe mais n'est pas la source du
transcript affiché par cette interface Flutter.

## Causes racines confirmées

### RC-01 — Deux chemins de reprise concurrents

`done/notListening` programme `_restartListeningLoop()`. Une erreur récupérable
lance simultanément `_recoverSpeechRecognizer()`, qui appelle `cancel()` puis
`_listenInternal()`. Le callback `done` émis par `cancel()` peut reprogrammer un
second démarrage. Le test `_speech.isListening` ne protège pas la fenêtre entre
le début de deux appels asynchrones à `listen()`.

### RC-02 — Rejeu Android insuffisamment dédupliqué

`stripCommittedClinicalTranscriptPrefix()` ne retire un rejeu que si l'entrée
commence par l'intégralité du transcript déjà commis. Après une reprise, Android
rejoue généralement seulement les derniers mots. Ce suffixe récent est alors
considéré comme un nouveau passage. Une légère variation ASR empêche aussi le
recouvrement exact.

### RC-03 — Restauration d'un transcript déjà analysé

Flutter restaure `existing.transcript` pour tous les statuts sauf `NONE`. Le
backend conserve ce champ après `ANALYZED`. À la réouverture, une ancienne dictée
redevient donc un brouillon éditable.

### RC-04 — Effacement local non durable sémantiquement

Le format du brouillon contient `explicitlyCleared`, mais la restauration ne le
consulte pas. Ensuite `dispose()` écrit directement un snapshot avec la valeur
par défaut `false`, susceptible d'écraser le marqueur d'effacement.

### RC-05 — Idempotence déclarée mais absente

Le DTO realtime expose `eventId`, mais le contrôleur ne le transmet pas au
service. Un retry HTTP peut donc rappeler le modèle et créer une nouvelle
révision pour la même requête.

### RC-06 — Historique Vitals trop large

Le GET Vitals utilise `list()` et renvoie tous les statuts. Le working set de
reprise doit utiliser `listActive()` afin d'exclure `CONSUMED` et `DISCARDED`.

## Décision de correction

- sérialiser les démarrages de `listen()` et invalider les callbacks d'un cycle arrêté ;
- inhiber la reprise `done` pendant un recovery explicite ;
- retirer le plus grand chevauchement suffixe/préfixe récent, exact ou quasi exact ;
- restaurer côté Flutter uniquement `PENDING_REVIEW` ;
- respecter le marqueur `explicitlyCleared` et ne pas le réécrire à `dispose()` ;
- rendre `eventId` idempotent pendant la session backend ;
- vider le transcript serveur après analyse/effacement tout en conservant la réponse courante ;
- utiliser `listActive()` pour Vitals.

## Limites du diagnostic

Les tests automatisés peuvent prouver les invariants de fusion, reprise logique,
restauration et idempotence. Ils ne peuvent pas certifier la qualité acoustique
du microphone, du fabricant Android ou du moteur STT. Une recette sur appareil
avec parole continue, silences, bruit et verrouillage d'écran reste obligatoire.
