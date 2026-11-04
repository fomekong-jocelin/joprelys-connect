import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'auth_controller.dart';

/// Identity of the professional session currently exposed to the UI, or
/// `null` when nobody is signed in.
///
/// Every provider caching patient data must `ref.watch` it: logout, server
/// expiry or a different practitioner signing in then rebuilds the provider,
/// so the previous practitioner's data is never exposed again. Locking the
/// app keeps the same scope (the session is unchanged).
final authSessionScopeProvider = Provider<String?>((ref) {
  return ref.watch(
    authControllerProvider.select((auth) => _sessionScopeKey(auth.value)),
  );
});

String? _sessionScopeKey(AuthState? state) {
  final session = state?.session;
  if (state == null || session == null) return null;
  return switch (state.status) {
    AuthStatus.authenticated ||
    AuthStatus.locked ||
    AuthStatus.recoveryError => '${session.email}|${session.sessionId ?? ''}',
    AuthStatus.unauthenticated || AuthStatus.otpRequired => null,
  };
}
