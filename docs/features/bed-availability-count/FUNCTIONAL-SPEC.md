# Spécification fonctionnelle — Compteur des lits disponibles

## Problème métier

Le tableau d'occupation déduit les lits libres du nombre total moins les lits occupés. Cette formule présente à tort comme disponibles les lits en nettoyage ou en maintenance et peut orienter un utilisateur vers une capacité qui n'est pas mobilisable.

## Utilisateurs concernés

- cadre infirmier et bed manager ;
- médecins et infirmiers consultant l'occupation ;
- admissions et administrateurs habilités.

## Objectif

Afficher un nombre de lits effectivement revendicables par le workflow legacy : seuls les lits au statut `FREE` sont disponibles.

## Parcours attendu

1. L'utilisateur ouvre la gestion spatiale.
2. Il sélectionne un service.
3. Le backend retourne les lits et les compteurs consolidés.
4. La carte « lits libres » affiche `availableBedsCount`.
5. Après un changement d'état, le rechargement actualise ce compteur.

## Règles métier

- `FREE` : compté disponible ;
- `OCCUPIED` : compté occupé et non disponible ;
- `CLEANING` : non disponible ;
- `MAINTENANCE` : non disponible ;
- le total comprend tous les lits configurés ;
- le frontend ne reconstruit pas la règle de disponibilité.

## Hors périmètre

La capacité ouverte, les fermetures planifiées, les réservations, le turnover détaillé et la modification du taux d'occupation seront traités par HOS-BED-001/HOS-BED-002.

## Critères d'acceptation

- Étant donné quatre lits dans les quatre états legacy, lorsque l'occupation est consultée, alors un seul lit est disponible.
- Étant donné un lit en nettoyage ou maintenance, lorsque l'écran est affiché, alors il n'augmente jamais le compteur vert.
- Étant donné un lit repassé à `FREE`, lorsque l'écran se recharge, alors le compteur augmente d'une unité.

## Hypothèse explicitée

Dans ce correctif transitoire, `FREE` signifie « disponible ». Cette équivalence cessera d'être suffisante lorsque les axes installé/ouvert/hygiène/réservation seront introduits.
