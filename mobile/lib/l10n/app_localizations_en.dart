// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'Joprelys Connect';

  @override
  String get foundationLanguageTitle => 'Language';

  @override
  String get foundationThemeTitle => 'Appearance';

  @override
  String foundationWelcomeTitle(String name) {
    return 'Welcome, $name';
  }

  @override
  String get foundationWelcomeSubtitle =>
      'Your secure professional workspace is ready.';

  @override
  String get foundationIdentityTitle => 'Professional account';

  @override
  String get foundationSecurityTitle => 'Application security';

  @override
  String get foundationBiometricEnabled =>
      'Biometric lock is enabled on this device.';

  @override
  String get foundationBiometricDisabled =>
      'Protect local access with this device’s biometrics.';

  @override
  String get dashboardQueueTitle => 'Patients waiting';

  @override
  String get dashboardQueueSubtitleLoading => 'Refreshing the queue…';

  @override
  String dashboardQueueSubtitle(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count patients in the queue',
      one: '1 patient in the queue',
      zero: 'No patients in the queue',
    );
    return '$_temp0';
  }

  @override
  String get dashboardQueueRefresh => 'Refresh the waiting queue';

  @override
  String get dashboardQueueTotal => 'Active patients';

  @override
  String get dashboardQueueTotalCompact => 'Waiting';

  @override
  String get dashboardQueueWithVitals => 'Vitals recorded';

  @override
  String get dashboardQueueWithVitalsCompact => 'Ready';

  @override
  String get dashboardQueueWithoutVitals => 'To assess';

  @override
  String get dashboardQueueEmptyTitle => 'Everything is up to date';

  @override
  String get dashboardQueueEmptyBody =>
      'No patient is currently waiting for care.';

  @override
  String get dashboardQueueLoadError =>
      'The waiting queue cannot be loaded right now.';

  @override
  String get dashboardQueueRetry => 'Retry';

  @override
  String get dashboardQueueVitalsReady => 'Vitals ready';

  @override
  String get dashboardQueueVitalsReadyCompact => 'Ready';

  @override
  String get dashboardQueueVitalsPending => 'To assess';

  @override
  String dashboardQueueVisitReference(String visitNumber, String patientDpu) {
    return '$visitNumber · DPU $patientDpu';
  }

  @override
  String dashboardQueueArrivedAt(String time) {
    return 'Arrived $time';
  }

  @override
  String get languageFrench => 'Français';

  @override
  String get languageEnglish => 'English';

  @override
  String get themeSystem => 'System';

  @override
  String get themeLight => 'Light';

  @override
  String get themeDark => 'Dark';

  @override
  String get authLoadingTitle => 'Securely restoring your session…';

  @override
  String get authLoginTitle => 'Professional access';

  @override
  String get authLoginSubtitle =>
      'Sign in to your secure Joprelys Connect workspace.';

  @override
  String get authEmailLabel => 'Email address';

  @override
  String get authEmailHint => 'first.last@clinic.com';

  @override
  String get authPasswordLabel => 'Password';

  @override
  String get authPasswordHint => 'Enter your password';

  @override
  String get authShowPassword => 'Show password';

  @override
  String get authHidePassword => 'Hide password';

  @override
  String get authInvalidEmail => 'Enter a valid email address.';

  @override
  String get authPasswordRequired => 'Password is required.';

  @override
  String get authSignIn => 'Sign in';

  @override
  String get authOtpTitle => 'Security verification';

  @override
  String authOtpSubtitle(String email) {
    return 'Enter the code sent for $email.';
  }

  @override
  String get authOtpLabel => 'Verification code';

  @override
  String get authOtpRequired => 'The verification code is required.';

  @override
  String get authVerifyOtp => 'Verify code';

  @override
  String get authBackToLogin => 'Back to sign-in';

  @override
  String get authUnlockTitle => 'Application locked';

  @override
  String authUnlockSubtitle(String name) {
    return 'Unlock $name’s session using this device’s biometrics.';
  }

  @override
  String get authUnlockAction => 'Unlock';

  @override
  String get authLogout => 'Sign out';

  @override
  String get authBiometricUnlockReason =>
      'Unlock your Joprelys Connect session';

  @override
  String get authRecoveryTitle => 'Session temporarily unavailable';

  @override
  String get authRecoverySubtitle =>
      'The local session is preserved, but the server cannot currently be reached.';

  @override
  String get authRetry => 'Retry';

  @override
  String get authForgetSession => 'Remove this session';

  @override
  String get authRecoveryUnavailable =>
      'The session cannot be restored right now. Check your connection and retry.';

  @override
  String get authBiometricCancelled => 'Biometric unlock was cancelled.';

  @override
  String get authBiometricUnavailable =>
      'Biometrics are unavailable or not configured on this device.';

  @override
  String get authBiometricLocked =>
      'Biometrics are temporarily locked. Use the device security options.';

  @override
  String get authBiometricFailed => 'Biometric verification failed.';

  @override
  String get authOtpContextMissing =>
      'The verification session expired. Start sign-in again.';

  @override
  String get authOtpInvalid => 'The verification code is invalid or expired.';

  @override
  String get authInvalidCredentials => 'Incorrect email address or password.';

  @override
  String get authRateLimited => 'Too many attempts. Wait before trying again.';

  @override
  String get authNetworkUnavailable => 'No network connection is available.';

  @override
  String get authRequestTimeout =>
      'The server is taking too long to respond. Try again.';

  @override
  String get authGenericError => 'An error is preventing sign-in. Try again.';

  @override
  String authSignedInAs(String name) {
    return 'Signed in as $name';
  }

  @override
  String authRoleLabel(String role) {
    return 'Role: $role';
  }

  @override
  String get authEnableBiometrics => 'Enable biometric lock';

  @override
  String get authDisableBiometrics => 'Disable biometric lock';

  @override
  String get authBiometricEnableReason =>
      'Confirm enabling Joprelys Connect biometric lock';
}
