import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'package:joprelys_mobile/core/i18n/app_locale_formatters.dart';

void main() {
  setUpAll(() async {
    await Future.wait([
      initializeDateFormatting('fr_FR'),
      initializeDateFormatting('en_GB'),
    ]);
  });

  test('formats dates according to French and English locales', () {
    final value = DateTime(2026, 7, 30, 16, 5);

    final french = AppLocaleFormatters.formatDate(value, const Locale('fr'));
    final english = AppLocaleFormatters.formatDate(value, const Locale('en'));

    expect(french, contains('2026'));
    expect(english, contains('2026'));
    expect(french, isNot(equals(english)));
    expect(AppLocaleFormatters.formatTime(value, const Locale('fr')), '16:05');
  });

  test('formats decimal numbers according to the active locale', () {
    final french = AppLocaleFormatters.formatNumber(
      1234.5,
      const Locale('fr'),
      decimalDigits: 1,
    );
    final english = AppLocaleFormatters.formatNumber(
      1234.5,
      const Locale('en'),
      decimalDigits: 1,
    );

    expect(french, endsWith(',5'));
    expect(english, endsWith('.5'));
    expect(french, isNot(equals(english)));
  });

  test('uses French formatting for an unsupported locale', () {
    final french = AppLocaleFormatters.formatDate(
      DateTime(2026, 7, 30),
      const Locale('fr'),
    );
    final fallback = AppLocaleFormatters.formatDate(
      DateTime(2026, 7, 30),
      const Locale('de'),
    );

    expect(fallback, french);
  });
}
