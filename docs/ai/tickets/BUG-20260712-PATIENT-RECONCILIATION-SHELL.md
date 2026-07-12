# BUG-20260712-PATIENT-RECONCILIATION-SHELL — Shell de navigation absent

## Contexte

La route `/clinic/patient-reconciliation` affichait son contenu sans le shell clinique commun, donc sans en-tête, menu, fil d’Ariane ni navigation mobile.

## Critères d’acceptation

- [x] La page est encapsulée dans `AppShellComponent`.
- [x] Aucun menu local ou texte utilisateur supplémentaire n’est créé.
- [x] Un test de non-régression vérifie la présence du shell.
- [x] Les tests Angular et le build de production réussissent (176 tests, build réussi).
- [ ] La vérification visuelle authentifiée confirme l’en-tête et le menu.

## Analyse et impacts

- Cause : l’import et la balise `app-shell` étaient absents du composant autonome.
- Impact : interface uniquement ; aucune API, donnée, autorisation ou règle de rapprochement n’est modifiée.
- Sécurité : les guards et permissions de route restent inchangés.
- Design system : le shell partagé conserve les thèmes light/dark, la langue et les rayons sobres existants.

## Action plan

- [x] Diagnostiquer la différence avec les autres routes cliniques.
- [x] Encapsuler le contenu dans le shell partagé.
- [x] Ajouter le test de non-régression ciblé.
- [x] Exécuter les vérifications Angular.
- [ ] Vérifier visuellement l’environnement déployé après livraison du correctif.

## Décision SemVer

Correction UI rétrocompatible : PATCH lors de la prochaine livraison, sans préparation de release dans cette intervention.
