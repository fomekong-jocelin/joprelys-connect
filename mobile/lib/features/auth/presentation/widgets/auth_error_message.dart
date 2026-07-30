import '../../../../l10n/app_localizations.dart';

String? authErrorMessage(AppLocalizations l10n, String? code) {
  if (code == null || code.isEmpty) {
    return null;
  }
  return switch (code) {
    'AUTH_SESSION_RECOVERY_UNAVAILABLE' => l10n.authRecoveryUnavailable,
    'BIOMETRIC_CANCELLED' => l10n.authBiometricCancelled,
    'BIOMETRIC_UNAVAILABLE' => l10n.authBiometricUnavailable,
    'BIOMETRIC_LOCKED' => l10n.authBiometricLocked,
    'BIOMETRIC_FAILED' => l10n.authBiometricFailed,
    'AUTH_OTP_CONTEXT_MISSING' => l10n.authOtpContextMissing,
    'AUTH_OTP_FAILED' || 'AUTH_OTP_INVALID_RESPONSE' => l10n.authOtpInvalid,
    'UNAUTHORIZED' ||
    'AUTHENTICATION_FAILED' ||
    'INVALID_CREDENTIALS' => l10n.authInvalidCredentials,
    'RATE_LIMITED' => l10n.authRateLimited,
    'NETWORK_UNAVAILABLE' => l10n.authNetworkUnavailable,
    'REQUEST_TIMEOUT' => l10n.authRequestTimeout,
    _ => l10n.authGenericError,
  };
}
