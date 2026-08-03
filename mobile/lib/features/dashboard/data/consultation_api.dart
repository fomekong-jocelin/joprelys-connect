import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_client_providers.dart';
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
    final response = await _client.get<dynamic>(
      '/api/visits/$visitId/consultation',
    );
    final data = response.data;
    if (response.statusCode == 204 ||
        data == null ||
        (data is String && data.trim().isEmpty)) {
      return null;
    }
    if (data is! Map) {
      throw const FormatException('Invalid consultation note format');
    }
    return ConsultationNote.fromJson(Map<String, dynamic>.from(data));
  }

  @override
  Future<ConsultationNote> saveConsultationNote(
    String visitId,
    ConsultationNote note,
  ) async {
    final response = await _client.post<dynamic>(
      '/api/visits/$visitId/consultation',
      data: note.toJson(),
    );
    final data = response.data;
    if (data is! Map) {
      throw const FormatException('Invalid save consultation note response');
    }
    final saved = ConsultationNote.fromJson(Map<String, dynamic>.from(data));

    // The durable voice working set remains recoverable until the clinical note
    // itself is saved. Cleanup is deliberately best-effort: a cleanup outage must
    // never make the UI report that an already persisted consultation has failed.
    try {
      await _client.post<void>(
        '/api/ai/consultations/$visitId/realtime-intake/consume',
      );
    } catch (_) {
      // A later save or explicit clear retries cleanup; clinical persistence wins.
    }
    return saved;
  }
}
