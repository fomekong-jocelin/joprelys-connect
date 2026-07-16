# Plan de test — Envoi sécurisé des secrets d'authentification

- Vérifier que les réponses création staff/admin ne contiennent pas `temporaryPassword`.
- Vérifier que login OTP, récupération et demande OTP patient ne contiennent pas `otpCode`.
- Mock SMTP : vérifier destinataire, sujet et présence du secret dans le message envoyé.
- Vérifier expiration et trois tentatives OTP.
- Vérifier qu'un patient sans e-mail est rejeté sans fuite d'information.
- Exécuter `mvn test`, `npm test` et `npm run build`.
- En staging, envoyer un message réel et vérifier SPF, DKIM, DMARC et classement antispam.
