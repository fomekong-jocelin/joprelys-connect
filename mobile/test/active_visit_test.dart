import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/domain/active_visit.dart';

void main() {
  group('ActiveVisit', () {
    test('parses the active visit contract and vitals', () {
      final visit = ActiveVisit.fromJson({
        'id': 'visit-1',
        'visitNumber': 'VIS-2026-001',
        'patientId': 'patient-1',
        'patientName': 'Marie Test',
        'patientDpu': 'DPU-001',
        'reason': 'Fièvre persistante',
        'orientation': 'Consultation',
        'service': 'Médecine générale',
        'mainPractitionerId': 'doctor-1',
        'status': 'ACTIVE',
        'arrivalAt': '2026-07-30T08:15:00Z',
        'createdAt': '2026-07-30T08:10:00Z',
        'closedAt': null,
        'vitals': {'temperature': 38.2, 'pulse': 92, 'spo2': 98},
      });

      expect(visit.id, 'visit-1');
      expect(visit.patientName, 'Marie Test');
      expect(visit.queueSince, DateTime.utc(2026, 7, 30, 8, 15));
      expect(visit.hasVitals, isTrue);
      expect(visit.vitals?.temperature, 38.2);
      expect(visit.vitals?.pulse, 92);
      expect(visit.vitals?.spo2, 98);
    });

    test('uses creation time when arrival time is absent', () {
      final visit = ActiveVisit.fromJson({
        'id': 'visit-2',
        'visitNumber': 'VIS-2026-002',
        'patientId': 'patient-2',
        'patientName': 'Jean Test',
        'patientDpu': 'DPU-002',
        'reason': 'Contrôle',
        'orientation': 'Consultation',
        'status': 'ACTIVE',
        'createdAt': '2026-07-30T09:00:00Z',
      });

      expect(visit.queueSince, DateTime.utc(2026, 7, 30, 9));
      expect(visit.hasVitals, isFalse);
    });

    test('rejects an incomplete payload', () {
      expect(
        () => ActiveVisit.fromJson({
          'id': 'visit-3',
          'visitNumber': 'VIS-2026-003',
        }),
        throwsFormatException,
      );
    });
  });
}
