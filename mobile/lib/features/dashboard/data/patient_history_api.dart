import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_client_providers.dart';
import '../../../core/network/api_exception.dart';
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

  Future<List<dynamic>> _safeFetchList(String path) async {
    try {
      final response = await _client.get<dynamic>(path);
      final data = response.data;
      if (data is List) return data;
      return [];
    } on ApiException catch (e) {
      if (e.statusCode == 404) return [];
      rethrow;
    }
  }

  @override
  Future<PatientMedicalHistory> getMedicalHistory(String patientId) async {
    final results = await Future.wait([
      _safeFetchList('/api/patients/$patientId/allergies'),
      _safeFetchList('/api/patients/$patientId/medical-history'),
      _safeFetchList('/api/patients/$patientId/consultations'),
    ]);

    final allergiesData = results[0];
    final historyData = results[1];
    final consultationsData = results[2];

    final allergies = allergiesData
        .whereType<Map>()
        .map((e) => PatientAllergy.fromJson(Map<String, dynamic>.from(e)))
        .toList();
    final antecedents = historyData
        .whereType<Map>()
        .map((e) => MedicalAntecedent.fromJson(Map<String, dynamic>.from(e)))
        .toList();
    final pastVisits = consultationsData.whereType<Map>().map((m) {
      final map = Map<String, dynamic>.from(m);
      final vitalsMap = map['vitals'] is Map
          ? Map<String, dynamic>.from(map['vitals'] as Map)
          : null;
      return PastVisitSummary(
        id: (map['visitId'] ?? map['id'] ?? '') as String,
        visitNumber: (map['visitNumber'] ?? '') as String,
        date: map['createdAt'] != null
            ? (DateTime.tryParse(map['createdAt'] as String) ?? DateTime.now())
            : DateTime.now(),
        practitionerName:
            (map['doctorName'] ?? map['practitionerName'] ?? '') as String,
        chiefComplaint: (map['symptoms'] ?? map['reason'] ?? '') as String,
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
  }
}
