import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/app/app.dart';
import 'package:joprelys_mobile/core/i18n/locale_controller.dart';
import 'package:joprelys_mobile/core/theme/theme_controller.dart';
import 'package:joprelys_mobile/features/auth/application/auth_controller.dart';
import 'package:joprelys_mobile/features/auth/application/effective_access_controller.dart';
import 'package:joprelys_mobile/features/auth/domain/effective_access.dart';
import 'package:joprelys_mobile/features/auth/domain/professional_session.dart';
import 'package:joprelys_mobile/features/dashboard/application/active_queue_controller.dart';
import 'package:joprelys_mobile/features/dashboard/data/active_visits_api.dart';
import 'package:joprelys_mobile/features/dashboard/domain/active_visit.dart';
import 'package:joprelys_mobile/features/foundation/presentation/widgets/professional_app_bar.dart';

void main() {
  testWidgets(
    'dashboard renders the queue without overlap on a phone viewport',
    (tester) async {
      tester.view.devicePixelRatio = 1;
      tester.view.physicalSize = const Size(360, 800);
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
            effectiveAccessProvider.overrideWith(
              (ref) async => const EffectiveAccess(
                userId: 'fixture-user',
                roles: {'CLINICAL_TEST'},
                permissions: {'VISIT_READ'},
              ),
            ),
            themeModeProvider.overrideWith(_DarkThemeController.new),
            authControllerProvider.overrideWith(
              () => _FakeAuthController(AuthState.authenticated(session)),
            ),
            activeVisitsApiProvider.overrideWithValue(
              _FakeActiveVisitsGateway([
                _visit(1, 'MOMO Allons', 'Fièvre'),
                _visit(2, 'MANI Alime', 'Contrôle'),
                _visit(3, 'Patient Test', 'Consultation'),
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
      expect(find.byType(ProfessionalAppBar), findsOneWidget);
      expect(find.byKey(const ValueKey('app-brand-mark-dark')), findsOneWidget);

      final appBarTop = tester.getTopLeft(find.byType(ProfessionalAppBar));
      final greeting = find.byKey(
        const ValueKey('professional-dashboard-greeting'),
      );
      final greetingTop = tester.getTopLeft(greeting);

      await tester.drag(
        find.byKey(const ValueKey('professional-dashboard-scroll')),
        const Offset(0, -260),
      );
      await tester.pumpAndSettle();

      expect(tester.getTopLeft(find.byType(ProfessionalAppBar)), appBarTop);
      expect(tester.getTopLeft(greeting).dy, lessThan(greetingTop.dy));
    },
  );
}

ActiveVisit _visit(int index, String patientName, String reason) {
  return ActiveVisit(
    id: 'visit-$index',
    visitNumber: 'VIS-20260725-00$index',
    patientId: 'patient-$index',
    patientName: patientName,
    patientDpu: 'DPU-00$index',
    reason: reason,
    orientation: 'Médecine générale',
    service: 'Médecine générale',
    status: 'ACTIVE',
    arrivalAt: DateTime.utc(2026, 7, 30, 13, 20 + index),
    createdAt: DateTime.utc(2026, 7, 30, 13, 20),
    vitals: index == 1 ? const VisitVitals(temperature: 38.2) : null,
  );
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
