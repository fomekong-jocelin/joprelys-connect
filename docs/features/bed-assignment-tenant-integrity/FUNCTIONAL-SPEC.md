# Spécification fonctionnelle — Cohérence établissement des affectations de lit

## Besoin

Une affectation de lit est un fait clinique et opérationnel appartenant à un seul établissement. Son séjour et son lit doivent relever du même périmètre, même en cas d'import ou de contournement du chemin applicatif nominal.

## Comportement attendu

- Le chemin nominal refuse avant persistance un lit d'un autre établissement.
- La base refuse toute combinaison affectation/séjour/lit dont les `organization_id` diffèrent.
- Le refus applicatif retourne HTTP 409 avec un message générique.
- L'incohérence n'est jamais corrigée ou masquée automatiquement.

## Préflight

Le script `PRE-MIGRATION-CHECKS.sql` recherche séparément :

1. les affectations dont l'établissement diffère du séjour ;
2. les affectations dont l'établissement diffère du lit.

Chaque requête doit retourner zéro ligne avant V78.

## Limites

Cet incrément ne traite ni les chevauchements historiques, ni la future hiérarchie groupe/établissement/site, ni la cohérence tenant de toutes les tables legacy.
