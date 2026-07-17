# Consultation IA interactive — Plan de tests

## 1. Objectif

Valider que la conversation, les transcriptions, clarifications et propositions restent contrôlées par le médecin et qu’aucune donnée clinique n’est appliquée ou sauvegardée automatiquement.

## 2. Tests backend unitaires

### Session et messages

- création d’une session avec message d’accueil ;
- ordre stable des messages multi-tour ;
- limite du nombre de tours ;
- expiration de session ;
- isolation visite/utilisateur/organisation ;
- incrément de `revision` à chaque mutation ;
- rejet d’une commande avec `expectedRevision` obsolète.

### Transcription

- audio valide produit `PENDING_REVIEW` ;
- aucun appel chat n’est exécuté lors de la transcription seule ;
- édition d’une transcription en attente ;
- confirmation analyse le texte édité et non le texte original ;
- abandon n’appelle pas le provider de chat ;
- interdiction d’éditer/confirmer une transcription déjà confirmée ou abandonnée ;
- audio vide, trop gros et MIME non autorisé.

### Clarifications

- parsing d’une clarification structurée ;
- champ hors allowlist rejeté ;
- réponse liée à la clarification correcte ;
- réponse à une clarification résolue rejetée ;
- clarification ambiguë sur dosage marquée ouverte ;
- clarification résolue conservée dans l’historique.

### Propositions

- calcul serveur de l’ancienne valeur ;
- proposition identique ignorée ;
- acceptation met à jour uniquement `acceptedDraft` ;
- rejet conserve l’ancienne valeur ;
- accept-all et reject-all ;
- décision répétée idempotente ou rejetée selon contrat final ;
- champ inconnu filtré ;
- longueur maximum appliquée.

### Provider/parser

- JSON valide avec propositions ;
- JSON invalide ;
- bloc Markdown nettoyé ;
- contenu manquant ;
- médicament ambigu crée incertitude/clarification ;
- prompt injection utilisateur n’élargit pas l’allowlist.

## 3. Tests controller et sécurité

- accès médecin autorisé ;
- accès admin clinique autorisé ;
- accès sans `CLINICAL_WRITE` refusé ;
- utilisateur non authentifié refusé ;
- JWT legacy normalisé toujours supporté ;
- visite d’une autre organisation inaccessible ;
- visite non `EN_COURS` rejetée ;
- Problem Detail et statuts HTTP conformes.

## 4. Tests Angular

### Façade

- URLs relatives ;
- mapping SessionView ;
- erreurs 409 session conflict ;
- actions transcription/clarification/proposition ;
- polling sans requêtes concurrentes inutiles.

### ConversationThreadComponent

- messages affichés dans l’ordre ;
- rôle et type correctement rendus ;
- état vide ;
- erreur ;
- light/dark ;
- libellés FR/EN.

### TranscriptionReviewComponent

- texte original prérempli ;
- modification ;
- confirmer ;
- abandonner ;
- boutons désactivés pendant traitement ;
- aucune analyse avant confirmation.

### ClarificationCardComponent

- question et champ affichés ;
- réponse texte ;
- choix optionnels ;
- état résolu ;
- validation input.

### ProposalCardComponent

- ancienne/nouvelle valeur ;
- incertitude ;
- accept/reject ;
- état décidé ;
- navigation clavier et focus visible.

### Assistant panel

- orchestration sans `HttpClient` direct ;
- application uniquement du brouillon accepté ;
- conservation de l’état lors du polling ;
- expiration de session ;
- libération microphone.

## 5. Scénarios d’acceptation

### A — Correction textuelle

1. Brouillon : « douleur à gauche ».
2. Médecin : « Corrige, la douleur est à droite et non à gauche ».
3. Le fil affiche la demande.
4. Une proposition avant/après apparaît.
5. Avant acceptation, le brouillon accepté reste inchangé.
6. Après acceptation, le brouillon accepté contient « droite ».
7. Le formulaire principal reste inchangé avant « Appliquer ».

### B — Audio corrigé avant analyse

1. Audio transcrit « quinze milligrammes ».
2. Le médecin corrige en « cinquante milligrammes ».
3. Il confirme.
4. Le chat reçoit uniquement le texte corrigé.
5. Une proposition est générée avec incertitude si nécessaire.

### C — Clarification

1. Médecin dicte une posologie ambiguë.
2. Le système affiche une clarification liée au champ concerné.
3. Le médecin répond.
4. La clarification devient résolue.
5. Une proposition distincte est créée.

### D — Rejet

1. L’IA propose un changement incorrect.
2. Le médecin rejette.
3. L’ancienne valeur est conservée.
4. La décision reste visible dans le fil.

### E — Deux appareils

1. Session ouverte sur desktop et téléphone avec le même compte.
2. Le téléphone crée une transcription en attente.
3. Le desktop récupère cet état.
4. Une décision sur desktop est visible sur téléphone.
5. Une commande avec ancienne révision reçoit `409` puis recharge l’état.

## 6. Commandes obligatoires

```bash
cd backend
./mvnw test
./mvnw clean verify -B -Dspring.profiles.active=test

cd ../web
npm test
npm run build
```

Exécuter `npm run lint` si le script existe ; sinon documenter son absence.

## 7. Critères de sortie

- Aucun test ignoré.
- CI backend et frontend verte.
- Tous les scénarios A à E couverts automatiquement ou tracés en recette manuelle.
- Aucun contenu clinique présent dans les logs de test.
- Validation visuelle light/dark et FR/EN.
- Revue médecin référent pour les scénarios de clarification et dosage.
