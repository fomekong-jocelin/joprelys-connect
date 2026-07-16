# Plan de test — Fiabilité de la création des cliniques

- Test d'intégration : création avec `logoPath`, réponse et base conformes.
- Test d'intégration : panne du service e-mail, réponse `503` et absence du compte en base.
- Test d'intégration : succès d'envoi, réponse `201` sans mot de passe.
- Test Angular : restitution des erreurs normalisées et erreur visible de téléversement.
- Suite Maven complète, tests Angular et builds de production.
