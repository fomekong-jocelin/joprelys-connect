# TEST PLAN — MOB-2803 Internationalisation Flutter

## Objectif

Vérifier que l’application mobile supporte réellement FR/EN, change de langue sans redémarrage, applique un fallback sûr et formate dates/heures/nombres selon la locale sans régression du thème existant.

## Tests unitaires

### LOC-01 — Résolution FR
- entrée `fr_CM` ;
- attendu `fr`.

### LOC-02 — Résolution EN
- entrée `en_GB` ou `en_US` ;
- attendu `en`.

### LOC-03 — Fallback
- entrée `de_DE` ;
- attendu `fr`.

### LOC-04 — Changement explicite
- état initial EN ;
- `useFrench()` → FR ;
- `useEnglish()` → EN.

### FMT-01 — Date
- même `DateTime` en FR et EN ;
- rendu localisé différent et année conservée.

### FMT-02 — Heure
- 16:05 reste une heure locale lisible au format 24 h attendu.

### FMT-03 — Nombre
- `1234.5` en FR se termine par `,5` ;
- en EN se termine par `.5`.

### FMT-04 — Locale non supportée
- formateur appelé avec une langue non supportée ;
- résultat identique au fallback FR.

## Widget tests

### UI-01 — Fondation FR
- application montée avec locale plateforme FR ;
- libellés `Langue`, `Apparence`, `Système`, `Clair`, `Sombre` disponibles.

### UI-02 — Bascule EN
- toucher `English` ;
- `MaterialApp.locale = en` ;
- libellés `Language`, `Appearance`, `System`, `Light`, `Dark` visibles.

### UI-03 — Retour FR
- toucher `Français` ;
- retour immédiat à `fr`.

### UI-04 — Non-régression thème
- sous locale FR, basculer system → light → dark → system ;
- `ThemeMode` attendu à chaque étape ;
- aucune modification de `AppTheme` requise.

## Contrôle d’architecture

- `AppTheme` absent du diff MOB-2803 ;
- `AppDesignTokens` absent du diff MOB-2803 ;
- aucun style métier ajouté à `core/theme` ;
- aucun stockage de donnée sensible ;
- aucune traduction chargée via un second mécanisme réseau parallèle.

## Gate CI

Depuis `mobile/` :

```bash
flutter pub get
dart format --output=none --set-exit-if-changed .
flutter analyze
flutter test
flutter build apk --debug
```

## Validation manuelle ultérieure

Sur appareil Android :
- démarrage téléphone FR ;
- démarrage téléphone EN ;
- démarrage avec langue système non supportée ;
- bascule FR/EN pendant l’application ;
- vérification light/dark après changement de langue ;
- absence de débordement avec les libellés anglais/français.

Cette recette appareil ne bloque pas la fondation purement Flutter si le gate automatisé exact-HEAD est vert ; elle devient obligatoire avant les écrans métier/pilote.

## Critère de sortie

Aucune fusion sans format, analyze, tests et APK debug verts sur le HEAD final de la PR, documentation incluse.
