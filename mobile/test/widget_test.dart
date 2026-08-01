import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/app/app.dart';
import 'package:joprelys_mobile/core/i18n/locale_controller.dart';
import 'package:joprelys_mobile/features/auth/application/auth_controller.dart';
import 'package:joprelys_mobile/features/auth/domain/professional_session.dart';
import 'package:joprelys_mobile/features/dashboard/application/active_queue_controller.dart';
import 'package:joprelys_mobile/features/dashboard/data/active_visits_api.dart';
import 'package:joprelys_mobile/features/dashboard/domain/active_visit.dart';
import 'package:joprelys_mobile/shared/widgets/app_brand_lockup.dart';
import 'package:joprelys_mobile/shared/widgets/app_text_field.dart';

void main() {
  testWidgets('professional login follows the web mobile hierarchy', (
    tester,
  ) async {
    await tester.pumpWidget(_buildApp(const AuthState.unauthenticated()));
    await tester.pumpAndSettle();

    expect(find.text('Connexion professionnelle'), findsOneWidget);
    expect(
      find.text('Connectez-vous à votre espace professionnel sécurisé.'),
      findsOneWidget,
    );
    expect(find.text('Adresse e-mail'), findsOneWidget);
    expect(find.text('Mot de passe'), findsOneWidget);
    expect(find.text('Se connecter'), findsOneWidget);
    expect(find.text('FR'), findsOneWidget);
    expect(find.text('EN'), findsOneWidget);
    expect(find.text('MOB-2805'), findsNothing);
    expect(find.byType(AppTextField), findsNWidgets(2));
    expect(
      find.byKey(const ValueKey('app-brand-wordmark-light')),
      findsOneWidget,
    );

    final themeControl = tester.getCenter(
      find.byIcon(Icons.brightness_auto_outlined),
    );
    final frenchControl = tester.getCenter(find.text('FR'));
    final brand = tester.getTopLeft(find.byType(AppBrandLockup));
    final title = tester.getTopLeft(find.text('Connexion professionnelle'));
    final firstField = tester.getTopLeft(find.byType(AppTextField).first);

    expect(themeControl.dx, lessThan(frenchControl.dx));
    expect(brand.dy, lessThan(title.dy));
    expect(title.dy, lessThan(firstField.dy));
  });

  testWidgets('switches between light dark and system theme modes', (
    tester,
  ) async {
    await tester.pumpWidget(_buildApp(const AuthState.unauthenticated()));
    await tester.pumpAndSettle();

    MaterialApp app = tester.widget(find.byType(MaterialApp));
    expect(app.themeMode, ThemeMode.system);

    await tester.tap(find.byIcon(Icons.brightness_auto_outlined));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Clair'));
    await tester.pumpAndSettle();
    app = tester.widget(find.byType(MaterialApp));
    expect(app.themeMode, ThemeMode.light);

    await tester.tap(find.byIcon(Icons.light_mode_outlined).first);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Sombre'));
    await tester.pumpAndSettle();
    app = tester.widget(find.byType(MaterialApp));
    expect(app.themeMode, ThemeMode.dark);

    await tester.tap(find.byIcon(Icons.dark_mode_outlined).first);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Système'));
    await tester.pumpAndSettle();
    app = tester.widget(find.byType(MaterialApp));
    expect(app.themeMode, ThemeMode.system);
  });

  testWidgets('switches locale from the compact language segment', (
    tester,
  ) async {
    await tester.pumpWidget(_buildApp(const AuthState.unauthenticated()));
    await tester.pumpAndSettle();

    await tester.tap(find.text('EN'));
    await tester.pumpAndSettle();

    MaterialApp app = tester.widget(find.byType(MaterialApp));
    expect(app.locale?.languageCode, 'en');
    expect(find.text('Professional access'), findsOneWidget);

    await tester.tap(find.text('FR'));
    await tester.pumpAndSettle();
    app = tester.widget(find.byType(MaterialApp));
    expect(app.locale?.languageCode, 'fr');
    expect(find.text('Connexion professionnelle'), findsOneWidget);
  });

  testWidgets('authenticated home is a compact clinical dashboard', (
    tester,
  ) async {
    final session = ProfessionalSession(
      accessToken: 'fixture-access-token',
      expiresAt: DateTime.utc(2030),
      email: 'professionnel@example.test',
      name: 'Alex Martin',
      role: 'ADMIN_CLINIQUE',
    );

    await tester.pumpWidget(
      _buildApp(
        AuthState.authenticated(session),
        visits: [
          ActiveVisit(
            id: 'visit-1',
            visitNumber: 'VIS-20260730-001',
            patientId: 'patient-1',
            patientName: 'Momo Allons',
            patientDpu: 'DPU-001',
            reason: 'Fièvre',
            orientation: 'Médecine générale',
            status: 'ACTIVE',
            createdAt: DateTime.utc(2026, 7, 30, 12),
            arrivalAt: DateTime.utc(2026, 7, 30, 13, 23),
            vitals: const VisitVitals(temperature: 38.2),
          ),
        ],
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Ravi de vous revoir, Alex'), findsOneWidget);
    expect(find.text('Patients en attente'), findsOneWidget);
    expect(find.text('Momo Allons'), findsOneWidget);
    expect(find.text('Fièvre'), findsOneWidget);
    expect(find.text('Compte professionnel'), findsNothing);
    expect(find.text('Sécurité de l’application'), findsNothing);
    expect(find.text('MOB-2805'), findsNothing);

    await tester.tap(find.text('AM'));
    await tester.pumpAndSettle();

    expect(find.text('Mon espace professionnel'), findsOneWidget);
    expect(find.text('Compte professionnel'), findsOneWidget);
    expect(find.text('Sécurité de l’application'), findsOneWidget);
    expect(find.text('professionnel@example.test'), findsOneWidget);
    expect(find.text('ADMIN_CLINIQUE'), findsOneWidget);
    expect(find.text('Se déconnecter'), findsOneWidget);
  });
}

Widget _buildApp(
  AuthState initialState, {
  List<ActiveVisit> visits = const <ActiveVisit>[],
}) {
  return ProviderScope(
    overrides: [
      platformLocaleProvider.overrideWithValue(const Locale('fr')),
      authControllerProvider.overrideWith(
        () => _FakeAuthController(initialState),
      ),
      activeVisitsApiProvider.overrideWithValue(
        _FakeActiveVisitsGateway(visits),
      ),
    ],
    child: const JoprelysApp(),
  );
}

final class _FakeActiveVisitsGateway implements ActiveVisitsGateway {
  const _FakeActiveVisitsGateway(this.visits);

  final List<ActiveVisit> visits;

  @override
  Future<List<ActiveVisit>> getActiveVisits() async => visits;
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
