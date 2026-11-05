# UX-20261105-B — Écrans d'administration clinique compréhensibles (epic)

Mode : Engineering (UX) + Project Manager (découpage). Suite de UX-20261105-HOSPITAL-ORGANIZATION-CLARITY.
Demande utilisateur du 2026-11-05 :
- « Lorsque tu crées un service, tu ne peux plus créer les pôles et autres éléments. »
- « Tu dois faire pareil pour le suivi des lits, configurer chambres, rôles et permissions. »

Décision utilisateur (S1) : vue globale et nouvel endpoint, plutôt qu'une liste déroulante par chambre.

Statut : IN_REVIEW. S0 à S3 sont terminées : suite web complète 839/839 verte, `ng build` OK. Les recettes Maestro sont écrites mais non lancées. Rien n'est commité.

## Stories

| Story | Écran | SP | Taille | Statut |
|---|---|---:|---|---|
| S0 | Organisation hospitalière : menu « Ajouter une unité » toujours disponible | 1 | XS | Terminé |
| S1 | Suivi des lits : vue globale et `GET /api/spatial/inpatient-occupancy` | 5 | L | Terminé (recette Maestro non lancée) |
| S2 | Configurer chambres et lits : vocabulaire, arbre des lieux, découpage | 5 | L | Terminé (recette Maestro non lancée) |
| S3 | Rôles et autorisations : permissions lisibles, recherche, découpage | 5 | L | Terminé (recette Maestro non lancée) |

## Estimation

| Élément | Valeur |
|---|---|
| Story points | 16 (1 + 5 + 5 + 5) |
| Taille | XL : découpé en 4 stories livrables séparément |
| Profil recommandé | Intermédiaire, avec une revue senior pour S1 (API) et S3 (sécurité) |
| Effort senior | 4 à 5 j |
| Effort intermédiaire | 6 à 7 j |
| Risque | Moyen : S1 ajoute un endpoint ; S3 touche l'écran qui attribue les droits |
| Raison | 3 écrans de 300 à 400 lignes à découper, un endpoint, i18n FR/EN, tests et recettes Maestro |

Capacité : demande hors sprint, traitée au fil de l'eau. Aucune capacité d'équipe n'est engagée. Chaque story est livrable seule (bump PATCH).

## S0 — Ajouter n'importe quel niveau après le premier service
Avant, quand un service existait, seul « Ajouter un service » restait en tête de page. On ne pouvait donc plus créer de pôle ni de département.
- [x] Composant partagé `shared/ui/menu-button` : menu accessible avec `aria-expanded`, flèches, Échap qui rend le focus, fermeture au clic extérieur. 4 tests.
- [x] En-tête : « Ajouter une unité » propose les 4 niveaux, chacun avec sa description.
- [x] Après la création d'un pôle ou d'un département alors que des services existent, un message explique comment y ranger un service (« Modifier » › « Rattaché à »).
- [x] Tests de la page (2 ajoutés), recette Maestro mise à jour.

## S1 — Suivi des lits en vue globale
Diagnostic de l'écran d'avant :
- Il fallait choisir une chambre dans une liste déroulante pour voir ses lits, sans jamais avoir de vue d'ensemble.
- Six compteurs abstraits (installés, ouverts, prêts…) étaient en majuscules de 10 px.
- Chaque lit portait trois badges (capacité, préparation, occupation) et jusqu'à trois boutons de 10 px.
- Les surtitres étaient décoratifs, un lit occupé était rouge (couleur d'alerte) et le serveur renvoyait des messages bruts.

Acceptation :
- Une seule page montre toutes les chambres d'hospitalisation actives, chacune avec ses lits.
- La synthèse dit « N lits libres sur M lits ouverts · occupation X % », accordé au nombre en FR et en EN.
- Chaque lit a un seul état lisible : Libre, Occupé, En nettoyage, En maintenance, Fermé, ou Indisponible si le serveur le dit.
- Les filtres par état servent aussi de légende et de compteurs. Un second appui revient à « Tous ».
- Actions sur un lit libre : menu « Changer l'état ». Pour un lit en nettoyage ou en maintenance : bouton direct pour terminer. Pour un lit fermé : « Rouvrir le lit ». Aucune action sur un lit occupé.
- La fermeture d'un lit est toujours confirmée.
- Une seule action à la fois. Après l'action, la liste est rafraîchie sans squelette et le changement est annoncé aux lecteurs d'écran.
- Erreurs traduites : en cas de conflit (409), la liste est rafraîchie ; un refus (403) a son propre message.
- La page gère les états de chargement (squelette), vide (lien de configuration), erreur avec « Réessayer », et propose « Actualiser » avec l'heure de mise à jour.

Actions :
- [x] Backend : contrat `InpatientOccupancyQuery`, implémentation `DefaultInpatientOccupancyQuery` et endpoint `GET /api/spatial/inpatient-occupancy` (`HOSPITALIZATION_READ`). Le travail se fait en 3 requêtes (chambres, lits, affectations actives) au lieu d'une par chambre. Le contrôleur ne fait que déléguer.
- [x] Backend : `BedRepository.findBySpaceIdInOrderByBedNumberAsc`.
- [x] Tests backend : un test d'intégration (Testcontainers) vérifie que seules les chambres de l'établissement sont renvoyées, sans salle non hospitalière ni chambre d'un autre établissement, avec les lits triés et les compteurs. Un test d'autorisation complète. 13 + 8 tests verts.
- [x] Web : `bed-display-state.ts` (état et actions, fonctions pures), `bed-tile.component`, page réécrite (TS 224 lignes, gabarit séparé de 124), méthode `SpatialApiService.listInpatientOccupancy()`.
- [x] Traductions FR/EN `spatial.tracking.*`, `spatial.bedState.*`. 32 clés mortes supprimées (ancien écran et ancien modèle « ward »).
- [x] Tests web : état des lits (9), tuile (5), page (15). Dossier spatial, organisation et menu : 66/66. `ng build` OK.
- [ ] Recette Maestro `.maestro/web/bed-tracking.yaml` : écrite, non lancée. Il faut démarrer la pile et fournir le mot de passe.
- [ ] Contrôle visuel mobile 375 px et thème sombre.

## S2 — Configurer chambres et lits
Diagnostic de l'écran d'avant :
- Il s'appelait « Configuration des chambres et des lits », mais son sous-titre parlait de géographie et de cartes « Géographie », « Espaces » et « Utilisation des espaces », avec des surtitres.
- Les énumérations étaient brutes (`SITE`, `OPEN · READY · UNASSIGNED`, `INTENSIVE CARE`) et les libellés « Code » étaient codés en dur.
- Les boutons n'avaient qu'une icône (une clé à molette pour « Modifier »).
- Les rattachements étaient listés à part sous la forme « unité ↔ espace … → ∞ ».
- Une quarantaine de textes FR servaient de valeur par défaut, sans traduction EN.
- Les erreurs du serveur étaient affichées derrière la fenêtre.

- [x] Titre « Chambres et lits » et sous-titre qui dit le but. Plus de surtitres.
- [x] Arbre des lieux avec les salles imbriquées. Pour chaque salle : services en cours (période lisible) et lits avec leur état.
- [x] Énumérations traduites : types de lieux expliqués, niveaux de confort, états des lits.
- [x] Actions nommées. Désactiver ou supprimer passe par la fenêtre, avec confirmation en deux temps ; un lit occupé n'est pas supprimable.
- [x] Code interne proposé à partir du nom. Validation après un essai d'envoi. Le type d'une chambre avec lits est verrouillé.
- [x] Erreurs dans la fenêtre : message traduit, plus la précision du serveur quand l'écran est en français.
- [x] Découpage : page 291 lignes de TS et 165 de gabarit ; fenêtre 254 de TS et 247 de gabarit ; carte de salle 122 ; arbre 74.
- [x] 96 clés FR/EN ajoutées ou réécrites dans le fichier de la fonctionnalité. Les clés de l'ancien modèle « ward » sont supprimées et les doublons du dictionnaire principal sont fusionnés.
- [x] Tests : arbre (5), fenêtre (8), page (10). Dossiers spatial, organisation et menu : 85/85. `ng build` OK.
- [ ] Recette Maestro `.maestro/web/rooms-and-beds.yaml` : écrite, non lancée.

## S3 — Rôles et autorisations
Diagnostic de l'écran d'avant :
- Surtitres décoratifs et guide « 1. Choisir une base / 2. Cocher les droits / 3. Affecter le rôle » codé en dur en français.
- Autorisations affichées sous forme de codes (`CASH_PAYMENT_COLLECT`) dans les puces et dans la matrice.
- Pas de recherche de personne ; résumé des rôles par code ; deux badges « Lecture seule » pour un rôle système.
- Historique avec actions et cibles brutes (`ROLE_PERMISSIONS_REPLACED`, `USER`).
- Composant de 413 lignes, gabarit de 381 lignes et CSS de 283 lignes ; textes FR en valeur par défaut, sans EN.

- [x] Trois onglets : Personnes, Rôles, Historique (motif ARIA « tabs », flèches gauche/droite, cibles ≥ 44 px).
- [x] Personnes : recherche par nom ou e-mail sans accents, noms des rôles, enregistrement actif seulement après une modification, au moins un rôle, autorisations effectives par nom et par domaine traduit (repliable), compte connecté en lecture seule expliqué.
- [x] Rôles : rôle système = modèle en lecture seule (un seul indicateur), « Dupliquer ce rôle » ou « Créer un rôle vide » ; code proposé à partir du nom (même règle que le serveur), validation inline ; autorisations par domaine repliable avec compteur, sans code ; Entrée enregistre.
- [x] Historique : actions traduites (une action inconnue reste lisible), dates `dd/MM/yyyy HH:mm`.
- [x] Erreurs traduites (403 dédié) + précision du serveur en français ; la saisie est conservée en cas d'échec.
- [x] Découpage : page 244 lignes de TS et 62 de gabarit ; Personnes 77 + 126 ; Rôles 145 + 150 ; Historique 61 ; fonctions pures 54. CSS dédiée supprimée (Tailwind v4 et jetons).
- [x] 86 clés FR/EN dans `assets/i18n/features/rbac/`, chargées par `I18nService`. `rbac.users.selfReadOnly` est déplacée depuis `ux-hardening`. L'écran Personnel profite des libellés `rbac.roles.system` et `custom`, désormais traduits.
- [x] Tests : fonctions pures (5), Personnes (6), Rôles (6), Historique (3), page (9), plus le service existant : 34/34. Données fictives dans `src/testing/rbac-fixtures.ts`.
- [ ] Recette Maestro `.maestro/web/rbac.yaml` : écrite, non lancée (aucun enregistrement : recherche, lecture seule, duplication, historique).
- [ ] Contrôle visuel mobile 375 px et thème sombre.

## Impacts / risques
- S1 ajoute un endpoint en lecture seule, de même permission que `GET /spaces/{id}/occupancy`. L'isolement entre établissements passe par `HospitalLocationConfigurationService.listSpaces`, qui fixe le tenant, puis par `@TenantId` sur les lits.
- Aucune migration, aucune règle métier modifiée. Le serveur reste maître des états et des transitions ; l'écran ne fait que proposer les actions permises.
- Les endpoints `GET /inpatient-spaces` et `GET /spaces/{id}/occupancy` sont conservés pour leurs autres usages.
- Version : MINOR candidat (nouvel endpoint rétrocompatible), sans bump ni release tant que l'utilisateur ne le demande pas.
