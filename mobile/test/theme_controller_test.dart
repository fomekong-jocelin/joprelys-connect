import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/core/theme/theme_controller.dart';

void main() {
  test('theme mode defaults to system and switches explicitly', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);

    expect(container.read(themeModeProvider), ThemeMode.system);

    container.read(themeModeProvider.notifier).useLight();
    expect(container.read(themeModeProvider), ThemeMode.light);

    container.read(themeModeProvider.notifier).useDark();
    expect(container.read(themeModeProvider), ThemeMode.dark);

    container.read(themeModeProvider.notifier).useSystem();
    expect(container.read(themeModeProvider), ThemeMode.system);
  });
}
