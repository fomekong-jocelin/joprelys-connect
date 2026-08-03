import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_client_providers.dart';
import 'clinical_voice_ai_api.dart';

final clinicalVoiceCaptureApiProvider = Provider<ClinicalVoiceCaptureGateway>((
  ref,
) {
  return ClinicalVoiceCaptureApi(ref.watch(apiClientProvider));
});

abstract interface class ClinicalVoiceCaptureGateway {
  Future<List<ClinicalVoiceIntake>> listActive(String visitId);

  Future<ClinicalVoiceIntake> ingestSegment(
    String visitId, {
    required String eventId,
    required String itemId,
    required String transcript,
  });

  Future<ClinicalVoiceIntake> correctSegment(
    String visitId,
    String intakeId,
    String transcript,
  );

  Future<void> discardSegment(String visitId, String intakeId);

  Future<void> discardAll(String visitId);

  Future<void> consume(String visitId);

  Future<ClinicalAiState> analyzeProgressiveSegment(
    String visitId, {
    required String eventId,
    required String transcript,
  });

  Future<ClinicalAiState> rebuild(
    String visitId,
    Map<String, String> draft, {
    required String locale,
  });

  Future<void> deleteSession(String visitId);
}

@immutable
final class ClinicalVoiceIntake {
  const ClinicalVoiceIntake({
    required this.id,
    required this.sequence,
    required this.eventId,
    required this.itemId,
    required this.transcript,
    required this.captureStatus,
    required this.reviewRequired,
  });

  factory ClinicalVoiceIntake.fromJson(Map<String, dynamic> json) {
    return ClinicalVoiceIntake(
      id: json['id']?.toString() ?? '',
      sequence: (json['sequence'] as num?)?.toInt() ?? 0,
      eventId: json['eventId']?.toString() ?? '',
      itemId: json['itemId']?.toString(),
      transcript: json['transcript']?.toString() ?? '',
      captureStatus: json['captureStatus']?.toString() ?? 'CAPTURED',
      reviewRequired: json['reviewRequired'] == true,
    );
  }

  final String id;
  final int sequence;
  final String eventId;
  final String? itemId;
  final String transcript;
  final String captureStatus;
  final bool reviewRequired;
}

final class ClinicalVoiceCaptureApi implements ClinicalVoiceCaptureGateway {
  const ClinicalVoiceCaptureApi(this._client);

  final ApiClient _client;

  @override
  Future<List<ClinicalVoiceIntake>> listActive(String visitId) async {
    final response = await _client.get<dynamic>(
      '/api/ai/consultations/$visitId/realtime-intake',
    );
    final data = _arrayFrom(
      response.data,
      'Invalid clinical voice intake response',
    );
    return data
        .whereType<Map>()
        .map(
          (item) =>
              ClinicalVoiceIntake.fromJson(Map<String, dynamic>.from(item)),
        )
        .where(
          (item) => item.id.isNotEmpty && item.transcript.trim().isNotEmpty,
        )
        .toList(growable: false);
  }

  @override
  Future<ClinicalVoiceIntake> ingestSegment(
    String visitId, {
    required String eventId,
    required String itemId,
    required String transcript,
  }) async {
    final response = await _client.post<dynamic>(
      '/api/ai/consultations/$visitId/realtime-intake',
      data: <String, dynamic>{
        'eventId': eventId,
        'itemId': itemId,
        'transcript': transcript.trim(),
        // speech_to_text does not expose one stable confidence for a committed
        // passage. The durable intake preserves this fact as review-required.
        'confidence': null,
      },
    );
    return ClinicalVoiceIntake.fromJson(
      _objectFrom(response.data, 'Invalid clinical voice intake response'),
    );
  }

  @override
  Future<ClinicalVoiceIntake> correctSegment(
    String visitId,
    String intakeId,
    String transcript,
  ) async {
    final response = await _client.post<dynamic>(
      '/api/ai/consultations/$visitId/realtime-intake/$intakeId/correction',
      data: <String, dynamic>{'transcript': transcript.trim()},
    );
    return ClinicalVoiceIntake.fromJson(
      _objectFrom(response.data, 'Invalid clinical voice correction response'),
    );
  }

  @override
  Future<void> discardSegment(String visitId, String intakeId) async {
    await _client.delete<void>(
      '/api/ai/consultations/$visitId/realtime-intake/$intakeId',
    );
  }

  @override
  Future<void> discardAll(String visitId) async {
    await _client.delete<void>(
      '/api/ai/consultations/$visitId/realtime-intake',
    );
  }

  @override
  Future<void> consume(String visitId) async {
    await _client.post<void>(
      '/api/ai/consultations/$visitId/realtime-intake/consume',
    );
  }

  @override
  Future<ClinicalAiState> analyzeProgressiveSegment(
    String visitId, {
    required String eventId,
    required String transcript,
  }) async {
    final response = await _client.post<dynamic>(
      '/api/ai/consultations/$visitId/messages/realtime',
      data: <String, dynamic>{
        'eventId': eventId,
        'transcript': transcript.trim(),
        // The text is analyzed only after speech_to_text marked the passage final
        // and after it was durably acknowledged. This is transport confidence,
        // never an acoustic confidence persisted in the intake ledger.
        'confidence': 1.0,
      },
    );
    return ClinicalAiState.fromJson(
      _objectFrom(response.data, 'Invalid progressive AI response'),
    );
  }

  @override
  Future<ClinicalAiState> rebuild(
    String visitId,
    Map<String, String> draft, {
    required String locale,
  }) async {
    final response = await _client.post<dynamic>(
      '/api/ai/consultations/$visitId/capture/rebuild',
      data: <String, dynamic>{'draft': draft, 'locale': locale},
    );
    return ClinicalAiState.fromJson(
      _objectFrom(response.data, 'Invalid clinical voice rebuild response'),
    );
  }

  @override
  Future<void> deleteSession(String visitId) async {
    await _client.delete<void>('/api/ai/consultations/$visitId/session');
  }

  List<dynamic> _arrayFrom(Object? data, String message) {
    final decoded = _decode(data, message);
    if (decoded is! List) throw FormatException(message);
    return decoded;
  }

  Map<String, dynamic> _objectFrom(Object? data, String message) {
    final decoded = _decode(data, message);
    if (decoded is! Map) throw FormatException(message);
    return Map<String, dynamic>.from(decoded);
  }

  Object _decode(Object? data, String message) {
    if (data == null) throw FormatException(message);
    if (data is! String) return data;
    final body = data.trim();
    if (body.isEmpty) throw FormatException(message);
    try {
      return jsonDecode(body) ?? (throw FormatException(message));
    } on FormatException {
      throw FormatException(message);
    }
  }
}
