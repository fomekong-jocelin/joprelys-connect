import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/app/app.dart';
import 'package:joprelys_mobile/core/i18n/locale_controller.dart';
import 'package:joprelys_mobile/core/lifecycle/app_activity_registry.dart';
import 'package:joprelys_mobile/features/auth/application/auth_controller.dart';
import 'package:joprelys_mobile/features/auth/application/effective_access_controller.dart';
import 'package:joprelys_mobile/features/auth/domain/effective_access.dart';
import 'package:joprelys_mobile/features/auth/domain/professional_session.dart';
import 'package:joprelys_mobile/features/auth/presentation/pages/unlock_page.dart';
import 'package:joprelys_mobile/features/dashboard/application/active_queue_controller.dart';
import 'package:joprelys_mobile/features/dashboard/data/active_visits_api.dart';
import 'package:joprelys_mobile/features/dashboard/domain/active_visit.dart';
import 'package:joprelys_mobile/features/foundation/presentation/pages/foundation_page.dart';

void main() {
  testWidgets(
    'lock over a clinical capture renders the unlock page without error',
    (tester) async {
      final auth = _LockableAuthController();

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            platformLocaleProvider.overrideWithValue(const Locale('fr')),
            authControllerProvider.overrideWith(() => auth),
            appForegroundActivityProvider.overrideWith(
              _ClinicalCaptureActivity.new,
            ),
            effectiveAccessProvider.overrideWith(
              (ref) async => const EffectiveAccess(
                userId: 'fixture-user',
                roles: {'CLINICAL_TEST'},
                permissions: {'VISIT_READ'},
              ),
            ),
            activeVisitsApiProvider.overrideWithValue(_EmptyQueueGateway()),
          ],
          child: const JoprelysApp(),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.byType(UnlockPage), findsNothing);
      final workspace = tester.state(find.byType(FoundationPage).first);

      auth.lockNow();
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);
      expect(find.byType(UnlockPage), findsOneWidget);
      expect(find.text('Application verrouillée'), findsOneWidget);
      // The clinical route stays mounted (same state) underneath the lock.
      expect(tester.state(find.byType(FoundationPage).first), same(workspace));

      // Menus of the auth shell need the lock's own Navigator and Overlay.
      await tester.tap(
        find.descendant(
          of: find.byType(UnlockPage),
          matching: find.byIcon(Icons.brightness_auto_outlined),
        ),
      );
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      expect(find.text('Clair'), findsOneWidget);
    },
  );
}

final class _LockableAuthController extends AuthController {
  static final ProfessionalSession _session = ProfessionalSession(
    accessToken: 'fixture-access-token',
    expiresAt: DateTime.utc(2030),
    email: 'praticien@example.test',
    name: 'Praticien Fixture',
    role: 'MEDECIN',
    biometricEnabled: true,
  );

  @override
  Future<AuthState> build() async => AuthState.authenticated(_session);

  void lockNow() => state = AsyncData(AuthState.locked(_session));
}

final class _ClinicalCaptureActivity extends AppForegroundActivityController {
  @override
  Set<AppForegroundActivity> build() => const <AppForegroundActivity>{
    AppForegroundActivity.clinicalVoiceAssistant,
  };
}

final class _EmptyQueueGateway implements ActiveVisitsGateway {
  @override
  Future<List<ActiveVisit>> getActiveVisits() async => const <ActiveVisit>[];
}
