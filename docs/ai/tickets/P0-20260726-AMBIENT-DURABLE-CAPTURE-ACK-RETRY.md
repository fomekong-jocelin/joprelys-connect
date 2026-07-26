# P0 — Capture ambient durable, chiffrée, ACK/retry

## Objectif

Fermer le trou de sûreté identifié dans #167 : une coupure WebRTC, une panne backend ou une erreur HTTP ne doit plus faire disparaître silencieusement la parole clinique.

Cette phase complète le transcript ledger fusionné par #174.

## Architecture

```text
Microphone
  ├─ Realtime WebRTC → UX immédiate / copilote
  │
  └─ AudioWorklet indépendant
       ↓ PCM mono
     WAV autonome ~10 s
       ↓
     AES-GCM 256
       ↓
     IndexedDB local
       ↓
     file d'upload séquentielle
       ↓
     POST chunkId + startOffset + locale
       ↓
     journal serveur SHA-256
       ↓
     diarisation + transcript ledger FINAL
       ↓
     ACK HTTP 2xx
       ↓
     suppression locale
```

## Invariants

1. Le Realtime n'est plus l'unique copie audio.
2. Une pause automatique du Realtime pendant l'analyse IA ne coupe pas la capture de sécurité.
3. Une déconnexion Realtime ne coupe pas la capture de sécurité.
4. Une action explicite du médecin « Couper le micro » arrête les deux captures.
5. Les chunks locaux sont chiffrés AES-GCM ; le plaintext n'est reconstruit qu'au moment de l'upload.
6. Un chunk n'est supprimé du navigateur qu'après une réponse serveur 2xx.
7. HTTP 0/408/425/429/5xx et `AI_AMBIENT_CHUNK_PROCESSING` restent retryables.
8. Un conflit de hash n'est jamais masqué comme une simple panne transitoire.
9. Le serveur journalise `PROCESSING | COMPLETED | FAILED` avant/après la diarisation.
10. Le serveur compare SHA-256 + offset pour empêcher qu'un même `chunkId` transporte deux audios différents.
11. Un retry après ACK perdu renvoie les items déjà `COMPLETED` sans rappeler le fournisseur de transcription.
12. Le dossier clinique conserve donc un effet idempotent même avec un réseau at-least-once.

## Capture navigateur

La capture utilise `AudioWorklet` plutôt que des fragments `MediaRecorder` WebM : chaque bloc généré est un WAV PCM16 mono autonome à 16 kHz, donc décodable indépendamment.

Le graphe audio utilise une sortie à gain zéro pour maintenir l'AudioWorklet actif sans réinjecter le microphone dans les haut-parleurs.

Le coffre demande également `navigator.storage.persist()` en best effort et surveille la pression de quota. Au-delà de 85 %, l'UX passe en alerte critique mais aucun fragment non confirmé n'est supprimé automatiquement.

## Résilience réseau

La file d'upload :

- reprend à l'événement `online` ;
- traite les chunks dans l'ordre temporel ;
- utilise un backoff borné 2/5/15/30/60 s ;
- conserve le nombre de tentatives et la dernière erreur ;
- reprend également les chunks d'une page précédente au redémarrage du service.

## Journal serveur

Flyway V97 crée `ai_ambient_audio_chunks` :

- tenant / visite ;
- chunk ID unique ;
- SHA-256 du WAV ;
- offset audio ;
- type MIME ;
- état PROCESSING / COMPLETED / FAILED ;
- claim timestamp ;
- auteur ;
- erreur contrôlée.

Un claim PROCESSING de moins de 2 minutes retourne `AI_AMBIENT_CHUNK_PROCESSING`. Un claim plus ancien peut être repris après crash serveur.

## Limite honnête restante

Cette phase réduit fortement la perte silencieuse, mais **ne clôt pas encore #167** : le bloc PCM en cours de constitution réside encore en mémoire avant son chiffrement IndexedDB. Un crash brutal du processus navigateur peut donc perdre jusqu'à environ 10 secondes de fragment courant malgré les flush `pagehide` best effort.

Pour revendiquer un vrai « zéro perte » même face à un crash brutal, la phase suivante devra persister des micro-fragments sub-seconde / worker puis les agréger avant diarisation.

Cette limite doit rester visible dans #167 et ne doit jamais être présentée comme résolue tant que les tests de crash/kill ne sont pas passés.

## Tests exigés

- WAV autonome 16 kHz / mono / PCM16 ;
- downsampling 48 → 16 kHz ;
- capture ambient continue pendant `processing` Realtime ;
- capture ambient continue pendant panne Realtime ;
- mute explicite arrête toute capture ;
- suppression IndexedDB uniquement après 2xx ;
- 503 garde le chunk ;
- hash mismatch garde le chunk et arrête les retries automatiques ;
- serveur : completed retry n'appelle pas le provider ;
- serveur : claim concurrent récent = 409 contrôlé ;
- serveur : claim stale = reprise ;
- serveur : payload différent sous même ID = rejet.
