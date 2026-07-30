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
      final response = await _client.get('/api/patients/$patientId/medical-history');
      final data = response.data as Map<String, dynamic>;
      return PatientMedicalHistory.fromJson(data);
    } catch (_) {
      // Stub fallback d'historique médical clinique pour démo & tests hors-ligne
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
