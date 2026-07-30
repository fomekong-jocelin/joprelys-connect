import 'package:flutter/widgets.dart';

import 'app_environment.dart';

abstract final class AppConfig {
  static const String appName = 'Joprelys Connect';

  static const Locale defaultLocale = Locale('fr');
  static const List<Locale> supportedLocales = <Locale>[
    Locale('fr'),
    Locale('en'),
  ];

  static final AppRuntimeConfig runtime = AppRuntimeConfig.fromValues(
    environmentName: const String.fromEnvironment(
      'APP_ENV',
      defaultValue: 'dev',
    ),
    apiBaseUrl: const String.fromEnvironment(
      'API_BASE_URL',
      defaultValue: 'http://10.0.2.2:8080',
    ),
  );

  static Locale resolveSupportedLocale(Locale? locale) {
    if (locale == null) {
      return defaultLocale;
    }

    for (final supported in supportedLocales) {
      if (supported.languageCode == locale.languageCode) {
        return supported;
      }
    }

    return defaultLocale;
  }
}
