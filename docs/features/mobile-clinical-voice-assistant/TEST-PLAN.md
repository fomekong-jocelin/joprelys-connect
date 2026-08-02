# Plan de test — Assistant vocal clinique mobile

## Tests Flutter unitaires

- hypothèse cumulative, correction courte et chevauchement exact ;
- même fenêtre rejouée deux, trois et quatre fois ;
- chevauchement récent avec une variation d'un token ;
- mot coupé entre deux fenêtres sans perte ;
- restauration d'un brouillon local normal ;
- non-restauration d'un brouillon `explicitlyCleared` ;
- restauration serveur uniquement pour `PENDING_REVIEW` ;
- non-restauration pour `ANALYZED` et `NONE` ;
- contrat d'état : une erreur terminale n'est jamais `listening`.

## Tests Spring Boot unitaires

- un `eventId` realtime rejoué retourne la même réponse avec un seul appel IA ;
- la réutilisation d'un `eventId` avec un payload différent retourne `409` ;
- après analyse, le GET session ne renvoie plus le transcript comme capture active ;
- après effacement, le transcript et le pending sont nuls avec statut `NONE` ;
- le listing Vitals utilise le working set actif.

## Commandes ciblées

```bash
cd mobile
flutter test test/clinical_speech_hypothesis_test.dart
flutter test test/clinical_transcript_draft_store_test.dart test/clinical_voice_capture_first_restoration_test.dart
flutter analyze lib/features/dashboard test

cd backend
./mvnw -Dtest=AiConsultationContinuousCaptureTest,AiConsultationServiceTest,RealtimeClinicalIntakeServiceTest test
```

## Recette appareil obligatoire

Sur Android réel, en FR puis EN : parole continue, silence, bruit de fond,
verrouillage/reprise, perte réseau pendant analyse, suppression puis réouverture.
Vérifier consultation et constantes en light/dark. Aucun GO production ne peut
être fondé sur les seuls tests automatisés de `speech_to_text`.
