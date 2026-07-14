# STORY-2401 — Plan de tests

## Tests unitaires

### Génération et hash

- deux tokens générés sont différents ;
- entropie minimale de 32 octets avant encodage ;
- encodage Base64 URL sans caractères interdits ;
- hash SHA-256 stable sur la même valeur ;
- hash différent pour deux tokens ;
- aucune méthode de représentation ne révèle le token brut.

### Politique d’expiration

- calcul de l’expiration absolue ;
- calcul de l’expiration d’inactivité ;
- plafonnement de l’inactivité par l’expiration absolue ;
- refus après expiration absolue ;
- refus après expiration d’inactivité ;
- session active juste avant les limites.

### Métadonnées

- user-agent tronqué ;
- caractères de contrôle supprimés ;
- IPv4 masquée ;
- IPv6 masquée ;
- valeur absente acceptée.

## Tests du use case

### Émission

- login standard crée une session et un JWT avec `sid` ;
- validation OTP crée la session uniquement après succès ;
- organisation inactive refusée ;
- utilisateur désactivé refusé ;
- organisation nulle supportée pour compte plateforme.

### Rotation

- token actif produit une nouvelle génération ;
- ancienne génération marquée `ROTATED` ;
- même famille conservée ;
- expiration absolue inchangée ;
- inactivité prolongée sans dépasser l’absolue ;
- nouveau JWT porte le nouveau `sid` ;
- token inconnu refusé ;
- token déjà consommé refusé ;
- token expiré refusé ;
- utilisateur désactivé refusé ;
- organisation inactive refusée ;
- incohérence tenant refusée.

## Tests HTTP

- login retourne les champs historiques et les nouveaux champs ;
- cookie `HttpOnly`, chemin et SameSite présents ;
- cookie `Secure` selon profil ;
- challenge OTP ne pose aucun cookie ;
- `/api/auth/refresh` est accessible sans Bearer mais exige le cookie ;
- refresh réussi remplace le cookie ;
- refresh refusé retourne 401 et expire le cookie ;
- aucun token brut dans le JSON.

## Concurrence

Deux transactions présentent exactement le même refresh token :

- une seule retourne 200 ;
- l’autre retourne 401 ;
- une seule nouvelle génération active existe ;
- l’ancienne possède un unique `replaced_by_session_id` ;
- aucune contrainte unique ou erreur 500 n’est exposée.

## Persistance et redémarrage

- créer une session ;
- vider/reconstruire le contexte de service ;
- renouveler depuis la ligne persistée ;
- vérifier que le mécanisme ne dépend d’aucune map mémoire.

## Base de données

### H2

- Flyway V1 à V66 ;
- contraintes et index créés ;
- mapping JPA validé.

### PostgreSQL 16

- migration complète via Testcontainers ;
- insert/rotation sous verrou ;
- concurrence réelle ;
- contraintes de dates et d’unicité.

## Non-régression

- login sans OTP ;
- login avec OTP ;
- logout JWT historique ;
- tokens patients sans `sid` ;
- parsing des anciens JWT sans `sid` ;
- RBAC et tenant après création du nouveau JWT.

## Sécurité

- rechercher le token brut dans les logs capturés ;
- vérifier la base : uniquement hash 64 caractères ;
- vérifier que l’erreur ne révèle pas l’état du compte ;
- vérifier que l’organisation vient du compte/session et non d’une entrée client ;
- vérifier les bornes des métadonnées.

## Commandes de validation

```bash
cd backend
./mvnw test
./mvnw clean verify -B -Dspring.profiles.active=test
```

La story n’est pas terminée si H2 seulement est vert : le test PostgreSQL 16 est obligatoire.