# Functional Spec — AI Realtime Continuous Conversation

## Problème
Le mode WebRTC actuel se comporte encore comme une dictée contrôlée : après un tour, la transcription est placée en relecture et le micro est bloqué. Cela oblige le professionnel à relancer manuellement l'écoute.

## Utilisateurs concernés
Médecins et professionnels autorisés à utiliser la consultation IA avec `CLINICAL_WRITE`.

## Objectif
Permettre une conversation vocale continue : le professionnel active le copilote une fois, parle, écoute Joprelys, répond ou interrompt, sans réactiver le microphone entre les tours.

## Inclus
- Analyse immédiate des transcriptions issues de WebRTC.
- Routage automatique vers une clarification pending.
- Barge-in pendant la réponse vocale Joprelys.
- Fin de tour audio pilotée par le buffer WebRTC réel.
- Pause uniquement pendant une courte analyse ou une validation clinique décisionnelle.

## Exclus
- Acceptation automatique d'une proposition clinique.
- Sauvegarde automatique dans le dossier patient.
- Commande vocale d'acceptation/rejet des révisions.
- Modification du workflow de dictée classique contrôlée.

## Parcours attendu
1. Le professionnel active le copilote une seule fois.
2. La connexion WebRTC reste active.
3. Il parle naturellement.
4. La fin du tour est détectée par VAD et la transcription est envoyée au moteur clinique Joprelys.
5. Joprelys structure, propose ou clarifie avec ses garde-fous existants.
6. La réponse approuvée par le backend est vocalisée sur la même session Realtime.
7. Le micro reste disponible pendant la réponse afin de permettre une interruption.
8. Si Joprelys pose une clarification, la phrase suivante est automatiquement sa réponse.
9. Si une révision clinique nécessite une décision, le micro est mis en pause jusqu'à la validation explicite.

## Critères d'acceptation
- Cinq tours consécutifs possibles sans cliquer sur le micro.
- Une clarification peut être répondue oralement sans action UI intermédiaire.
- Une interruption pendant la réponse Joprelys coupe l'audio et conserve la nouvelle parole.
- Aucun transcript Realtime normal n'est créé en `PENDING_REVIEW`.
- Une proposition clinique reste `PENDING` jusqu'à décision explicite.
- La dictée classique continue de demander une relecture de transcription.

## Cas limites
- Parole pendant l'analyse backend : courte pause acceptable, réarmement automatique obligatoire.
- Réponse audio annulée par barge-in : ne pas attendre une fin audio qui n'arrivera plus.
- Erreur Realtime : restaurer l'écoute dès que possible et conserver le fallback existant.
- Echo/haut-parleur : s'appuyer sur `echoCancellation`, `noiseSuppression` et recette matérielle desktop/mobile.
