# FIX-20260730 — Alignement visuel mobile auth / web

## Mode d’intervention

Engineering UI Flutter, rattaché à `MOB-2805` et à `EPIC-0028`.

## Constat

La recette Android montre deux écarts visibles avec la connexion web Joprelys :

- la marque, le titre et le formulaire sont regroupés dans une composition haute et peu lisible ;
- les contrôles de langue/thème occupent trop d’espace et ne reprennent pas la hiérarchie mobile-first du web ;
- les champs utilisent des labels flottants alors que le web présente des libellés stables au-dessus des champs ;
- l’écran connecté expose un identifiant technique `MOB-2805`, des réglages dupliqués et une action de déconnexion rouge surdimensionnée ;
- la palette est correcte, mais la composition ne traduit pas le niveau de finition premium attendu.

## Objectif

Aligner l’authentification Flutter sur les intentions visuelles de la connexion web sans modifier les contrats backend, la gestion de session, la biométrie, les guards ou les permissions.

## Critères d’acceptation

- [x] Le thème est accessible par un contrôle compact à gauche et la langue par un segment `FR | EN` à droite.
- [x] Le logo officiel et le nom `Connect` forment un repère de marque lisible dans les thèmes light et dark.
- [x] Le titre et le sous-titre sont placés hors de la carte du formulaire.
- [x] La carte contient uniquement le contenu actionnable du parcours courant.
- [x] Les champs e-mail et mot de passe ont des libellés fixes, une icône et un état de focus clair.
- [x] La CTA principale mesure au moins 44 px et reste pleine largeur.
- [x] L’écran connecté n’affiche plus d’identifiant de ticket ni de réglages langue/thème dupliqués.
- [x] L’identité, le rôle, la biométrie et la déconnexion sont hiérarchisés dans des surfaces compactes.
- [x] La déconnexion est clairement destructive sans devenir l’élément dominant de la page.
- [x] Les textes visibles existent en français et en anglais.
- [x] Les rayons restent compris entre 4 et 8 px et les ombres restent sobres.
- [x] Le parcours login, OTP, unlock et recovery conserve son comportement.
- [x] L’analyse statique et les 53 tests Flutter sont verts.
- [ ] Le build APK debug est vert sur un runner disposant du cache Gradle ou du réseau.

## Impacts

- Flutter présentation : auth shell, préférences, login et page de fondation.
- Design system : champ à libellé externe et bouton destructif secondaire réutilisables.
- i18n : libellés de l’accueil professionnel et de la sécurité biométrique.
- Aucun impact API, DB, backend, RBAC, cookie, stockage sécurisé ou CI/CD.

## Sécurité / régression

- Le mot de passe et l’OTP restent uniquement en mémoire.
- Aucun token, cookie ou PII supplémentaire n’est affiché ou journalisé.
- Le backend reste maître de l’authentification et du rôle.
- Les actions existantes du `AuthController` sont réutilisées sans duplication métier.

## Estimation / planning

- Estimation : 1 SP, 0,5 à 1 jour senior Flutter.
- Profil : Senior Flutter + QA visuelle mobile.
- Reviewer : Tech Lead Flutter + UX + sécurité mobile.
- Sprint : correctif de recette rattaché à MOB-2805, sans élargissement de l’EPIC.

## Action plan

- [x] Analyser les captures de recette et le design web.
- [x] Vérifier `DESIGN.md`, les tokens Flutter, l’i18n et les composants partagés.
- [x] Documenter le comportement attendu.
- [x] Recomposer le shell d’authentification.
- [x] Aligner les champs et actions sur les primitives partagées.
- [x] Recomposer l’accueil professionnel.
- [x] Ajouter ou adapter les tests widget.
- [x] Exécuter format, analyse, tests et build web.
- [x] Vérifier light/dark, FR/EN et le viewport de référence `393 × 852`.
- [x] Produire les goldens et le rapport `design-qa.md`.
- [x] Mettre à jour changelog et suivi central.
- [ ] Exécuter le build APK debug sur CI ou sur un poste autorisé à utiliser Gradle.
- [ ] Réaliser la recette Android réelle à 360/390 px, texte agrandi et clavier ouvert.

## Reste à faire

Build APK debug, gate exact-HEAD puis recette sur appareil Android avec backend de
recette. Le sandbox local ne peut pas télécharger Gradle 8.14 et ne peut pas
verrouiller le cache Gradle utilisateur existant.

## Impact SemVer

PATCH mobile rétrocompatible : correction UI sans changement de contrat.
