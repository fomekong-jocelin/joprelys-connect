import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client_providers.dart';
import '../data/active_visits_api.dart';
import '../domain/active_visit.dart';

final activeVisitsApiProvider = Provider<ActiveVisitsApi>((ref) {
  return ActiveVisitsApi(ref.watch(apiClientProvider));
});

class ActiveQueueController extends AsyncNotifier<List<ActiveVisit>> {
  @override
  Future<List<ActiveVisit>> build() => _load();

  Future<void> refreshQueue() async {
    state = const AsyncLoading<List<ActiveVisit>>();
    state = await AsyncValue.guard(_load);
  }

  Future<List<ActiveVisit>> _load() async {
    final visits = await ref.read(activeVisitsApiProvider).getActiveVisits();
    final sorted = [...visits]
      ..sort((left, right) => left.queueSince.compareTo(right.queueSince));
    return List<ActiveVisit>.unmodifiable(sorted);
  }
}

final activeQueueControllerProvider =
    AsyncNotifierProvider<ActiveQueueController, List<ActiveVisit>>(
      ActiveQueueController.new,
    );
