import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/core/network/api_client.dart';
import 'package:joprelys_mobile/core/network/api_exception_mapper.dart';
import 'package:joprelys_mobile/features/dashboard/application/clinical_voice_accepted_result_persistence.dart';
import 'package:joprelys_mobile/features/dashboard/data/clinical_voice_structured_api.dart';
import 'package:joprelys_mobile/features/dashboard/data/consultation_api.dart';
import 'package:joprelys_mobile/features/dashboard/domain/active_visit.dart';
import 'package:joprelys_mobile/features/dashboard/domain/consultation_note.dart';

import 'support/queue_http_client_adapter.dart';

void main() {
  group('ClinicalVoiceStructuredApi', () {
    test(
      'keeps a medication draft without inventing a missing dosage',
      () async {
        final adapter = QueueHttpClientAdapter(
          (_, _) => jsonResponse(200, const <String, dynamic>{}),
        );
        final api = buildStructuredApi(adapter);

        await api.savePrescriptionDraft(
          consultationId: 'consultation-1',
          prescriptionJson: '[{"drugName":"inhalateur"}]',
        );

        final request = adapter.requests.single;
        expect(request.path, '/api/consultations/consultation-1/prescription');
        final payload = Map<String, dynamic>.from(request.data as Map);
        final items = List<Map<String, dynamic>>.from(
          (payload['items'] as List).map(
            (item) => Map<String, dynamic>.from(item as Map),
          ),
        );
        expect(items, hasLength(1));
        expect(items.single['drugName'], 'inhalateur');
        expect(items.single['dosage'], '');
      },
    );

    test('preserves explicitly extracted prescription attributes', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => jsonResponse(200, const <String, dynamic>{}),
      );
      final api = buildStructuredApi(adapter);

      await api.savePrescriptionDraft(
        consultationId: 'consultation-1',
        prescriptionJson: jsonEncode([
          {
            'drugName': 'Traitement X',
            'dosage': '2 bouffées',
            'frequency': 'matin et soir',
            'duration': '7 jours',
          },
        ]),
      );

      final payload = Map<String, dynamic>.from(
        adapter.requests.single.data as Map,
      );
      final item = Map<String, dynamic>.from(
        (payload['items'] as List).single as Map,
      );
      expect(item['drugName'], 'Traitement X');
      expect(item['dosage'], '2 bouffées');
      expect(item['frequency'], 'matin et soir');
      expect(item['duration'], '7 jours');
    });

    test(
      'creates a neutral exam request without inferring a specialty',
      () async {
        final adapter = QueueHttpClientAdapter(
          (_, _) => jsonResponse(201, const <String, dynamic>{}),
        );
        final api = buildStructuredApi(adapter);

        await api.saveLabOrders(
          patientId: 'patient-1',
          visitId: 'visit-1',
          labOrdersJson: jsonEncode([
            'numération complète',
            'bilan inflammatoire',
            'radiographie du thorax',
          ]),
        );

        final request = adapter.requests.single;
        expect(request.path, '/api/lab-orders');
        final payload = Map<String, dynamic>.from(request.data as Map);
        expect(payload['patientId'], 'patient-1');
        expect(payload['visitId'], 'visit-1');
        expect(payload['examType'], 'AUTRE');
        expect(
          payload['exams'],
          equals([
            'numération complète',
            'bilan inflammatoire',
            'radiographie du thorax',
          ]),
        );
      },
    );

    test('rejects malformed structured JSON before network I/O', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => jsonResponse(200, const <String, dynamic>{}),
      );
      final api = buildStructuredApi(adapter);

      await expectLater(
        api.savePrescriptionDraft(
          consultationId: 'consultation-1',
          prescriptionJson: '{"drugName":"inhalateur"}',
        ),
        throwsA(isA<FormatException>()),
      );
      expect(adapter.requests, isEmpty);
    });
  });

  group('ClinicalVoiceAcceptedResultPersistence', () {
    test('saves SOAP and extras before consuming the voice source', () async {
      final consultation = _FakeConsultationGateway();
      final structured = _FakeStructuredGateway();
      final persistence = ClinicalVoiceAcceptedResultPersistence(
        consultationGateway: consultation,
        structuredGateway: structured,
      );

      await persistence.save(
        visit: visit,
        note: const ConsultationNote(symptoms: 'Toux sèche'),
        prescriptionJson: '[{"drugName":"inhalateur"}]',
        labOrdersJson: '["radiographie du thorax"]',
      );

      expect(consultation.saveCalls, 1);
      expect(consultation.consumeCalls, 1);
      expect(structured.prescriptionCalls, 1);
      expect(structured.labOrderCalls, 1);
      expect(structured.lastConsultationId, 'consultation-1');
      expect(structured.lastPatientId, 'patient-1');
      expect(structured.lastVisitId, 'visit-1');
    });

    test(
      'keeps voice source and avoids duplicate prescription after lab failure',
      () async {
        final consultation = _FakeConsultationGateway();
        final structured = _FakeStructuredGateway(failFirstLabOrder: true);
        final persistence = ClinicalVoiceAcceptedResultPersistence(
          consultationGateway: consultation,
          structuredGateway: structured,
        );

        Future<void> save() async {
          await persistence.save(
            visit: visit,
            note: const ConsultationNote(symptoms: 'Toux sèche'),
            prescriptionJson: '[{"drugName":"inhalateur"}]',
            labOrdersJson: '["radiographie du thorax"]',
          );
        }

        await expectLater(save(), throwsStateError);
        expect(structured.prescriptionCalls, 1);
        expect(structured.labOrderCalls, 1);
        expect(consultation.consumeCalls, 0);

        await save();
        expect(consultation.saveCalls, 2);
        expect(structured.prescriptionCalls, 1);
        expect(structured.labOrderCalls, 2);
        expect(consultation.consumeCalls, 1);
      },
    );

    test('consumes voice source after SOAP-only accepted result', () async {
      final consultation = _FakeConsultationGateway();
      final structured = _FakeStructuredGateway();
      final persistence = ClinicalVoiceAcceptedResultPersistence(
        consultationGateway: consultation,
        structuredGateway: structured,
      );

      await persistence.save(
        visit: visit,
        note: const ConsultationNote(symptoms: 'Toux sèche'),
      );

      expect(consultation.saveCalls, 1);
      expect(consultation.consumeCalls, 1);
      expect(structured.prescriptionCalls, 0);
      expect(structured.labOrderCalls, 0);
    });
  });
}

final visit = ActiveVisit(
  id: 'visit-1',
  visitNumber: 'VIS-1',
  patientId: 'patient-1',
  patientName: 'Patient Test',
  patientDpu: 'DPU-1',
  reason: 'Toux',
  orientation: 'Médecine générale',
  status: 'ACTIVE',
  createdAt: DateTime.utc(2026, 8, 7),
);

ClinicalVoiceStructuredApi buildStructuredApi(QueueHttpClientAdapter adapter) {
  final dio = Dio(BaseOptions(baseUrl: 'https://api.test'))
    ..httpClientAdapter = adapter;
  return ClinicalVoiceStructuredApi(
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

final class _FakeConsultationGateway implements ConsultationGateway {
  int saveCalls = 0;
  int consumeCalls = 0;

  @override
  Future<ConsultationNote?> getConsultationNote(String visitId) async => null;

  @override
  Future<SavedConsultationNote> saveConsultationNote(
    String visitId,
    ConsultationNote note,
  ) async {
    saveCalls += 1;
    return SavedConsultationNote(consultationId: 'consultation-1', note: note);
  }

  @override
  Future<void> consumeVoiceWorkingSet(String visitId) async {
    consumeCalls += 1;
  }
}

final class _FakeStructuredGateway implements ClinicalVoiceStructuredGateway {
  _FakeStructuredGateway({this.failFirstLabOrder = false});

  final bool failFirstLabOrder;
  int prescriptionCalls = 0;
  int labOrderCalls = 0;
  String? lastConsultationId;
  String? lastPatientId;
  String? lastVisitId;

  @override
  Future<void> savePrescriptionDraft({
    required String consultationId,
    required String prescriptionJson,
  }) async {
    prescriptionCalls += 1;
    lastConsultationId = consultationId;
  }

  @override
  Future<void> saveLabOrders({
    required String patientId,
    required String visitId,
    required String labOrdersJson,
  }) async {
    labOrderCalls += 1;
    lastPatientId = patientId;
    lastVisitId = visitId;
    if (failFirstLabOrder && labOrderCalls == 1) {
      throw StateError('temporary lab failure');
    }
  }
}
