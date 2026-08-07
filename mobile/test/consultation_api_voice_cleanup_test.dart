import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/core/network/api_client.dart';
import 'package:joprelys_mobile/core/network/api_exception_mapper.dart';
import 'package:joprelys_mobile/features/dashboard/data/consultation_api.dart';
import 'package:joprelys_mobile/features/dashboard/domain/consultation_note.dart';

import 'support/queue_http_client_adapter.dart';

void main() {
  test(
    'keeps voice intake after SOAP save until cleanup is explicitly requested',
    () async {
      var requestIndex = 0;
      final adapter = QueueHttpClientAdapter((options, _) {
        requestIndex++;
        if (requestIndex == 1) {
          expect(options.method, 'POST');
          expect(options.path, '/api/visits/visit-123/consultation');
          return jsonResponse(200, noteJson());
        }
        expect(options.method, 'POST');
        expect(
          options.path,
          '/api/ai/consultations/visit-123/realtime-intake/consume',
        );
        return ResponseBody.fromString('', 204);
      });
      final api = buildApi(adapter);

      final saved = await api.saveConsultationNote(
        'visit-123',
        const ConsultationNote(symptoms: 'Toux depuis trois jours'),
      );

      expect(saved.note.symptoms, 'Toux depuis trois jours');
      expect(adapter.requests, hasLength(1));

      await api.consumeVoiceWorkingSet('visit-123');
      expect(adapter.requests, hasLength(2));
    },
  );

  test(
    'does not report persisted resources as failed when cleanup fails',
    () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => jsonResponse(503, <String, dynamic>{'message': 'Unavailable'}),
      );
      final api = buildApi(adapter);

      await api.consumeVoiceWorkingSet('visit-123');

      expect(adapter.requests, hasLength(1));
    },
  );

  test(
    'a failed SOAP persistence never triggers cleanup on its own',
    () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => jsonResponse(500, <String, dynamic>{'message': 'Failure'}),
      );
      final api = buildApi(adapter);

      await expectLater(
        api.saveConsultationNote(
          'visit-123',
          const ConsultationNote(symptoms: 'Toux depuis trois jours'),
        ),
        throwsA(anything),
      );
      expect(adapter.requests, hasLength(1));
    },
  );
}

ConsultationApi buildApi(QueueHttpClientAdapter adapter) {
  final dio = Dio(BaseOptions(baseUrl: 'https://api.test'))
    ..httpClientAdapter = adapter;
  return ConsultationApi(
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

Map<String, dynamic> noteJson() {
  return <String, dynamic>{
    'id': 'consultation-123',
    'symptoms': 'Toux depuis trois jours',
    'clinicalExam': null,
    'diagnosis': null,
    'conclusion': null,
    'advice': null,
    'followUp': null,
    'updatedAt': '2026-08-03T20:00:00Z',
  };
}
