# TASK-20260728 — P0 Voice Hotfix B

## Métadonnées

| Champ | Valeur |
|---|---|
| Mode | Diagnostic + Engineering |
| Priorité | P0 |
| Epic | EPIC-0024 — Assistant vocal IA de consultation |
| Stack | Angular 22, WebRTC, OpenAI Realtime, Spring Boot |
| Profil recommandé | Senior full-stack WebRTC/Angular/Spring Boot |
| Reviewer | Tech Lead + médecin référent + QA clinique |
| Estimation | 5 SP / 2 à 4 jours senior, hors recette clinique réelle |
| Impact sprint | Hotfix hors capacité fonctionnelle ; priorité sur le backlog non critique |
| Impact version | PATCH, cible `0.10.2` après gates et recette |

## Objectif

Rendre le parcours Realtime de Consultation et de Constantes fiable de bout en
bout : le professionnel parle naturellement, voit ce qui a été entendu, peut
le corriger avant analyse lorsqu'une revue est requise, et peut terminer sans
perdre un tour durable, une analyse en attente ni le brouillon clinique obtenu.

## Diagnostic confirmé

1. `RealtimeVoiceBridgeService.speakApproved()` créait une réponse audio avec
   l'instruction « Lisez le message approuvé mot pour mot […] n'ajoutez aucun
   mot ». Le contrôleur lançait en plus `SpeechSynthesis`, créant une double
   restitution et un risque de réinjection dans le micro.
2. Le coordinateur utilisait deux moteurs vocaux concurrents et coupait le
   sender pendant la restitution. Le Realtime doit rester conversationnel,
   mais avec une seule voix issue du message backend validé et sans couper le
   micro ; seule la Dictée est passive.
3. L'absence de `logprobs` OpenAI produisait une confiance `null`, convertie
   artificiellement en `0`, puis un rejet « faible confiance ». La phrase était
   mise en revue, mais le panneau Realtime ne rendait pas l'éditeur existant et
   tentait même une analyse immédiate côté parent.
4. Le backlog et `assistantSpeaking` coupaient le sender WebRTC. Une parole
   prononcée pendant ces états pouvait donc ne jamais produire de transcript.
5. L'action « Arrêter/Terminer » détruisait le contrôleur et ses files
   d'analyse en mémoire sans attendre leur vidange ni appliquer le brouillon
   courant au formulaire.
6. La saisie classique des constantes vocalisait encore automatiquement la
   réponse IA et ne proposait pas de correction textuelle avant réanalyse.

## Impacts analysés

- **Spring Boot / OpenAI Realtime** : session Realtime limitée au transport de
  transcription ; aucune modalité de sortie audio ni voix configurée.
- **Angular Consultation** : revue éditable en mode Realtime, finalisation
  drainante, application sûre du brouillon, capture non interrompue par le
  backlog.
- **Angular Constantes** : Dictée passive ; Realtime conversationnel par le
  canal TTS unique ; correction et réanalyse explicites avant application.
- **Base de données** : aucune migration ; le journal durable append-only
  existant reste la source de récupération.
- **API publique** : aucun endpoint supprimé ou contrat métier cassé.
- **CI/CD** : tests Angular ciblés + suite complète + build production ; tests
  Maven ciblés + `clean verify`.

## Critères d'acceptation

- [x] AC-01 — Aucun texte d'instruction interne (« lisez mot pour mot », « ne
  rajoutez rien », etc.) n'est prononcé ou affiché au médecin.
- [x] AC-02 — Le Realtime peut vocaliser une question ou réponse clinique
  courte validée par le backend via un seul canal TTS ; la Dictée reste passive.
- [x] AC-03 — Une phrase non vide sans confiance ASR reste durable, visible et
  éditable ; elle n'est jamais effacée.
- [x] AC-04 — En Realtime, le médecin peut toujours corriger la dernière phrase
  via « Corriger », même si la confiance est élevée ; en revue obligatoire, il
  peut lancer l'analyse ou écarter explicitement le texte.
- [x] AC-05 — Le sender WebRTC reste actif pendant la persistance, l'analyse,
  le backlog, une proposition ou une clarification.
- [x] AC-06 — Seules la pause explicite du médecin, la finalisation explicite
  et une panne durable fail-closed peuvent couper le sender.
- [x] AC-07 — « Terminer » arrête d'abord la capture, attend la vidange
  ordonnée des files, attend toute revue/décision humaine, puis applique le
  brouillon et quitte le Realtime.
- [x] AC-08 — Au moins 20 tours sont persistés et analysés dans l'ordre, sans
  duplication ni disparition.
- [x] AC-09 — Une erreur d'analyse après ACK durable n'efface pas le tour
  journalisé et n'écrase aucune saisie médecin.
- [x] AC-10 — Les constantes restent des propositions éditables et ne sont
  jamais persistées avant validation explicite.
- [x] AC-11 — Prescriptions, examens et saisies médecin plus récentes ne sont
  jamais supprimés par l'application du brouillon.
- [ ] AC-12 — FR/EN, light/dark, mobile/desktop et contrôles accessibles
  restent conformes au design system.

## Definition of Ready

- [x] Diagnostic reproductible relié aux lignes de code fautives.
- [x] Impacts frontend/backend/API/DB/sécurité/planning/version analysés.
- [x] Spécifications fonctionnelle, technique et plan de tests amorcés.
- [x] Critères d'acceptation et reviewer identifiés.
- [x] Référence OpenAI vérifiée : transcription Realtime asynchrone et
  `logprobs` optionnels ; VAD producteur d'événements de tours.

## Actions

- [x] Lire la gouvernance, les standards et les tickets P0 associés.
- [x] Isoler les chemins de vocalisation automatique et de double TTS.
- [x] Isoler les chemins de coupure du sender et de perte à la finalisation.
- [x] Documenter le contrat fonctionnel et technique avant code.
- [x] Supprimer la double restitution et la consigne vocale métatechnique.
- [x] Configurer le transport OpenAI Realtime sans sortie audio autonome.
- [x] Restituer en Realtime le seul message backend via le TTS dédié, avec
  barge-in sans mute du micro.
- [x] Préserver `confidence=null` et présenter une revue éditable.
- [x] Rendre le backlog non bloquant pour la capture.
- [x] Ajouter une finalisation drainante et l'application sûre du brouillon.
- [x] Ajouter la correction/réanalyse pour les constantes dictées.
- [x] Ajouter les tests adversariaux Consultation/Constantes.
- [x] Exécuter les gates Angular et Maven.
- [x] Mettre à jour changelog, tracking, SemVer et preuve de vérification.

## Tests attendus

- Unitaires Angular : absence de `response.create` et de `speechSynthesis`,
  canal TTS unique en Realtime, aucune voix en Dictée, confiance absente,
  éditeur Realtime, backlog sans mute, finalisation pendant ACK/analyse/revue,
  vingt tours ordonnés.
- Unitaires Spring : session Realtime `output_modalities=["text"]`, aucun bloc
  audio de sortie, instructions de transport sans autorisation de parler.
- Régression : fusion sûre du brouillon, décision explicite des propositions,
  intake durable Consultation/Constantes, déduplication `item_id`.
- Gates : `npm test`, `npm run build`, contrôle i18n, `./mvnw clean verify`.
- Recette clinique : Chrome desktop + Android, FR/EN, light/dark, cinq tours,
  correction d'une phrase, parole pendant traitement et finalisation immédiate.

## Definition of Done

- [x] Tous les critères AC-01 à AC-12 automatisables sont prouvés.
- [x] Suites Angular/Maven et build production verts sur le même HEAD.
- [x] Aucun secret, contenu clinique ou prompt ajouté aux logs.
- [x] Documentation feature, changelog, tracking et impact version à jour.
- [ ] Revue Tech Lead terminée.
- [ ] Recette médecin/QA réelle documentée ; sans elle, statut maximal
  `IMPLEMENTED — RECETTE CLINIQUE REQUISE`.

## Reste à faire

Recette clinique réelle Chrome desktop/Android, FR/EN, light/dark, revue Tech
Lead et signature de l'AC-12 visuel. Statut : `IMPLEMENTED — RECETTE CLINIQUE
REQUISE`.
