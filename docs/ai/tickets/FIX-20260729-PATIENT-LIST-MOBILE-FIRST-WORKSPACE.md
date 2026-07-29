# FIX-20260729 — Refonte mobile-first de la liste Patients

GitHub issue : #233

## Contexte

La liste Patients reste conçue comme une page de présentation alors que son usage principal est opérationnel : retrouver rapidement un patient puis ouvrir son dossier, ou démarrer une nouvelle admission.

La recette mobile réelle montre :

- un en-tête `Dossier Patient Unique (DPU)` trop volumineux ;
- un lien `Retour` redondant avec le breadcrumb ;
- un bouton `Nouvelle admission` surdimensionné ;
- une recherche avec un bouton séparé qui consomme inutilement la largeur ;
- des cartes patient trop hautes, elles-mêmes enfermées dans un conteneur blanc ;
- un bouton `Voir le dossier` plein format répété sur chaque patient.

## Objectif

Transformer la page en poste de travail mobile-first : recherche prioritaire, admission accessible mais secondaire, identification rapide du patient et ouverture du dossier en un geste.

## Périmètre inclus

- titre court `Patients` et sous-titre compact ;
- suppression du retour local du PageHeader ;
- recherche compacte avec icône, Enter et effacement rapide ;
- suppression du bouton `Rechercher` séparé ;
- repositionnement de `Nouvelle admission` près de la recherche ;
- contexte de liste / nombre de résultats ;
- cartes mobile compactes et entièrement cliquables ;
- suppression du bouton `Voir le dossier` dans les cartes mobiles ;
- conservation du tableau desktop existant ;
- FR/EN et tests de régression.

## Hors périmètre

- API, backend et base de données ;
- modification des permissions ;
- changement de tri ou pagination serveur ;
- déploiement recette/production.

## Critères d’acceptation

- [ ] Le PageHeader affiche `Patients` sans lien Retour local.
- [ ] Le sous-titre est court et orienté tâche.
- [ ] La recherche affiche `Nom, téléphone ou DPU` et accepte Enter.
- [ ] Une action accessible permet d’effacer une recherche active et recharge la liste.
- [ ] Aucun gros bouton `Rechercher` n’est rendu.
- [ ] `Nouvelle admission` est sous la recherche sur mobile et compacte à droite sur desktop.
- [ ] Hors recherche, la liste est identifiée comme `Liste des patients`.
- [ ] Pendant une recherche, le nombre de patients trouvés est affiché.
- [ ] Une carte mobile affiche nom, sexe, DPU, téléphone et ville dans une hauteur réduite.
- [ ] Toute la carte mobile ouvre le dossier ; le bouton `Voir le dossier` disparaît sur mobile.
- [ ] Les cartes mobile ne sont plus enfermées dans une grande carte parent.
- [ ] Le tableau desktop et ses identifiants non wrap sont préservés.
- [ ] Aucun changement de contrat API ou de logique admission.
- [ ] Tests Angular et build production verts.

## Estimation

1 SP — 0,5 à 1 jour senior frontend + QA responsive.

## SemVer

PATCH : amélioration UX rétrocompatible sans contrat cassé.
