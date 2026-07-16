# Conception technique — Fiabilité de la création des cliniques

## Backend

- Ajouter `logoPath` au DTO de création et le recopier vers `OrganizationEntity`.
- Déplacer la création d'admin dans un service applicatif `@Transactional`.
- Transformer l'échec SMTP en exception métier, traduite en HTTP `503 Service Unavailable` par le gestionnaire global.
- Utiliser `mail.joprelys.com:465` : le MX du domaine et un test d'authentification confirment ce serveur.
- Exposer `joprelys.storage.upload-dir` via `JOPRELYS_UPLOAD_DIR`, avec un chemin local par défaut.

## Frontend

- Lire le format d'erreur normalisé `{ error: { code, message, traceId } }` avant le repli générique.
- Afficher explicitement l'erreur de téléversement au lieu d'un simple `console.error`.

## Sécurité et exploitation

- Le mot de passe temporaire reste absent des réponses et logs.
- La transaction annule la création si la remise du secret échoue.
- En production, `JOPRELYS_UPLOAD_DIR` doit pointer vers un volume persistant hors du répertoire remplacé au déploiement.

## SemVer

PATCH : correction de fiabilité sans rupture de contrat pour les consommateurs existants.
