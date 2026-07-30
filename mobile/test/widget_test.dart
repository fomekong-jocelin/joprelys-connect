import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/app/app.dart';
import 'package:joprelys_mobile/core/config/app_config.dart';
import 'package:joprelys_mobile/core/i18n/locale_controller.dart';

void main() {
  Widget buildFrenchApp() {
    return ProviderScope(
      overrides: [
        platformLocaleProvider.overrideWithValue(const Locale('fr')),
      ],
      child: const JoprelysApp(),
    );
  }

  testWidgets('renders the localized Joprelys mobile foundation', (
    tester,
  ) async {
    await tester.pumpWidget(buildFrenchApp());
    await tester.pumpAndSettle();

    expect(find.text(AppConfig.appName), findsOneWidget);
    expect(find.byIcon(Icons.health_and_safety_outlined), findsOneWidget);
    expect(find.text('MOB-2803'), findsOneWidget);
    expect(find.text('Langue'), findsOneWidget);
    expect(find.text('Apparence'), findsOneWidget);
    expect(find.byIcon(Icons.add), findsNothing);
  });

  testWidgets('switches between light dark and system theme modes', (
    tester,
  ) async {
    await tester.pumpWidget(buildFrenchApp());
    await tester.pumpAndSettle();

    MaterialApp app = tester.widget(find.byType(MaterialApp));
    expect(app.themeMode, ThemeMode.system);

    await tester.tap(find.text('Clair'));
    await tester.pumpAndSettle();
    app = tester.widget(find.byType(MaterialApp));
    expect(app.themeMode, ThemeMode.light);

    await tester.tap(find.text('Sombre'));
    await tester.pumpAndSettle();
    app = tester.widget(find.byType(MaterialApp));
    expect(app.themeMode, ThemeMode.dark);

    await tester.tap(find.text('Système'));
    await tester.pumpAndSettle();
    app = tester.widget(find.byType(MaterialApp));
    expect(app.themeMode, ThemeMode.system);
  });

  testWidgets('switches locale from French to English without restart', (
    tester,
  ) async {
    await tester.pumpWidget(buildFrenchApp());
    await tester.pumpAndSettle();

    MaterialApp app = tester.widget(find.byType(MaterialApp));
    expect(app.locale?.languageCode, 'fr');
    expect(find.text('Apparence'), findsOneWidget);

    await tester.tap(find.text('English'));
    await tester.pumpAndSettle();

    app = tester.widget(find.byType(MaterialApp));
    expect(app.locale?.languageCode, 'en');
    expect(find.text('Language'), findsOneWidget);
    expect(find.text('Appearance'), findsOneWidget);
    expect(find.text('System'), findsOneWidget);
    expect(find.text('Light'), findsOneWidget);
    expect(find.text('Dark'), findsOneWidget);

    await tester.tap(find.text('Français'));
    await tester.pumpAndSettle();

    app = tester.widget(find.byType(MaterialApp));
    expect(app.locale?.languageCode, 'fr');
    expect(find.text('Apparence'), findsOneWidget);
  });
}
