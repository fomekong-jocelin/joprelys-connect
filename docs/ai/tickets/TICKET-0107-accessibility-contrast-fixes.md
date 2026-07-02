# TICKET-0107 — Correction de l'Accessibilité et des Contrastes Visuels (WCAG)

## 1. Objectif

Ce ticket traite des correctifs d'accessibilité (conformité WCAG AA) sur l'interface utilisateur en mode sombre :
1. **Contraste des Textes** : Augmenter le ratio de contraste des textes de description sur le tableau de bord clinique. Les descriptions étaient auparavant de couleur trop sombre sur fond bleu nuit, les rendant presque illisibles.
2. **Visibilité des Actions** : Améliorer la lisibilité des liens et boutons d'action (comme "Accéder", "Consulter", "Configurer") sur fond sombre en utilisant des nuances de cyan plus claires.
3. **Labels de Formulaires** : Réajuster le contraste des labels de saisie du formulaire de connexion dans le thème sombre.
4. **Correction du Nom d'Utilisateur** : Correction d'une classe Tailwind invalide (`dark:text-slate-250` au lieu de `dark:text-white`), qui masquait le nom de l'utilisateur connecté dans l'en-tête (pasted-image-51862.png).

## 2. Critères d'acceptation

- [x] Les descriptions de cartes sur le tableau de bord utilisent la classe `dark:text-slate-400` ou `dark:text-slate-350` en mode sombre pour garantir un ratio supérieur à 4.5:1.
- [x] Les liens d'action sur le tableau de bord utilisent `dark:text-[#22d3ee]` / `dark:text-cyan-400` pour une meilleure visibilité.
- [x] Les labels de formulaires de connexion utilisent `dark:text-slate-400` pour éviter d'être trop sombres.
- [x] Le nom d'utilisateur dans l'en-tête du tableau de bord est parfaitement visible et lisible en mode sombre (utilisation de `dark:text-white`).
- [x] L'application compile sans erreur de styles ou de logique.
- [x] Les tests unitaires Vitest s'exécutent avec succès.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | QUAL |
| User story parent | STORY-0102 |
| Sprint cible | SPRINT-0002 |
| Priorité business | P0 |
| Complexité | XS |
| Story points | 1 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.03j |
| Effort estimé intermédiaire | 0.05j |
| Effort estimé junior | 0.1j |
| Responsable | Gemini |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Aucun |
| Risque technique | Aucun |
| Dépendances | STORY-0102 |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] Capture d'écran utilisateur `pasted-image-27991.png` analysée (contraste des cartes)
- [x] Capture d'écran utilisateur `pasted-image-51862.png` analysée (nom d'utilisateur masqué dans l'en-tête)

## 5. Hypothèses

- L'interface en mode sombre doit être aussi accessible et lisible que l'interface en mode clair, conformément aux exigences d'accessibilité logicielle médicale.

## 6. Action plan

- [x] Initialiser le ticket (`TICKET-0107-accessibility-contrast-fixes.md`)
- [x] Modifier `dashboard.component.html` pour renforcer les classes de contrastes textuels en mode sombre
- [x] Modifier `login.component.html` pour améliorer la visibilité des labels de formulaire
- [x] Remplacer la classe Tailwind invalide `dark:text-slate-250` par `dark:text-white` dans l'en-tête du tableau de bord
- [x] Valider par compilation `npm run build`
- [x] Valider par tests unitaires `npm run test`
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`

## 7. Implémentation réalisée

- [x] **Correctifs Dashboard** :
  - Remplacement de `text-slate-500` sans alternative sombre par `dark:text-slate-400` ou `dark:text-slate-350` sur les textes de présentation et la liste d'état d'interopérabilité.
  - Utilisation de `dark:text-cyan-400` et `dark:text-[#22d3ee]` pour tous les liens d'actions secondaires pour un contraste optimal.
  - Correction de l'en-tête pour afficher le nom de l'utilisateur avec la couleur blanche en mode sombre (`dark:text-white`).
- [x] **Correctifs Formulaire** :
  - Remplacement de `dark:text-slate-500` par `dark:text-slate-400` sur les labels du formulaire de connexion.

## 8. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-02 | Gemini | 0.05j | 100% | Aucun | Aucun | Correctifs d'accessibilité WCAG AA validés avec succès |

## 9. Tests et vérifications

* `npm run build` -> **SUCCESS**
* `npm run test` -> **9 tests passed** (100% OK)

## 10. Statut final

Statut : DONE
