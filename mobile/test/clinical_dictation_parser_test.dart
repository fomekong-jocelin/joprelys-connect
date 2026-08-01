import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/application/clinical_dictation_parser.dart';

void main() {
  group('ClinicalDictationParser', () {
    const parser = ClinicalDictationParser();

    test('extracts temperature, blood pressure, pulse and spo2 correctly', () {
      const dictation =
          'Température 38.5, tension 120/80, pouls 75, SpO2 98%, patient fiévreux avec céphalées.';

      final result = parser.parse(dictation);
      final vitals = result.vitals;

      expect(vitals.temperature, 38.5);
      expect(vitals.systolic, 120);
      expect(vitals.diastolic, 80);
      expect(vitals.pulse, 75);
      expect(vitals.spo2, 98);

      expect(result.note.symptoms, contains('patient fiévreux'));
      expect(result.note.clinicalExam, contains('T°: 38.5°C'));
      expect(result.note.diagnosis, isNull);
      expect(result.note.followUp, isNull);
    });

    test('extracts weight, height, glycemia and pain scale correctly', () {
      const dictation =
          'Poids 75.5 kg, taille 175 cm, glycémie 1.10, douleur 4';

      final result = parser.parse(dictation);
      final vitals = result.vitals;

      expect(vitals.weight, 75.5);
      expect(vitals.height, 175);
      expect(vitals.glycemia, 1.10);
      expect(vitals.painScale, 4);
    });

    test('returns empty objects on empty dictation text', () {
      final result = parser.parse('   ');
      expect(result.vitals.isEmpty, true);
      expect(result.note.isEmpty, true);
    });
  });
}
