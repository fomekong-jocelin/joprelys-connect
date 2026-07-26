# BUG-20260726 — Finitions i18n, thème et mobile des interfaces IA

- Type : bug UI/UX, i18n et responsive
- Priorité : P1
- Statut : IMPLEMENTED — QA automatisée verte / recette visuelle authentifiée requise
- Stack : Angular 22 / Tailwind CSS v4 / design system / i18n
- Estimation : 2 SP — 1 jour senior frontend
- Reviewer : Lead Frontend + QA clinique
- Impact version : PATCH

## Constat

La recette visuelle fournie met en évidence :

- un panneau de marque illisible en thème sombre sur la connexion ;
- un logo transformé en rectangle blanc en thème sombre ;
- des textes marketing traduits par pseudo-éléments CSS, donc hors du système i18n ;
- des libellés IA encore codés en français ;
- une modalité de saisie des constantes trop dense et peu adaptée aux petits écrans ;
- une intégration impérative de l'assistant de constantes dans la modale.

## Causes confirmées

- le logo du panneau sombre hérite de `dark:brightness-0 dark:invert` ;
- le panneau utilise `--text-inverse`, token destiné aux boutons cyan et sombre en thème dark ;
- les contenus du panneau gauche sont injectés via `content:` et `:has(...)` ;
- plusieurs composants IA utilisent des libellés français directs pour les champs cliniques et les aides ;
- l'assistant de constantes est ouvert par défaut et son template est monolithique ;
- le dashboard monte l'assistant via `createComponent` et une recherche DOM fragile.

## Actions

- [x] Analyser les captures fournies et les sources Angular avec IntelliJ.
- [x] Identifier les impacts thème, i18n, mobile, accessibilité et sécurité clinique.
- [x] Remplacer le contenu CSS du panneau de marque par du HTML réellement traduit.
- [x] Rendre le logo partagé compatible avec un fond sombre sans régression globale.
- [x] Corriger les libellés IA codés en dur et compléter FR/EN.
- [x] Réduire la charge cognitive et adapter l'assistant de constantes au mobile.
- [x] Remplacer le montage DOM impératif de l'assistant par une composition Angular déclarative.
- [x] Adapter les tests ciblés.
- [x] Stabiliser le stockage navigateur du seul environnement de test Node 25.
- [x] Exécuter i18n, tests, build et inspections IntelliJ.
- [x] Mettre à jour le suivi, le changelog et la checklist.

## Critères d'acceptation

- [x] Le panneau gauche utilise des tokens lisibles sur son fond en light et dark.
- [x] Le logo officiel n'est jamais inversé sur le fond de marque.
- [x] Le contenu de vitrine bascule réellement entre FR et EN.
- [x] Aucun nouveau texte visible n'est codé en dur dans les composants modifiés.
- [x] Sur mobile, l'assistant de constantes démarre replié et s'ouvre dans une surface bornée.
- [x] Les actions principales occupent toute la largeur utile sur mobile.
- [x] L'assistant reste dans la modale sans manipulation directe du DOM.
- [x] Aucune donnée clinique n'est enregistrée sans validation explicite.
- [x] Suite Angular complète, contrôle i18n et build production verts.
- [ ] Recette visuelle authentifiée sur les viewports et variantes du plan de test.

## Preuves de validation

- IntelliJ : aucune erreur sur les fichiers Angular modifiés.
- Angular : 81 fichiers de tests et 388 tests réussis.
- i18n shell : 49 clés présentes en français et en anglais.
- Build production : succès, bundle initial 527,82 kB sous le budget de 600 kB.
- `git diff --check` : aucune erreur d'espace.
- Node 25.9.0 reste un runtime non-LTS averti par Angular ; le bootstrap de test remplace uniquement son `localStorage` incomplet et ne participe pas au bundle applicatif.

## Risques de régression

- Authentification Personnel/Patient et OTP ;
- contraste des logos partagés dans le shell ;
- ouverture/fermeture de la modale de constantes ;
- arrêt du microphone lors du repli de l'assistant ;
- préremplissage des constantes sans sauvegarde automatique.

## Reste à faire

Recette visuelle humaine authentifiée mobile/tablette/desktop en FR/EN et light/dark.
