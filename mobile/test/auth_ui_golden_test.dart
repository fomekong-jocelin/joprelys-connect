import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/app/app.dart';
import 'package:joprelys_mobile/core/config/app_config.dart';
import 'package:joprelys_mobile/core/i18n/locale_controller.dart';
import 'package:joprelys_mobile/core/theme/theme_controller.dart';
import 'package:joprelys_mobile/features/auth/application/auth_controller.dart';
import 'package:joprelys_mobile/features/auth/domain/professional_session.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUpAll(_loadRobotoForGoldens);

  testWidgets('dark professional login matches the visual baseline', (
    tester,
  ) async {
    await _configureMobileSurface(tester);
    await tester.pumpWidget(_buildDarkApp(const AuthState.unauthenticated()));
    await tester.pumpAndSettle();
    await _precacheBrandAsset(tester);

    await expectLater(
      find.byType(JoprelysApp),
      matchesGoldenFile('goldens/auth_login_dark.png'),
    );
  });

  testWidgets('dark professional home matches the visual baseline', (
    tester,
  ) async {
    await _configureMobileSurface(tester);
    final session = ProfessionalSession(
      accessToken: 'fixture-access-token',
      expiresAt: DateTime.utc(2030),
      email: 'professionnel@example.test',
      name: 'Alex Martin',
      role: 'ADMIN_CLINIQUE',
    );

    await tester.pumpWidget(_buildDarkApp(AuthState.authenticated(session)));
    await tester.pumpAndSettle();
    await _precacheBrandAsset(tester);

    await expectLater(
      find.byType(JoprelysApp),
      matchesGoldenFile('goldens/professional_home_dark.png'),
    );
  });
}

Future<void> _loadRobotoForGoldens() async {
  final flutterRoot = Platform.environment['FLUTTER_ROOT'];
  if (flutterRoot == null) {
    throw StateError('FLUTTER_ROOT is required for deterministic goldens.');
  }
  final fontFile = File(
    '$flutterRoot/bin/cache/artifacts/material_fonts/roboto-regular.ttf',
  );
  final fontBytes = await fontFile.readAsBytes();
  await (FontLoader(
    'Roboto',
  )..addFont(Future.value(ByteData.sublistView(fontBytes)))).load();

  final iconsFile = File(
    '$flutterRoot/bin/cache/artifacts/material_fonts/'
    'materialicons-regular.otf',
  );
  final iconBytes = await iconsFile.readAsBytes();
  await (FontLoader(
    'MaterialIcons',
  )..addFont(Future.value(ByteData.sublistView(iconBytes)))).load();
}

Future<void> _precacheBrandAsset(WidgetTester tester) async {
  final context = tester.element(find.byType(JoprelysApp));
  await tester.runAsync(
    () => precacheImage(const AssetImage(AppConfig.logoOnDarkAsset), context),
  );
  await tester.pumpAndSettle();
}

Future<void> _configureMobileSurface(WidgetTester tester) async {
  tester.view.devicePixelRatio = 1;
  tester.view.physicalSize = const Size(393, 852);
  addTearDown(tester.view.resetDevicePixelRatio);
  addTearDown(tester.view.resetPhysicalSize);
}

Widget _buildDarkApp(AuthState initialState) {
  return ProviderScope(
    overrides: [
      platformLocaleProvider.overrideWithValue(const Locale('fr')),
      themeModeProvider.overrideWith(_DarkThemeController.new),
      authControllerProvider.overrideWith(
        () => _GoldenAuthController(initialState),
      ),
    ],
    child: const JoprelysApp(),
  );
}

final class _DarkThemeController extends ThemeController {
  @override
  ThemeMode build() => ThemeMode.dark;
}

final class _GoldenAuthController extends AuthController {
  _GoldenAuthController(this.initialState);

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
