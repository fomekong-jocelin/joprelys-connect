import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/effective_access_api.dart';
import '../domain/effective_access.dart';
import 'auth_session_scope.dart';

final effectiveAccessProvider = FutureProvider.autoDispose<EffectiveAccess>((
  ref,
) {
  // Rights belong to one session: never reuse them for another practitioner.
  ref.watch(authSessionScopeProvider);
  return ref.watch(effectiveAccessApiProvider).getMyAccess();
});

void refreshEffectiveAccess(WidgetRef ref) {
  ref.invalidate(effectiveAccessProvider);
}
