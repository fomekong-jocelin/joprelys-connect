# USER-GUIDE — Assistant vocal IA de consultation

## 1. À quoi sert cette fonctionnalité ?

Elle aide le médecin à préparer un brouillon de consultation depuis la voix ou
le texte. Elle ne remplace ni le jugement médical ni la validation du formulaire.

## 2. Qui peut l’utiliser ?

Un médecin ou administrateur clinique autorisé dans la même clinique que la
visite. La fonctionnalité peut être désactivée.

## 3. Parcours prévu

1. Ouvrir « Scanner une visite » sur le téléphone.
2. Autoriser la caméra et scanner le QR.
3. Vérifier le patient et la visite.
4. Choisir Realtime pour un échange continu, ou Dictée pour une saisie passive.
5. Autoriser le microphone et dicter une observation.
6. Relire la transcription et les champs proposés.
7. En Realtime, répondre aux questions cliniques courtes de l’assistant. Le
   bouton « Corriger » de la dernière phrase reste disponible même si l’IA
   estime la transcription fiable ; modifier le texte puis envoyer la
   correction.
8. Choisir « Appliquer au formulaire » ou « Terminer ». La finalisation attend
   les phrases déjà reçues et les validations encore nécessaires.
9. Relire et modifier manuellement.
10. Enregistrer avec l’action habituelle.

## 4. Points de vigilance

- Ne jamais valider une information non vérifiée.
- Ne pas énoncer inutilement l’identité complète du patient.
- La Dictée ne parle pas. Le Realtime peut poser une question clinique à voix
  haute ; une reprise de parole du médecin interrompt cette lecture sans couper
  le microphone.
- En cas de doute, utiliser le formulaire manuel.

## 5. Messages fréquents

| Message | Cause | Action |
|---|---|---|
| Assistant indisponible | feature/quota/provider | continuer manuellement |
| Visite non active | clôturée/annulée | vérifier la visite |
| Caméra/micro refusé | permission | autoriser ou mode texte |
| Audio trop volumineux | trop long | segment plus court |
| Transcription à relire | nombre, dose, négation ou confiance à vérifier | corriger le texte puis relancer l’analyse |

## 6. FAQ

| Question | Réponse |
|---|---|
| L’IA enregistre-t-elle automatiquement ? | Non, elle produit un brouillon. |
| Puis-je corriger à la voix ? | Oui, puis vous relisez. |
| Puis-je corriger au clavier en Realtime ? | Oui. Utilisez « Corriger » sous la dernière phrase, modifiez le texte puis envoyez la correction. |
| L’audio est-il conservé ? | Joprelys ne le conserve pas ; le fournisseur doit être validé avant production. |
| Puis-je continuer sans IA ? | Oui, toujours. |
