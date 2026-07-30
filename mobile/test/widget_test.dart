import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/app/app.dart';
import 'package:joprelys_mobile/core/config/app_config.dart';

void main() {
  testWidgets('renders the Joprelys mobile foundation', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: JoprelysApp()));
    await tester.pumpAndSettle();

    expect(find.text(AppConfig.appName), findsOneWidget);
    expect(find.byIcon(Icons.health_and_safety_outlined), findsOneWidget);
    expect(find.text('MOB-2802'), findsOneWidget);
    expect(find.byIcon(Icons.add), findsNothing);
  });

  testWidgets('switches between light dark and system theme modes', (
    tester,
  ) async {
    await tester.pumpWidget(const ProviderScope(child: JoprelysApp()));
    await tester.pumpAndSettle();

    MaterialApp app = tester.widget(find.byType(MaterialApp));
    expect(app.themeMode, ThemeMode.system);

    await tester.tap(find.text('Light'));
    await tester.pumpAndSettle();
    app = tester.widget(find.byType(MaterialApp));
    expect(app.themeMode, ThemeMode.light);

    await tester.tap(find.text('Dark'));
    await tester.pumpAndSettle();
    app = tester.widget(find.byType(MaterialApp));
    expect(app.themeMode, ThemeMode.dark);

    await tester.tap(find.text('System'));
    await tester.pumpAndSettle();
    app = tester.widget(find.byType(MaterialApp));
    expect(app.themeMode, ThemeMode.system);
  });
}
