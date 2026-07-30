import 'package:local_auth/local_auth.dart';

abstract interface class BiometricAuthenticator {
  Future<bool> isAvailable();

  Future<BiometricUnlockResult> authenticate({required String reason});
}

enum BiometricUnlockResult { success, cancelled, unavailable, locked, failed }

final class LocalBiometricAuthenticator implements BiometricAuthenticator {
  LocalBiometricAuthenticator({LocalAuthentication? localAuthentication})
    : _localAuthentication = localAuthentication ?? LocalAuthentication();

  final LocalAuthentication _localAuthentication;

  @override
  Future<bool> isAvailable() async {
    try {
      final supported = await _localAuthentication.isDeviceSupported();
      if (!supported || !await _localAuthentication.canCheckBiometrics) {
        return false;
      }
      return (await _localAuthentication.getAvailableBiometrics()).isNotEmpty;
    } on LocalAuthException {
      return false;
    }
  }

  @override
  Future<BiometricUnlockResult> authenticate({required String reason}) async {
    try {
      final authenticated = await _localAuthentication.authenticate(
        localizedReason: reason,
        biometricOnly: true,
        persistAcrossBackgrounding: true,
      );
      return authenticated
          ? BiometricUnlockResult.success
          : BiometricUnlockResult.cancelled;
    } on LocalAuthException catch (error) {
      if (error.code == LocalAuthExceptionCode.userCanceled ||
          error.code == LocalAuthExceptionCode.systemCanceled) {
        return BiometricUnlockResult.cancelled;
      }
      if (error.code == LocalAuthExceptionCode.temporaryLockout ||
          error.code == LocalAuthExceptionCode.biometricLockout) {
        return BiometricUnlockResult.locked;
      }
      if (error.code == LocalAuthExceptionCode.noBiometricHardware ||
          error.code == LocalAuthExceptionCode.noBiometricsEnrolled ||
          error.code == LocalAuthExceptionCode.noCredentialsSet ||
          error.code ==
              LocalAuthExceptionCode.biometricHardwareTemporarilyUnavailable) {
        return BiometricUnlockResult.unavailable;
      }
      return BiometricUnlockResult.failed;
    } catch (_) {
      return BiometricUnlockResult.failed;
    }
  }
}
