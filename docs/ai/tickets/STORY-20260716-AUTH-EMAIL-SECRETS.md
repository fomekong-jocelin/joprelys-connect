# STORY-20260716 — Transmission sécurisée des identifiants par e-mail

## Objectif

Ne plus afficher ni retourner les mots de passe temporaires et OTP dans l'interface, l'API ou les journaux, et les transmettre à l'adresse e-mail du compte.

## Critères d'acceptation

- [x] Le mot de passe généré lors de la création d'un compte staff/admin est envoyé par e-mail.
- [x] L'OTP des connexions professionnelles sensibles est envoyé par e-mail.
- [x] L'OTP de récupération de mot de passe est envoyé par e-mail.
- [x] L'OTP patient est envoyé à l'e-mail du dossier patient.
- [x] Aucun de ces secrets n'est renvoyé dans les réponses API ou prérempli dans Angular.
- [x] Aucun secret SMTP n'est versionné.
- [ ] Valider un envoi réel avec les paramètres SMTP de production.
- [ ] Exécuter la suite backend complète dans un environnement ayant accès aux dépendances Maven.

## Actions

- [x] Analyser les parcours auth, staff, organisations, récupération et patient.
- [x] Ajouter un port e-mail et son adaptateur SMTP Spring.
- [x] Externaliser la configuration SMTP par variables d'environnement.
- [x] Retirer les secrets des DTO et écrans.
- [x] Compiler le frontend de production.
- [x] Déclarer explicitement le `JavaMailSender` requis par les contextes Spring Boot.
- [x] Mettre à jour la documentation et le suivi.

## Estimation et responsabilités

- Estimation : 1 jour senior full-stack, 2 points.
- Profil : senior Spring Boot / Angular, avec revue sécurité.
- Reviewer : Lead Backend + QA sécurité.
- DoR : adresse expéditrice et serveur SMTP disponibles.
- DoD : tests verts, test SMTP réel, secret injecté par le gestionnaire de secrets de production.

## Risques

- Le nom d'hôte SMTP par défaut est `smtp.hostinger.com`; il doit être remplacé via `MAIL_HOST` si le fournisseur de `joprelys.com` diffère.
- Un patient sans adresse e-mail ne peut plus demander de code et reçoit une erreur explicite.
- Le mot de passe communiqué dans la conversation doit être renouvelé après configuration.

## Statut

QA — implémentation terminée, validation SMTP réelle et tests backend complets restants.
