# Pré-enregistrement public — conception technique

## Ticket

- GitHub : #247
- Stack : Angular 22 / Tailwind CSS / signals / Vitest
- Impact SemVer : PATCH

## Architecture actuelle

La route `/public/register` charge `PatientSelfRegistrationComponent` en standalone. Le composant :

- lit `orgId` / `organizationId` depuis les query params ;
- charge un captcha public ;
- maintient les champs du formulaire via Angular signals ;
- appelle `PatientApiService.submitPublicPreRegistration()` ;
- affiche un état de succès après HTTP 2xx.

Le template contient actuellement toutes les sections simultanément et possède sa propre topbar publique.

## Conception cible

### 1. Navigation progressive

Ajouter au composant un signal `currentStep` typé `1 | 2 | 3 | 4`.

Méthodes prévues :

- `nextStep()` ;
- `previousStep()` ;
- `setLang(locale)` ;
- validation locale de l'étape d'identité avant passage à l'étape 2.

La soumission finale continue d'utiliser `submitForm()` sans changement du payload.

### 2. Répartition des champs

- Étape 1 : type d'admission + identité obligatoire.
- Étape 2 : groupe sanguin + coordonnées.
- Étape 3 : contact d'urgence.
- Étape 4 : captcha + soumission.

Le DOM n'affiche qu'une étape à la fois avec les blocs conditionnels Angular `@if`.

### 3. Topbar publique

Réutiliser les conventions du shell :

- `app-topbar` / `app-container` ;
- logo Joprelys Connect ;
- boutons explicites FR et EN avec état actif ;
- bouton thème de forme et dimensions identiques au shell.

Ajouter deux actions de sortie :

- site public Joprelys (`APP_BRAND_CONFIG.publicSiteUrl`) ;
- connexion patient via `RouterLink` vers `/patient/login`.

Ces actions sont également affichées sur l'état de succès.

### 4. Configuration de marque

Étendre `AppBrandConfig` avec `publicSiteUrl` afin d'éviter un URL institutionnel codé en dur dans le composant.

Valeur initiale : `https://joprelys.com`.

### 5. i18n

Ne pas ajouter de nouvelles chaînes au gros dictionnaire racine. Créer un dictionnaire de feature :

- `web/src/assets/i18n/features/public-self-registration/fr.json` ;
- `web/src/assets/i18n/features/public-self-registration/en.json`.

Le dictionnaire contient uniquement les nouvelles chaînes du parcours : navigation, progression, sorties et titres simplifiés. `I18nService` le charge comme dictionnaire optionnel et le fusionne après la base.

### 6. Tests

Mettre à jour `patient-self-registration.component.spec.ts` pour couvrir :

- chargement initial du captcha ;
- sélection FR/EN via `setLocale` ;
- impossibilité de quitter l'étape 1 si l'identité obligatoire est incomplète ;
- passage 1 → 2 → 3 → 4 ;
- retour arrière ;
- soumission inchangée ;
- reset ramenant `currentStep` à 1 ;
- présence des liens vers le site public et `/patient/login` dans le rendu.

`RouterLink` nécessitera `provideRouter([])` dans le TestBed.

## Sécurité / permissions

Aucun changement d'authentification, d'autorisation ou de permission. La route reste publique. L'`organizationId` reste obligatoire et validé comme UUID avant chargement du captcha.

## API / données

Aucun changement :

- endpoint inchangé ;
- DTO inchangé ;
- base de données inchangée ;
- aucune migration.

## Observabilité

Aucun nouveau log applicatif requis. Les erreurs HTTP et captcha continuent d'être présentées via `errorMessage`.

## Risques et parades

1. **Régression du payload** : conserver la construction actuelle de `PatientPreRegistrationRequest` et la tester strictement.
2. **Régression RouterLink en test** : fournir le router dans le TestBed.
3. **Traductions manquantes** : dictionnaires FR/EN symétriques et chargement optionnel dans `I18nService`.
4. **Scroll après changement d'étape** : chaque étape est suffisamment compacte pour rester dans le viewport mobile ; ne pas ajouter d'animation lourde ou de scroll forcé.
5. **Lien de site dépendant de l'environnement** : centraliser l'URL dans `APP_BRAND_CONFIG` pour permettre un changement ultérieur sans toucher au composant.

## Validation attendue

- `npm test -- --watch=false` ou commande CI équivalente ;
- `npm run build` ;
- recette visuelle FR/EN, light/dark, 375/768/1366 px ;
- vérification qu'aucun fichier backend n'est modifié.