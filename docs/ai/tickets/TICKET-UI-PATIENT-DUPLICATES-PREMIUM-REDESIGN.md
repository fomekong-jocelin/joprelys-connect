# TICKET-UI-PATIENT-DUPLICATES-PREMIUM-REDESIGN — Refonte Premium de la Page Gestion des Doublons Patients

## 1. Objectif

Rendre la page de gestion des suspicions de doublons patients (`/clinic/duplicates`) conforme au design system premium de Joprelys Connect (`DESIGN.md`). Auparavant, cette page s'affichait brute, sans structure de menu (`app-shell`), sans en-tête de page standard (`app-page-header`), avec une signalétique d'alerte et de boutons non unifiée, et avec des arrondis ne respectant pas les critères graphiques. 

## 2. Critères d'acceptation

- [x] Envelopper l'IHM dans le composant structurel global `<app-shell>`.
- [x] Remplacer le titre et sous-titre manuels par le composant unifié `<app-page-header>`.
- [x] Utiliser le composant `<app-empty-state>` pour afficher le message d'état vide de manière standard.
- [x] Appliquer la classe de style `.ui-card` pour les cartes de suspicion avec des arrondis limités à `8px` max (`rounded-lg`).
- [x] Remplacer les classes de couleurs `indigo` (non présentes dans le design system) par `brand-primary` (Medical Cyan) et `emerald` (Success).
- [x] Mettre en valeur visuellement les attributs divergents dans la table de comparaison de fusion grâce à une surbrillance douce (`bg-amber-500/5` / `dark:bg-amber-950/10`).
- [x] Valider la compilation Angular sans erreur ni avertissement.
- [x] Écrire les tests unitaires couvrant les fonctionnalités du composant et s'assurer que toute la suite passe avec succès.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0014 — Alignement modules 4 à 12 du CDC |
| User story parent | STORY-1910 — Portails patient, pro, labo, pharmacie et vérification publique conformes CDC |
| Sprint cible | SPRINT-0011 |
| Priorité business | P1 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.05j |
| Effort estimé intermédiaire | 0.1j |
| Effort estimé junior | 0.2j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | Aucune |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu si nécessaire
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Image `pasted-image-4.png` analysée
- [x] Code source de `duplicates-page.component.ts` et `duplicates-page.component.html` analysé
- [x] Règle des arrondis sobres respectée

## 5. Hypothèses

- L'intégration de la gestion des doublons dans le layout global et l'adaptation à la palette de couleurs de marque (Cyan/Slate Blue) répond entièrement aux exigences d'ergonomie et de premium design.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Aucun risque identifié | Très faible | NA |

## 7. Action plan

- [x] Créer la spécification fonctionnelle dans `docs/features/patient-duplicates-premium-redesign/FUNCTIONAL-SPEC.md`
- [x] Créer la spécification technique dans `docs/features/patient-duplicates-premium-redesign/TECHNICAL-DESIGN.md`
- [x] Modifier `duplicates-page.component.ts` pour importer `AppShellComponent`, `PageHeaderComponent` et `EmptyStateComponent`
- [x] Modifier `duplicates-page.component.html` pour intégrer la nouvelle structure et les styles conformes
- [x] Écrire le fichier de test unitaire `duplicates-page.component.spec.ts`
- [x] Valider la compilation Angular avec `npm run build`
- [x] Exécuter les tests unitaires Angular avec `npm run test`
- [x] Mettre à jour `CHANGELOG.md`
- [x] Mettre à jour `PROJECT-TRACKING.md`

## 8. Implémentation réalisée

1. **Intégration Enveloppe & En-tête** :
   - Ajout de `<app-shell>` pour encapsuler toute la page et hériter de la barre de navigation latérale.
   - Intégration de `<app-page-header>` pour afficher le titre et le sous-titre de manière harmonieuse.
2. **Design System & Arrondis Sobres** :
   - Application de la structure `.ui-card` aux suspicions avec des arrondis limités à `8px` (`rounded-lg`).
   - Remplacement de la couleur de surbrillance de sélection `indigo` par la couleur de marque `brand-primary`.
   - Utilisation de conteneurs internes désaturés et de polices Montserrat/Inter adaptées.
3. **Surbrillance des Conflits** :
   - Mise en évidence douce (`bg-amber-500/5` / `dark:bg-amber-950/10`) des lignes de la table de comparaison de fusion présentant des valeurs divergentes (par exemple, des différences dans l'adresse, le téléphone ou l'orthographe du nom complet), guidant ainsi plus facilement le choix de l'administrateur clinique.
4. **Tests Unitaires et Nettoyage** :
   - Création de `duplicates-page.component.spec.ts` avec 4 tests couvrant le chargement, l'ouverture/fermeture du modal, le rejet de doublon et la fusion réussie.
   - Retrait des avertissements de compilation (suppression de l'import unused de `CardComponent`).

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-06 | Antigravity | 0.05j | 100% | Aucun | Aucun | Modification code, écriture des tests, build de production validés avec succès |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Angular
npm run test
npm run build
```

### Résultats

- [x] Tests unitaires OK (84 tests Vitest au vert, incluant les 4 nouveaux tests de doublons)
- [x] Build OK (Compilation de production réussie avec succès, zéro avertissement)

## 11. Documentation

- [x] Spécifications fonctionnelles rédigées (`docs/features/patient-duplicates-premium-redesign/FUNCTIONAL-SPEC.md`)
- [x] Spécifications techniques rédigées (`docs/features/patient-duplicates-premium-redesign/TECHNICAL-DESIGN.md`)
- [x] Changelog mis à jour (`docs/ai/CHANGELOG.md`)
- [x] Suivi projet mis à jour (`docs/ai/PROJECT-TRACKING.md`)

## 12. Reste à faire

Aucun.

## 13. Statut final

Statut : DONE

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Refonte esthétique premium de la gestion des doublons patients et ajout de tests unitaires |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |
