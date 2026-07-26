# Review checklist — Sécurité vocale IA et démarrage RxNorm

## Verdict

**BLOCKED POUR DÉPLOIEMENT**, correctifs source implémentés et frontend validé.
Le merge doit attendre une exécution Maven/CI verte ; la réactivation RxNorm en
Recette doit attendre ce build et le redéploiement.

## P0

- Aucun défaut P0 résiduel identifié dans le flux Angular courant.
- Le parcours vocal courant ne peut plus analyser une transcription sans relecture
  médicale explicite.
- Le test de contexte Spring ajouté pour RxNorm n'a pas pu être exécuté localement :
  cette preuve est obligatoire avant déploiement.

## P1

- La recette clinique réelle silence, voix faible, bruit et médicament reste à faire.
- L'ancien endpoint `POST /messages/audio` reste disponible pour les clients
  historiques ; le client Angular courant ne l'utilise plus. Sa suppression ou sa
  migration doit être traitée comme un changement de contrat séparé.

## P2

- La suite Angular complète garde 23 échecs hors périmètre sous Node 25 liés à
  `localStorage`/TestBed. Les 15 tests du périmètre passent et le build est vert.
- Utiliser une version Node LTS supportée dans le gate CI.
- `AiConsultationService` (491 lignes), `VoiceAssistantPanelComponent` (498 lignes)
  et `RealtimeVoiceBridgeService` (404 lignes) restent au-dessus du seuil d'alerte
  de 300 lignes, mais sous la limite bloquante de 500 après les extractions réalisées.

## P3

- Aucun.

## Architecture et standards

- ✅ Documentation First, tickets, specs, plans de test et contrat API présents.
- ✅ Backend maître : la transcription est déposée en `PENDING_REVIEW`; aucune
  analyse clinique avant confirmation.
- ✅ Controller limité à validation/délégation ; workflow de transcription extrait.
- ✅ Capture audio extraite dans `ClassicVoiceRecorderService`.
- ✅ Classes modifiées sous 500 lignes ; composants/services réutilisables.
- ✅ Maven, YAML, proxy Angular, URLs relatives et `.gitignore` vérifiés.
- ✅ Aucun Gradle, Angular Material, Tailwind v3, secret ou URL backend codée en dur.
- ✅ FR/EN et thèmes existants préservés ; aucun changement visuel de design.
- ➖ Base de données, migration, Flutter et ADR : non concernés.

## Sécurité

- ✅ Réponse autonome OpenAI Realtime désactivée (`create_response=false`).
- ✅ Reconnexion en boucle bornée à une tentative par visite.
- ✅ Aucun timeout data channel créé avant succès du POST SDP.
- ✅ Une exception Realtime inattendue devient un `503` sûr, jamais un `500`
  exposant un détail interne.
- ✅ Capture silencieuse classique rejetée avant envoi lorsque Web Audio est
  disponible.
- ✅ Seuil VAD OpenAI et confiance de transcription externalisés.
- ✅ Confiance faible rejetée en `422` avant toute révision/prescription.
- ✅ Aucun audio, transcript, secret ou donnée patient ajouté aux logs.
- ✅ Identité authentifiée et isolation visite/organisation existantes conservées.

## Tests vérifiés

- `npm test -- --watch=false --include=...` : 4 fichiers, 15 tests, 0 échec.
- `npm run build` : succès.
- `npm run i18n:check` : succès, 49 clés shell FR/EN.
- suite Angular complète : 364 succès, 23 échecs environnementaux hors périmètre.
- Maven ciblé : bloqué avant compilation, parent Spring Boot 4.1.0 absent du cache.
- `git diff --check` : aucune erreur d'espace.

## Risques restants

- Validation Maven/CI et test réel de démarrage Spring obligatoires.
- Réactivation progressive des flags RxNorm uniquement après déploiement validé.
- Recette clinique avec médecin référent obligatoire.
- Aucun déploiement, redémarrage ou changement de variables Recette réalisé dans
  cette intervention locale.
