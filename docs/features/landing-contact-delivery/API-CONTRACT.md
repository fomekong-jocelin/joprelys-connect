# Contrat — demande de démonstration

`POST /api/public/demo-requests`, JSON public sans authentification. Chemin et réponse inchangés.

| Champ | Validation |
|---|---|
| fullName, organizationName | Obligatoires, non blancs, 255 caractères maximum |
| phone | Obligatoire, non blanc, 50 caractères maximum |
| role, city | Optionnels, 100 caractères maximum |
| email | Optionnel, email valide si renseigné, 255 caractères maximum |
| message | Optionnel, 2000 caractères maximum |
| source, locale | Optionnels, respectivement 50 et 10 caractères maximum |

201 renvoie `id`, `fullName`, `organizationName`, `status`, `createdAt`, `message`. Le succès confirme la persistance, pas la livraison SMTP. 400 utilise les erreurs de validation existantes. Une notification à `JOPRELYS_CONTACT_EMAIL` est tentée seulement après commit ; l'échec SMTP est journalisé par identifiant sans modifier la réponse ni annuler la demande.
