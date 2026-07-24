# FUNCTIONAL-SPEC — Redirection et gestion d'expiration de session utilisateur

## 1. Objectif Fonctionnel

Garantir qu'à l'expiration d'une session JWT ou en cas d'invalidation de jeton (révocation, déconnexion, cookie expiré), l'utilisateur soit instantanément et proprement déconnecté, et redirigé vers l'écran de connexion avec une notification expliquant l'expiration de la session.

## 2. Rôles et Comportements Attendus

- **Tout utilisateur (Médecin, Admin, Infirmier, Accueil, Patient)** :
  - Dès qu'une requête HTTP vers l'API échoue pour cause de session expirée/invalide (401/403) et que le rafraîchissement échoue, l'application purge les informations de session du navigateur.
  - L'application redirige immédiatement vers la page `/` (formulaire de connexion).
  - L'écran de connexion affiche le message : *"Votre session a expiré. Veuillez vous re-connecter."*
  - L'URL de provenance (`returnUrl`) est conservée afin de rediriger l'utilisateur vers son écran d'origine après ré-authentification.

## 3. Critères d'Acceptation

- [x] L'utilisateur n'est jamais laissé bloqué sur un écran avec des menus disparus.
- [x] Une requête HTTP protégée émise sans session active déclenche la redirection vers la page de connexion.
- [x] Un rafraîchissement rejeté (401/403) déclenche systématiquement la purge de session et la redirection vers `/`.
