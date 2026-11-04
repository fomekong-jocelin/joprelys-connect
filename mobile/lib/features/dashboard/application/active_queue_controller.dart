import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client_providers.dart';
import '../../auth/application/auth_session_scope.dart';
import '../data/active_visits_api.dart';
import '../domain/active_visit.dart';

final activeVisitsApiProvider = Provider<ActiveVisitsGateway>((ref) {
  return ActiveVisitsApi(ref.watch(apiClientProvider));
});

class ActiveQueueController extends AsyncNotifier<List<ActiveVisit>> {
  @override
  FutureOr<List<ActiveVisit>> build() {
    // Rebuilt (and emptied synchronously) whenever the session ends or changes.
    if (ref.watch(authSessionScopeProvider) == null) {
      return const <ActiveVisit>[];
    }
    return _load();
  }

  Future<void> refreshQueue() async {
    // Captured ref: it is unmounted if the session scope changes meanwhile,
    // so a late response can never land in another practitioner's state.
    final scopedRef = ref;
    if (scopedRef.read(authSessionScopeProvider) == null) {
      state = const AsyncData<List<ActiveVisit>>(<ActiveVisit>[]);
      return;
    }
    state = const AsyncLoading<List<ActiveVisit>>();
    final result = await AsyncValue.guard(_load);
    if (!scopedRef.mounted) return;
    state = result;
  }

  Future<List<ActiveVisit>> _load() async {
    final visits = await ref.read(activeVisitsApiProvider).getActiveVisits();
    final sorted = [...visits]
      ..sort((left, right) => left.queueSince.compareTo(right.queueSince));
    return List<ActiveVisit>.unmodifiable(sorted);
  }
}

final activeQueueControllerProvider =
    AsyncNotifierProvider.autoDispose<ActiveQueueController, List<ActiveVisit>>(
      ActiveQueueController.new,
    );
