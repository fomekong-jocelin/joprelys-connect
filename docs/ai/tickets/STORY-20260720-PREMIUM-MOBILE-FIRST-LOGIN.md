# STORY-20260720 — Page de connexion premium mobile-first

## Statut

IN_PROGRESS

## Référence

- Issue GitHub : #78
- Branche : `feat/78-premium-mobile-first-login`
- Type : Feature UI / Authentification
- Priorité : P1
- Estimation : 1,5 j senior frontend
- Reviewer : Lead Frontend + QA

## Besoin métier

La page de connexion unifiée Personnel / Patient doit refléter le positionnement premium de Joprelys Connect tout en conservant strictement les parcours d'authentification existants.

## Décisions validées

- conserver le vrai logo partagé `app-logo` déjà utilisé sur la page ;
- concevoir mobile-first, puis adapter tablette et desktop ;
- ajouter le changement de thème light/dark avant connexion ;
- afficher les drapeaux français et britannique pour le choix FR/EN ;
- supprimer tout bloc décoratif « Flux clinique synchronisé » ;
- ne pas inventer de nouveau mode d'authentification ;
- conserver les flux Personnel, Patient et OTP existants.

## Critères d'acceptation

- [ ] Le logo partagé est visible et n'est pas redessiné dans le template.
- [ ] La mise en page est utilisable sans débordement sur mobile étroit.
- [ ] Le rendu desktop conserve une présence premium sans carte « Flux clinique synchronisé ».
- [ ] Le thème light/dark est piloté par `ThemeService` et persiste.
- [ ] Les boutons de langue affichent 🇫🇷 et 🇬🇧 avec un état actif accessible.
- [ ] Les deux modes Personnel / Patient restent fonctionnels.
- [ ] Les étapes OTP restent inchangées fonctionnellement.
- [ ] Les erreurs et états de chargement restent visibles.
- [ ] Les contrôles ont des focus visibles et des attributs ARIA appropriés.
- [ ] Les tests Angular et le build production sont verts.

## Impacts

- `web/src/app/auth/login.component.ts`
- `web/src/app/auth/login.component.html`
- `web/src/app/auth/login.component.css`
- `web/src/app/auth/login.component.spec.ts`
- documentation fonctionnelle, technique, tests, design system, suivi et changelog

## Risques de régression

- navigation post-login ou après OTP ;
- bascule Personnel / Patient ;
- contraste dark mode ;
- hauteur de page sur petits écrans ;
- accessibilité des contrôles langue/thème.

## Action plan

- [x] Analyser l'écran, les tests, le logo partagé et `ThemeService`.
- [x] Documenter le comportement fonctionnel et technique.
- [ ] Implémenter la structure premium mobile-first.
- [ ] Brancher le thème et les langues avec drapeaux.
- [ ] Adapter les tests Angular.
- [ ] Exécuter la CI frontend et backend.
- [ ] Réaliser la revue responsive light/dark FR/EN.
- [ ] Mettre à jour le suivi et le changelog.

## Définition de fini

- PR ouverte et relue ;
- CI complète verte ;
- aucune régression des flux Auth ;
- recette visuelle mobile/tablette/desktop en light/dark et FR/EN ;
- documentation et suivi à jour.
