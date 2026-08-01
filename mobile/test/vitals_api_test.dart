import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/domain/patient_vitals.dart';

void main() {
  group('PatientVitals', () {
    test('calculates BMI accurately from weight and height', () {
      const vitals = PatientVitals(weight: 70, height: 175);

      final bmi = vitals.calculatedBmi;
      expect(bmi, isNotNull);
      expect(bmi!.toStringAsFixed(1), '22.9');
    });

    test('serializes and deserializes JSON correctly', () {
      final json = <String, dynamic>{
        'temperature': 37.8,
        'weight': 70.5,
        'height': 175,
        'pulse': 78,
        'systolic': 120,
        'diastolic': 80,
        'spo2': 98,
        'glycemia': 1.1,
        'respiratoryRate': 16,
        'painScale': 2,
      };

      final vitals = PatientVitals.fromJson(json);
      expect(vitals.temperature, 37.8);
      expect(vitals.pulse, 78);
      expect(vitals.systolic, 120);

      final serialized = vitals.toJson();
      expect(serialized['temperature'], 37.8);
      expect(serialized['pulse'], 78);
    });
  });
}
