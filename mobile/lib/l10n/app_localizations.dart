import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_fr.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('fr'),
  ];

  /// No description provided for @appTitle.
  ///
  /// In fr, this message translates to:
  /// **'Joprelys Connect'**
  String get appTitle;

  /// No description provided for @foundationLanguageTitle.
  ///
  /// In fr, this message translates to:
  /// **'Langue'**
  String get foundationLanguageTitle;

  /// No description provided for @foundationThemeTitle.
  ///
  /// In fr, this message translates to:
  /// **'Apparence'**
  String get foundationThemeTitle;

  /// No description provided for @foundationWelcomeTitle.
  ///
  /// In fr, this message translates to:
  /// **'Bienvenue, {name}'**
  String foundationWelcomeTitle(String name);

  /// No description provided for @foundationWelcomeSubtitle.
  ///
  /// In fr, this message translates to:
  /// **'Votre espace professionnel sécurisé est prêt.'**
  String get foundationWelcomeSubtitle;

  /// No description provided for @foundationIdentityTitle.
  ///
  /// In fr, this message translates to:
  /// **'Compte professionnel'**
  String get foundationIdentityTitle;

  /// No description provided for @foundationSecurityTitle.
  ///
  /// In fr, this message translates to:
  /// **'Sécurité de l’application'**
  String get foundationSecurityTitle;

  /// No description provided for @foundationBiometricEnabled.
  ///
  /// In fr, this message translates to:
  /// **'Le verrouillage biométrique est activé sur cet appareil.'**
  String get foundationBiometricEnabled;

  /// No description provided for @foundationBiometricDisabled.
  ///
  /// In fr, this message translates to:
  /// **'Protégez l’accès local avec la biométrie de cet appareil.'**
  String get foundationBiometricDisabled;

  /// No description provided for @languageFrench.
  ///
  /// In fr, this message translates to:
  /// **'Français'**
  String get languageFrench;

  /// No description provided for @languageEnglish.
  ///
  /// In fr, this message translates to:
  /// **'English'**
  String get languageEnglish;

  /// No description provided for @themeSystem.
  ///
  /// In fr, this message translates to:
  /// **'Système'**
  String get themeSystem;

  /// No description provided for @themeLight.
  ///
  /// In fr, this message translates to:
  /// **'Clair'**
  String get themeLight;

  /// No description provided for @themeDark.
  ///
  /// In fr, this message translates to:
  /// **'Sombre'**
  String get themeDark;

  /// No description provided for @authLoadingTitle.
  ///
  /// In fr, this message translates to:
  /// **'Restauration sécurisée de la session…'**
  String get authLoadingTitle;

  /// No description provided for @authLoginTitle.
  ///
  /// In fr, this message translates to:
  /// **'Connexion professionnelle'**
  String get authLoginTitle;

  /// No description provided for @authLoginSubtitle.
  ///
  /// In fr, this message translates to:
  /// **'Connectez-vous à votre espace professionnel sécurisé.'**
  String get authLoginSubtitle;

  /// No description provided for @authEmailLabel.
  ///
  /// In fr, this message translates to:
  /// **'Adresse e-mail'**
  String get authEmailLabel;

  /// No description provided for @authEmailHint.
  ///
  /// In fr, this message translates to:
  /// **'prenom.nom@clinique.com'**
  String get authEmailHint;

  /// No description provided for @authPasswordLabel.
  ///
  /// In fr, this message translates to:
  /// **'Mot de passe'**
  String get authPasswordLabel;

  /// No description provided for @authPasswordHint.
  ///
  /// In fr, this message translates to:
  /// **'Saisissez votre mot de passe'**
  String get authPasswordHint;

  /// No description provided for @authShowPassword.
  ///
  /// In fr, this message translates to:
  /// **'Afficher le mot de passe'**
  String get authShowPassword;

  /// No description provided for @authHidePassword.
  ///
  /// In fr, this message translates to:
  /// **'Masquer le mot de passe'**
  String get authHidePassword;

  /// No description provided for @authInvalidEmail.
  ///
  /// In fr, this message translates to:
  /// **'Saisissez une adresse e-mail valide.'**
  String get authInvalidEmail;

  /// No description provided for @authPasswordRequired.
  ///
  /// In fr, this message translates to:
  /// **'Le mot de passe est requis.'**
  String get authPasswordRequired;

  /// No description provided for @authSignIn.
  ///
  /// In fr, this message translates to:
  /// **'Se connecter'**
  String get authSignIn;

  /// No description provided for @authOtpTitle.
  ///
  /// In fr, this message translates to:
  /// **'Vérification de sécurité'**
  String get authOtpTitle;

  /// No description provided for @authOtpSubtitle.
  ///
  /// In fr, this message translates to:
  /// **'Saisissez le code envoyé pour {email}.'**
  String authOtpSubtitle(String email);

  /// No description provided for @authOtpLabel.
  ///
  /// In fr, this message translates to:
  /// **'Code de vérification'**
  String get authOtpLabel;

  /// No description provided for @authOtpRequired.
  ///
  /// In fr, this message translates to:
  /// **'Le code de vérification est requis.'**
  String get authOtpRequired;

  /// No description provided for @authVerifyOtp.
  ///
  /// In fr, this message translates to:
  /// **'Vérifier le code'**
  String get authVerifyOtp;

  /// No description provided for @authBackToLogin.
  ///
  /// In fr, this message translates to:
  /// **'Revenir à la connexion'**
  String get authBackToLogin;

  /// No description provided for @authUnlockTitle.
  ///
  /// In fr, this message translates to:
  /// **'Application verrouillée'**
  String get authUnlockTitle;

  /// No description provided for @authUnlockSubtitle.
  ///
  /// In fr, this message translates to:
  /// **'Déverrouillez la session de {name} avec la biométrie de cet appareil.'**
  String authUnlockSubtitle(String name);

  /// No description provided for @authUnlockAction.
  ///
  /// In fr, this message translates to:
  /// **'Déverrouiller'**
  String get authUnlockAction;

  /// No description provided for @authLogout.
  ///
  /// In fr, this message translates to:
  /// **'Se déconnecter'**
  String get authLogout;

  /// No description provided for @authBiometricUnlockReason.
  ///
  /// In fr, this message translates to:
  /// **'Déverrouiller votre session Joprelys Connect'**
  String get authBiometricUnlockReason;

  /// No description provided for @authRecoveryTitle.
  ///
  /// In fr, this message translates to:
  /// **'Session temporairement indisponible'**
  String get authRecoveryTitle;

  /// No description provided for @authRecoverySubtitle.
  ///
  /// In fr, this message translates to:
  /// **'La session locale est conservée, mais le serveur ne peut pas être joint pour le moment.'**
  String get authRecoverySubtitle;

  /// No description provided for @authRetry.
  ///
  /// In fr, this message translates to:
  /// **'Réessayer'**
  String get authRetry;

  /// No description provided for @authForgetSession.
  ///
  /// In fr, this message translates to:
  /// **'Supprimer cette session'**
  String get authForgetSession;

  /// No description provided for @authRecoveryUnavailable.
  ///
  /// In fr, this message translates to:
  /// **'Impossible de restaurer la session actuellement. Vérifiez votre connexion puis réessayez.'**
  String get authRecoveryUnavailable;

  /// No description provided for @authBiometricCancelled.
  ///
  /// In fr, this message translates to:
  /// **'Le déverrouillage biométrique a été annulé.'**
  String get authBiometricCancelled;

  /// No description provided for @authBiometricUnavailable.
  ///
  /// In fr, this message translates to:
  /// **'La biométrie n’est pas disponible ou configurée sur cet appareil.'**
  String get authBiometricUnavailable;

  /// No description provided for @authBiometricLocked.
  ///
  /// In fr, this message translates to:
  /// **'La biométrie est temporairement verrouillée. Utilisez les options de sécurité de l’appareil.'**
  String get authBiometricLocked;

  /// No description provided for @authBiometricFailed.
  ///
  /// In fr, this message translates to:
  /// **'Le contrôle biométrique a échoué.'**
  String get authBiometricFailed;

  /// No description provided for @authOtpContextMissing.
  ///
  /// In fr, this message translates to:
  /// **'La vérification a expiré. Recommencez la connexion.'**
  String get authOtpContextMissing;

  /// No description provided for @authOtpInvalid.
  ///
  /// In fr, this message translates to:
  /// **'Le code de vérification est invalide ou expiré.'**
  String get authOtpInvalid;

  /// No description provided for @authInvalidCredentials.
  ///
  /// In fr, this message translates to:
  /// **'Adresse e-mail ou mot de passe incorrect.'**
  String get authInvalidCredentials;

  /// No description provided for @authRateLimited.
  ///
  /// In fr, this message translates to:
  /// **'Trop de tentatives. Patientez avant de réessayer.'**
  String get authRateLimited;

  /// No description provided for @authNetworkUnavailable.
  ///
  /// In fr, this message translates to:
  /// **'Aucune connexion réseau disponible.'**
  String get authNetworkUnavailable;

  /// No description provided for @authRequestTimeout.
  ///
  /// In fr, this message translates to:
  /// **'Le serveur met trop de temps à répondre. Réessayez.'**
  String get authRequestTimeout;

  /// No description provided for @authGenericError.
  ///
  /// In fr, this message translates to:
  /// **'Une erreur empêche la connexion. Réessayez.'**
  String get authGenericError;

  /// No description provided for @authSignedInAs.
  ///
  /// In fr, this message translates to:
  /// **'Connecté en tant que {name}'**
  String authSignedInAs(String name);

  /// No description provided for @authRoleLabel.
  ///
  /// In fr, this message translates to:
  /// **'Rôle : {role}'**
  String authRoleLabel(String role);

  /// No description provided for @authEnableBiometrics.
  ///
  /// In fr, this message translates to:
  /// **'Activer le verrouillage biométrique'**
  String get authEnableBiometrics;

  /// No description provided for @authDisableBiometrics.
  ///
  /// In fr, this message translates to:
  /// **'Désactiver le verrouillage biométrique'**
  String get authDisableBiometrics;

  /// No description provided for @authBiometricEnableReason.
  ///
  /// In fr, this message translates to:
  /// **'Confirmer l’activation du verrouillage biométrique Joprelys Connect'**
  String get authBiometricEnableReason;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'fr'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'fr':
      return AppLocalizationsFr();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
