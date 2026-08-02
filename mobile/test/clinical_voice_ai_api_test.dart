import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/core/network/api_client.dart';
import 'package:joprelys_mobile/core/network/api_exception_mapper.dart';
import 'package:joprelys_mobile/features/dashboard/data/clinical_voice_ai_api.dart';

import 'support/queue_http_client_adapter.dart';

void main() {
  group('ClinicalVoiceAiApi session decoding', () {
    test('maps a 204 response to an absent session', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => ResponseBody.fromString('', 204),
      );
      final api = buildApi(adapter);

      expect(await api.getSession('visit-123'), isNull);
    });

    test('decodes the SessionView returned as a JSON map', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => jsonResponse(200, sessionViewJson()),
      );
      final api = buildApi(adapter);

      final state = await api.getSession('visit-123');

      expect(state, isNotNull);
      expect(state!.sessionId, 'session-123');
      expect(state.visitId, 'visit-123');
      expect(state.sessionStatus, 'ACTIVE');
      expect(state.expiresAt, '2026-08-02T15:00:00Z');
      expect(state.transcriptStatus, 'ANALYZED');
      expect(state.noteFrom().symptoms, 'Fièvre depuis deux jours');
    });

    test('decodes the SessionView returned as a JSON string', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => stringResponse(200, jsonEncode(sessionViewJson())),
      );
      final api = buildApi(adapter);

      final state = await api.getSession('visit-123');

      expect(state?.sessionId, 'session-123');
      expect(state?.draft['diagnosis'], 'Paludisme simple suspecté');
    });

    test(
      'treats an empty successful GET body as no existing session',
      () async {
        final adapter = QueueHttpClientAdapter(
          (_, _) => stringResponse(200, '   '),
        );
        final api = buildApi(adapter);

        expect(await api.getSession('visit-123'), isNull);
      },
    );

    test('rejects a non-object JSON session payload', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => stringResponse(200, '[1, 2, 3]'),
      );
      final api = buildApi(adapter);

      await expectLater(
        api.getSession('visit-123'),
        throwsA(
          isA<FormatException>().having(
            (error) => error.message,
            'message',
            'Invalid AI session response',
          ),
        ),
      );
    });
  });

  group('ClinicalVoiceAiApi durable transcript', () {
    test('replaces the pending transcript with one PUT request', () async {
      final adapter = QueueHttpClientAdapter((options, _) {
        expect(options.method, 'PUT');
        expect(
          options.path,
          '/api/ai/consultations/visit-123/transcriptions/pending',
        );
        expect(
          Map<String, dynamic>.from(options.data as Map),
          <String, dynamic>{'transcript': 'Texte corrigé et complet'},
        );
        return jsonResponse(200, transcriptionViewJson());
      });
      final api = buildApi(adapter);

      await api.savePendingTranscript(
        'visit-123',
        'Texte corrigé et complet',
      );

      expect(adapter.requests, hasLength(1));
    });

    test('deletes the pending transcript when the reviewed text is empty', () async {
      final adapter = QueueHttpClientAdapter((options, _) {
        expect(options.method, 'DELETE');
        expect(
          options.path,
          '/api/ai/consultations/visit-123/transcriptions/pending',
        );
        return ResponseBody.fromString('', 204);
      });
      final api = buildApi(adapter);

      await api.savePendingTranscript('visit-123', '   ');

      expect(adapter.requests, hasLength(1));
    });
  });

  group('ClinicalVoiceAiApi progressive analysis', () {
    test(
      'keeps using the realtime endpoint and decodes MessageView strings',
      () async {
        final adapter = QueueHttpClientAdapter((options, _) {
          expect(
            options.path,
            '/api/ai/consultations/visit-123/messages/realtime',
          );
          final request = Map<String, dynamic>.from(options.data as Map);
          expect(request['transcript'], 'Le patient présente une forte fièvre');
          expect(request['confidence'], 1.0);
          return stringResponse(200, jsonEncode(messageViewJson()));
        });
        final api = buildApi(adapter);

        final state = await api.analyzeTranscript(
          'visit-123',
          'Le patient présente une forte fièvre',
        );

        expect(state.sessionId, 'session-123');
        expect(state.visitId, isNull);
        expect(state.noteFrom(includePending: true).symptoms, 'Forte fièvre');
        expect(state.revisions.single.hasPendingProposals, isTrue);
      },
    );
  });
}

ClinicalVoiceAiApi buildApi(QueueHttpClientAdapter adapter) {
  final dio = Dio(BaseOptions(baseUrl: 'https://api.test'))
    ..httpClientAdapter = adapter;
  return ClinicalVoiceAiApi(
    ApiClient(dio: dio, exceptionMapper: const ApiExceptionMapper()),
  );
}

ResponseBody jsonResponse(int statusCode, Map<String, dynamic> payload) {
  return ResponseBody.fromString(
    jsonEncode(payload),
    statusCode,
    headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    },
  );
}

ResponseBody stringResponse(int statusCode, String payload) {
  return ResponseBody.fromString(
    payload,
    statusCode,
    headers: {
      Headers.contentTypeHeader: ['text/plain; charset=utf-8'],
    },
  );
}

Map<String, dynamic> transcriptionViewJson() {
  return <String, dynamic>{
    'sessionId': 'session-123',
    'transcript': 'Texte corrigé et complet',
    'status': 'PENDING_REVIEW',
    'expiresAt': '2026-08-02T15:00:00Z',
  };
}

Map<String, dynamic> sessionViewJson() {
  return <String, dynamic>{
    'sessionId': 'session-123',
    'visitId': 'visit-123',
    'status': 'ACTIVE',
    'expiresAt': '2026-08-02T15:00:00Z',
    'draft': <String, String>{
      'symptoms': 'Fièvre depuis deux jours',
      'diagnosis': 'Paludisme simple suspecté',
    },
    'transcript': 'Le patient présente une fièvre depuis deux jours.',
    'pendingTranscript': null,
    'transcriptStatus': 'ANALYZED',
    'conversation': const <Object>[],
    'clarifications': const <Object>[],
    'revisions': const <Object>[],
    'assistantMessage': 'Les premiers éléments ont été extraits.',
    'needsClarification': false,
  };
}

Map<String, dynamic> messageViewJson() {
  return <String, dynamic>{
    'sessionId': 'session-123',
    'transcript': 'Le patient présente une forte fièvre',
    'draft': const <String, String>{},
    'changedFields': const <String>['symptoms'],
    'assistantMessage': 'Symptôme détecté.',
    'needsClarification': false,
    'conversation': const <Object>[],
    'clarifications': const <Object>[],
    'revisions': <Map<String, dynamic>>[
      <String, dynamic>{
        'id': 'revision-1',
        'sequence': 1,
        'status': 'PENDING',
        'createdAt': '2026-08-02T14:00:00Z',
        'proposals': <Map<String, dynamic>>[
          <String, dynamic>{
            'id': 'proposal-1',
            'field': 'symptoms',
            'operation': 'SET',
            'previousValue': null,
            'proposedValue': 'Forte fièvre',
            'reason': 'Mention explicite dans la dictée',
            'uncertainty': 'LOW',
            'status': 'PENDING',
            'createdAt': '2026-08-02T14:00:00Z',
            'decidedAt': null,
          },
        ],
      },
    ],
    'expiresAt': '2026-08-02T15:00:00Z',
  };
}
