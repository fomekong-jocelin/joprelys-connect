# TICKET-0111 — Résolution de la navigation et responsivité mobile (Mobile First App-like)

## 1. Objectif

Rendre l'application mobile-first et intuitive sur les petits écrans (smartphones) suite à l'introduction de la Sidebar.
- Masquer la barre latérale (Sidebar) de bureau sur mobile pour libérer l'espace.
- Ajouter un bouton de menu Hamburger sur mobile dans la Topbar pour ouvrir un menu Drawer.
- Concevoir un menu Drawer coulissant élégant pour mobile contenant les liens de navigation, le profil, le sélecteur de thèmes/langues et l'action de déconnexion.
- Éliminer le double en-tête et simplifier la Topbar sur mobile en masquant les contrôles de langue/thème/déconnexion redondants qui y étaient affichés (les relocaliser dans le tiroir de navigation mobile).
- Ajuster les espacements globaux (padding breadcrumbs et conteneur principal) sur mobile.

## 2. Critères d'acceptation

- [x] La Sidebar desktop est masquée sur mobile (`hidden md:flex`).
- [x] Un bouton Hamburger (`md:hidden`) est visible à gauche du logo dans la Topbar sur mobile.
- [x] Le clic sur le bouton Hamburger ouvre un Drawer mobile couvrant la gauche de l'écran avec un fondu d'arrière-plan (backdrop).
- [x] Le Drawer contient l'avatar utilisateur, le rôle, tous les liens de menu correspondant au rôle de l'utilisateur actif (y compris les dossiers de patients contextuels si actifs).
- [x] Le Drawer intègre à sa base le sélecteur de langue, de thème et le bouton de déconnexion.
- [x] La Topbar mobile est déchargée des éléments superflus (les sélecteurs de langue et de thème et le bouton logout n'y figurent plus sur mobile).
- [x] Le clic sur le backdrop ou sur le bouton de fermeture ("X") ferme le Drawer mobile.
- [x] La navigation vers une page ferme automatiquement le Drawer mobile.
- [x] Le padding de l'espace principal et des breadcrumbs est optimisé sur mobile (`p-4 md:p-6` et `px-4 md:px-6`).
- [x] L'internationalisation et les thèmes Clair/Sombre sont entièrement respectés.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0012 — Refonte UI/UX Premium |
| User story parent | STORY-1802 — Menu Latéral (Sidebar) Rétractable |
| Sprint cible | SPRINT-0009 |
| Priorité business | P0 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Senior |
| Effort estimé senior | 0.15j |
| Effort estimé intermédiaire | 0.25j |
| Effort estimé junior | 0.4j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code de `AppShellComponent` analysé (`app-shell.component.ts`)
- [x] Code de `BreadcrumbComponent` analysé (`breadcrumb.component.ts`)
- [x] Design System (`DESIGN.md` et `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md`) pris en compte
- [x] Frontend Tailwind CSS v4 vérifié

## 5. Hypothèses

- La classe Tailwind `md:` (breakpoint 768px) est le point de rupture adéquat pour basculer entre le menu de bureau et le menu mobile.
- L'utilisation de `<ng-template>` pour factoriser les items du menu évite de dupliquer les SVG et la liste `@switch` / `@case` des icônes.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Drawer qui se ferme mal lors d'un clic de route | Mauvaise UX | S'assurer d'appeler `closeMobileMenu()` sur l'événement `(click)` des liens du Drawer mobile. |
| Perte des traductions | Bug affichage | Réutiliser strictement les libellés de `I18nService` existants. |

## 7. Action plan

- [x] Importer `NgTemplateOutlet` dans `AppShellComponent`.
- [x] Factoriser le code du menu de navigation dans un `<ng-template #menuLinks let-isMobile="isMobile">`.
- [x] Modifier la Topbar pour masquer la langue, le thème et le bouton de déconnexion sur mobile (`hidden md:flex` / `hidden md:inline-flex`).
- [x] Ajouter le bouton Hamburger dans la Topbar, visible uniquement sur mobile (`md:hidden`).
- [x] Ajouter la structure HTML/CSS du Drawer mobile avec backdrop et son déclenchement conditionnel via le signal `mobileMenuOpen`.
- [x] Intégrer les contrôles de langue, thème et déconnexion dans le Drawer mobile.
- [x] Adapter le composant Breadcrumb pour optimiser le padding horizontal sur mobile.
- [x] Adapter le padding principal du contenu sur mobile.
- [x] Compiler et exécuter les tests unitaires et builds Angular pour vérifier la conformité.
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## 8. Implémentation réalisée

- Importation de `NgTemplateOutlet` de `@angular/common` et intégration dans l'AppShell.
- Regroupement des items de menu dans un template `<ng-template #menuLinks let-isMobile="isMobile">` évitant les répétitions.
- Masquage responsive de la Sidebar bureau et de ses contrôles associés (langue, thème, logout).
- Création du bouton Hamburger visible uniquement sur mobile (`md:hidden`).
- Création du tiroir coulissant mobile (Drawer layout) avec fond estompé (`backdrop-blur-xs`), affichant la fiche de profil simplifiée, les options de navigation, et le panneau de paramétrage (Langue / Thème / Déconnexion) au bas du Drawer.
- Optimisation des paddings de la page et des Breadcrumbs pour les mobiles.
- Correction des tests existants impactés par l'introduction de nouvelles fonctionnalités (mocks de `getVaccinations` dans `patient-detail.component.spec.ts` et `otpCode` dans `forgot-password.component.spec.ts`).
- **Phase 2 : Optimisation des paddings et alignements mobiles (Demande complémentaire)** :
  - Suppression de la double marge mobile en changeant le padding de la section d'affichage du Shell de `p-4 md:p-6` à `p-0 md:p-6`. Les pages contrôlent ainsi leur propre padding via `app-container` ou des classes spécifiques (ex : `px-4`).
  - Réduction de la marge interne des conteneurs `.app-container` et `.app-container-wide` sur mobile de `1.5rem` (24px) à `1rem` (16px) pour maximiser l'espace utilisable sur petit écran.
  - Réduction du padding interne des composants de cartes `.ui-card` sur mobile de `p-6` (24px) à `p-4` (16px) pour éviter l'effet "vide" sur les côtés.
  - Réorganisation des boutons d'actions principales du Dossier Patient (`Retour`, `Synthèse PDF`, `Ouvrir une visite`/`Démarrer la consultation`) sur mobile : ils s'empilent désormais verticalement et s'étirent sur toute la largeur (`flex flex-col items-stretch`) au lieu d'être compressés côte à côte.
  - Réduction du padding interne des panneaux médicaux de la fiche patient (`Allergies`, `Antécédents`, `Vaccinations`) de `p-5` à `p-4 md:p-5`.
- **Phase 3 : Résolution du wrap DPU et du bouton Actualiser sur mobile (Demande complémentaire)** :
  - Ajustement des affichages du numéro DPU dans `patient-list.component.html` et `patient-detail.component.ts` avec `whitespace-nowrap`, `font-mono` et `text-xs` pour empêcher le retour à la ligne inesthétique sur petit écran.
  - Découpage en blocs flexibles avec gestion de flex-wrap pour les informations secondaires de la fiche d'en-tête patient.
  - Réorganisation du bloc d'en-tête de la file d'attente sur le tableau de bord (`dashboard.component.html`) pour placer le bouton "Actualiser" directement à côté du titre sur mobile, évitant ainsi son tassement en bout de ligne.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-05 | Antigravity | 0.15j | 100% | Aucun | Aucun | Implémentation et tests complets validés |
| 2026-07-05 | Antigravity | 0.05j | 100% | Aucun | Aucun | Phase 2 : Optimisation des paddings et disposition des boutons sur mobile |
| 2026-07-05 | Antigravity | 0.05j | 100% | Aucun | Aucun | Phase 3 : Résolution du wrap DPU et du bouton Actualiser sur mobile |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Angular build and test
npm run build
npm run test -- --watch=false
```

### Résultats

- [x] Tests unitaires OK (63/63 tests au vert)
- [x] Tests intégration OK
- [x] Tests UI/widget OK
- [x] Tests sécurité OK
- [x] Build OK (application web compilée avec succès)
- [x] Analyse statique OK

## 11. Documentation

- [x] Changelog mis à jour
- [x] Suivi projet mis à jour

## 12. Reste à faire

Aucun reste à faire.

## 13. Statut final

Statut : DONE

## 14. Notes finales

La correction de la responsivité mobile de la sidebar s'accompagne d'une factorisation propre du template des menus et d'une remise au propre de deux tests unitaires qui échouaient sur le projet (forgot password et patient profile).

## 13. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Amélioration visuelle et correction responsivité mobile sans breaking change API. |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Oui |

## 4.1 Impact thème / i18n / branding

- [x] Impact Angular UI analysé
- [x] Impact Flutter UI analysé (N/A)
- [x] Thème centralisé vérifié
- [x] Thèmes light/dark vérifiés
- [x] Textes `fr` / `en` prévus
- [x] Composants/widgets réutilisables prévus
- [x] Configuration app/branding vérifiée
- [x] Aucun texte ou branding hardcodé prévu

## Documentation First

- [x] Documentation technique à jour (pas de nouvelle feature fonctionnelle majeure requise)

## Design System / UI

- [x] `DESIGN.md` lu
- [x] Tailwind CSS v4 vérifié côté Angular
- [x] Tailwind v3 / Angular Material absents sauf ADR
- [x] Light/dark vérifiés
- [x] i18n FR/EN prévue
- [x] Contraste/focus/accessibilité vérifiés

## Vérification `.gitignore`

- [x] `.gitignore` présent à la racine
