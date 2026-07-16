# Conception technique — Envoi sécurisé des secrets d'authentification

## Architecture

`AccountMailService` est le port applicatif. `SmtpAccountMailService` est l'adaptateur Spring Mail. `MailSenderConfig` construit explicitement le `JavaMailSender` depuis les propriétés externalisées lorsque le contexte n'en fournit pas. Les services d'authentification et de création de compte appellent le port après génération cryptographiquement sûre du secret.

## Configuration

Variables obligatoires/recommandées :

```text
MAIL_HOST=smtp.hostinger.com
MAIL_PORT=465
MAIL_USERNAME=noreply@joprelys.com
MAIL_PASSWORD=<secret injecté hors Git>
MAIL_SSL_ENABLED=true
MAIL_STARTTLS_ENABLED=false
```

Pour un serveur SMTP STARTTLS, utiliser généralement le port 587, désactiver SSL direct et activer STARTTLS. Le mot de passe ne doit figurer ni dans YAML, ni dans `.env.example`, ni dans les logs.

## Sécurité et exploitation

- OTP générés avec `SecureRandom`, expiration cinq minutes et trois essais maximum.
- Aucun mot de passe/OTP dans les DTO de réponse ou logs.
- Échec d'envoi propagé : le parcours n'annonce pas faussement un envoi réussi.
- SMTP de production à valider avec TLS et rotation du secret communiqué hors dépôt.
