import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/core/network/api_client.dart';
import 'package:joprelys_mobile/core/network/api_exception_mapper.dart';
import 'package:joprelys_mobile/features/dashboard/data/vitals_api.dart';
import 'package:joprelys_mobile/features/dashboard/domain/patient_vitals.dart';

import 'support/queue_http_client_adapter.dart';

void main() {
  test('consumes voice intake only after vitals persistence succeeds', () async {
    var requestIndex = 0;
    final adapter = QueueHttpClientAdapter((options, _) {
      requestIndex++;
      if (requestIndex == 1) {
        expect(options.method, 'POST');
        expect(options.path, '/api/visits/visit-123/vitals');
        return jsonResponse(200, vitalsJson());
      }
      expect(options.method, 'POST');
      expect(
        options.path,
        '/api/ai/consultations/visit-123/realtime-intake/consume',
      );
      return ResponseBody.fromString('', 204);
    });

    final saved = await buildApi(adapter).saveVitals(
      'visit-123',
      const PatientVitals(temperature: 38.2, pulse: 92),
    );

    expect(saved.temperature, 38.2);
    expect(saved.pulse, 92);
    expect(adapter.requests, hasLength(2));
  });

  test('cleanup outage does not invalidate already saved vitals', () async {
    var requestIndex = 0;
    final adapter = QueueHttpClientAdapter((options, _) {
      requestIndex++;
      if (requestIndex == 1) return jsonResponse(200, vitalsJson());
      return jsonResponse(503, <String, dynamic>{'message': 'Unavailable'});
    });

    final saved = await buildApi(adapter).saveVitals(
      'visit-123',
      const PatientVitals(temperature: 38.2, pulse: 92),
    );

    expect(saved.temperature, 38.2);
    expect(adapter.requests, hasLength(2));
  });

  test('never consumes voice intake when vitals persistence fails', () async {
    final adapter = QueueHttpClientAdapter(
      (_, _) => jsonResponse(500, <String, dynamic>{'message': 'Failure'}),
    );

    await expectLater(
      buildApi(adapter).saveVitals(
        'visit-123',
        const PatientVitals(temperature: 38.2),
      ),
      throwsA(anything),
    );
    expect(adapter.requests, hasLength(1));
  });
}

VitalsApi buildApi(QueueHttpClientAdapter adapter) {
  final dio = Dio(BaseOptions(baseUrl: 'https://api.test'))
    ..httpClientAdapter = adapter;
  return VitalsApi(
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

Map<String, dynamic> vitalsJson() {
  return <String, dynamic>{
    'temperature': 38.2,
    'weight': null,
    'height': null,
    'pulse': 92,
    'systolic': null,
    'diastolic': null,
    'spo2': null,
    'glycemia': null,
    'respiratoryRate': null,
    'painScale': null,
  };
}
