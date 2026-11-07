# Joprelys ID en développement (Keycloak)

Fournisseur d'identité de liaison de l'écosystème (ADR-20261107-JOPRELYS-ID-KEYCLOAK). **Dev et test uniquement** : mode `start-dev`, HTTP, comptes fictifs.

## Démarrer

```bash
cp infra/keycloak/.env.example .env      # puis remplir chaque valeur (openssl rand -base64 32)
docker compose --profile eco up -d keycloak
```

- Console d'administration : http://localhost:8180/admin (identifiants de `.env`).
- Découverte OIDC : http://localhost:8180/realms/joprelys-id/.well-known/openid-configuration
- Le realm est importé au **premier** démarrage seulement. Pour le réimporter après une modification du fichier : `docker compose --profile eco down -v` (efface la base Keycloak de dev) puis relancer.

Le profil Compose `eco` garde `docker compose up` inchangé pour le reste de l'équipe.

## Contenu du realm `joprelys-id`

| Élément | Rôle |
|---|---|
| `allopharma-mobile` | Client public, authorization code + PKCE S256 obligatoire, consentement exigé, redirection `cm.allopharma.app:/oauth2redirect` |
| `allopharma-server` | Client confidentiel, client credentials uniquement ; secret = `ALLOPHARMA_SERVER_CLIENT_SECRET` |
| `joprelys-api` | Audience des jetons, aucun flux de connexion |
| Scope `eco:patient-link` | Optionnel, demandé par le mobile ; ajoute la claim `dpu` et l'audience `joprelys-api` |
| Scope `eco:consent.revoke` | Par défaut sur `allopharma-server` ; ajoute l'audience `joprelys-api` |
| Scope `basic` | Claim `sub` uniquement (aucune donnée d'identité dans le jeton d'accès) |
| `patient.demo1`, `patient.demo2` | Comptes fictifs, DPU `DPU-TEST-000123` et `DPU-TEST-000456`, mot de passe = `JOPRELYS_ID_TEST_USER_PASSWORD` |

Profil utilisateur : attributs non déclarés refusés ; `dpu` visible et modifiable par l'administrateur seulement, format `^DPU-[A-Z0-9-]+$`.

Jetons d'accès de 5 minutes ; *refresh tokens* à usage unique ; protection contre la force brute.

## Vérifications rapides

```bash
# Jeton serveur à serveur (doit contenir aud=joprelys-api et scope eco:consent.revoke)
curl -s -X POST http://localhost:8180/realms/joprelys-id/protocol/openid-connect/token \
  -d grant_type=client_credentials -d client_id=allopharma-server \
  -d "client_secret=$ALLOPHARMA_SERVER_CLIENT_SECRET"

# Le mot de passe direct est désactivé pour le mobile (doit répondre unauthorized_client)
curl -s -X POST http://localhost:8180/realms/joprelys-id/protocol/openid-connect/token \
  -d grant_type=password -d client_id=allopharma-mobile -d username=patient.demo1 -d password=x
```

Une demande d'autorisation de `allopharma-mobile` sans `code_challenge` est refusée (`Missing parameter: code_challenge_method`).

## Placeholders

Le fichier de realm utilise `${VARIABLE}` : Keycloak les remplace par les variables d'environnement à l'import. Ne jamais y écrire de valeur réelle ; ne pas ajouter de placeholder dont le nom existe déjà dans l'environnement Windows (`USERNAME`…).
