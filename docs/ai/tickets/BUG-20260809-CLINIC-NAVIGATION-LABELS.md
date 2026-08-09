# BUG-20260809 — Liens de navigation clinique ambigus

## Statut

CORRIGÉ — les liens sont regroupés par intention et nommés selon l’action attendue.

## Problème

Les libellés « Organisation hospitalière », « Occupation des lits » et « Structure hospitalière » ne permettaient pas de distinguer clairement la gestion des unités de soins, le suivi des lits et la configuration des chambres.

## Correction

- rubrique « Établissement » : `Services & unités de soins` ;
- rubrique « Capacité d’accueil » : `Suivi des lits` et `Configurer chambres & lits` ;
- traductions FR/EN ajoutées pour le lien d’organisation hospitalière et les rubriques ;
- titres et sous-titres des pages alignés sur ces intentions.

## Vérifications

- test de navigation ajouté pour l’ordre, les rubriques et les routes ;
- build Angular et suite de tests à exécuter après modification.
