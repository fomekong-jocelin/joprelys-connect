# TECHNICAL DESIGN — Liste Patients mobile-first

## Stack

Angular standalone + Tailwind CSS v4 + design system Joprelys Connect.

Aucun changement backend, DB ou contrat API.

## Composants impactés

- `web/src/app/patient/patient-list.component.ts`
- `web/src/app/patient/patient-list.component.html`
- `web/src/app/patient/patient-list.component.spec.ts`
- `web/src/app/core/i18n/i18n.service.ts`
- `web/src/assets/i18n/features/patient-list/fr.json`
- `web/src/assets/i18n/features/patient-list/en.json`
- `docs/features/patient/FUNCTIONAL-SPEC.md`

## Structure cible

```text
PageHeader compact
  └─ Patients + sous-titre

Workspace principal
  ├─ recherche pleine largeur
  ├─ Nouvelle admission
  ├─ contexte liste / résultats
  ├─ mobile : cartes patient indépendantes
  └─ desktop : tableau existant
```

## PageHeader

- `backLink` non fourni : le breadcrumb global reste l’unique retour de niveau ;
- aucun CTA n’est projeté dans le PageHeader ;
- `patients.title` et `patients.subtitle` sont raccourcis.

## Recherche

- input existant conservé ;
- `(keyup.enter)` appelle `load()` ;
- recherche active : bouton iconique `X` à droite de l’input ;
- `clearSearch()` remet le signal à vide puis appelle `load()` ;
- `appliedSearchQuery` distingue la saisie en cours du filtre réellement soumis afin que le compteur ne décrive jamais une liste obsolète comme résultat de la nouvelle saisie ;
- pas de debounce ni changement du contrat API dans ce correctif.

## Liste mobile

- suppression du `app-ui-card` parent autour de toutes les cartes ;
- chaque patient devient un `<button type="button">` `ui-card` pleine largeur avec `text-left` ;
- structure interne : nom + badge sexe, DPU, ligne téléphone/ville, chevron ;
- pas de bouton imbriqué ;
- `viewDetail(patient)` reste le handler unique ;
- focus visible, cible tactile largement supérieure à 44 px.

## Desktop

Le tableau existant est conservé dans une carte dédiée visible à partir de `md` :

- largeur minimale actuelle ;
- DPU / numéro local `whitespace-nowrap` ;
- action `Voir le dossier` inchangée.

## i18n

La feature possède son dictionnaire `features/patient-list/{fr,en}.json`, chargé après le dictionnaire de base par `I18nService`. Il surcharge uniquement les libellés de cette surface sans modifier les traductions historiques globales.

Clés surchargées :

- `patients.title`
- `patients.subtitle`
- `patients.searchPlaceholder`

Ajouts :

- `patients.listHeading`
- `patients.resultOne`
- `patients.resultsMany`
- `patients.clearSearch`

## API / RBAC

Aucun changement :

- `GET /api/patients?q=...` reste l’unique source ;
- `PATIENT_READ` côté API inchangé ;
- admission et routage inchangés.

## Risques

- ne pas appeler la liste non filtrée « Patients récents » : l’API utilise actuellement `findAll()` lorsque `q` est vide et ne garantit pas un ordre de récence ;
- conserver les chaînes DPU intactes et non tronquées ;
- ne pas imbriquer de bouton dans la carte mobile cliquable.

## SemVer

PATCH — UI rétrocompatible.
