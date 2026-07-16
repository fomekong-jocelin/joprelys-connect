# Plan de test — E-mails transactionnels premium

- Vérifier le sujet, le texte alternatif et le HTML des quatre variantes.
- Vérifier l'échappement du nom du destinataire.
- Vérifier le multipart MIME et la présence du logo inline.
- Exécuter les tests Maven ciblés puis la suite backend.
- Envoyer un e-mail réel et contrôler son rendu dans le webmail Joprelys.
