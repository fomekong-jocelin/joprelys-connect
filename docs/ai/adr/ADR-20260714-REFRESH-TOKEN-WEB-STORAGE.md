# ADR — Stockage et transport du refresh token web

- Statut : Accepté pour STORY-2401
- Date : 2026-07-14
- Décideurs : Tech Lead / sécurité Joprelys
- Tickets : #29, #31, #34

## Contexte

L’application Angular conserve actuellement uniquement l’access token dans `sessionStorage`. STORY-2401 ajoute un refresh token longue durée. Exposer ce secret au JavaScript augmenterait fortement l’impact d’une faille XSS dans une application manipulant des données de santé.

Le système doit également préparer la rotation, la révocation et la détection de rejeu sans dépendre d’un fournisseur OTP payant.

## Options étudiées

### A. Refresh token dans `localStorage`

Rejetée : persistance longue et lecture directe par tout script exécuté dans l’origine. Le risque de vol par XSS est incompatible avec la sensibilité de l’application.

### B. Refresh token dans `sessionStorage`

Rejetée : la durée est plus courte que `localStorage`, mais le secret reste accessible au JavaScript et disparaît à la fermeture de l’onglet, ce qui contredit l’objectif de session persistante.

### C. Refresh token retourné dans le JSON et conservé uniquement en mémoire

Rejetée pour le web : réduit la persistance mais ne permet pas une reprise fiable après rechargement et expose encore le secret à JavaScript au moment de la réponse.

### D. Cookie `HttpOnly`, `Secure`, `SameSite=Lax`

Retenue :

- inaccessible à JavaScript ;
- automatiquement envoyé uniquement au chemin d’authentification ;
- `Secure` obligatoire en production ;
- `SameSite=Lax` empêche l’envoi sur les requêtes POST cross-site ordinaires ;
- aucun refresh token brut dans le JSON ou le stockage navigateur ;
- compatible avec une SPA servie sur la même origine que l’API.

## Décision

Pour le client web :

- cookie nommé par configuration, défaut `joprelys_refresh` ;
- `HttpOnly=true` ;
- `Secure=true` dans le profil de production ;
- `SameSite=Lax` ;
- `Path=/api/auth` ;
- absence de valeur `Domain` afin de rester host-only ;
- expiration du cookie limitée par la plus proche des expirations absolue et d’inactivité ;
- suppression du cookie lors d’un refresh refusé ou d’un logout.
- toute suppression du refresh cookie ajoute `Clear-Site-Data: "cache", "cookies", "storage"` afin de matérialiser une frontière navigateur complète ;
- Angular purge en parallèle `sessionStorage`, `localStorage`, les cookies accessibles et les états mémoire enregistrés à la déconnexion ou au changement d'identité ;
- la rotation transparente du même compte conserve les préférences et remplace uniquement l'access token ;
- le passage vers une session patient efface explicitement un éventuel refresh cookie professionnel.

L’access token reste retourné dans le JSON et envoyé en Bearer. Sa durée est courte et indépendante de celle de la session.

## Modèle serveur

- token opaque généré par `SecureRandom` avec au moins 256 bits d’entropie ;
- encodage Base64 URL sans padding ;
- stockage exclusif du SHA-256 hexadécimal ;
- comparaison par recherche exacte du hash ;
- rotation atomique sous verrou pessimiste ;
- ancienne génération conservée comme révoquée et liée à sa remplaçante ;
- aucun token brut dans les logs, audits, exceptions ou DTO persistants.

## Conséquences

### Positives

- réduction de l’exposition XSS ;
- contrat Angular plus simple dans #34 ;
- support naturel de la persistance après rechargement ;
- base adaptée à la détection de rejeu de #33.

### Contraintes

- le déploiement web doit servir l’API et Angular sur une origine compatible ou configurer correctement CORS avec credentials ;
- HTTPS obligatoire en production ;
- les tests locaux HTTP utilisent un cookie non `Secure` via le profil de test/local ;
- le client natif Flutter ne peut pas réutiliser directement ce mécanisme et nécessitera une stratégie de stockage sécurisé dédiée lorsqu’il sera intégré.
- la purge complète réinitialise volontairement le thème, la langue et la préférence de sidebar au prochain chargement ;
- si le serveur est inaccessible au moment précis du logout, JavaScript ne peut pas supprimer un cookie HttpOnly : l'état local est tout de même purgé et l'expiration serveur reste requise dès que la connectivité revient.

## Alternatives futures

- cookie `SameSite=Strict` si aucun parcours d’authentification externe n’est introduit ;
- token binding ou DPoP pour clients natifs ;
- OIDC Authorization Code + PKCE lors d’une future fédération d’identité.

## Sécurité CSRF

L’endpoint `/api/auth/refresh` ne modifie pas les données métier et le cookie `SameSite=Lax` n’est pas envoyé sur un POST cross-site normal. La réponse ne peut pas être lue par une origine non autorisée. Une politique CORS stricte et les en-têtes `Origin` restent obligatoires en production. Toute future utilisation de cookies pour des opérations métier imposera une protection CSRF explicite.

## Réversibilité

La décision est additive. Le cookie peut être remplacé par un autre transport dans une version majeure, sans changer la représentation persistante des sessions et des familles de tokens.
