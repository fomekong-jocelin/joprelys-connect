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
4. Autoriser le microphone ou utiliser le mode texte.
5. Dicter une observation courte.
6. Relire la transcription et les champs proposés.
7. Répondre ou dire « Non, corrige [champ]… ».
8. Choisir « Appliquer au formulaire ».
9. Relire et modifier manuellement.
10. Enregistrer avec l’action habituelle.

## 4. Points de vigilance

- Ne jamais valider une information non vérifiée.
- Ne pas énoncer inutilement l’identité complète du patient.
- Le haut-parleur est désactivé par défaut.
- En cas de doute, utiliser le formulaire manuel.

## 5. Messages fréquents

| Message | Cause | Action |
|---|---|---|
| Assistant indisponible | feature/quota/provider | continuer manuellement |
| Visite non active | clôturée/annulée | vérifier la visite |
| Caméra/micro refusé | permission | autoriser ou mode texte |
| Audio trop volumineux | trop long | segment plus court |
| Réponse à vérifier | sortie incertaine | corriger manuellement |

## 6. FAQ

| Question | Réponse |
|---|---|
| L’IA enregistre-t-elle automatiquement ? | Non, elle produit un brouillon. |
| Puis-je corriger à la voix ? | Oui, puis vous relisez. |
| L’audio est-il conservé ? | Joprelys ne le conserve pas ; le fournisseur doit être validé avant production. |
| Puis-je continuer sans IA ? | Oui, toujours. |
