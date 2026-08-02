import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/effective_access_api.dart';
import '../domain/effective_access.dart';

final effectiveAccessProvider = FutureProvider.autoDispose<EffectiveAccess>((
  ref,
) {
  return ref.watch(effectiveAccessApiProvider).getMyAccess();
});

void refreshEffectiveAccess(WidgetRef ref) {
  ref.invalidate(effectiveAccessProvider);
}
