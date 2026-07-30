import 'package:flutter/widgets.dart';

abstract final class AppConfig {
  static const String appName = 'Joprelys Connect';

  static const Locale defaultLocale = Locale('fr');
  static const List<Locale> supportedLocales = <Locale>[
    Locale('fr'),
    Locale('en'),
  ];

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
