# STORY-1802 — Menu Latéral (Sidebar) Rétractable pour le Back-office

## 1. Objectif

Remplacer le header classique par un layout moderne comprenant une barre latérale (Sidebar) rétractable contenant le menu de navigation et les sous-menus ordonnés selon le rôle de l'utilisateur actif (Staff clinique vs Admin vs Patient).

## 2. Critères d'acceptation

- [ ] L'IHM back-office intègre une barre latérale rétractable sur grand écran (desktop) et rétractée par défaut ou sous forme de drawer sur mobile.
- [ ] La sidebar contient des icônes premium pour chaque item de menu (ex: Tableau de bord, Patients, Équipe, Ordonnances, Labo, etc.).
- [ ] La sidebar est rétractable via un bouton toggle avec transition fluide et état persistant en session.
- [ ] Les sous-menus (ex: Pharmacie -> Vérification, Pharmacie -> Gestion des Stocks) sont collapsibles de façon élégante.
- [ ] Tous les liens de menu et de sous-menus utilisent le routage Angular (`routerLink`) avec gestion automatique de l'état actif (`routerLinkActive`).
- [ ] L'intégration se fait sans régression sur l'app shell et conserve le sélecteur de langue et l'identité de l'utilisateur connecté.
- [ ] Aucun libellé ou texte n'est codé en dur (utilisation de `I18nService`).

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0012 — Refonte UI/UX Premium |
| User story parent | N/A |
| Sprint cible | SPRINT-0009 |
| Priorité business | P1 |
| Complexité | M |
| Story points | 5 |
| Profil recommandé | Senior |
| Effort estimé senior | 0.5j |
| Effort estimé intermédiaire | 0.8j |
| Effort estimé junior | 1.3j |
| Responsable | Frontend Agent |
| Reviewer obligatoire | Antigravity |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | STORY-1801 |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] Structure de routage `app.routes.ts` analysée.
- [x] Rôles et habilitations du personnel clinique vérifiés.
- [x] Standards d'arrondis et d'ombres dans `DESIGN.md` et `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md` pris en compte.

## 5. Action plan

- [ ] Créer le composant de navigation latérale et l'intégrer dans `AppShellComponent`.
- [ ] Implémenter la logique de rétractation (responsive et desktop layout grid).
- [ ] Ajouter les libellés de traduction i18n dans les dictionnaires.
- [ ] Raccorder les routes pour chaque rôle.
- [ ] Tester les comportements visuels en mode light et dark.
