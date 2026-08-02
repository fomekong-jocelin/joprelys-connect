import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/lifecycle/app_activity_registry.dart';
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
  ref.listen(appForegroundActivityProvider, (previous, next) => refresh.notify());
  ref.onDispose(refresh.dispose);

  final router = createAppRouter(
    refreshListenable: refresh,
    redirect: (context, state) {
      final auth = ref.read(authControllerProvider);
      final preserveLockedRoute = ref
          .read(appForegroundActivityProvider)
          .preservesRouteOnLock;
      return auth.when(
        loading: () =>
            redirectToRoute(state.matchedLocation, AppRoutePath.authLoading),
        error: (error, stackTrace) =>
            redirectToRoute(state.matchedLocation, AppRoutePath.login),
        data: (authState) => authRedirect(
          authState,
          state.matchedLocation,
          preserveLockedRoute: preserveLockedRoute,
        ),
      );
    },
  );
  ref.onDispose(router.dispose);
  return router;
});

GoRouter createAppRouter({
  Listenable? refreshListenable,
  GoRouterRedirect? redirect,
}) {
  return GoRouter(
    initialLocation: AppRoutePath.foundation,
    refreshListenable: refreshListenable,
    redirect: redirect,
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
}

String authRouteFor(AuthStatus status) {
  return switch (status) {
    AuthStatus.unauthenticated => AppRoutePath.login,
    AuthStatus.otpRequired => AppRoutePath.otp,
    AuthStatus.authenticated => AppRoutePath.foundation,
    AuthStatus.locked => AppRoutePath.unlock,
    AuthStatus.recoveryError => AppRoutePath.recovery,
  };
}

String? authRedirect(
  AuthState authState,
  String currentLocation, {
  bool preserveLockedRoute = false,
}) {
  if (authState.status == AuthStatus.locked &&
      preserveLockedRoute &&
      !AppRoutePath.isAuthPath(currentLocation)) {
    return null;
  }

  final destination = authRouteFor(authState.status);
  if (authState.status == AuthStatus.authenticated &&
      !AppRoutePath.isAuthPath(currentLocation)) {
    return null;
  }
  return redirectToRoute(currentLocation, destination);
}

String? redirectToRoute(String currentLocation, String destination) {
  return currentLocation == destination ? null : destination;
}

final class _RouterRefreshNotifier extends ChangeNotifier {
  void notify() => notifyListeners();
}
