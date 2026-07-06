# Spécification Technique — Refonte Premium du Profil Patient (Portail Patient)

## 1. Stack technique concernée
- Frontend Angular (v19)
- Tailwind CSS v4
- Fichiers de traduction JSON i18n (`fr.json`, `en.json`)

## 2. Fichiers impactés
- `web/src/app/patient/portal/pages/patient-profile-page.component.ts` : refonte du template HTML et styles associés.
- `web/src/assets/i18n/fr.json` : ajout de la clé de traduction `patient.profile.activeStatus`.
- `web/src/assets/i18n/en.json` : ajout de la clé de traduction `patient.profile.activeStatus`.

## 3. Approche technique et architecture cible
L'implémentation repose sur le remplacement de la mise en page simpliste actuelle par une structure de cartes premium tirant parti de Tailwind CSS v4 et des variables CSS globales de `styles.css`.

### Éléments d'implémentation :
1. **Bannière d'en-tête responsive** :
   - Un bloc en haut de la grille de profil avec un fond en dégradé subtil (`bg-gradient-to-r`).
   - Un grand avatar (`w-24 h-24`) affichant l'initiale du patient avec un dégradé de couleur primaire.
   - Intégration du groupe sanguin sous forme de badge superposé en bas à droite de l'avatar.
   - Présentation de la fiche d'identité de base (nom, DPU, date de naissance, genre) alignée à l'aide d'icônes SVG intégrées.

2. **Cartes d'informations thématiques** :
   - Chaque bloc de données (Coordonnées, Contact d'urgence, Allergies, Antécédents) est logé dans une carte `section` personnalisée utilisant la classe `.ui-card` avec une transition d'élévation sur hover (`transition-all hover:shadow-[var(--shadow-panel)] duration-300`).
   - Les en-têtes de cartes comportent un conteneur d'icône coloré (`bg-[color-mix(in srgb,var(--brand-primary)_12%,var(--app-surface))]`) pour apporter du contraste et du rythme visuel.
   - Les champs d'information sont encapsulés dans des conteneurs `.ui-card-muted` pour un effet "fiche technique" propre et structuré.

3. **Traduction i18n** :
   - Ajout de la clé `"patient.profile.activeStatus"` dans les dictionnaires bilingues pour labelliser l'état du dossier médical.

## 4. Stratégie de tests
- **Tests unitaires** : s'assurer que les tests existants sur le profil patient (`patient-pages.spec.ts`) s'exécutent avec succès.
- **Build Angular** : s'assurer que la compilation via `npm run build` n'échoue pas.
- **Validation responsive** : s'assurer que le comportement s'empile correctement sur mobile (1 colonne) et s'étale sur desktop (3 colonnes).

## 5. Impact SemVer prévu
- **Bump SemVer** : `PATCH` (Amélioration esthétique non disruptive).
