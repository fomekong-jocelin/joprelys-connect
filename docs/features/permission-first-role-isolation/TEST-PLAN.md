# TEST-PLAN — Cloisonnement permission-first de tous les rôles

## P0 automatisé

- cache médecin invalidé à la connexion patient ;
- cache caissier invalidé à la connexion infirmier ;
- réponse RBAC d'une ancienne session ignorée ;
- menu patient exclusif malgré des permissions résiduelles ;
- route professionnelle refusée au patient avant tout appel RBAC ;
- route permissionnée refusée si seul le rôle correspond ;
- disponibilités refusées au patient, au claim mixte et aux rôles sans `AVAILABILITY_MANAGE` ;
- disponibilités autorisées au médecin et à l'administrateur clinique via l'authority.
- déconnexion : stockages, cookie accessible, patient actif et cache RBAC purgés ;
- changement médecin → caissier : aucune donnée locale du médecin ne subsiste ;
- logout, logout-all et login patient : cookie expiré et en-tête `Clear-Site-Data` présent.

## Matrice EPIC-0026

Pour chaque permission : rôle système autorisé, rôle personnalisé autorisé, rôle voisin refusé, patient refusé, autre tenant refusé, ressource d'un autre propriétaire refusée. Les tests couvrent menu, navigation directe et HTTP.

Contrôles statiques obligatoires :

- aucun `hasRole` ou `hasAnyRole` dans une annotation `@PreAuthorize` de contrôleur ;
- aucun identifiant de rôle professionnel dans `expectedRoles` côté Angular ;
- chaque route professionnelle sensible déclare `expectedPermissions` ;
- les seules authorities patient sont injectées par la branche JWT patient exclusive.
- aucune permission effective n'est ajoutée ou déduite côté Angular ;
- chaque entrée de navigation professionnelle est compatible avec la politique de sa route ;
- `LAB_ORDER_READ` seul n'affiche pas la file laboratoire globale ;
- un écran composite ne déclenche aucun appel caisse, facturation ou laboratoire sans la permission exacte.

## Critères de sortie

- 100 % des chemins AuthZ modifiés ont un test positif et négatif.
- Suites Angular et Maven vertes.
- E2E multi-rôles vert en environnement de recette.
- Revue RSSI sans finding critique ou élevé non traité.

## Résultats du 2026-07-18

- Maven : **453 tests**, 0 échec, 0 erreur, 1 ignoré.
- Angular : **267 tests**, 0 échec.
- Build production Angular : vert.
- Contrôle i18n : 47 clés du shell présentes en français et en anglais.
- Cas ajoutés : médecin sans finance, rôle personnalisé à permission minimale, file
  laboratoire seule, historique caisse seul, lecture de créances sans relance, dashboard
  comptable sans export/historique et actions financières masquées sans authority.
- Contrôles statiques permission-first : verts.
- À signer hors automatisation : recette E2E multi-rôles et revue RSSI/métiers.
