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
- contrat d'état : une erreur terminale n'est jamais `listening` ;
- le résultat vocal accepté conserve `prescriptions` et `labOrders` jusqu'au clic d'enregistrement ;
- la sérialisation SOAP n'envoie pas `prescriptions`/`labOrders` dans le contrat consultation ;
- le résultat de sauvegarde de consultation expose l'identifiant nécessaire à l'upsert d'ordonnance ;
- un JSON prescription valide est transformé en payload `items` sans modifier médicament, dose, posologie, durée, voie ou fréquence ;
- un JSON prescription invalide échoue avant appel réseau ;
- un tableau `labOrders` valide produit une demande avec `examType=AUTRE`, `patientId` et `visitId` exacts ;
- l'orchestrateur ne crée aucune ressource structurée lorsqu'il n'y a pas d'extra accepté ;
- après réussite connue d'un POST examens dans la session UI, un retry ne répète pas ce POST.

## Tests Spring Boot unitaires

- un `eventId` realtime rejoué retourne la même réponse avec un seul appel IA ;
- la réutilisation d'un `eventId` avec un payload différent retourne `409` ;
- après analyse, le GET session ne renvoie plus le transcript comme capture active ;
- après effacement, le transcript et le pending sont nuls avec statut `NONE` ;
- le listing Vitals utilise le working set actif ;
- le prompt de capture exige la suppression des questions conversationnelles dans les champs SOAP ;
- une réponse négative reliée à une question explicite peut être reformulée sans enrichissement médical ;
- une question non répondue ne produit aucun fait ;
- une tentative d'introduire un diagnostic ou un synonyme médical absent reste rejetée par le garde de factualité ;
- `prescription`, `labOrders` et `vitals` conservent leur encodage factuel structuré.

## Scénarios de reformulation de référence

### Dialogue → Subjectif

Entrée :

```text
Avez-vous des frissons, des sueurs nocturnes ou une perte de poids récente ? Pas de frissons. Je transpire un peu la nuit, mais pas énormément. Par contre, j'ai moins d'appétit depuis une semaine.
```

Attendu :

- `Pas de frissons.` est permis ;
- une phrase déclarative sur les sueurs nocturnes modérées est permise avec les mots source ;
- la diminution d'appétit depuis une semaine est conservée ;
- aucune absence de perte de poids n'est inventée.

### Dialogue → Objectif

Entrée :

```text
Votre température est normale, votre tension est à 12/8 et votre saturation en oxygène est correcte. J'entends un léger sifflement du côté droit mais rien de très marqué.
```

Attendu : phrases déclaratives concises, sans répétition et sans diagnostic ajouté.

## Commandes ciblées

```bash
cd mobile
flutter test test/clinical_speech_hypothesis_test.dart
flutter test test/clinical_transcript_draft_store_test.dart test/clinical_voice_capture_first_restoration_test.dart
flutter test test/clinical_voice_structured_persistence_test.dart test/consultation_api_test.dart
flutter analyze lib/features/dashboard test

cd backend
./mvnw -Dtest=AiClinicalReformulationContractTest,AiClinicalFactualityGuardTest,AiConsultationContinuousCaptureTest,AiConsultationServiceTest,RealtimeClinicalIntakeServiceTest test
./mvnw clean verify
```

## Recette appareil obligatoire

Sur Android réel, en FR puis EN : parole continue, silence, bruit de fond,
verrouillage/reprise, perte réseau pendant analyse, suppression puis réouverture.
Vérifier consultation et constantes en light/dark. Aucun GO production ne peut
être fondé sur les seuls tests automatisés de reconnaissance vocale.

Ajouter pour BUG-20260807 :

1. dicter au moins deux minutes avec questions/réponses, une négation, une temporalité et une question volontairement non répondue ;
2. vérifier que le transcript reste disponible comme source mais que la synthèse finale ne recopie pas les questions ;
3. dicter une ordonnance et une liste mixte d'examens (biologie + imagerie) ;
4. accepter le résultat IA sans enregistrer : vérifier qu'aucune ressource n'est encore créée ;
5. cliquer Enregistrer : vérifier la note SOAP, l'ordonnance et la demande d'examens dans le dossier ;
6. rouvrir la consultation et confirmer que les ressources sont toujours présentes ;
7. provoquer une erreur réseau sur la sauvegarde structurée puis retenter, sans doublon connu dans la session UI.
