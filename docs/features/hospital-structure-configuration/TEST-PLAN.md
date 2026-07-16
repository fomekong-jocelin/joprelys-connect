# Plan de test — Structure hospitalière

## Backend

- accès refusé à un utilisateur non administrateur ;
- lecture limitée à la clinique courante ;
- création et modification de chaque niveau ;
- refus des doublons insensibles à la casse ;
- refus d'un lit au-delà de la capacité ;
- refus d'une réduction de capacité invalide ;
- refus de suppression d'un parent non vide ;
- refus de suppression d'un lit occupé ou historisé ;
- audit des opérations.

## Frontend

- route et menu visibles pour l'administrateur ;
- chargement et affichage de la hiérarchie ;
- formulaires de création et modification ;
- confirmations de suppression ;
- affichage des erreurs API ;
- traductions FR/EN et thèmes light/dark.

## Régression

- vue d'occupation existante ;
- changement de statut des lits ;
- transfert d'un patient ;
- hospitalisation et facturation des chambres.
