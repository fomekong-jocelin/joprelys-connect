import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/app/app.dart';
import 'package:joprelys_mobile/core/i18n/locale_controller.dart';
import 'package:joprelys_mobile/features/auth/application/auth_controller.dart';
import 'package:joprelys_mobile/features/auth/domain/professional_session.dart';
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

  testWidgets(
    'authenticated home exposes account and security without ticket id',
    (tester) async {
      final session = ProfessionalSession(
        accessToken: 'fixture-access-token',
        expiresAt: DateTime.utc(2030),
        email: 'professionnel@example.test',
        name: 'Alex Martin',
        role: 'ADMIN_CLINIQUE',
      );

      await tester.pumpWidget(_buildApp(AuthState.authenticated(session)));
      await tester.pumpAndSettle();

      expect(find.text('Bienvenue, Alex Martin'), findsOneWidget);
      expect(find.text('Compte professionnel'), findsOneWidget);
      expect(find.text('Sécurité de l’application'), findsOneWidget);
      expect(find.text('professionnel@example.test'), findsOneWidget);
      expect(find.text('ADMIN_CLINIQUE'), findsOneWidget);
      expect(find.text('Se déconnecter'), findsOneWidget);
      expect(find.text('MOB-2805'), findsNothing);
      expect(find.text('Langue'), findsNothing);
      expect(find.text('Apparence'), findsNothing);
    },
  );
}

Widget _buildApp(AuthState initialState) {
  return ProviderScope(
    overrides: [
      platformLocaleProvider.overrideWithValue(const Locale('fr')),
      authControllerProvider.overrideWith(
        () => _FakeAuthController(initialState),
      ),
    ],
    child: const JoprelysApp(),
  );
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
