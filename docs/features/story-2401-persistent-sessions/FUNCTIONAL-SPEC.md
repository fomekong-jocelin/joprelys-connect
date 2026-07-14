# STORY-2401 — Spécification fonctionnelle des sessions persistantes

## Besoin

Un professionnel de santé authentifié doit pouvoir poursuivre son travail sans ressaisir fréquemment ses identifiants, tout en utilisant un access token court et révocable indirectement par une session persistante.

## Acteurs

- professionnel de santé ;
- personnel administratif ;
- administrateur clinique ou plateforme ;
- backend d’authentification.

## Parcours nominal

1. L’utilisateur saisit ses identifiants.
2. Les rôles sensibles terminent le contrôle OTP existant.
3. Le backend crée une famille de session persistante.
4. Le backend retourne un access token court dans le JSON.
5. Le backend place le refresh token opaque dans un cookie HttpOnly.
6. Lorsque l’access token expire, le client appelle `/api/auth/refresh`.
7. Le backend vérifie le hash, l’état de l’utilisateur, l’organisation et les expirations.
8. Le backend révoque l’ancienne génération et crée la suivante dans une transaction unique.
9. Un nouvel access token et un nouveau cookie sont émis.

## Règles fonctionnelles

- Une session appartient à un utilisateur et, lorsqu’il existe, à son établissement.
- Une organisation inactive interdit tout nouveau login et tout renouvellement.
- Un utilisateur désactivé ne peut pas renouveler.
- La session possède une expiration absolue non prolongeable.
- L’expiration d’inactivité est prolongée à chaque rotation, sans dépasser l’expiration absolue.
- Une génération de refresh token ne peut réussir qu’une seule fois.
- Le refresh token brut n’est jamais affiché, retourné dans le JSON ou journalisé.
- Un refresh refusé retourne une erreur générique et supprime le cookie côté client.
- Les tokens JWT historiques sans identifiant de session restent acceptables pendant la transition, mais ne sont pas renouvelables.

## Compatibilité

Le JSON de login conserve les champs existants :

- `accessToken` ;
- `tokenType` ;
- `expiresAt` ;
- `email` ;
- `name` ;
- `role` ;
- `requiresOtp` ;
- `otpCode` uniquement dans les environnements explicitement autorisés.

Il ajoute :

- `sessionExpiresAt` ;
- `sessionId`.

Le refresh token n’est pas un champ JSON.

## Erreurs fonctionnelles

Les causes suivantes produisent une réponse 401 générique sans révéler l’existence d’un compte ou d’une session :

- cookie absent ;
- token inconnu ;
- token déjà consommé ;
- session révoquée ;
- expiration d’inactivité ;
- expiration absolue ;
- utilisateur désactivé ;
- organisation inactive ;
- incohérence utilisateur/organisation.

## Hors périmètre

- liste des sessions ;
- révocation d’une autre session ;
- logout-all ;
- révocation de famille après rejeu ;
- interface Angular des sessions ;
- authentification patient ;
- envoi OTP externe.

## Critères d’acceptation

- login et vérification OTP créent une session persistante ;
- le cookie respecte les attributs de sécurité configurés ;
- la rotation est atomique ;
- le token précédent ne peut plus être réutilisé ;
- les expirations sont appliquées côté serveur ;
- la session survit au redémarrage du backend ;
- aucune donnée secrète n’est exposée ;
- H2 et PostgreSQL 16 sont validés.