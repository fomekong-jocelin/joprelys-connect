# STORY-1803 — Fil d'Ariane (Breadcrumbs) et Titrage Dynamique

## 1. Objectif

Ajouter un Fil d'Ariane (Breadcrumbs) dynamique au-dessus du contenu principal afin d'indiquer clairement la position de l'utilisateur dans l'application et de faciliter la navigation arrière.

## 2. Critères d'acceptation

- [ ] Un composant `BreadcrumbComponent` autonome (standalone) est créé.
- [ ] Le composant écoute les événements de navigation Angular Router pour reconstruire dynamiquement le chemin actif.
- [ ] Chaque étape du fil d'Ariane affiche un titre explicite et traduit (via des clés i18n associées à la configuration de la route dans `data.breadcrumb` ou résolues dynamiquement).
- [ ] Le fil d'Ariane est cliquable pour permettre de remonter dans l'arborescence, à l'exception du dernier élément représentant la page active.
- [ ] Le design est épuré, avec des séparateurs discrets (ex: chevron `/` ou `>`) conformes à `DESIGN.md`.
- [ ] Intégration harmonieuse dans le layout de `AppShellComponent` juste au-dessus du conteneur de contenu principal.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0012 — Refonte UI/UX Premium |
| User story parent | N/A |
| Sprint cible | SPRINT-0009 |
| Priorité business | P2 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.2j |
| Effort estimé intermédiaire | 0.3j |
| Effort estimé junior | 0.5j |
| Responsable | Frontend Agent |
| Reviewer obligatoire | Antigravity |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | STORY-1802 |
| Bloquants connus | Aucun |

## 4. Action plan

- [ ] Créer le composant autonome `BreadcrumbComponent` sous `shared/ui/`.
- [ ] Modifier `app.routes.ts` pour associer des métadonnées de breadcrumb traduisibles à chaque route.
- [ ] Implémenter le résolveur de chemin dynamique en écoutant les événements `NavigationEnd`.
- [ ] Ajouter les traductions et styles nécessaires.
- [ ] Raccorder le composant à `AppShellComponent` et tester sur plusieurs routes.
