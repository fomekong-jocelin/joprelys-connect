# FIX-20260729 — Recherche patient progressive et espacement compact

GitHub issue : #235

## Contexte

Après la PR #234, deux ajustements restent nécessaires sur mobile :

- réduire l’espace entre le sous-titre de la page Patients et la zone de recherche ;
- déclencher la recherche au fil de la saisie sans imposer la touche Entrée.

## Décision UX

- Le `PageHeaderComponent` reçoit une option `compact` rétrocompatible ; la liste Patients l’active pour réduire le padding bas.
- Le conteneur principal de la liste utilise un `pt-2` mobile afin de limiter l’espace cumulé avec le header.
- La recherche est déclenchée automatiquement après 250 ms de pause de saisie.
- Une nouvelle saisie annule immédiatement le timer ou la requête précédente via `switchMap`, empêchant une réponse obsolète d’écraser les résultats courants.
- Entrée reste disponible comme raccourci immédiat.
- Le bouton X annule toute recherche précédente et recharge immédiatement la liste complète.

## Critères d’acceptation

- [x] Espacement du haut de page réduit sur mobile.
- [x] Recherche automatique après 250 ms.
- [x] Requête précédente annulée lors d’une nouvelle saisie.
- [x] Entrée reste compatible mais facultative.
- [x] Effacement immédiat via X.
- [x] Aucun changement backend/API/DB/RBAC.
- [ ] Tests Angular et build production verts.

## Tests ajoutés

- classes de spacing compact ;
- recherche après 250 ms sans Entrée ;
- annulation d’une requête en cours lors d’une nouvelle frappe ;
- raccourci Entrée conservé ;
- effacement de recherche conservé.

## SemVer

PATCH — correction UX rétrocompatible.
