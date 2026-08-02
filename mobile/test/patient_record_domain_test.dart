import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_connect/features/dashboard/domain/active_visit.dart';
import 'package:joprelys_connect/features/dashboard/domain/patient_history.dart';
import 'package:joprelys_connect/features/dashboard/domain/patient_record.dart';

void main() {
  const target = PatientRecordTarget(
    patientId: 'patient-1',
    patientName: 'Patient Test',
    patientDpu: 'DPU-001',
  );

  test('uses directory target as identity fallback', () {
    final identity = PatientIdentity.fromJson({
      'id': 'patient-1',
      'status': 'ACTIVE',
    }, fallback: target);

    expect(identity.fullName, 'Patient Test');
    expect(identity.globalPatientNumber, 'DPU-001');
    expect(identity.status, 'ACTIVE');
  });

  test('maps backend allergy aliases and critical severity', () {
    final allergy = PatientAllergy.fromJson({
      'substance': 'Pénicilline',
      'severity': 'CRITICAL',
      'reaction': 'Anaphylaxie',
    });

    expect(allergy.allergen, 'Pénicilline');
    expect(allergy.severity, AllergySeverity.severe);
    expect(allergy.reaction, 'Anaphylaxie');
  });

  test('patient target may retain its active visit', () {
    final visit = ActiveVisit(
      id: 'visit-1',
      visitNumber: 'VIS-001',
      patientId: 'patient-1',
      patientName: 'Patient Test',
      patientDpu: 'DPU-001',
      reason: 'Contrôle',
      orientation: 'CONSULTATION',
      status: 'OPEN',
      createdAt: DateTime.utc(2026, 8, 3),
    );

    final withVisit = PatientRecordTarget(
      patientId: target.patientId,
      patientName: target.patientName,
      patientDpu: target.patientDpu,
      activeVisit: visit,
    );

    expect(withVisit.activeVisit?.visitNumber, 'VIS-001');
  });
}
