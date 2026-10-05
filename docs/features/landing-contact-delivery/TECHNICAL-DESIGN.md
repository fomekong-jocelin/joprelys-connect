# Conception — contact landing

- Angular : `APP_BRAND_CONFIG.contactEmail` et `contactWhatsAppPhone` ; service HTTP relatif `/api/public/demo-requests`, erreur propagée et timeout ; composant orchestre chargement/succès/erreur sans persistance locale des contacts. Liens WhatsApp dynamiques en FR/EN.
- Spring : service transactionnel persiste puis publie un événement immuable. Listener `AFTER_COMMIT` délègue à un port de notification, implémenté par SMTP existant. Le listener journalise uniquement référence et statut ; une panne SMTP ne peut pas annuler le commit. Aucun envoi lors d'un rollback. Notification en texte brut, objet fixe et destinataire configuré, pas de Reply-To contrôlé par le visiteur.
- Configuration : `joprelys.contact.email` via `JOPRELYS_CONTACT_EMAIL`, défaut contact@joprelys.com ; connexion SMTP existante `MAIL_HOST/PORT/USERNAME/PASSWORD`, délais SMTP bornés via propriétés YAML. Pas de nouvelle dépendance.
- API : POST existant, mêmes champs/réponse et HTTP 201 pour l'enregistrement ; validation `@Email` ajoutée au champ optionnel, 400 si invalide. Pas de migration ni impact Flutter.
- Tests : HTTP Angular succès/erreur/timeout, FR/EN et double clic ; tests SMTP simulé (destinataire/contenu/erreur) et intégration H2 avec commit/rollback. Aucun mail réel dans les tests.
- Limites : notification best-effort, sans outbox/retry automatique ; recette SMTP déployée nécessaire. PATCH candidat, VERSION inchangé.
