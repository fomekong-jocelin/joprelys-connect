import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/domain/active_visit.dart';
import 'package:joprelys_mobile/features/dashboard/domain/patient_history.dart';
import 'package:joprelys_mobile/features/dashboard/domain/patient_record.dart';

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

  test('keeps the complete SOAP consultation and prescription payload', () {
    final visit = PastVisitSummary.fromJson({
      'id': 'consultation-1',
      'visitId': 'visit-1',
      'visitNumber': 'VIS-001',
      'createdAt': '2026-08-08T00:30:00Z',
      'doctorName': 'Dr Test',
      'symptoms': 'Fatigue depuis trois semaines et toux sèche.',
      'clinicalExam': 'Léger sifflement du côté droit.',
      'diagnosis': '',
      'conclusion': 'Bilan complémentaire demandé.',
      'advice': 'Hydratation.',
      'followUp': 'Contrôle après les résultats.',
      'status': 'BROUILLON',
      'documentNumber': 'DOC-CONS-001',
      'vitals': {
        'temperature': 36.8,
        'systolic': 120,
        'diastolic': 80,
        'pulse': 72,
      },
      'prescriptionNumber': 'ORD-001',
      'prescriptionStatus': 'DRAFT',
      'prescriptionItems': [
        {
          'drugName': 'Inhalateur',
          'dosage': '',
          'duration': '7 jours',
          'instructions': 'Selon prescription',
        },
      ],
    });

    expect(visit.id, 'visit-1');
    expect(visit.consultationId, 'consultation-1');
    expect(visit.symptoms, 'Fatigue depuis trois semaines et toux sèche.');
    expect(visit.clinicalExam, 'Léger sifflement du côté droit.');
    expect(visit.diagnosis, isEmpty);
    expect(visit.conclusion, 'Bilan complémentaire demandé.');
    expect(visit.advice, 'Hydratation.');
    expect(visit.followUp, 'Contrôle après les résultats.');
    expect(visit.status, 'BROUILLON');
    expect(visit.temperature, 36.8);
    expect(visit.systolic, 120);
    expect(visit.diastolic, 80);
    expect(visit.pulse, 72);
    expect(visit.prescriptionItems, hasLength(1));
    expect(visit.prescriptionItems.single.drugName, 'Inhalateur');
    expect(visit.prescriptionItems.single.dosage, isNull);
    expect(visit.prescriptionItems.single.duration, '7 jours');
  });

  test('still accepts a legacy minimal consultation payload', () {
    final visit = PastVisitSummary.fromJson({
      'id': 'legacy-consultation',
      'visitNumber': 'VIS-OLD',
      'date': '2026-07-01T10:00:00Z',
      'practitionerName': 'Dr Legacy',
      'reason': 'Contrôle',
    });

    expect(visit.visitNumber, 'VIS-OLD');
    expect(visit.practitionerName, 'Dr Legacy');
    expect(visit.consultationId, 'legacy-consultation');
    expect(visit.symptoms, 'Contrôle');
    expect(visit.diagnosis, isEmpty);
    expect(visit.prescriptionItems, isEmpty);
  });

  test('keeps structured lab order metadata and individual exams', () {
    final order = PatientLabOrderSummary.fromJson({
      'id': 'order-1',
      'examRequestNumber': 'EXAM-001',
      'status': 'PENDING',
      'examType': 'AUTRE',
      'priority': 'ROUTINE',
      'reason': 'Fatigue persistante',
      'requesterPractitionerName': 'Dr Test',
      'createdAt': '2026-08-08T00:30:00Z',
      'exams': [
        'Numération complète',
        'Bilan inflammatoire',
        'Radiographie du thorax',
      ],
    });

    expect(order.number, 'EXAM-001');
    expect(order.status, 'PENDING');
    expect(order.examType, 'AUTRE');
    expect(order.priority, 'ROUTINE');
    expect(order.reason, 'Fatigue persistante');
    expect(order.practitioner, 'Dr Test');
    expect(order.exams, [
      'Numération complète',
      'Bilan inflammatoire',
      'Radiographie du thorax',
    ]);
  });
}
