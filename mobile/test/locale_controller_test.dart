import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/core/config/app_config.dart';
import 'package:joprelys_mobile/core/i18n/locale_controller.dart';

void main() {
  test('keeps supported French and English locales', () {
    expect(
      AppConfig.resolveSupportedLocale(const Locale('fr', 'CM')),
      const Locale('fr'),
    );
    expect(
      AppConfig.resolveSupportedLocale(const Locale('en', 'GB')),
      const Locale('en'),
    );
  });

  test('falls back to French for unsupported locale', () {
    expect(
      AppConfig.resolveSupportedLocale(const Locale('de')),
      AppConfig.defaultLocale,
    );
  });

  test('initializes from the supported device locale and switches explicitly', () {
    final container = ProviderContainer(
      overrides: [
        platformLocaleProvider.overrideWithValue(const Locale('en', 'US')),
      ],
    );
    addTearDown(container.dispose);

    expect(container.read(appLocaleProvider), const Locale('en'));

    container.read(appLocaleProvider.notifier).useFrench();
    expect(container.read(appLocaleProvider), const Locale('fr'));

    container.read(appLocaleProvider.notifier).useEnglish();
    expect(container.read(appLocaleProvider), const Locale('en'));
  });

  test('uses French when the device locale is unsupported', () {
    final container = ProviderContainer(
      overrides: [
        platformLocaleProvider.overrideWithValue(const Locale('de', 'DE')),
      ],
    );
    addTearDown(container.dispose);

    expect(container.read(appLocaleProvider), AppConfig.defaultLocale);
  });
}
