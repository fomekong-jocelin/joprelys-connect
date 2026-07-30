# FUNCTIONAL SPEC — MOB-2801 Fondation Flutter

## Problème

Le dossier `mobile/` existe mais affiche encore l’application compteur générée par Flutter. Il n’existe pas encore de shell applicatif Joprelys permettant de démarrer les futures fonctionnalités mobiles avec une navigation, une injection de dépendances et une CI dédiées.

## Utilisateurs concernés

Équipe de développement et QA mobile. Aucun parcours clinique métier n’est livré dans ce lot.

## Objectif

Fournir une application Flutter qui démarre proprement sur une surface de fondation neutre, avec un bootstrap, un router et une racine DI structurés, prête à recevoir les stories MOB-2802 et suivantes.

## Inclus

- démarrage applicatif Joprelys ;
- navigation centralisée ;
- provider scope racine ;
- feature `foundation` minimale ;
- configuration centrale du nom applicatif ;
- CI Flutter dédiée ;
- tests de fondation.

## Exclu

- authentification réelle ;
- dashboard ;
- patients ;
- consultation ;
- constantes ;
- design system final ;
- i18n complète ;
- appels backend ;
- audio clinique.

## Parcours attendu

1. L’application démarre.
2. Le bootstrap initialise Flutter et Riverpod.
3. Le router ouvre la route de fondation `/`.
4. Une surface neutre confirme visuellement que la fondation mobile est chargée sans présenter de fausses données cliniques.
5. Toute route métier reste absente tant que la story correspondante n’est pas livrée.

## Règles

- le backend reste maître des règles métier ;
- aucune donnée patient n’est embarquée ;
- aucune permission métier n’est simulée ;
- aucun secret n’est embarqué ;
- la fondation ne doit pas prétendre offrir une fonctionnalité non implémentée.

## Critères d’acceptation

- [ ] suppression du compteur Flutter ;
- [ ] bootstrap et app séparés ;
- [ ] router nommé centralisé ;
- [ ] DI/état racine via Riverpod ;
- [ ] surface neutre sans données fictives ;
- [ ] tests smoke/router ;
- [ ] CI Flutter format/analyze/test/build ;
- [ ] aucun impact backend/web.

## Cas limites

- deep-link vers une route non livrée : ne doit pas exposer un écran métier factice ;
- erreur de bootstrap : doit échouer explicitement plutôt que continuer dans un état incohérent ;
- changement de stack web/backend : ne doit pas lancer inutilement la CI Flutter hors modification du workflow commun.
