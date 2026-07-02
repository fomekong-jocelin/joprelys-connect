# TICKET-0106 — Refonte UI Épurée & Intégration de la Charte Graphique Joprelys HealthTech

## 1. Objectif

Ce ticket couvre les travaux de refonte visuelle du frontend [web](file:///C:/MES-APPLICATIONS/joprelys-connect/web) :
1. **Refonte UI Minimaliste** : Remplacer l'interface initiale trop chargée par un design inspiré du site "Innerly" (mise en page aérée, centrée, sans bordure de carte lourde ou ombres portées prononcées).
2. **Respect du Guide de Marque** : Appliquer scrupuleusement la charte graphique de **Joprelys HealthTech** définie dans [JOPRELYS_HEALTHTECH_GUIDE_DE_MARQUE.docx](file:///C:/MES-APPLICATIONS/joprelys-connect/JOPRELYS_HEALTHTECH_GUIDE_DE_MARQUE.docx) :
   - Couleurs de marque : Bleu nuit principal (`#0A1D3D`), Bleu cyan (`#0B91B2`), Vert santé (`#16A34A`), Vert menthe (`#D1FAE5`) et Gris très clair (`#F2F4F7`).
   - Typographies : **Montserrat** (titres, accents) et **Inter** (textes, boutons, formulaires).
   - Logotype : Reconstruire le logo vectoriel avec le double anneau en dégradé de couleur conformément au diagramme d'architecture de produit.

## 2. Critères d'acceptation

- [x] Polices Montserrat et Inter importées proprement à la racine du fichier de styles global.
- [x] Variables de couleurs `@theme` déclarées de manière centralisée dans [web/src/styles.css](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/styles.css).
- [x] Logo vectoriel SVG intégrant le dégradé de la marque présent sur l'écran d'accueil.
- [x] L'interface de connexion et de session active suit la disposition aérée et épurée "Innerly" (page blanche, pas de conteneur en relief, inputs gris clair se transformant en blanc au focus avec bordure cyan).
- [x] L'avertissement d'import esbuild (`All "@import" rules must come first`) est résolu en déplaçant l'import Google Fonts tout en haut de `styles.css`.
- [x] Le frontend compile et passe les tests unitaires avec succès (`npm run build` et `npm run test`).

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | QUAL |
| User story parent | STORY-0101 |
| Sprint cible | SPRINT-0002 |
| Priorité business | P0 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Senior |
| Effort estimé senior | 0.1j |
| Effort estimé intermédiaire | 0.15j |
| Effort estimé junior | 0.3j |
| Responsable | Gemini |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Aucun |
| Risque technique | Faible |
| Dépendances | TICKET-0105 |
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
- [x] Captures d'écran Innerly analysées
- [x] Fichier [JOPRELYS_HEALTHTECH_GUIDE_DE_MARQUE.docx](file:///C:/MES-APPLICATIONS/joprelys-connect/JOPRELYS_HEALTHTECH_GUIDE_DE_MARQUE.docx) lu et analysé par script d'extraction ZIP XML

## 5. Hypothèses

- L'esthétique générale doit inspirer le professionnalisme médical et la modernité logicielle (sobre, propre, clair).

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Avertissement de compilation esbuild | Fichier CSS non généré ou mal ordonné | Placement de l'import `@import url(...)` avant l'import Tailwind dans `styles.css` |

## 7. Action plan

- [x] Créer le ticket actionnable (`TICKET-0106-ui-redesign-minimalist.md`)
- [x] Modifier `web/src/styles.css` pour inclure la charte et les polices Montserrat/Inter
- [x] Ré-implémenter `login.component.html` avec le style aéré
- [x] Exécuter le build frontend pour vérifier l'absence d'avertissements d'import
- [x] Exécuter les tests unitaires frontend
- [x] Mettre à jour `CHANGELOG.md`
- [x] Mettre à jour `PROJECT-TRACKING.md`
- [x] Documenter la refonte dans la réponse finale

## 8. Implémentation réalisée

- [x] **Importation de polices et Thème** :
  - Déclaration de l'import Google Fonts à la première ligne de [web/src/styles.css](file:///C:/MES-APPLICATIONS/joprelys-connect/web/src/styles.css).
  - Déclaration des variables de couleurs de marque (`--color-brand-night`, `--color-brand-cyan`, etc.) et de polices (`--font-display`, `--font-sans`) dans le bloc `@theme` de Tailwind v4.
- [x] **Refonte HTML** :
  - Intégration du logo de Joprelys Connect sous forme de SVG vectoriel composé de deux anneaux croisés avec dégradés linéaires.
  - Structure de formulaire épurée (inputs de couleur grise `#F2F4F7`, bordure subtile, focus cyan sur fond blanc).
  - Affichage de session active repensé : avatar circulaire avec initiale de l'utilisateur, rôle dans une pilule vert menthe/santé, et bouton "Se déconnecter" s'étirant harmonieusement.
  - Section de pied de page reprenant les copyrights institutionnels et liens d'aide ("Conditions d'utilisation", "Politique de confidentialité", "Centre d'aide").
- [x] **Compilation** :
  - Résolution réussie du warning `invalid-@import` de esbuild. Le build sort propre et valide les styles.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-02 | Gemini | 0.15j | 100% | Aucun | Aucun | Refonte visuelle validée par build et tests unitaires |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
cd web && npm run build
cd web && npm run test -- --watch=false
```

### Résultats

- [x] Tests unitaires OK (les 6 tests frontend passent avec succès)
- [x] Build OK (pas de warning de compilation esbuild/CSS)
- [x] Non exécuté avec justification

## 11. Documentation

- [x] README mis à jour si nécessaire
- [x] API docs mises à jour si nécessaire
- [x] ADR créé si décision structurante (non requis ici)
- [x] Changelog mis à jour
- [x] Suivi projet mis à jour

## 12. Reste à faire

- Aucun

## 13. Statut final

Statut : DONE

## 14. Notes finales

L'application est conforme aux nouvelles directives d'identité visuelle de tous les produits Joprelys HealthTech.

## 15. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Refonte stylistique mineure de l'interface d'authentification pour respecter la charte graphique officielle |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |
