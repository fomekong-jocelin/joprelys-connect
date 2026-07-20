# STORY-20260720 — Page de connexion premium mobile-first

## Statut

QA TECHNIQUE VERTE

## Référence

- Issue GitHub : #78
- Pull request : #79
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

- [x] Le logo partagé est visible et n'est pas redessiné dans le template.
- [x] La mise en page est conçue mobile-first avec des adaptations tablette et desktop.
- [x] Le rendu desktop conserve une présence premium sans carte « Flux clinique synchronisé ».
- [x] Le thème light/dark est piloté par `ThemeService` et persiste.
- [x] Les boutons de langue affichent 🇫🇷 et 🇬🇧 avec un état actif accessible.
- [x] Les deux modes Personnel / Patient restent fonctionnels.
- [x] Les étapes OTP restent inchangées fonctionnellement.
- [x] Les erreurs et états de chargement restent visibles.
- [x] Les contrôles ont des focus visibles et des attributs ARIA appropriés.
- [x] Les tests Angular, le build production et la suite Maven sont verts.
- [ ] La recette visuelle humaine est validée sur les viewports cibles en light/dark et FR/EN.

## Impacts

- `web/angular.json`
- `web/src/app/auth/login.component.ts`
- `web/src/app/auth/login.component.html`
- `web/src/app/auth/login.component.css`
- `web/src/app/auth/login.host.css`
- `web/src/app/auth/login.component.spec.ts`
- documentation fonctionnelle, technique, tests et design system

## Risques de régression

- navigation post-login ou après OTP ;
- bascule Personnel / Patient ;
- contraste dark mode ;
- hauteur de page sur petits écrans ;
- accessibilité des contrôles langue/thème.

## Validation technique

Pipeline GitHub Actions **#862** :

- tests Angular : succès ;
- build Angular production : succès ;
- build et tests Maven stricts : succès.

Les styles premium sont chargés comme style global dédié afin de respecter le budget Angular `anyComponentStyle` de 8 kB. Le composant conserve un fichier de style hôte minimal.

## Action plan

- [x] Analyser l'écran, les tests, le logo partagé et `ThemeService`.
- [x] Documenter le comportement fonctionnel et technique.
- [x] Implémenter la structure premium mobile-first.
- [x] Brancher le thème et les langues avec drapeaux.
- [x] Adapter les tests Angular.
- [x] Exécuter la CI frontend et backend.
- [ ] Réaliser la revue responsive light/dark FR/EN.
- [ ] Fusionner après validation visuelle humaine.

## Définition de fini

- PR ouverte et CI complète verte ;
- aucune régression automatisée des flux Auth ;
- recette visuelle mobile/tablette/desktop en light/dark et FR/EN à valider ;
- documentation fonctionnelle, technique, tests et design system à jour.
