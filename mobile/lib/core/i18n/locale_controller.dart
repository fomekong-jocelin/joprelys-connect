import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../config/app_config.dart';

final platformLocaleProvider = Provider<Locale?>((ref) {
  return WidgetsBinding.instance.platformDispatcher.locale;
});

class AppLocaleController extends Notifier<Locale> {
  @override
  Locale build() {
    return AppConfig.resolveSupportedLocale(ref.watch(platformLocaleProvider));
  }

  void setLocale(Locale locale) {
    state = AppConfig.resolveSupportedLocale(locale);
  }

  void useFrench() => setLocale(const Locale('fr'));

  void useEnglish() => setLocale(const Locale('en'));
}

final appLocaleProvider = NotifierProvider<AppLocaleController, Locale>(
  AppLocaleController.new,
);
