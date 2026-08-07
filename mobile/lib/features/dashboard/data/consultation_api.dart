import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_client_providers.dart';
import '../domain/consultation_note.dart';

@immutable
final class SavedConsultationNote {
  const SavedConsultationNote({
    required this.consultationId,
    required this.note,
  });

  final String consultationId;
  final ConsultationNote note;
}

abstract interface class ConsultationGateway {
  Future<ConsultationNote?> getConsultationNote(String visitId);
  Future<SavedConsultationNote> saveConsultationNote(
    String visitId,
    ConsultationNote note,
  );
  Future<void> consumeVoiceWorkingSet(String visitId);
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
  Future<SavedConsultationNote> saveConsultationNote(
    String visitId,
    ConsultationNote note,
  ) async {
    final response = await _client.post<dynamic>(
      '/api/visits/$visitId/consultation',
      data: note.toSoapJson(),
    );
    final data = response.data;
    if (data is! Map) {
      throw const FormatException('Invalid save consultation note response');
    }
    final json = Map<String, dynamic>.from(data);
    final consultationId = json['id']?.toString().trim() ?? '';
    if (consultationId.isEmpty) {
      throw const FormatException('Missing consultation id in save response');
    }
    return SavedConsultationNote(
      consultationId: consultationId,
      note: ConsultationNote.fromJson(json),
    );
  }

  @override
  Future<void> consumeVoiceWorkingSet(String visitId) async {
    // Cleanup is best-effort and deliberately separate from SOAP persistence so
    // structured prescription/exam retries keep their durable voice source.
    try {
      await _client.post<void>(
        '/api/ai/consultations/$visitId/realtime-intake/consume',
      );
    } catch (_) {
      // Clinical resources are already persisted; cleanup can be retried later.
    }
  }
}
