import 'package:flutter/widgets.dart';
import 'package:intl/intl.dart';

import '../config/app_config.dart';

abstract final class AppLocaleFormatters {
  static String formatDate(DateTime value, Locale locale) {
    return DateFormat.yMMMd(_intlLocale(locale)).format(value);
  }

  static String formatTime(DateTime value, Locale locale) {
    return DateFormat.Hm(_intlLocale(locale)).format(value);
  }

  static String formatDateTime(DateTime value, Locale locale) {
    return '${formatDate(value, locale)} · ${formatTime(value, locale)}';
  }

  static String formatNumber(
    num value,
    Locale locale, {
    int? decimalDigits,
  }) {
    final formatter = NumberFormat.decimalPattern(_intlLocale(locale));
    if (decimalDigits != null) {
      formatter.minimumFractionDigits = decimalDigits;
      formatter.maximumFractionDigits = decimalDigits;
    }
    return formatter.format(value);
  }

  static String _intlLocale(Locale locale) {
    final supported = AppConfig.resolveSupportedLocale(locale);
    return supported.languageCode == 'fr' ? 'fr_FR' : 'en_GB';
  }
}
