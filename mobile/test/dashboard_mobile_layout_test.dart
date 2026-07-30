import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/app/app.dart';
import 'package:joprelys_mobile/core/i18n/locale_controller.dart';
import 'package:joprelys_mobile/core/theme/theme_controller.dart';
import 'package:joprelys_mobile/features/auth/application/auth_controller.dart';
import 'package:joprelys_mobile/features/auth/domain/professional_session.dart';
import 'package:joprelys_mobile/features/dashboard/application/active_queue_controller.dart';
import 'package:joprelys_mobile/features/dashboard/data/active_visits_api.dart';
import 'package:joprelys_mobile/features/dashboard/domain/active_visit.dart';

void main() {
  testWidgets('dashboard renders the queue without overlap on a phone viewport', (
    tester,
  ) async {
    tester.view.devicePixelRatio = 1;
    tester.view.physicalSize = const Size(393, 852);
    addTearDown(tester.view.resetDevicePixelRatio);
    addTearDown(tester.view.resetPhysicalSize);

    final session = ProfessionalSession(
      accessToken: 'fixture-access-token',
      expiresAt: DateTime.utc(2030),
      email: 'charmande@example.test',
      name: 'NOUPOUE Charmande',
      role: 'MEDECIN',
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          platformLocaleProvider.overrideWithValue(const Locale('fr')),
          themeModeProvider.overrideWith(_DarkThemeController.new),
          authControllerProvider.overrideWith(
            () => _FakeAuthController(AuthState.authenticated(session)),
          ),
          activeVisitsApiProvider.overrideWithValue(
            _FakeActiveVisitsGateway([
              ActiveVisit(
                id: 'visit-1',
                visitNumber: 'VIS-20260725-001',
                patientId: 'patient-1',
                patientName: 'MOMO Allons',
                patientDpu: 'DPU-001',
                reason: 'Fièvre',
                orientation: 'Médecine générale',
                service: 'Médecine générale',
                status: 'ACTIVE',
                arrivalAt: DateTime.utc(2026, 7, 30, 13, 23),
                createdAt: DateTime.utc(2026, 7, 30, 13, 20),
                vitals: const VisitVitals(temperature: 38.2),
              ),
            ]),
          ),
        ],
        child: const JoprelysApp(),
      ),
    );

    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('MOMO Allons'), findsOneWidget);
    expect(find.text('Fièvre'), findsOneWidget);
    expect(find.byIcon(Icons.refresh_rounded), findsOneWidget);
  });
}

final class _DarkThemeController extends ThemeController {
  @override
  ThemeMode build() => ThemeMode.dark;
}

final class _FakeAuthController extends AuthController {
  _FakeAuthController(this.initialState);

  final AuthState initialState;

  @override
  Future<AuthState> build() async => initialState;

  @override
  Future<void> login({required String email, required String password}) async {}

  @override
  Future<void> logout() async {}

  @override
  Future<bool> enableBiometrics({required String reason}) async => true;

  @override
  Future<void> disableBiometrics() async {}
}

final class _FakeActiveVisitsGateway implements ActiveVisitsGateway {
  const _FakeActiveVisitsGateway(this.visits);

  final List<ActiveVisit> visits;

  @override
  Future<List<ActiveVisit>> getActiveVisits() async => [...visits];
}
