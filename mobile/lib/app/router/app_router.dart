import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/foundation/presentation/pages/foundation_page.dart';
import 'route_names.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final router = createAppRouter();
  ref.onDispose(router.dispose);
  return router;
});

GoRouter createAppRouter() {
  return GoRouter(
    initialLocation: AppRoutePath.foundation,
    routes: [
      GoRoute(
        path: AppRoutePath.foundation,
        name: AppRouteName.foundation,
        builder: (context, state) => const FoundationPage(),
      ),
    ],
  );
}
