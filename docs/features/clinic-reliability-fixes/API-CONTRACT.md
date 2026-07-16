# Contrat API — Correctifs de fiabilité clinique

## `POST /api/organizations`

Le corps accepte désormais le champ optionnel `logoPath`. La réponse existante renvoie ce même chemin.

## `POST /api/organizations/{id}/admin`

- `201` : compte créé et mot de passe temporaire transmis par e-mail.
- `409` : adresse déjà utilisée.
- `503` / `MAIL_DELIVERY_UNAVAILABLE` : service e-mail temporairement indisponible ; aucun compte n'est conservé.

Le mot de passe temporaire n'est jamais renvoyé.
