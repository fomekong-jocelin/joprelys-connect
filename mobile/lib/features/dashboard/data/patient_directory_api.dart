import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_client_providers.dart';
import '../../../core/network/api_exception.dart';
import '../domain/patient_directory_item.dart';

abstract interface class PatientDirectoryGateway {
  Future<List<PatientDirectoryItem>> searchPatients({String? query});
}

final patientDirectoryApiProvider = Provider<PatientDirectoryGateway>((ref) {
  final client = ref.watch(apiClientProvider);
  return PatientDirectoryApi(client);
});

final class PatientDirectoryApi implements PatientDirectoryGateway {
  PatientDirectoryApi(this._client);

  final ApiClient _client;

  @override
  Future<List<PatientDirectoryItem>> searchPatients({String? query}) async {
    try {
      final qParam = query?.trim();
      final path = (qParam != null && qParam.isNotEmpty)
          ? '/api/patients?q=${Uri.encodeComponent(qParam)}'
          : '/api/patients';

      final response = await _client.get<dynamic>(path);
      final data = response.data;
      if (data is! List) return [];

      return data
          .whereType<Map>()
          .map((m) => PatientDirectoryItem.fromJson(Map<String, dynamic>.from(m)))
          .toList();
    } on ApiException catch (e) {
      if (e.statusCode == 404) return [];
      rethrow;
    }
  }
}
