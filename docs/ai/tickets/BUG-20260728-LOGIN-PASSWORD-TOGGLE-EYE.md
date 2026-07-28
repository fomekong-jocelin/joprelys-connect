# BUG-20260728-LOGIN-PASSWORD-TOGGLE-EYE — Bouton d'affichage/masquage du mot de passe sur la page de connexion

## Statut
- **Statut** : DONE
- **Priorité** : P1
- **Mode** : Engineering (Angular Frontend)
- **Assigné** : Antigravity
- **Date de création** : 2026-07-28
- **Date de résolution** : 2026-07-28

## Description
Sur la page de connexion (`/login`), le champ mot de passe ne disposait pas d'un bouton (icône d'œil) permettant à l'utilisateur de vérifier son mot de passe en basculant la visibilité entre le mode masque (`password`) et le mode texte clair (`text`).

## Objectif
Ajouter une icône d'œil interactive accessible, traduite (FR/EN) et compatible avec les thèmes clair et sombre (light/dark) au sein du champ mot de passe de la page de connexion.

## Fichiers modifiés
- `docs/features/premium-login/FUNCTIONAL-SPEC.md`
- `docs/features/premium-login/TECHNICAL-DESIGN.md`
- `web/src/assets/i18n/fr.json`
- `web/src/assets/i18n/en.json`
- `web/src/app/auth/login.component.ts`
- `web/src/app/auth/login.component.html`
- `web/src/app/auth/login.component.css`
- `web/src/app/auth/login.component.spec.ts`
- `docs/ai/PROJECT-TRACKING.md`
- `docs/ai/CHANGELOG.md`

## Actions réalisées
- [x] Créer le ticket `BUG-20260728-LOGIN-PASSWORD-TOGGLE-EYE.md`
- [x] Mettre à jour la documentation fonctionnelle et technique dans `docs/features/premium-login/`
- [x] Ajouter les clés i18n FR/EN pour `showPassword` et `hidePassword`
- [x] Implémenter le signal `showPassword` et la méthode `toggleShowPassword()` dans `LoginComponent`
- [x] Mettre à jour `login.component.html` avec le bouton d'action et les icônes SVG d'œil (ouvert / barré)
- [x] Ajouter le style CSS dans `login.component.css` avec positionnement propre et `padding-right` adapté
- [x] Ajouter les tests unitaires dans `login.component.spec.ts`
- [x] Valider les tests et la conformité aux standards UI et d'accessibilité (WCAG AA, focus visible, aria-label dynamique)
- [x] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`

## Suivi et validation
- [x] Build Angular OK
- [x] Tests unitaires Angular OK (test de bascule du mot de passe vert)
