import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/application/active_queue_controller.dart';
import 'package:joprelys_mobile/features/dashboard/data/active_visits_api.dart';
import 'package:joprelys_mobile/features/dashboard/domain/active_visit.dart';

void main() {
  test('sorts active visits by queue arrival time', () async {
    final gateway = _FakeActiveVisitsGateway([
      _visit('late', DateTime.utc(2026, 7, 30, 10)),
      _visit('early', DateTime.utc(2026, 7, 30, 8)),
      _visit('middle', DateTime.utc(2026, 7, 30, 9)),
    ]);
    final container = ProviderContainer(
      overrides: [activeVisitsApiProvider.overrideWithValue(gateway)],
    );
    addTearDown(container.dispose);

    final visits = await container.read(activeQueueControllerProvider.future);

    expect(visits.map((visit) => visit.id), ['early', 'middle', 'late']);
  });

  test('refreshes the queue from the gateway', () async {
    final gateway = _FakeActiveVisitsGateway([
      _visit('first', DateTime.utc(2026, 7, 30, 8)),
    ]);
    final container = ProviderContainer(
      overrides: [activeVisitsApiProvider.overrideWithValue(gateway)],
    );
    addTearDown(container.dispose);

    await container.read(activeQueueControllerProvider.future);
    gateway.visits = [
      _visit('second', DateTime.utc(2026, 7, 30, 9)),
      _visit('first', DateTime.utc(2026, 7, 30, 8)),
    ];

    await container.read(activeQueueControllerProvider.notifier).refreshQueue();

    final refreshed = container
        .read(activeQueueControllerProvider)
        .requireValue;
    expect(refreshed.map((visit) => visit.id), ['first', 'second']);
    expect(gateway.calls, 2);
  });
}

ActiveVisit _visit(String id, DateTime queueSince) {
  return ActiveVisit(
    id: id,
    visitNumber: 'VIS-$id',
    patientId: 'PAT-$id',
    patientName: 'Patient $id',
    patientDpu: 'DPU-$id',
    reason: 'Consultation',
    orientation: 'Médecine générale',
    status: 'ACTIVE',
    arrivalAt: queueSince,
    createdAt: queueSince.subtract(const Duration(minutes: 5)),
  );
}

final class _FakeActiveVisitsGateway implements ActiveVisitsGateway {
  _FakeActiveVisitsGateway(this.visits);

  List<ActiveVisit> visits;
  int calls = 0;

  @override
  Future<List<ActiveVisit>> getActiveVisits() async {
    calls += 1;
    return [...visits];
  }
}
