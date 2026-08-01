import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_client_providers.dart';
import '../../../core/network/api_exception.dart';
import '../domain/patient_vitals.dart';

abstract interface class VitalsGateway {
  Future<PatientVitals?> getVitals(String visitId);
  Future<PatientVitals> saveVitals(String visitId, PatientVitals vitals);
}

final vitalsApiProvider = Provider<VitalsGateway>((ref) {
  final client = ref.watch(apiClientProvider);
  return VitalsApi(client);
});

final class VitalsApi implements VitalsGateway {
  const VitalsApi(this._client);

  final ApiClient _client;

  @override
  Future<PatientVitals?> getVitals(String visitId) async {
    try {
      final response = await _client.get<dynamic>('/api/visits/$visitId/vitals');
      final data = response.data;
      if (data == null) return null;
      if (data is! Map) {
        throw const FormatException('Invalid vitals response format');
      }
      return PatientVitals.fromJson(Map<String, dynamic>.from(data));
    } on ApiException catch (e) {
      if (e.statusCode == 404) return null;
      rethrow;
    }
  }

  @override
  Future<PatientVitals> saveVitals(String visitId, PatientVitals vitals) async {
    final response = await _client.post<dynamic>(
      '/api/visits/$visitId/vitals',
      data: vitals.toJson(),
    );
    final data = response.data;
    if (data is! Map) {
      throw const FormatException('Invalid save vitals response format');
    }
    return PatientVitals.fromJson(Map<String, dynamic>.from(data));
  }
}
