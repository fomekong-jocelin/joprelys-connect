# BUG-20260718 — Invitation personnel : erreurs e-mail non actionnables

## Statut

`QA_TECHNIQUE_EN_COURS`

## Constat production

Depuis `/clinic/staff`, l'invitation d'un collaborateur avec une adresse rejetée par le serveur SMTP retournait `503 Service Unavailable` et l'interface affichait uniquement une erreur interne générique.

Le même statut était utilisé pour :

- une boîte destinataire inexistante ou rejetée ;
- une panne temporaire du serveur SMTP ;
- une erreur d'authentification ou de configuration SMTP.

Le frontend ignorait par ailleurs le contrat normalisé `{ error: { code, message, trace_id } }`.

## Risques

- l'administrateur ne sait pas corriger l'adresse saisie ;
- les tentatives sont répétées sans distinguer une erreur permanente d'une panne temporaire ;
- la création du compte et l'affectation RBAC doivent rester atomiques lorsque l'e-mail de première connexion n'est pas délivré ;
- les exceptions SMTP peuvent contenir une adresse personnelle et ne doivent pas être journalisées intégralement.

## Décision

### Rejet permanent du destinataire

- HTTP `422 Unprocessable Entity` ;
- code `MAIL_RECIPIENT_REJECTED` ;
- message utilisateur : vérifier l'adresse et sa capacité à recevoir des messages ;
- aucune création de compte ni affectation de rôle persistée.

### Indisponibilité temporaire

- HTTP `503 Service Unavailable` ;
- code `MAIL_DELIVERY_UNAVAILABLE` ;
- invitation annulée transactionnellement ;
- nouvel essai autorisé avec la même adresse.

### Confidentialité

- le log conserve uniquement le `trace_id` et le type technique de l'échec ;
- l'adresse destinataire et le mot de passe temporaire ne sont pas inscrits dans les logs ;
- le backend ne prétend pas qu'une boîte « n'existe pas » : une adresse peut aussi être désactivée, pleine ou refusée par une politique de messagerie.

## Implémentation

- classification des chaînes `MailSendException`, `SendFailedException`, `MessagingException` et erreurs d'adresse SMTP permanentes ;
- exception dédiée `MailRecipientRejectedException` ;
- mapping API distinct `422` / `503` ;
- lecture du code métier normalisé dans l'écran de gestion du personnel ;
- messages FR/EN ;
- correction des libellés de sélection de rôles en mode anglais.

## Scénarios de test

- destinataire rejeté → `422`, compte absent ;
- SMTP indisponible → `503`, compte absent ;
- rejet puis correction/reprise → deuxième invitation possible ;
- erreur d'authentification SMTP non classée comme adresse rejetée ;
- interface Angular affiche le message propre au code métier ;
- aucun collaborateur ajouté localement après un échec.

## Critères d'acceptation

- [x] adresse rejetée distinguée d'une panne SMTP ;
- [x] messages actionnables en français et en anglais ;
- [x] rollback du compte couvert par tests ;
- [x] nouvel essai couvert par test ;
- [x] logs sans adresse destinataire ni secret ;
- [ ] CI backend et frontend verte ;
- [ ] recette authentifiée sur l'environnement déployé.
