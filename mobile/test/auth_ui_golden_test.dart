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
import 'package:joprelys_mobile/features/dashboard/application/active_queue_controller.dart';
import 'package:joprelys_mobile/features/dashboard/data/active_visits_api.dart';
import 'package:joprelys_mobile/features/dashboard/domain/active_visit.dart';
import 'package:joprelys_mobile/features/foundation/presentation/widgets/professional_app_bar.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUpAll(_configureGoldens);

  testWidgets('dark professional login matches the visual baseline', (
    tester,
  ) async {
    await _configureMobileSurface(tester);
    await tester.pumpWidget(_buildDarkApp(const AuthState.unauthenticated()));
    await tester.pumpAndSettle();
    await _precacheBrandAsset(tester);

    expect(
      find.byKey(const ValueKey('app-brand-wordmark-dark')),
      findsOneWidget,
    );

    await expectLater(
      find.byType(JoprelysApp),
      matchesGoldenFile('goldens/auth_login_dark.png'),
    );
  });

  testWidgets('dark professional home uses the compact native dashboard', (
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
    final visits = [
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
    ];

    await tester.pumpWidget(
      _buildDarkApp(AuthState.authenticated(session), visits: visits),
    );
    await tester.pumpAndSettle();
    await _precacheBrandAsset(tester);

    expect(find.text('Ravi de vous revoir, Alex'), findsOneWidget);
    expect(find.text('Patients en attente'), findsOneWidget);
    expect(find.text('Momo Allons'), findsOneWidget);
    expect(find.text('Fièvre'), findsOneWidget);
    expect(find.text('Compte professionnel'), findsNothing);
    expect(find.byType(ProfessionalAppBar), findsOneWidget);
    expect(find.byKey(const ValueKey('app-brand-mark-dark')), findsOneWidget);

    await expectLater(
      find.byType(JoprelysApp),
      matchesGoldenFile('goldens/professional_home_dark.png'),
    );
  });
}

Future<void> _configureGoldens() async {
  await _loadRobotoForGoldens();

  final currentComparator = goldenFileComparator;
  if (currentComparator is! LocalFileComparator) {
    throw StateError(
      'A LocalFileComparator is required for deterministic goldens.',
    );
  }

  goldenFileComparator = _TolerantGoldenFileComparator(
    currentComparator.basedir.resolve('auth_ui_golden_test.dart'),
    precisionTolerance: 0.05,
  );
}

Future<void> _loadRobotoForGoldens() async {
  final flutterRoot = Platform.environment['FLUTTER_ROOT'];
  if (flutterRoot == null) {
    throw StateError('FLUTTER_ROOT is required for deterministic goldens.');
  }
  final materialFontsDirectory =
      '$flutterRoot/bin/cache/artifacts/material_fonts';
  final fontFile = File('$materialFontsDirectory/Roboto-Regular.ttf');
  final fontBytes = await fontFile.readAsBytes();
  await (FontLoader(
    'Roboto',
  )..addFont(Future.value(ByteData.sublistView(fontBytes)))).load();

  final iconsFile = File('$materialFontsDirectory/MaterialIcons-Regular.otf');
  final iconBytes = await iconsFile.readAsBytes();
  await (FontLoader(
    'MaterialIcons',
  )..addFont(Future.value(ByteData.sublistView(iconBytes)))).load();
}

final class _TolerantGoldenFileComparator extends LocalFileComparator {
  _TolerantGoldenFileComparator(
    super.testFile, {
    required double precisionTolerance,
  }) : assert(
         precisionTolerance >= 0 && precisionTolerance <= 1,
         'precisionTolerance must be between 0 and 1',
       ),
       _precisionTolerance = precisionTolerance;

  final double _precisionTolerance;

  @override
  Future<bool> compare(Uint8List imageBytes, Uri golden) async {
    final result = await GoldenFileComparator.compareLists(
      imageBytes,
      await getGoldenBytes(golden),
    );

    final passed = result.passed || result.diffPercent <= _precisionTolerance;
    if (passed) {
      result.dispose();
      return true;
    }

    final error = await generateFailureOutput(result, golden, basedir);
    result.dispose();
    throw FlutterError(error);
  }
}

Future<void> _precacheBrandAsset(WidgetTester tester) async {
  final context = tester.element(find.byType(JoprelysApp));
  await tester.runAsync(() async {
    await precacheImage(const AssetImage(AppConfig.logoOnDarkAsset), context);
    await precacheImage(
      const AssetImage(AppConfig.logoIconOnDarkAsset),
      context,
    );
  });
  await tester.pumpAndSettle();
}

Future<void> _configureMobileSurface(WidgetTester tester) async {
  tester.view.devicePixelRatio = 1;
  tester.view.physicalSize = const Size(393, 852);
  addTearDown(tester.view.resetDevicePixelRatio);
  addTearDown(tester.view.resetPhysicalSize);
}

Widget _buildDarkApp(
  AuthState initialState, {
  List<ActiveVisit> visits = const <ActiveVisit>[],
}) {
  return ProviderScope(
    overrides: [
      platformLocaleProvider.overrideWithValue(const Locale('fr')),
      themeModeProvider.overrideWith(_DarkThemeController.new),
      authControllerProvider.overrideWith(
        () => _GoldenAuthController(initialState),
      ),
      activeVisitsApiProvider.overrideWithValue(
        _GoldenActiveVisitsGateway(visits),
      ),
    ],
    child: const JoprelysApp(),
  );
}

final class _GoldenActiveVisitsGateway implements ActiveVisitsGateway {
  const _GoldenActiveVisitsGateway(this.visits);

  final List<ActiveVisit> visits;

  @override
  Future<List<ActiveVisit>> getActiveVisits() async => visits;
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
