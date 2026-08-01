import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/core/network/api_client.dart';
import 'package:joprelys_mobile/core/network/api_exception.dart';
import 'package:joprelys_mobile/core/network/api_exception_mapper.dart';
import 'package:joprelys_mobile/features/dashboard/data/consultation_api.dart';
import 'package:joprelys_mobile/features/dashboard/domain/consultation_note.dart';

import 'support/queue_http_client_adapter.dart';

void main() {
  group('ConsultationNote', () {
    test('serializes and deserializes the canonical backend fields', () {
      final json = consultationJson();

      final note = ConsultationNote.fromJson(json);

      expect(note.symptoms, 'Douleur thoracique depuis deux heures');
      expect(note.clinicalExam, 'Auscultation normale');
      expect(note.diagnosis, 'Reflux gastro-œsophagien');
      expect(note.conclusion, 'Absence de signe de gravité immédiat');
      expect(note.advice, 'Consulter en urgence si aggravation');
      expect(note.followUp, 'Contrôle dans 48 heures');
      expect(note.isEmpty, isFalse);
      expect(
        note.toJson(),
        containsPair('diagnosis', 'Reflux gastro-œsophagien'),
      );
      expect(note.toJson().containsKey('suspectedDiagnosis'), isFalse);
      expect(note.toJson().containsKey('finalDiagnosis'), isFalse);
    });

    test('detects empty notes correctly', () {
      expect(const ConsultationNote().isEmpty, isTrue);
      expect(
        const ConsultationNote(symptoms: 'Fièvre isolée').isEmpty,
        isFalse,
      );
    });
  });

  group('ConsultationApi', () {
    test('loads a consultation from the canonical endpoint', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => jsonResponse(200, consultationJson()),
      );
      final api = buildConsultationApi(adapter);

      final note = await api.getConsultationNote('visit-123');

      expect(
        adapter.requests.single.path,
        '/api/visits/visit-123/consultation',
      );
      expect(note?.diagnosis, 'Reflux gastro-œsophagien');
    });

    test('maps 204 to an absent consultation', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => ResponseBody.fromString('', 204),
      );
      final api = buildConsultationApi(adapter);

      expect(await api.getConsultationNote('visit-123'), isNull);
    });

    test('saves the detailed SOAP fields on the canonical endpoint', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => jsonResponse(200, consultationJson()),
      );
      final api = buildConsultationApi(adapter);
      final note = ConsultationNote.fromJson(consultationJson());

      await api.saveConsultationNote('visit-123', note);

      final request = adapter.requests.single;
      final requestData = Map<String, dynamic>.from(request.data as Map);
      expect(request.method, 'POST');
      expect(request.path, '/api/visits/visit-123/consultation');
      expect(requestData, containsPair('symptoms', note.symptoms));
      expect(requestData, containsPair('clinicalExam', note.clinicalExam));
      expect(requestData, containsPair('diagnosis', note.diagnosis));
      expect(requestData.containsKey('suspectedDiagnosis'), isFalse);
      expect(requestData.containsKey('finalDiagnosis'), isFalse);
      expect(requestData.containsKey('subjective'), isFalse);
      expect(requestData.containsKey('plan'), isFalse);
    });

    test('does not hide a true 404 as an absent consultation', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => jsonResponse(404, {'detail': 'Visite introuvable.'}),
      );
      final api = buildConsultationApi(adapter);

      await expectLater(
        api.getConsultationNote('missing-visit'),
        throwsA(
          isA<ApiException>().having(
            (error) => error.statusCode,
            'statusCode',
            404,
          ),
        ),
      );
    });
  });
}

ConsultationApi buildConsultationApi(QueueHttpClientAdapter adapter) {
  final dio = Dio(BaseOptions(baseUrl: 'https://api.test'))
    ..httpClientAdapter = adapter;
  return ConsultationApi(
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

Map<String, dynamic> consultationJson() {
  return <String, dynamic>{
    'symptoms': 'Douleur thoracique depuis deux heures',
    'clinicalExam': 'Auscultation normale',
    'diagnosis': 'Reflux gastro-œsophagien',
    'conclusion': 'Absence de signe de gravité immédiat',
    'advice': 'Consulter en urgence si aggravation',
    'followUp': 'Contrôle dans 48 heures',
    'updatedAt': '2026-07-30T22:00:00.000Z',
  };
}
