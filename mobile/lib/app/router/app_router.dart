import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/application/auth_controller.dart';
import '../../features/auth/presentation/pages/auth_loading_page.dart';
import '../../features/auth/presentation/pages/login_page.dart';
import '../../features/auth/presentation/pages/otp_page.dart';
import '../../features/auth/presentation/pages/recovery_page.dart';
import '../../features/auth/presentation/pages/unlock_page.dart';
import '../../features/foundation/presentation/pages/foundation_page.dart';
import 'route_names.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final refresh = _RouterRefreshNotifier();
  ref.listen(authControllerProvider, (previous, next) => refresh.notify());
  ref.onDispose(refresh.dispose);

  final router = GoRouter(
    initialLocation: AppRoutePath.foundation,
    refreshListenable: refresh,
    redirect: (context, state) {
      final auth = ref.read(authControllerProvider);
      return auth.when(
        loading: () => _redirectTo(state.matchedLocation, AppRoutePath.authLoading),
        error: (error, stackTrace) =>
            _redirectTo(state.matchedLocation, AppRoutePath.login),
        data: (authState) {
          final destination = switch (authState.status) {
            AuthStatus.unauthenticated => AppRoutePath.login,
            AuthStatus.otpRequired => AppRoutePath.otp,
            AuthStatus.authenticated => AppRoutePath.foundation,
            AuthStatus.locked => AppRoutePath.unlock,
            AuthStatus.recoveryError => AppRoutePath.recovery,
          };

          if (authState.status == AuthStatus.authenticated &&
              !AppRoutePath.isAuthPath(state.matchedLocation)) {
            return null;
          }
          return _redirectTo(state.matchedLocation, destination);
        },
      );
    },
    routes: [
      GoRoute(
        path: AppRoutePath.foundation,
        name: AppRouteName.foundation,
        builder: (context, state) => const FoundationPage(),
      ),
      GoRoute(
        path: AppRoutePath.authLoading,
        name: AppRouteName.authLoading,
        builder: (context, state) => const AuthLoadingPage(),
      ),
      GoRoute(
        path: AppRoutePath.login,
        name: AppRouteName.login,
        builder: (context, state) => const LoginPage(),
      ),
      GoRoute(
        path: AppRoutePath.otp,
        name: AppRouteName.otp,
        builder: (context, state) => const OtpPage(),
      ),
      GoRoute(
        path: AppRoutePath.unlock,
        name: AppRouteName.unlock,
        builder: (context, state) => const UnlockPage(),
      ),
      GoRoute(
        path: AppRoutePath.recovery,
        name: AppRouteName.recovery,
        builder: (context, state) => const RecoveryPage(),
      ),
    ],
  );
  ref.onDispose(router.dispose);
  return router;
});

String? _redirectTo(String currentLocation, String destination) {
  return currentLocation == destination ? null : destination;
}

final class _RouterRefreshNotifier extends ChangeNotifier {
  void notify() => notifyListeners();
}
