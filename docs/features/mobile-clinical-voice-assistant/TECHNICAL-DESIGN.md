# Design Technique — Assistant Vocal Clinique & Extraction Intelligente (MOB-2816)

## 1. Architecture

```text
[ CloudSpeechStreamingService ]
              │ PCM + transcript
              ▼
[ ClinicalVoiceProgressiveCoordinator ]
              │ durable intake / rebuild
              ▼
[ Spring AI capture + factuality guard ]
              │
              ├── SOAP / vitals preview
              ├── prescription JSON
              └── labOrders JSON
              │
              ▼
[ ConsultationNotesSheet : état accepté non persisté ]
              │ clic explicite « Enregistrer »
              ▼
[ ClinicalVoiceAcceptedResultPersistence ]
              ├── POST /api/visits/{visitId}/consultation
              ├── POST /api/consultations/{consultationId}/prescription
              └── POST /api/lab-orders
```

Le transcript durable reste la source de preuve. Le mobile ne décide ni de la validité médicale d'une prescription ni du type métier d'un examen : il transporte uniquement le résultat explicitement accepté et appelle les APIs existantes, qui conservent validation et autorisation backend.

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

- Modale dédiée `ClinicalVoiceProgressiveAssistantSheet` pour la capture et la revue.
- Bouton de bascule `[🎙️ Assistant vocal]` intégré dans `PatientVitalsSheet` et `ConsultationNotesSheet`.
- Le résultat accepté contient `ConsultationNote.prescriptions` et `ConsultationNote.labOrders` sous forme JSON structurée, mais ces valeurs ne sont pas sérialisées dans le contrat SOAP final.
- `ConsultationNotesSheet` conserve temporairement les extras structurés acceptés jusqu'au clic explicite d'enregistrement.
- Une classe d'application dédiée orchestre les appels API ; le widget ne parse ni ne classe les données métier.

## 4. Coordination de la reconnaissance continue

`CloudSpeechStreamingService` garde capture micro et transport WebSocket découplés. Les coupures réseau courtes sont absorbées par un tampon PCM borné et un replay récent ; un watchdog détecte l'arrêt réel du flux audio. Le backend applique un VAD PCM et des fenêtres recouvrantes avant transcription.

La fusion applique successivement :

1. égalité / hypothèse cumulative ;
2. chevauchement exact suffixe → préfixe ;
3. chevauchement récent quasi exact avec au moins quatre tokens et une similarité forte ;
4. nouveau segment uniquement après finalisation réelle.

La comparaison quasi exacte reste limitée à la bordure récente pour ne pas supprimer une répétition volontaire prononcée plus tard dans la consultation.

## 5. Restauration et source de vérité

- Le brouillon chiffré local est indexé par visite et prioritaire lorsqu'il a du contenu.
- `explicitlyCleared=true` est une barrière de restauration : aucun contenu local ou serveur antérieur n'est réinjecté.
- Seul le statut serveur `PENDING_REVIEW` autorise la restauration du transcript.
- `ANALYZED` et `NONE` conservent les résultats structurés de session, mais ne préremplissent pas la timeline de capture.
- Le backend mémorise les réponses realtime par `eventId` pour la durée de la session et ne rappelle pas le modèle lors d'un retry identique.

## 6. Reformulation finale

La reconstruction batch utilise `AiClinicalCapturePrompt` et le garde de factualité. Le contrat final doit distinguer deux familles de champs :

### 6.1 Champs textuels SOAP

Pour `symptoms`, `clinicalExam`, `diagnosis`, `conclusion`, `advice` et `followUp` :

- convertir le dialogue en phrases déclaratives concises ;
- retirer salutations, questions, répétitions et marqueurs conversationnels ;
- autoriser uniquement des mots grammaticaux neutres et des formulations d'attribution sans contenu médical nouveau ;
- utiliser la question immédiatement associée comme contexte d'une réponse courte explicite ;
- conserver la preuve exacte sous forme d'`evidence` ;
- ne jamais transformer une question non répondue en fait.

Exemple sûr :

```text
Avez-vous des frissons ? — Pas de frissons.
```

peut devenir :

```text
Pas de frissons.
```

Une question multiple dont une partie n'est pas répondue ne justifie aucune négation sur cette partie.

### 6.2 Champs structurés

`prescription`, `labOrders` et `vitals` restent en JSON compact et ne font pas l'objet d'une reformulation stylistique. Les médicaments, doses, voies, fréquences, durées, examens et valeurs doivent rester littéraux.

## 7. Persistance des éléments acceptés

### 7.1 Note SOAP

`POST /api/visits/{visitId}/consultation` reste la persistance canonique de la note et doit retourner l'identifiant de consultation nécessaire à l'ordonnance.

### 7.2 Prescription

Le JSON `prescription` est décodé dans la couche data/application mobile puis envoyé tel que validé au contrat existant :

```text
POST /api/consultations/{consultationId}/prescription
```

L'endpoint est un upsert ; un retry ne crée donc pas une deuxième ordonnance pour la même consultation.

### 7.3 Examens

Le JSON `labOrders` est une liste de libellés explicites. Tant que l'extraction ne fournit pas un type d'examen fiable et explicitement fondé, le mobile ne doit pas inférer `LABORATOIRE`, `IMAGERIE`, etc. Il utilise le type neutre existant `AUTRE` et envoie :

```text
POST /api/lab-orders
```

avec `patientId`, `visitId`, `examType=AUTRE` et la liste des examens. Une évolution ultérieure pourra enrichir le contrat d'extraction avec un type explicite si elle est documentée et testée.

### 7.4 Erreurs partielles

La note SOAP ne doit jamais être déclarée non enregistrée si elle a déjà été persistée. Si une ressource structurée échoue ensuite :

- conserver les extras en mémoire tant que la feuille reste ouverte ;
- afficher une erreur précise ;
- permettre un retry sans recréer l'ordonnance ;
- ne consommer le working set vocal qu'après la réussite de la note SOAP, conformément au comportement existant.

La déduplication des demandes d'examens lors d'un retry est un point de vigilance : l'orchestration doit éviter de répéter le POST après une réussite connue dans la session UI.

## 8. Responsabilités

- Flutter capture : audio, fusion, brouillon local et états de présentation.
- Flutter application/data : conservation temporaire du résultat accepté, décodage structurel strict et orchestration des appels REST existants.
- Spring Boot consultation/prescription/lab : validation, permissions et persistance métier.
- Spring Boot IA : extraction, reformulation contrôlée, preuve et factualité.
- Aucun widget Flutter ne porte de règle médicale ou de classification clinique inférée.
