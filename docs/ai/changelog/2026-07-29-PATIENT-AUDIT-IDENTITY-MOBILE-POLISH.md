# 2026-07-29 — Patient audit + identity mobile polish

Issue #229 / PR #230.

## Journal d’audit

- remplacement de la timeline verticale par des cartes indépendantes ;
- chaque événement est replié au chargement et peut être ouvert/refermé sans modifier les autres ;
- résumé mobile : statut, motif/action, acteur et horodatage ;
- détails à l’ouverture : action technique, IP, ressource et user-agent disponible ;
- format de date piloté par la locale produit (`fr-FR` / `en-GB`) au lieu de la locale navigateur implicite ;
- titre de contenu redondant masqué sur mobile, la navigation patient portant déjà le nom de la section.

## Fiche d’identité

- remplacement de la mosaïque de cartes par une fiche alignée label/valeur ;
- e-mail et adresse autorisent la coupure longue sans troncature ;
- date de naissance et âge présentés en une valeur compacte ;
- groupe sanguin conservé comme donnée secondaire ;
- contact d’urgence toujours repliable.

## Contrats

- aucun changement backend, API, DB ou RBAC ;
- aucun champ patient/audit supprimé ;
- Tailwind v4 et tokens existants uniquement ;
- SemVer : PATCH.

## Validation

Tests ciblés ajoutés. Gate Angular complet requis sur le HEAD final avant fusion.
