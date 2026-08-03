import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/core/network/api_client.dart';
import 'package:joprelys_mobile/core/network/api_exception_mapper.dart';
import 'package:joprelys_mobile/features/dashboard/data/clinical_voice_capture_api.dart';

import 'support/queue_http_client_adapter.dart';

void main() {
  test('restores the active durable transcript working set', () async {
    final adapter = QueueHttpClientAdapter((options, _) {
      expect(options.method, 'GET');
      expect(options.path, '/api/ai/consultations/visit-123/realtime-intake');
      return ResponseBody.fromString(
        jsonEncode(<Map<String, dynamic>>[
          intakeJson(),
          <String, dynamic>{...intakeJson(), 'captureStatus': 'ANALYZED'},
        ]),
        200,
        headers: {
          Headers.contentTypeHeader: [Headers.jsonContentType],
        },
      );
    });

    final result = await buildApi(adapter).listActive('visit-123');

    expect(result, hasLength(2));
    expect(result.first.id, 'intake-1');
    expect(result.first.itemId, 'segment-1');
    expect(result.first.reviewRequired, isTrue);
  });

  test('persists a finalized passage before progressive analysis', () async {
    var requestIndex = 0;
    final adapter = QueueHttpClientAdapter((options, _) {
      requestIndex++;
      if (requestIndex == 1) {
        expect(options.method, 'POST');
        expect(options.path, '/api/ai/consultations/visit-123/realtime-intake');
        final body = Map<String, dynamic>.from(options.data as Map);
        expect(body['eventId'], 'mobile-segment-1');
        expect(body['itemId'], 'segment-1');
        expect(body['transcript'], 'Le patient tousse depuis trois jours');
        expect(body['confidence'], isNull);
        return jsonResponse(200, intakeJson());
      }

      expect(options.method, 'POST');
      expect(options.path, '/api/ai/consultations/visit-123/messages/realtime');
      final body = Map<String, dynamic>.from(options.data as Map);
      expect(body['eventId'], 'mobile-segment-1');
      expect(body['confidence'], 1.0);
      return jsonResponse(200, messageViewJson());
    });
    final api = buildApi(adapter);

    final intake = await api.ingestSegment(
      'visit-123',
      eventId: 'mobile-segment-1',
      itemId: 'segment-1',
      transcript: 'Le patient tousse depuis trois jours',
    );
    final state = await api.analyzeProgressiveSegment(
      'visit-123',
      eventId: 'mobile-segment-1',
      transcript: 'Le patient tousse depuis trois jours',
    );

    expect(intake.transcript, 'Le patient tousse depuis trois jours');
    expect(
      state.noteFrom().symptoms,
      'Le patient rapporte une toux depuis trois jours',
    );
    expect(adapter.requests, hasLength(2));
  });

  test('rebuilds the authoritative final draft from durable intake', () async {
    final adapter = QueueHttpClientAdapter((options, _) {
      expect(options.method, 'POST');
      expect(options.path, '/api/ai/consultations/visit-123/capture/rebuild');
      expect(Map<String, dynamic>.from(options.data as Map), <String, dynamic>{
        'draft': const <String, String>{},
        'locale': 'fr',
      });
      return jsonResponse(200, sessionViewJson());
    });

    final state = await buildApi(
      adapter,
    ).rebuild('visit-123', const <String, String>{}, locale: 'fr');

    expect(state.sessionId, 'session-123');
    expect(state.transcriptStatus, 'ANALYZED');
    expect(
      state.noteFrom().symptoms,
      'Le patient rapporte une toux depuis trois jours',
    );
  });

  test('consumes the durable working set only after clinical save', () async {
    final adapter = QueueHttpClientAdapter((options, _) {
      expect(options.method, 'POST');
      expect(
        options.path,
        '/api/ai/consultations/visit-123/realtime-intake/consume',
      );
      return ResponseBody.fromString('', 204);
    });

    await buildApi(adapter).consume('visit-123');

    expect(adapter.requests, hasLength(1));
  });
}

ClinicalVoiceCaptureApi buildApi(QueueHttpClientAdapter adapter) {
  final dio = Dio(BaseOptions(baseUrl: 'https://api.test'))
    ..httpClientAdapter = adapter;
  return ClinicalVoiceCaptureApi(
    ApiClient(dio: dio, exceptionMapper: const ApiExceptionMapper()),
  );
}

ResponseBody jsonResponse(int statusCode, Object payload) {
  return ResponseBody.fromString(
    jsonEncode(payload),
    statusCode,
    headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    },
  );
}

Map<String, dynamic> intakeJson() {
  return <String, dynamic>{
    'id': 'intake-1',
    'visitId': 'visit-123',
    'source': 'CONSULTATION',
    'sequence': 1,
    'eventId': 'mobile-segment-1',
    'itemId': 'segment-1',
    'transcript': 'Le patient tousse depuis trois jours',
    'originalTranscript': 'Le patient tousse depuis trois jours',
    'confidence': 0.0,
    'reviewRequired': true,
    'correctionCount': 0,
    'correctedAt': null,
    'captureStatus': 'CAPTURED',
    'receivedAt': '2026-08-03T20:00:00Z',
  };
}

Map<String, dynamic> messageViewJson() {
  return <String, dynamic>{
    'sessionId': 'session-123',
    'transcript': null,
    'draft': <String, String>{
      'symptoms': 'Le patient rapporte une toux depuis trois jours',
    },
    'changedFields': const <String>['symptoms'],
    'assistantMessage':
        'Transcription conservée et brouillon de travail mis à jour.',
    'needsClarification': false,
    'conversation': const <Object>[],
    'clarifications': const <Object>[],
    'revisions': const <Object>[],
    'expiresAt': '2026-08-03T21:00:00Z',
  };
}

Map<String, dynamic> sessionViewJson() {
  return <String, dynamic>{
    'sessionId': 'session-123',
    'visitId': 'visit-123',
    'status': 'ACTIVE',
    'expiresAt': '2026-08-03T21:00:00Z',
    'draft': <String, String>{
      'symptoms': 'Le patient rapporte une toux depuis trois jours',
    },
    'transcript': null,
    'pendingTranscript': null,
    'transcriptStatus': 'ANALYZED',
    'conversation': const <Object>[],
    'clarifications': const <Object>[],
    'revisions': const <Object>[],
    'assistantMessage': 'Synthèse reconstruite.',
    'needsClarification': false,
  };
}
