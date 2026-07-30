import 'package:flutter/widgets.dart';

import 'app_environment.dart';

abstract final class AppConfig {
  static const String appName = 'Joprelys Connect';

  static const Locale defaultLocale = Locale('fr');
  static const List<Locale> supportedLocales = <Locale>[
    Locale('fr'),
    Locale('en'),
  ];

  // Les APK de validation doivent cibler la recette par défaut. Le développement
  // local reste disponible en passant explicitement APP_ENV=dev et API_BASE_URL.
  static final AppRuntimeConfig runtime = AppRuntimeConfig.fromValues(
    environmentName: const String.fromEnvironment(
      'APP_ENV',
      defaultValue: 'recette',
    ),
    apiBaseUrl: const String.fromEnvironment(
      'API_BASE_URL',
      defaultValue: 'https://recette.joprelys.com',
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
