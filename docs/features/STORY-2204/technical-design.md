# STORY-2204 — Conception technique

## Backend
- Étendre `InsuranceBordereauStatus` sans supprimer `PAID` pour la compatibilité historique.
- Migrer `insurance_bordereaux.total_amount` vers `NUMERIC(19,4)`.
- Ajouter `accepted_amount`, `paid_amount`, `insurer_reference`, `payment_reference`, `rejection_reason` et les horodatages de progression.
- Exposer des commandes dédiées : `receive`, `accept`, `reject`, `pay`.
- Centraliser les transitions dans `InsuranceBordereauService`.
- Appliquer les règlements cumulés sur les créances assurance dans un ordre déterministe, sans dépasser leur solde.
- Toutes les recherches d'un bordereau sont tenantées par `organizationId`.

## Frontend
- Remplacer le composant inline par des fichiers `.ts`, `.html` et `.css` séparés.
- Utiliser des modèles typés, aucun `any` pour les bordereaux.
- Afficher une synthèse KPI et une liste responsive.
- Piloter les actions à partir du statut retourné par le backend.
- Ne réaliser aucun calcul monétaire métier : uniquement des différences de présentation à partir des montants backend.

## Sécurité
- Lecture et progression : `SECRETAIRE_COMPTABLE`, `DAF`, `ADMIN_CLINIQUE`.
- Acceptation, rejet et règlement : `DAF`, `ADMIN_CLINIQUE`.
- L'organisation n'est jamais fournie par le client.
- Les DTO ne contiennent aucune donnée médicale.

## Compatibilité
- `PAID` reste accepté en lecture et est normalisé visuellement comme soldé.
- Les endpoints existants `generate`, `list`, `details`, `send`, `pay` sont conservés.
- `pay` accepte désormais des paiements partiels et retourne le cumul.
