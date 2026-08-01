import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/application/clinical_value_formatter.dart';

void main() {
  test('formats a structured prescription without exposing JSON syntax', () {
    final formatted = formatClinicalValue(
      'prescription',
      '[{"drugName":"Doliprane","dosage":"100 mg","posology":"deux comprimés par prise","frequency":"matin, midi et soir","duration":"deux jours"}]',
      isFrench: true,
    );

    expect(formatted, contains('Doliprane'));
    expect(formatted, contains('Dosage : 100 mg'));
    expect(formatted, contains('Posologie : deux comprimés par prise'));
    expect(formatted, contains('Fréquence : matin, midi et soir'));
    expect(formatted, contains('Durée : deux jours'));
    expect(formatted, isNot(contains('drugName')));
    expect(formatted, isNot(contains('{')));
    expect(formatted, isNot(contains('}')));
  });

  test('formats multiple medications as separate readable blocks', () {
    final formatted = formatClinicalValue(
      'prescription',
      '[{"drugName":"Paracétamol","dosage":"500 mg"},{"drugName":"Amoxicilline","frequency":"trois fois par jour"}]',
      isFrench: true,
    );

    expect(formatted, contains('Paracétamol'));
    expect(formatted, contains('Amoxicilline'));
    expect(formatted, contains('\n\n'));
  });

  test('keeps an invalid legacy value visible instead of losing it', () {
    const raw = '{drugName: Doliprane, dosage: 100 mg}';

    expect(formatClinicalValue('prescription', raw, isFrench: true), raw);
  });
}
