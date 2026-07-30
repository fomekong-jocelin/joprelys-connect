import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_client_providers.dart';
import '../../../core/network/api_exception.dart';
import '../domain/consultation_note.dart';

abstract interface class ConsultationGateway {
  Future<ConsultationNote?> getConsultationNote(String visitId);
  Future<ConsultationNote> saveConsultationNote(
    String visitId,
    ConsultationNote note,
  );
}

final consultationApiProvider = Provider<ConsultationGateway>((ref) {
  final client = ref.watch(apiClientProvider);
  return ConsultationApi(client);
});

final class ConsultationApi implements ConsultationGateway {
  const ConsultationApi(this._client);

  final ApiClient _client;

  @override
  Future<ConsultationNote?> getConsultationNote(String visitId) async {
    try {
      final response = await _client.get<dynamic>('/api/visits/$visitId/consultation-notes');
      final data = response.data;
      if (data == null) return null;
      if (data is! Map) {
        throw const FormatException('Invalid consultation note format');
      }
      return ConsultationNote.fromJson(Map<String, dynamic>.from(data));
    } on ApiException catch (e) {
      if (e.statusCode == 404) return null;
      rethrow;
    }
  }

  @override
  Future<ConsultationNote> saveConsultationNote(
    String visitId,
    ConsultationNote note,
  ) async {
    final response = await _client.post<dynamic>(
      '/api/visits/$visitId/consultation-notes',
      data: note.toJson(),
    );
    final data = response.data;
    if (data is! Map) {
      throw const FormatException('Invalid save consultation note response');
    }
    return ConsultationNote.fromJson(Map<String, dynamic>.from(data));
  }
}
