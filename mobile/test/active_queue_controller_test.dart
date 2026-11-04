import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/auth/application/auth_controller.dart';
import 'package:joprelys_mobile/features/auth/application/auth_session_scope.dart';
import 'package:joprelys_mobile/features/auth/domain/professional_session.dart';
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
      overrides: [
        authSessionScopeProvider.overrideWithValue('doctor-a|session-a'),
        activeVisitsApiProvider.overrideWithValue(gateway),
      ],
    );
    addTearDown(container.dispose);
    container.listen(activeQueueControllerProvider, (_, _) {});

    final visits = await container.read(activeQueueControllerProvider.future);

    expect(visits.map((visit) => visit.id), ['early', 'middle', 'late']);
  });

  test('refreshes the queue from the gateway', () async {
    final gateway = _FakeActiveVisitsGateway([
      _visit('first', DateTime.utc(2026, 7, 30, 8)),
    ]);
    final container = ProviderContainer(
      overrides: [
        authSessionScopeProvider.overrideWithValue('doctor-a|session-a'),
        activeVisitsApiProvider.overrideWithValue(gateway),
      ],
    );
    addTearDown(container.dispose);
    container.listen(activeQueueControllerProvider, (_, _) {});

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

  test('never exposes the previous practitioner queue after logout and '
      'a different sign-in', () async {
    final auth = _SwitchableAuthController(
      AuthState.authenticated(_session('doctor-a@example.test', 'session-a')),
    );
    final gateway = _FakeActiveVisitsGateway([
      _visit('queue-of-a', DateTime.utc(2026, 7, 30, 8)),
    ]);
    final container = ProviderContainer(
      overrides: [
        authControllerProvider.overrideWith(() => auth),
        activeVisitsApiProvider.overrideWithValue(gateway),
      ],
    );
    addTearDown(container.dispose);
    final history = <List<String>>[];
    container.listen(activeQueueControllerProvider, (_, next) {
      history.add(
        next.value?.map((visit) => visit.id).toList() ?? const <String>[],
      );
    }, fireImmediately: true);
    await container.read(authControllerProvider.future);

    final first = await container.read(activeQueueControllerProvider.future);
    expect(first.map((visit) => visit.id), ['queue-of-a']);

    final logoutIndex = history.length;
    auth.emit(const AuthState.unauthenticated());
    await Future<void>.delayed(Duration.zero);

    expect(container.read(activeQueueControllerProvider).value, isEmpty);

    gateway.visits = [_visit('queue-of-b', DateTime.utc(2026, 7, 30, 9))];
    auth.emit(
      AuthState.authenticated(_session('doctor-b@example.test', 'session-b')),
    );
    final second = await container.read(activeQueueControllerProvider.future);

    expect(second.map((visit) => visit.id), ['queue-of-b']);
    final afterLogout = history.sublist(logoutIndex);
    expect(afterLogout, isNotEmpty);
    expect(
      afterLogout.expand((ids) => ids),
      isNot(contains('queue-of-a')),
      reason: 'the previous practitioner queue must never be exposed again',
    );
    expect(afterLogout.last, ['queue-of-b']);
    expect(gateway.calls, 2);
  });

  test(
    'drops a refresh response that completes after the session ended',
    () async {
      final auth = _SwitchableAuthController(
        AuthState.authenticated(_session('doctor-a@example.test', 'session-a')),
      );
      final gateway = _FakeActiveVisitsGateway([
        _visit('queue-of-a', DateTime.utc(2026, 7, 30, 8)),
      ]);
      final container = ProviderContainer(
        overrides: [
          authControllerProvider.overrideWith(() => auth),
          activeVisitsApiProvider.overrideWithValue(gateway),
        ],
      );
      addTearDown(container.dispose);
      container.listen(activeQueueControllerProvider, (_, _) {});
      await container.read(authControllerProvider.future);
      await container.read(activeQueueControllerProvider.future);

      gateway.delay = const Duration(milliseconds: 20);
      final pendingRefresh = container
          .read(activeQueueControllerProvider.notifier)
          .refreshQueue();
      auth.emit(const AuthState.unauthenticated());
      await pendingRefresh;

      expect(container.read(activeQueueControllerProvider).value, isEmpty);
    },
  );
}

ProfessionalSession _session(String email, String sessionId) {
  return ProfessionalSession(
    accessToken: 'fixture-$sessionId',
    expiresAt: DateTime.utc(2030),
    sessionId: sessionId,
    email: email,
    name: 'Fixture Doctor',
    role: 'MEDECIN',
  );
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

final class _SwitchableAuthController extends AuthController {
  _SwitchableAuthController(this._initial);

  final AuthState _initial;

  @override
  Future<AuthState> build() async => _initial;

  void emit(AuthState next) => state = AsyncData(next);
}

final class _FakeActiveVisitsGateway implements ActiveVisitsGateway {
  _FakeActiveVisitsGateway(this.visits);

  List<ActiveVisit> visits;
  Duration delay = Duration.zero;
  int calls = 0;

  @override
  Future<List<ActiveVisit>> getActiveVisits() async {
    calls += 1;
    final snapshot = [...visits];
    if (delay > Duration.zero) await Future<void>.delayed(delay);
    return snapshot;
  }
}
