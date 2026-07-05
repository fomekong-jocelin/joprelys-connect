# STORY-1801 — Intégration du sélecteur de Thème (Clair / Sombre) dans l'AppShell

## 1. Objectif

Permettre aux utilisateurs de basculer dynamiquement entre le mode clair (light) et le mode sombre (dark) depuis le header de l'application (`AppShellComponent`). Le choix du thème doit être persistant dans le `localStorage` en s'appuyant sur le service `ThemeService` existant.

## 2. Critères d'acceptation

- [ ] Un bouton d'action premium avec icône dynamique (Soleil/Lune) est ajouté dans le header de `AppShellComponent`.
- [ ] Le bouton appelle la méthode `toggleTheme()` ou équivalente pour basculer le thème.
- [ ] Le thème sélectionné est appliqué sur la balise `<html>` (`data-theme="light"` ou `data-theme="dark"` et la classe `.dark`) de manière instantanée sans rechargement de page.
- [ ] Le choix est sauvegardé dans le stockage local et rechargé automatiquement lors des visites suivantes.
- [ ] Aucun texte n'est codé en dur (les attributs `title` ou `aria-label` du bouton utilisent l'injectable `I18nService`).
- [ ] Les icônes de type SVG respectent la charte graphique et s'adaptent aux couleurs du thème.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0012 — Refonte UI/UX Premium |
| User story parent | N/A |
| Sprint cible | SPRINT-0009 |
| Priorité business | P1 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.15j |
| Effort estimé intermédiaire | 0.2j |
| Effort estimé junior | 0.35j |
| Responsable | Frontend Agent |
| Reviewer obligatoire | Antigravity |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `DESIGN.md` lu (standards visuels, arrondis 4px/8px max, etc.)
- [x] `ThemeService` existant dans `core/theme/theme.service.ts` identifié.
- [x] Fichier `styles.css` contenant `@custom-variant dark` et variables CSS vérifié.

## 5. Action plan

- [ ] Modifier `AppShellComponent` pour y ajouter le sélecteur de thème.
- [ ] Relier le composant au `ThemeService`.
- [ ] Ajouter les libellés de traduction i18n correspondants dans `fr.json` et `en.json`.
- [ ] Tester le basculement dynamique et la persistance.
