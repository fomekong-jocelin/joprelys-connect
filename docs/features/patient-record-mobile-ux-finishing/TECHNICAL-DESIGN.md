# TECHNICAL DESIGN — Finition UX du dossier patient mobile

## Stack

Angular standalone components + Tailwind CSS v4 + design system Joprelys Connect.

Aucun changement Spring Boot, DB, API ou CI n’est requis.

## Composants impactés

- `web/src/app/patient/patient-detail.component.ts`
- `web/src/app/patient/detail/patient-profile-tab.component.ts`
- `web/src/app/patient/detail/patient-emergency-context.component.ts`
- `web/src/app/patient/detail/patient-profile-tab.component.spec.ts`
- `web/src/app/patient/patient-detail.component.spec.ts`
- `web/src/assets/i18n/fr.json`
- `web/src/assets/i18n/en.json`

`PatientAdministrativeSummaryComponent` est conservé comme composant de présentation ; seule sa position dans le parcours change.

## Architecture cible

### Profil

`PatientProfileTabComponent` devient une composition simple :

```text
Identité provisoire éventuelle
→ Résumé administratif visible directement
→ Contexte d’urgence repliable
→ Informations médicales repliables
```

L’état `activePanel` n’a plus besoin de gérer un panneau administratif. Il devient un simple `medicalExpanded` pour les informations médicales longitudinales.

### Contexte urgence

Le composant garde sa logique de chargement et son auto-ouverture d’urgence active. Seul le libellé d’en-tête et la présentation mobile sont simplifiés.

### Actions du shell patient

Le conteneur d’actions utilise deux stratégies responsive dans le même DOM :

- mobile `< lg` : grille avec action primaire pleine largeur en premier, puis actions secondaires ;
- desktop `lg+` : groupe horizontal compact, ordre `primaire → synthèse → retour` ou équivalent visuellement hiérarchisé.

Aucune duplication d’action ou de handler métier.

## Responsive

### Mobile 360–430 px

- titres d’accordéons : `text-sm` / `font-extrabold`, casse normale, `whitespace-nowrap`, `min-w-0` ;
- le chevron est `shrink-0` ;
- aucun titre clinique n’utilise une chaîne concaténée de trois catégories ;
- action primaire `w-full` ;
- secondaires dans une grille à deux colonnes lorsque les deux existent.

### Desktop

- pas de pleine largeur forcée ;
- actions alignées à droite ;
- aucun changement de la colonne de navigation patient.

## Design system

- Tailwind CSS v4 uniquement ;
- variables CSS existantes uniquement ;
- `ui-card` réutilisé ;
- rayons ≤ 8 px ;
- ombres existantes uniquement ;
- aucun Angular Material ;
- boutons sans retour à la ligne interne conformément à `DESIGN.md`.

## i18n

Deux libellés courts sont ajoutés/modifiés dans les catalogues FR/EN :

- `patient.urgTemp.emergency.title`
- `patient.profile.medicalSection`

Aucun texte visible nouveau n’est codé en dur.

## Sécurité / RBAC

- aucune permission modifiée ;
- les computed `canAdmit`, `canStartConsultation`, `canDownloadSummary` restent les seules sources d’affichage des actions ;
- le changement est purement présentationnel.

## API / données

Aucun contrat API, DTO, endpoint, table ou migration.

## Observabilité

Aucun nouvel événement nécessaire : le changement n’affecte ni le métier ni les appels réseau.

## Tests

- profil : résumé administratif présent immédiatement ;
- profil : absence du bouton administratif ;
- profil : informations médicales repliées puis ouvrables ;
- contexte urgence : titre i18n court ;
- shell : action primaire ordonnée avant les secondaires ;
- shell : classes de grille mobile vérifiées ;
- suite Angular complète ;
- build production.

## SemVer

PATCH : correction/finition UI rétrocompatible sans changement de contrat.

## Rollback

Revert de la PR : aucun état persistant ou migration à restaurer.