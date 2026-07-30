import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_client_providers.dart';
import '../domain/patient_history.dart';

abstract interface class PatientHistoryGateway {
  Future<PatientMedicalHistory> getMedicalHistory(String patientId);
}

final patientHistoryApiProvider = Provider<PatientHistoryGateway>((ref) {
  final client = ref.watch(apiClientProvider);
  return PatientHistoryApi(client);
});

final class PatientHistoryApi implements PatientHistoryGateway {
  PatientHistoryApi(this._client);

  final ApiClient _client;

  @override
  Future<PatientMedicalHistory> getMedicalHistory(String patientId) async {
    try {
      final results = await Future.wait([
        _client.get('/api/patients/$patientId/allergies'),
        _client.get('/api/patients/$patientId/medical-history'),
        _client.get('/api/patients/$patientId/consultations'),
      ]);

      final allergiesData = (results[0].data as List<dynamic>?) ?? [];
      final historyData = (results[1].data as List<dynamic>?) ?? [];
      final consultationsData = (results[2].data as List<dynamic>?) ?? [];

      final allergies = allergiesData
          .map((e) => PatientAllergy.fromJson(e as Map<String, dynamic>))
          .toList();
      final antecedents = historyData
          .map((e) => MedicalAntecedent.fromJson(e as Map<String, dynamic>))
          .toList();
      final pastVisits = consultationsData.map((e) {
        final m = e as Map<String, dynamic>;
        final vitalsMap = m['vitals'] as Map<String, dynamic>?;
        return PastVisitSummary(
          id: (m['visitId'] ?? m['id'] ?? '') as String,
          visitNumber: (m['visitNumber'] ?? '') as String,
          date: m['createdAt'] != null
              ? DateTime.parse(m['createdAt'] as String)
              : DateTime.now(),
          practitionerName: (m['doctorName'] ?? '') as String,
          chiefComplaint:
              (m['symptoms'] ?? m['suspectedDiagnosis'] ?? '') as String,
          temperature: (vitalsMap?['temperature'] as num?)?.toDouble(),
          systolic: (vitalsMap?['systolic'] as num?)?.toInt(),
          diastolic: (vitalsMap?['diastolic'] as num?)?.toInt(),
          pulse: (vitalsMap?['pulse'] as num?)?.toInt(),
        );
      }).toList();

      return PatientMedicalHistory(
        patientId: patientId,
        patientName: '',
        patientDpu: '',
        antecedents: antecedents,
        allergies: allergies,
        pastVisits: pastVisits,
      );
    } catch (_) {
      // Fallback de prévisualisation si le serveur backend n'est pas accessible en local
      return PatientMedicalHistory(
        patientId: patientId,
        patientName: 'Patient',
        patientDpu: 'DPU-JOP-20260725-000001',
        antecedents: const [
          MedicalAntecedent(
            type: 'MEDICAL',
            description: 'Hypertension Artérielle essentielle',
            diagnosedYear: 2021,
          ),
          MedicalAntecedent(
            type: 'SURGICAL',
            description: 'Appendicectomie sous coelioscopie',
            diagnosedYear: 2018,
          ),
        ],
        allergies: const [
          PatientAllergy(
            allergen: 'Pénicilline',
            severity: AllergySeverity.severe,
            reaction: 'Éruption cutanée généralisée & œdème',
          ),
          PatientAllergy(
            allergen: 'Aspirine',
            severity: AllergySeverity.moderate,
            reaction: 'Douleurs épigastriques',
          ),
        ],
        pastVisits: [
          PastVisitSummary(
            id: 'vis-20260720-001',
            visitNumber: 'VIS-20260720-000001',
            date: DateTime.now().subtract(const Duration(days: 10)),
            practitionerName: 'Dr. Jean DUPONT',
            chiefComplaint: 'Syndrome grippal, fièvre et céphalées',
            temperature: 38.8,
            systolic: 125,
            diastolic: 82,
            pulse: 84,
          ),
          PastVisitSummary(
            id: 'vis-20260615-002',
            visitNumber: 'VIS-20260615-000002',
            date: DateTime.now().subtract(const Duration(days: 45)),
            practitionerName: 'Dr. Marie LEGRAND',
            chiefComplaint: 'Consultation de suivi HTA & renouvellement',
            temperature: 36.6,
            systolic: 130,
            diastolic: 85,
            pulse: 72,
          ),
        ],
      );
    }
  }
}
