# 2026-07-18 — Invitation du personnel et erreurs de messagerie

## Corrigé

- distinction entre une adresse destinataire rejetée et une panne temporaire du service SMTP ;
- retour `422 MAIL_RECIPIENT_REJECTED` pour les rejets permanents ;
- conservation de `503 MAIL_DELIVERY_UNAVAILABLE` pour les indisponibilités temporaires ;
- affichage de messages actionnables dans l'écran de gestion du personnel ;
- traductions françaises et anglaises des erreurs d'invitation et des libellés de sélection de rôles ;
- suppression des traces d'exception SMTP complètes dans les logs de ces deux cas afin de ne pas exposer l'adresse destinataire.

## Garanties

- la création du compte et les affectations RBAC sont annulées si le mot de passe temporaire ne peut pas être délivré ;
- la même invitation peut être rejouée après correction de l'adresse ou rétablissement du service ;
- un échec d'authentification SMTP n'est pas présenté comme une adresse invalide ;
- aucun mot de passe temporaire n'est retourné par l'API ni écrit dans les logs.

## Validation

- tests unitaires de classification SMTP ;
- tests d'intégration du rollback et du rejeu ;
- tests Angular des messages `422` et `503` ;
- validation complète par GitHub Actions attendue sur la PR du correctif.
