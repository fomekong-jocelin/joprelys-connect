# Conception technique — E-mails transactionnels premium

## Architecture

`AccountMailTemplateFactory` construit un contenu indépendant du transport : objet, texte alternatif et HTML. `SmtpAccountMailService` reste l'adaptateur SMTP et assemble un message MIME multipart/alternative avec le logo officiel embarqué par CID.

## Design

- Fond `#f7fafc`, surface blanche, texte `#0a1d3d` et accent `#0b91b2` issus de `DESIGN.md`.
- Carte de 600 px maximum, rayon de 8 px et ombre légère.
- Styles inline et tableaux de présentation pour la compatibilité e-mail.
- Code avec espacement des caractères, contraste renforcé et police monospace.
- Mise en page fluide sur mobile.

## Sécurité et accessibilité

- Échappement HTML de toutes les valeurs dynamiques.
- Texte alternatif complet.
- Logo embarqué : aucune requête distante ni pixel de suivi.
- Aucun secret dans les logs, l'objet ou le nom de fichier.
- Contrastes visant WCAG AA et taille minimale de 14 px pour le corps.

## SemVer

PATCH : amélioration du rendu sans changement d'API.
