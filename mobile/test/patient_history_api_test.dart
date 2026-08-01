import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/domain/patient_history.dart';

void main() {
  group('PatientMedicalHistory Domain Tests', () {
    test('PatientMedicalHistory serializes and deserializes JSON correctly', () {
      final json = {
        'patientId': 'pat-102',
        'patientName': 'MOMO Allons',
        'patientDpu': 'DPU-JOP-20260725-000001',
        'antecedents': [
          {
            'type': 'MEDICAL',
            'description': 'Hypertension Artérielle',
            'diagnosedYear': 2021,
          }
        ],
        'allergies': [
          {
            'allergen': 'Pénicilline',
            'severity': 'SEVERE',
            'reaction': 'Éruption cutanée',
          }
        ],
        'pastVisits': [
          {
            'id': 'vis-1',
            'visitNumber': 'VIS-20260720-000001',
            'date': '2026-07-20T10:30:00.000Z',
            'practitionerName': 'Dr. DUPONT',
            'chiefComplaint': 'Syndrome grippal',
            'temperature': 38.5,
            'systolic': 120,
            'diastolic': 80,
            'pulse': 75,
          }
        ],
      };

      final history = PatientMedicalHistory.fromJson(json);

      expect(history.patientId, equals('pat-102'));
      expect(history.patientName, equals('MOMO Allons'));
      expect(history.antecedents.length, equals(1));
      expect(history.antecedents.first.description, contains('Hypertension'));
      expect(history.allergies.first.severity, equals(AllergySeverity.severe));
      expect(history.pastVisits.first.temperature, equals(38.5));

      final serialized = history.toJson();
      expect(serialized['patientId'], equals('pat-102'));
      expect(serialized['allergies'], isNotEmpty);
    });

    test('PatientAllergy maps severity enum accurately', () {
      final severe = PatientAllergy.fromJson({'allergen': 'A', 'severity': 'SEVERE'});
      final moderate = PatientAllergy.fromJson({'allergen': 'B', 'severity': 'MODERATE'});
      final low = PatientAllergy.fromJson({'allergen': 'C', 'severity': 'LOW'});

      expect(severe.severity, equals(AllergySeverity.severe));
      expect(moderate.severity, equals(AllergySeverity.moderate));
      expect(low.severity, equals(AllergySeverity.low));
    });
  });
}
