# Plan de tests — Administration RBAC clinique

## Backend

1. Le catalogue retourne les dix rôles cliniques administrables.
2. Un non-administrateur reçoit `403` sur le catalogue.
3. Une invitation multi-rôles est normalisée et dédupliquée.
4. `ADMIN_JOPRELYS` et `PATIENT` sont rejetés.
5. Une modification inter-établissements retourne `404`.
6. Une affectation ajoutée est reconnue par un jeton déjà émis dès la requête suivante.
7. Un utilisateur désactivé ne peut plus utiliser son jeton existant.
8. Un administrateur ne peut pas modifier son propre rôle ou son propre statut.
9. Le dernier administrateur actif ne peut pas être désactivé ou rétrogradé.
10. L’annuaire consulté par un non-administrateur masque les rôles financiers et de gouvernance.
11. Les rôles sensibles déclenchent l’OTP.

## Frontend

1. Le personnel et le catalogue sont chargés ensemble.
2. Plusieurs rôles peuvent être sélectionnés.
3. L’ordre envoyé respecte l’ordre canonique du catalogue.
4. Un formulaire sans rôle est refusé.
5. Les descriptions, catégories et marqueurs sensibles sont affichés.
6. Le tableau affiche correctement plusieurs libellés.
7. Le guard utilise `/api/auth/me` pour les comptes staff.
8. Une affectation ajoutée autorise immédiatement une route.
9. Une affectation retirée redirige vers `/unauthorized`.
10. Une session refusée par le backend est supprimée et redirigée vers la connexion.
11. Les comptes patient ne sollicitent pas l’endpoint staff.

## QA visuelle

- 360 px, 768 px et 1440 px ;
- thèmes clair et sombre ;
- navigation clavier complète ;
- focus visible sur chaque carte de rôle ;
- affichage lisible de dix rôles et de plusieurs rôles par collaborateur ;
- vérification FR/EN.

## Non-régression

- connexion et OTP existants ;
- espaces médecin, infirmier, accueil, pharmacie et laboratoire ;
- poste caissier ;
- workspace facturation DAF/comptabilité ;
- isolation tenant ;
- migrations H2 et PostgreSQL 16.
