# Spécification fonctionnelle — Envoi sécurisé des secrets d'authentification

Les secrets d'authentification ne sont jamais affichés par l'application. À la création d'un compte, le destinataire reçoit son mot de passe temporaire par e-mail. Lors d'une connexion protégée, d'une récupération de mot de passe ou d'une connexion patient, il reçoit un code valable cinq minutes.

Les réponses de demande restent sans secret. Une adresse e-mail patient est obligatoire pour le parcours OTP patient. Les messages rappellent de ne pas partager le code et, pour un mot de passe temporaire, de le remplacer après connexion.
