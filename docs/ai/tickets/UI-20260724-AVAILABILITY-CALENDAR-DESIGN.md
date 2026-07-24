# UI-20260724 — Disponibilités : calendrier hebdomadaire et états compacts

## Contexte

L'écran `Mes disponibilités` présente aujourd'hui trois grandes cartes, dont plusieurs restent largement vides lorsqu'aucune donnée n'est configurée. La grille de règles repose sur sept petites cartes par jour et ne donne pas la lecture temporelle attendue d'un agenda médical.

## Objectif

- remplacer la grille de sept cartes par un calendrier hebdomadaire de type agenda (lundi → dimanche, axe horaire) ;
- afficher les plages récurrentes et les indisponibilités directement dans leur contexte temporel ;
- permettre l'ajout d'une plage depuis une zone horaire vide ;
- compacter les sections secondaires (`règles`, `indisponibilités`, `aperçu des créneaux`) sous forme de panneaux repliables ;
- conserver strictement les CRUD/API existants et les règles métier STORY-2602 ;
- respecter `DESIGN.md` : tokens CSS centraux, rayon ≤ 8 px, espacement de section 24 px, aucun nouveau code couleur dur.

## Garde-fous démo

- aucune dépendance calendrier externe ;
- aucun drag-and-drop ou resize avant la démo ;
- aucun changement backend ;
- aucune modification du contrat API ;
- édition/désactivation des plages et création/suppression des indisponibilités conservées ;
- FR/EN conservés ;
- tests Angular + build production obligatoires avant merge.
