# STORY-2402 — Spécification fonctionnelle

## Finalité

La fonctionnalité protège les comptes du personnel lorsqu’un appareil est perdu, qu’une session paraît compromise ou qu’un utilisateur souhaite fermer toutes ses connexions.

## Acteurs

- Utilisateur authentifié : consulte et ferme ses propres sessions.
- Administrateur habilité : consulte et ferme les sessions d’un collaborateur du même établissement.
- Système : révoque une famille lorsqu’un refresh token déjà consommé est réutilisé.

## Règles fonctionnelles

### Liste personnelle

L’utilisateur voit ses sessions avec : identifiant, session courante, type de client, libellé d’appareil dérivé du user-agent, préfixe réseau masqué, création, dernière activité, expiration et état.

Aucun refresh token, hash, JTI ou adresse IP complète n’est exposé.

### Révocation d’une session

- Une session propre peut être révoquée sans permission particulière.
- Une session d’un autre utilisateur exige `AUTH_SESSION_MANAGE`.
- L’administrateur et la cible doivent appartenir au même établissement.
- Une session déjà révoquée retourne le même succès que la première révocation.
- Une session cross-tenant est traitée comme inexistante.

### Logout courant

Le logout révoque la session liée au claim `sid`, efface le cookie de refresh, demande au navigateur de purger cache/cookies/stockages et invalide immédiatement les access tokens de cette session. Le frontend purge également ses stockages et états mémoire même si la requête de logout échoue.

Pour un JWT historique sans `sid`, le JTI est persisté jusqu’à son expiration.

### Logout-all

Toutes les sessions actives du compte courant sont révoquées, y compris la session appelante. L’opération est idempotente.

Le passage d'un compte professionnel vers un compte patient efface aussi l'ancien cookie de refresh professionnel avant d'établir la session patient.

### Détection du rejeu

Lorsqu’un refresh token correspondant à une génération marquée `ROTATED` est présenté :

1. toutes les générations encore actives de la famille sont révoquées ;
2. un événement de sécurité `REFRESH_REPLAY_DETECTED` est écrit ;
3. le cookie est effacé ;
4. l’API retourne `401 AUTH_SESSION_INVALID`.

Un token inconnu, expiré ou révoqué pour une autre raison ne déclenche pas une révocation de famille.

## États

- `ACTIVE` : non révoquée et non expirée.
- `EXPIRED` : expiration absolue ou d’inactivité dépassée.
- `REVOKED` : révocation explicite, rotation, logout-all ou incident de rejeu.

## Codes d’erreur stables

- `AUTH_SESSION_INVALID` : refresh ou access token non utilisable.
- `AUTH_SESSION_NOT_FOUND` : session non visible ou inexistante.
- `ACCESS_DENIED` : permission d’administration absente.

Ces codes seront traduits en français et en anglais par #34. Le backend ne fournit pas une décision de sécurité localisée dépendant de la langue.
