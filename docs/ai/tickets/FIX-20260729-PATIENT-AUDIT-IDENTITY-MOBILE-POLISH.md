# FIX-20260729 — Refonte mobile journal d’audit + fiche d’identité

GitHub issue : #229

## Problème

La recette mobile montre deux zones encore trop denses :

- le journal d’audit patient repose sur une timeline verticale qui tasse le contenu à gauche et met la date en concurrence avec le titre ;
- la fiche d’identité utilise une grille de cartes uniformes alors que les informations n’ont pas toutes la même longueur ni la même importance.

## Objectif

Rendre ces deux vues lisibles à 360–430 px sans masquer de donnée ni modifier le backend.

## UX cible

### Journal d’audit

Chaque événement devient une carte/accordéon indépendant.

État fermé :
- statut ;
- titre ;
- acteur ;
- date/heure ;
- chevron.

État ouvert :
- action technique ;
- adresse IP ;
- identifiants de ressource disponibles ;
- user-agent lorsqu’il est fourni.

La timeline verticale décorative est supprimée. La date est formatée avec la locale produit afin d’éviter le format US observé sur la recette FR.

### Fiche d’identité

La mosaïque de cartes est remplacée par une fiche unique label/valeur à alignement constant. Ce format réduit les différences de hauteur et exploite mieux la largeur mobile.

Ordre de lecture :

1. sexe ;
2. naissance / âge ;
3. téléphone ;
4. e-mail ;
5. ville ;
6. quartier / district ;
7. adresse ;
8. groupe sanguin ;
9. contact d’urgence repliable.

Les valeurs longues utilisent `overflow-wrap:anywhere` / `break-words` sans troncature sémantique.

## Contraintes

- Angular standalone + Tailwind v4 ;
- design tokens existants ;
- rayons <= 8 px ;
- aucune API/DB/permission modifiée ;
- FR/EN existants préservés ;
- aucune donnée d’audit supprimée ;
- cibles tactiles >= 44 px.

## Critères d’acceptation

- [x] chaque audit est ouvrable/refermable indépendamment ;
- [x] aucun rail/timeline verticale ne comprime le mobile ;
- [x] titre, acteur et date utilisent un layout sans concurrence de colonnes rigides ;
- [x] les détails techniques ne sont visibles qu’après ouverture ;
- [x] la fiche identité ne force plus e-mail/adresse dans de petites cartes ;
- [x] la date de naissance et l’âge sont présentés comme une valeur compacte ;
- [x] aucune valeur n’est volontairement tronquée ;
- [x] contact d’urgence reste repliable ;
- [ ] tests Angular + build production verts.

## Tests

- [x] audit : tous les événements fermés au chargement ;
- [x] audit : toggle par `log.id`, sans ouvrir les autres ;
- [x] audit : détails techniques absents fermé / présents ouvert ;
- [x] audit : format de date lié à la locale produit ;
- [x] identité : fiche alignée unique au lieu de la mosaïque de cartes ;
- [x] identité : e-mail + adresse protégés contre les longues chaînes ;
- [x] identité : contact d’urgence toujours repliable ;
- [ ] gate frontend complet.

## SemVer

PATCH — finition UI rétrocompatible.
