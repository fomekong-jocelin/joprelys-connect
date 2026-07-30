// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for French (`fr`).
class AppLocalizationsFr extends AppLocalizations {
  AppLocalizationsFr([String locale = 'fr']) : super(locale);

  @override
  String get appTitle => 'Joprelys Connect';

  @override
  String get foundationLanguageTitle => 'Langue';

  @override
  String get foundationThemeTitle => 'Apparence';

  @override
  String foundationWelcomeTitle(String name) {
    return 'Bienvenue, $name';
  }

  @override
  String get foundationWelcomeSubtitle =>
      'Votre espace professionnel sécurisé est prêt.';

  @override
  String get foundationIdentityTitle => 'Compte professionnel';

  @override
  String get foundationSecurityTitle => 'Sécurité de l’application';

  @override
  String get foundationBiometricEnabled =>
      'Le verrouillage biométrique est activé sur cet appareil.';

  @override
  String get foundationBiometricDisabled =>
      'Protégez l’accès local avec la biométrie de cet appareil.';

  @override
  String get dashboardQueueTitle => 'Patients en attente';

  @override
  String get dashboardQueueSubtitleLoading => 'Actualisation de la file…';

  @override
  String dashboardQueueSubtitle(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count patients dans la file',
      one: '1 patient dans la file',
      zero: 'Aucun patient dans la file',
    );
    return '$_temp0';
  }

  @override
  String get dashboardQueueRefresh => 'Actualiser la file d’attente';

  @override
  String get dashboardQueueTotal => 'Patients actifs';

  @override
  String get dashboardQueueTotalCompact => 'En attente';

  @override
  String get dashboardQueueWithVitals => 'Constantes saisies';

  @override
  String get dashboardQueueWithVitalsCompact => 'Prêts';

  @override
  String get dashboardQueueWithoutVitals => 'À évaluer';

  @override
  String get dashboardQueueEmptyTitle => 'Tout est à jour';

  @override
  String get dashboardQueueEmptyBody =>
      'Aucun patient n’attend actuellement une prise en charge.';

  @override
  String get dashboardQueueLoadError =>
      'La file d’attente ne peut pas être chargée pour le moment.';

  @override
  String get dashboardQueueRetry => 'Réessayer';

  @override
  String get dashboardQueueVitalsReady => 'Constantes OK';

  @override
  String get dashboardQueueVitalsReadyCompact => 'Prêt';

  @override
  String get dashboardQueueVitalsPending => 'À évaluer';

  @override
  String dashboardQueueVisitReference(String visitNumber, String patientDpu) {
    return '$visitNumber · DPU $patientDpu';
  }

  @override
  String dashboardQueueArrivedAt(String time) {
    return 'Arrivée $time';
  }

  @override
  String get languageFrench => 'Français';

  @override
  String get languageEnglish => 'English';

  @override
  String get themeSystem => 'Système';

  @override
  String get themeLight => 'Clair';

  @override
  String get themeDark => 'Sombre';

  @override
  String get authLoadingTitle => 'Restauration sécurisée de la session…';

  @override
  String get authLoginTitle => 'Connexion professionnelle';

  @override
  String get authLoginSubtitle =>
      'Connectez-vous à votre espace professionnel sécurisé.';

  @override
  String get authEmailLabel => 'Adresse e-mail';

  @override
  String get authEmailHint => 'prenom.nom@clinique.com';

  @override
  String get authPasswordLabel => 'Mot de passe';

  @override
  String get authPasswordHint => 'Saisissez votre mot de passe';

  @override
  String get authShowPassword => 'Afficher le mot de passe';

  @override
  String get authHidePassword => 'Masquer le mot de passe';

  @override
  String get authInvalidEmail => 'Saisissez une adresse e-mail valide.';

  @override
  String get authPasswordRequired => 'Le mot de passe est requis.';

  @override
  String get authSignIn => 'Se connecter';

  @override
  String get authOtpTitle => 'Vérification de sécurité';

  @override
  String authOtpSubtitle(String email) {
    return 'Saisissez le code envoyé pour $email.';
  }

  @override
  String get authOtpLabel => 'Code de vérification';

  @override
  String get authOtpRequired => 'Le code de vérification est requis.';

  @override
  String get authVerifyOtp => 'Vérifier le code';

  @override
  String get authBackToLogin => 'Revenir à la connexion';

  @override
  String get authUnlockTitle => 'Application verrouillée';

  @override
  String authUnlockSubtitle(String name) {
    return 'Déverrouillez la session de $name avec la biométrie de cet appareil.';
  }

  @override
  String get authUnlockAction => 'Déverrouiller';

  @override
  String get authLogout => 'Se déconnecter';

  @override
  String get authBiometricUnlockReason =>
      'Déverrouiller votre session Joprelys Connect';

  @override
  String get authRecoveryTitle => 'Session temporairement indisponible';

  @override
  String get authRecoverySubtitle =>
      'La session locale est conservée, mais le serveur ne peut pas être joint pour le moment.';

  @override
  String get authRetry => 'Réessayer';

  @override
  String get authForgetSession => 'Supprimer cette session';

  @override
  String get authRecoveryUnavailable =>
      'Impossible de restaurer la session actuellement. Vérifiez votre connexion puis réessayez.';

  @override
  String get authBiometricCancelled =>
      'Le déverrouillage biométrique a été annulé.';

  @override
  String get authBiometricUnavailable =>
      'La biométrie n’est pas disponible ou configurée sur cet appareil.';

  @override
  String get authBiometricLocked =>
      'La biométrie est temporairement verrouillée. Utilisez les options de sécurité de l’appareil.';

  @override
  String get authBiometricFailed => 'Le contrôle biométrique a échoué.';

  @override
  String get authOtpContextMissing =>
      'La vérification a expiré. Recommencez la connexion.';

  @override
  String get authOtpInvalid =>
      'Le code de vérification est invalide ou expiré.';

  @override
  String get authInvalidCredentials =>
      'Adresse e-mail ou mot de passe incorrect.';

  @override
  String get authRateLimited =>
      'Trop de tentatives. Patientez avant de réessayer.';

  @override
  String get authNetworkUnavailable => 'Aucune connexion réseau disponible.';

  @override
  String get authRequestTimeout =>
      'Le serveur met trop de temps à répondre. Réessayez.';

  @override
  String get authGenericError => 'Une erreur empêche la connexion. Réessayez.';

  @override
  String authSignedInAs(String name) {
    return 'Connecté en tant que $name';
  }

  @override
  String authRoleLabel(String role) {
    return 'Rôle : $role';
  }

  @override
  String get authEnableBiometrics => 'Activer le verrouillage biométrique';

  @override
  String get authDisableBiometrics => 'Désactiver le verrouillage biométrique';

  @override
  String get authBiometricEnableReason =>
      'Confirmer l’activation du verrouillage biométrique Joprelys Connect';
}
