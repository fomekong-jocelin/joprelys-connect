import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/app/router/app_router.dart';
import 'package:joprelys_mobile/app/router/route_names.dart';
import 'package:joprelys_mobile/features/auth/application/auth_controller.dart';
import 'package:joprelys_mobile/features/auth/domain/professional_session.dart';

void main() {
  test('each auth state resolves to its canonical route', () {
    expect(authRouteFor(AuthStatus.unauthenticated), AppRoutePath.login);
    expect(authRouteFor(AuthStatus.otpRequired), AppRoutePath.otp);
    expect(authRouteFor(AuthStatus.authenticated), AppRoutePath.foundation);
    expect(authRouteFor(AuthStatus.locked), AppRoutePath.unlock);
    expect(authRouteFor(AuthStatus.recoveryError), AppRoutePath.recovery);
  });

  test('unauthenticated users cannot remain on the protected route', () {
    const state = AuthState.unauthenticated();

    expect(authRedirect(state, AppRoutePath.foundation), AppRoutePath.login);
    expect(authRedirect(state, AppRoutePath.login), isNull);
  });

  test('OTP, lock and recovery states cannot reach the protected route', () {
    final session = professionalSession();

    expect(
      authRedirect(
        const AuthState.otpRequired('doctor@example.test'),
        AppRoutePath.foundation,
      ),
      AppRoutePath.otp,
    );
    expect(
      authRedirect(AuthState.locked(session), AppRoutePath.foundation),
      AppRoutePath.unlock,
    );
    expect(
      authRedirect(AuthState.recoveryError(session), AppRoutePath.foundation),
      AppRoutePath.recovery,
    );
  });

  test('authenticated users leave auth routes and keep protected locations', () {
    final state = AuthState.authenticated(professionalSession());

    expect(authRedirect(state, AppRoutePath.login), AppRoutePath.foundation);
    expect(authRedirect(state, AppRoutePath.foundation), isNull);
    expect(authRedirect(state, '/future-protected-route'), isNull);
  });
}

ProfessionalSession professionalSession() {
  return ProfessionalSession(
    accessToken: 'access-token',
    expiresAt: DateTime.utc(2026, 7, 30, 12, 15),
    sessionExpiresAt: DateTime.utc(2026, 8, 6, 12),
    sessionId: 'session-id',
    email: 'doctor@example.test',
    name: 'Doctor Test',
    role: 'DOCTOR',
  );
}
