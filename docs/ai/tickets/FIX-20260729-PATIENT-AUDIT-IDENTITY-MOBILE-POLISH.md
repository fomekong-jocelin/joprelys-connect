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
- user-agent lorsqu’il est fourni ;
- statut et horodatage complet.

La timeline verticale décorative est supprimée.

### Fiche d’identité

La vue est organisée en groupes cohérents :

1. identité et naissance ;
2. contact ;
3. localisation ;
4. groupe sanguin / informations secondaires ;
5. contact d’urgence repliable.

Les valeurs courtes utilisent des cellules compactes. Les valeurs longues (e-mail, adresse) occupent toute la largeur utile et utilisent `overflow-wrap:anywhere` / `break-words` sans troncature sémantique.

## Contraintes

- Angular standalone + Tailwind v4 ;
- design tokens existants ;
- rayons <= 8 px ;
- aucune API/DB/permission modifiée ;
- FR/EN existants préservés ;
- aucune donnée d’audit supprimée ;
- cibles tactiles >= 44 px.

## Critères d’acceptation

- [ ] chaque audit est ouvrable/refermable indépendamment ;
- [ ] aucun rail/timeline verticale ne comprime le mobile ;
- [ ] titre, acteur et date restent lisibles sans scroll horizontal ;
- [ ] les détails techniques ne sont visibles qu’après ouverture ;
- [ ] la fiche identité ne force plus e-mail/adresse dans de petites colonnes ;
- [ ] la date de naissance et l’âge restent lisibles sans empilement artificiel ;
- [ ] aucune valeur n’est perdue ou tronquée ;
- [ ] contact d’urgence reste repliable ;
- [ ] tests Angular + build production verts.

## Tests

- audit : tous les événements fermés au chargement ;
- audit : toggle par `log.id`, sans ouvrir les autres ;
- audit : détails techniques absents fermé / présents ouvert ;
- identité : e-mail + adresse en largeur complète ;
- identité : grille mobile 1 colonne puis 2 colonnes à partir de `sm` ;
- identité : contact d’urgence toujours repliable ;
- gate frontend complet.

## SemVer

PATCH — finition UI rétrocompatible.
