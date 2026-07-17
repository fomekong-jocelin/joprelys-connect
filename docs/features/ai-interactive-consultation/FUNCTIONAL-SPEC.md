# Consultation IA interactive — Spécification fonctionnelle

## 1. Contexte

Le médecin dispose actuellement d’une dictée audio transformée immédiatement en brouillon clinique. Cette approche ne permet pas de corriger la transcription avant analyse, de suivre une conversation visible, de répondre formellement à une clarification ni de valider les changements champ par champ.

## 2. Utilisateurs

- Médecin avec `CLINICAL_WRITE`.
- Administrateur clinique autorisé à saisir une consultation.
- Médecin utilisant le même compte depuis ordinateur et téléphone dans le pilote QR.

## 3. Objectif

Transformer l’assistant de dictée en assistant conversationnel contrôlé, sans autoriser l’IA à sauvegarder ou décider à la place du médecin.

## 4. Périmètre inclus

- Conversation texte multi-tour visible.
- Dictée audio par segments.
- Transcription éditable avant analyse.
- Questions de clarification structurées.
- Propositions de modification avant/après.
- Acceptation ou rejet champ par champ.
- Application explicite au formulaire de consultation.
- Synchronisation du même état de session sur deux appareils authentifiés.

## 5. Périmètre exclu

- Streaming audio temps réel.
- Réponse vocale de l’assistant.
- Diarisation médecin/patient.
- Prescription automatique.
- Diagnostic autonome.
- Sauvegarde automatique du dossier.
- Accès QR public sans authentification.
- Persistance longue durée de la conversation dans le dossier médical.

## 6. Parcours principal

1. Le médecin ouvre une visite `EN_COURS`.
2. Il active l’assistant IA.
3. Le fil affiche le message d’accueil de l’assistant.
4. Le médecin écrit un message ou enregistre un segment audio.
5. Pour l’audio, le système affiche la transcription dans un éditeur.
6. Le médecin corrige, confirme ou abandonne la transcription.
7. Après confirmation, l’IA analyse le texte avec le contexte conversationnel et le brouillon validé.
8. Le système affiche :
   - le message de l’assistant ;
   - une éventuelle clarification ;
   - les propositions de modification avant/après.
9. Le médecin accepte ou rejette les propositions.
10. Le brouillon accepté évolue, sans modifier encore le formulaire principal.
11. Le médecin clique sur « Appliquer au formulaire ».
12. La sauvegarde de la consultation reste une action séparée.

## 7. Règles métier

### 7.1 États de transcription

- `PENDING_REVIEW` : transcription disponible mais non analysée.
- `CONFIRMED` : texte confirmé et envoyé à l’analyse.
- `DISCARDED` : transcription abandonnée.

Une transcription `PENDING_REVIEW` ne doit jamais modifier les propositions ni le brouillon accepté.

### 7.2 Messages

Chaque message possède : identifiant, rôle, type, contenu, date et statut.

Rôles : `USER`, `ASSISTANT`, `SYSTEM`.

Types : `TEXT`, `TRANSCRIPT`, `CLARIFICATION`, `DECISION`, `ERROR`.

### 7.3 Clarification

Une clarification doit comporter :

- un identifiant ;
- le champ clinique concerné ;
- une question explicite ;
- un statut `OPEN` ou `RESOLVED` ;
- des options facultatives ;
- la réponse associée lorsqu’elle est résolue.

Une clarification ne doit pas être représentée uniquement par un booléen.

### 7.4 Propositions

Une proposition est distincte du brouillon accepté et contient :

- champ ;
- ancienne valeur ;
- nouvelle valeur ;
- raison courte ;
- incertitude éventuelle ;
- décision `PENDING`, `ACCEPTED` ou `REJECTED`.

Le rejet conserve l’ancienne valeur. L’acceptation met à jour le brouillon accepté, mais pas le formulaire principal.

### 7.5 Médicaments et nombres

Le système ne doit jamais corriger silencieusement un nom, dosage, unité, fréquence, durée ou voie. Toute ambiguïté doit être marquée et générer une clarification ou une proposition nécessitant confirmation.

## 8. États UI attendus

- Session inactive.
- Session active sans message.
- Enregistrement en cours.
- Transcription en attente de relecture.
- Analyse en cours.
- Conversation avec clarification ouverte.
- Propositions en attente.
- Brouillon accepté prêt à appliquer.
- Session expirée.
- Fournisseur IA indisponible.

Tous les états doivent fonctionner en light/dark et en français/anglais.

## 9. Critères d’acceptation

- Le médecin voit l’historique complet de la session dans l’ordre.
- Une correction texte apparaît dans le fil après envoi.
- Une transcription peut être modifiée avant analyse.
- Abandonner une transcription ne déclenche aucun appel d’analyse.
- Une clarification est affichée avec son champ cible et peut recevoir une réponse.
- Les propositions affichent clairement ancienne et nouvelle valeur.
- Chaque proposition peut être acceptée ou rejetée indépendamment.
- Le formulaire principal ne change qu’après action « Appliquer au formulaire ».
- Aucun texte clinique n’est sauvegardé automatiquement.
- La synchronisation téléphone/ordinateur restitue messages, transcription en attente, clarifications et décisions.

## 10. Cas limites

- Session expirée pendant la relecture d’une transcription.
- Deux appareils tentent de décider la même proposition.
- Réponse IA JSON invalide.
- Message trop long.
- Audio vide, trop volumineux ou type non autorisé.
- Modèle renvoie un champ hors allowlist.
- Modèle propose une valeur identique à l’existant.
- Clarification ouverte alors qu’une nouvelle dictée arrive.
- Médecin rejette toutes les propositions.

## 11. Hypothèses

- Le pilote conserve une session en mémoire et un backend mono-instance.
- Le médecin reste authentifié sur les deux appareils.
- La validation DPO/fournisseur reste une condition de généralisation.
