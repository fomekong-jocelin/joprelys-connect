import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../domain/professional_session.dart';
import 'auth_providers.dart';
import 'auth_session_manager.dart';
import 'biometric_authenticator.dart';

enum AuthStatus {
  unauthenticated,
  otpRequired,
  authenticated,
  locked,
  recoveryError,
}

final class AuthState {
  const AuthState({
    required this.status,
    this.session,
    this.pendingEmail,
    this.errorCode,
  });

  const AuthState.unauthenticated({String? errorCode})
    : this(status: AuthStatus.unauthenticated, errorCode: errorCode);

  const AuthState.otpRequired(String email, {String? errorCode})
    : this(
        status: AuthStatus.otpRequired,
        pendingEmail: email,
        errorCode: errorCode,
      );

  const AuthState.authenticated(ProfessionalSession session)
    : this(status: AuthStatus.authenticated, session: session);

  const AuthState.locked(ProfessionalSession session, {String? errorCode})
    : this(status: AuthStatus.locked, session: session, errorCode: errorCode);

  const AuthState.recoveryError(
    ProfessionalSession session, {
    String? errorCode,
  }) : this(
         status: AuthStatus.recoveryError,
         session: session,
         errorCode: errorCode,
       );

  final AuthStatus status;
  final ProfessionalSession? session;
  final String? pendingEmail;
  final String? errorCode;

  bool get isAuthenticated => status == AuthStatus.authenticated;
}

class AuthController extends AsyncNotifier<AuthState> {
  @override
  Future<AuthState> build() => _restore();

  Future<void> login({required String email, required String password}) async {
    state = const AsyncLoading();
    try {
      final result = await ref
          .read(authSessionManagerProvider)
          .login(email: email, password: password);
      if (result.requiresOtp) {
        state = AsyncData(AuthState.otpRequired(result.email));
        return;
      }
      final session = result.session;
      state = session == null
          ? const AsyncData(
              AuthState.unauthenticated(errorCode: 'AUTH_RESPONSE_INVALID'),
            )
          : AsyncData(AuthState.authenticated(session));
    } on ApiException catch (error) {
      state = AsyncData(AuthState.unauthenticated(errorCode: error.code));
    } catch (_) {
      state = const AsyncData(
        AuthState.unauthenticated(errorCode: 'AUTH_LOGIN_FAILED'),
      );
    }
  }

  Future<void> verifyOtp(String otpCode) async {
    final current = state.value;
    final email = current?.pendingEmail;
    if (email == null || email.isEmpty) {
      state = const AsyncData(
        AuthState.unauthenticated(errorCode: 'AUTH_OTP_CONTEXT_MISSING'),
      );
      return;
    }

    state = const AsyncLoading();
    try {
      final result = await ref
          .read(authSessionManagerProvider)
          .verifyOtp(email: email, otpCode: otpCode);
      final session = result.session;
      if (session == null) {
        state = AsyncData(
          AuthState.otpRequired(email, errorCode: 'AUTH_OTP_INVALID_RESPONSE'),
        );
        return;
      }
      state = AsyncData(AuthState.authenticated(session));
    } on ApiException catch (error) {
      state = AsyncData(AuthState.otpRequired(email, errorCode: error.code));
    } catch (_) {
      state = AsyncData(
        AuthState.otpRequired(email, errorCode: 'AUTH_OTP_FAILED'),
      );
    }
  }

  Future<void> cancelOtp() async {
    state = const AsyncLoading();
    try {
      await ref.read(authSessionManagerProvider).clearLocalSession();
      state = const AsyncData(AuthState.unauthenticated());
    } catch (_) {
      state = const AsyncData(
        AuthState.unauthenticated(
          errorCode: 'AUTH_LOCAL_SESSION_CLEAR_FAILED',
        ),
      );
    }
  }

  Future<void> retryRestore() async {
    state = const AsyncLoading();
    state = AsyncData(await _restore());
  }

  Future<void> logout() async {
    final previous = state.value;
    state = const AsyncLoading();
    try {
      await ref.read(authSessionManagerProvider).logout();
      state = const AsyncData(AuthState.unauthenticated());
    } catch (_) {
      final session = previous?.session;
      state = session == null
          ? const AsyncData(
              AuthState.unauthenticated(
                errorCode: 'AUTH_LOCAL_SESSION_CLEAR_FAILED',
              ),
            )
          : AsyncData(
              AuthState.recoveryError(
                session,
                errorCode: 'AUTH_LOCAL_SESSION_CLEAR_FAILED',
              ),
            );
    }
  }

  Future<void> forgetSession() async {
    final previous = state.value;
    state = const AsyncLoading();
    try {
      await ref.read(authSessionManagerProvider).clearLocalSession();
      state = const AsyncData(AuthState.unauthenticated());
    } catch (_) {
      final session = previous?.session;
      state = session == null
          ? const AsyncData(
              AuthState.unauthenticated(
                errorCode: 'AUTH_LOCAL_SESSION_CLEAR_FAILED',
              ),
            )
          : AsyncData(
              AuthState.recoveryError(
                session,
                errorCode: 'AUTH_LOCAL_SESSION_CLEAR_FAILED',
              ),
            );
    }
  }

  Future<bool> enableBiometrics({required String reason}) async {
    final current = state.value;
    final session = current?.session;
    if (session == null || !current!.isAuthenticated) {
      return false;
    }

    final authenticator = ref.read(biometricAuthenticatorProvider);
    if (!await authenticator.isAvailable()) {
      state = AsyncData(
        AuthState.authenticated(session).withError('BIOMETRIC_UNAVAILABLE'),
      );
      return false;
    }
    final result = await authenticator.authenticate(reason: reason);
    if (result != BiometricUnlockResult.success) {
      state = AsyncData(
        AuthState.authenticated(session).withError(_biometricErrorCode(result)),
      );
      return false;
    }

    final updated = await ref
        .read(authSessionManagerProvider)
        .setBiometricEnabled(true);
    if (updated == null) {
      state = const AsyncData(AuthState.unauthenticated());
      return false;
    }
    state = AsyncData(AuthState.authenticated(updated));
    return true;
  }

  Future<void> disableBiometrics() async {
    final updated = await ref
        .read(authSessionManagerProvider)
        .setBiometricEnabled(false);
    state = updated == null
        ? const AsyncData(AuthState.unauthenticated())
        : AsyncData(AuthState.authenticated(updated));
  }

  void lock() {
    final current = state.value;
    final session = current?.session;
    if (current?.isAuthenticated == true && session?.biometricEnabled == true) {
      state = AsyncData(AuthState.locked(session!));
    }
  }

  Future<void> unlock({required String reason}) async {
    final current = state.value;
    final session = current?.session;
    if (current?.status != AuthStatus.locked || session == null) {
      return;
    }

    final result = await ref
        .read(biometricAuthenticatorProvider)
        .authenticate(reason: reason);
    if (result == BiometricUnlockResult.success) {
      state = AsyncData(AuthState.authenticated(session));
      return;
    }
    state = AsyncData(
      AuthState.locked(session, errorCode: _biometricErrorCode(result)),
    );
  }

  Future<AuthState> _restore() async {
    try {
      final session = await ref.read(authSessionManagerProvider).restore();
      if (session == null) {
        return const AuthState.unauthenticated();
      }
      if (session.biometricEnabled) {
        return AuthState.locked(session);
      }
      return AuthState.authenticated(session);
    } on SessionRecoveryUnavailable catch (error) {
      return AuthState.recoveryError(
        error.session,
        errorCode: 'AUTH_SESSION_RECOVERY_UNAVAILABLE',
      );
    }
  }

  String _biometricErrorCode(BiometricUnlockResult result) {
    return switch (result) {
      BiometricUnlockResult.success => '',
      BiometricUnlockResult.cancelled => 'BIOMETRIC_CANCELLED',
      BiometricUnlockResult.unavailable => 'BIOMETRIC_UNAVAILABLE',
      BiometricUnlockResult.locked => 'BIOMETRIC_LOCKED',
      BiometricUnlockResult.failed => 'BIOMETRIC_FAILED',
    };
  }
}

extension on AuthState {
  AuthState withError(String errorCode) {
    return AuthState(
      status: status,
      session: session,
      pendingEmail: pendingEmail,
      errorCode: errorCode,
    );
  }
}

final authControllerProvider = AsyncNotifierProvider<AuthController, AuthState>(
  AuthController.new,
);
