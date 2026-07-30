# Pré-enregistrement public — correctifs UX de suivi

## Objectif

Finaliser l'alignement du pré-enregistrement public avec le reste de Joprelys Connect après la refonte progressive.

## Correctifs

- Le logo et les liens de retour utilisent désormais l'origine courante du navigateur afin de rester dans le bon environnement (`recette.joprelys.com` en recette, `joprelys.com` en production).
- Le sélecteur de langue reprend les repères visuels FR/EN avec les drapeaux 🇫🇷 et 🇬🇧, cohérents avec l'écran de connexion.
- La validation CAPTCHA est restructurée en un bloc plus léger : contexte de sécurité, question clairement mise en avant, action « Changer de question » compacte et accessible, saisie de réponse séparée.
- Le contrat API du CAPTCHA et du pré-enregistrement reste inchangé.

## Vérifications attendues

- Sur recette, le logo et « Retour au site » restent sur `https://recette.joprelys.com`.
- En production, ils restent sur `https://joprelys.com`.
- Les boutons FR/EN affichent leurs drapeaux et conservent le changement de langue existant.
- Le changement de question CAPTCHA recharge une nouvelle question et vide la réponse précédente.
- Le parcours reste utilisable sur mobile sans débordement horizontal.
