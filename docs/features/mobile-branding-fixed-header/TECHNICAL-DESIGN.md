# TECHNICAL DESIGN — Branding adaptatif et headers mobiles fixes

## Stack et modules

- Flutter 3.44 / Dart 3.12 ;
- Riverpod pour thème, locale, auth et file active ;
- Material `Scaffold` / `AppBar` / `RefreshIndicator` ;
- assets PNG centralisés par `AppConfig`.

## Diagnostic technique

```text
AppBrandLockup
  -> Container blanc + bordure
  -> logo_principal.png déjà transparent

FoundationPage
  -> SafeArea
     -> RefreshIndicator
        -> SingleChildScrollView
           -> _DashboardTopBar  (défile à tort)

ConsultationNotesSheet
  -> Column
     -> ConsultationNotesHeader (fixe, trop haut)
     -> Expanded -> SingleChildScrollView (formulaire)
```

## Architecture cible

```text
Shared branding
  AppBrandLockup  -> wordmark sans surface artificielle
  AppBrandMark    -> pictogramme compact avec apparence selon Brightness

Professional home
  Scaffold
    appBar: ProfessionalAppBar (fixe)
    body: RefreshIndicator
      SingleChildScrollView (greeting + queue)

SOAP sheet
  Column
    ConsultationNotesHeader (drag/title/patient/actions)
    Expanded
      SingleChildScrollView
        ConsultationReasonBanner
        ConsultationNotesForm
```

## Choix de rendu du branding

- thème clair : PNG officiel transparent dans ses couleurs pleines ;
- thème sombre : même géométrie PNG, avec une apparence claire appliquée au
  rendu afin d’éviter tout aplat rectangulaire et de préserver la transparence ;
- aucune image n’est étirée, recadrée ni reconstruite ;
- les chemins restent centralisés dans `AppConfig` ;
- le pictogramme et le lockup partagent la même politique de thème.

La variante officielle blanche sur bleu reste un asset de communication aplati
avec fond. Elle n’est pas utilisée dans une surface dont le fond diffère, car son
rectangle deviendrait visible.

## Responsabilités / SOLID

- `AppBrandLockup` et `AppBrandMark` : présentation de marque uniquement ;
- `ProfessionalAppBar` : composition de navigation/préférences, sans logique
  métier ni accès réseau ;
- `ProfessionalProfileSheet` : composition du profil et émission d’intentions ;
- `FoundationPage` : orchestration des providers et du contenu scrollable ;
- `ConsultationNotesHeader` : header fixe compact ;
- `ConsultationReasonBanner` : présentation du motif dans le contenu.

Les contrôleurs Riverpod existants restent maîtres des changements de thème,
locale, biométrie, logout et refresh.

## Design system

- espacements sur l’échelle 4/8/16/24 ;
- rayons 4–8 px ; avatar seul autorisé en cercle ;
- toolbar de hauteur native compacte et fond `surface` ;
- séparation par bordure/ombre légère selon les tokens existants ;
- cibles tactiles `AppDesignTokens.minTouchTarget`.

## API / données / sécurité

- aucun endpoint ou DTO modifié ;
- aucun changement de payload SOAP ;
- aucun stockage, log ou permission ajouté ;
- aucune donnée des captures intégrée au dépôt ;
- backend maître inchangé.

## Stratégie de tests

- widget login sombre : logo transparent rendu sans conteneur blanc ;
- widget dashboard sombre : pictogramme en apparence contrastée ;
- layout dashboard : position de l’app bar inchangée après scroll ;
- widget SOAP : motif présent dans le scroll et header toujours visible ;
- golden login/dashboard sombre 393 × 852 ;
- `dart format`, `flutter analyze`, `flutter test`.

## Rollback

Revenir au commit du correctif restaure uniquement les widgets de présentation.
Aucune migration de données, de configuration ou d’API n’est nécessaire.

## Impact SemVer

PATCH mobile. Aucun breaking change.
