import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/core/network/api_client.dart';
import 'package:joprelys_mobile/core/network/api_exception_mapper.dart';
import 'package:joprelys_mobile/features/dashboard/data/lab_order_api.dart';
import 'package:joprelys_mobile/features/dashboard/domain/patient_record.dart';

import 'support/queue_http_client_adapter.dart';

void main() {
  group('Lab domain', () {
    test('maps canonical order lifecycle without inventing data', () {
      final order = PatientLabOrderSummary.fromJson(orderJson());

      expect(order.number, 'EXAM-REQ-20260808-000001');
      expect(order.examType, 'LABORATOIRE');
      expect(order.exams, ['NFS', 'CRP']);
      expect(order.items, hasLength(2));
      expect(order.items.first.id, 'item-nfs');
      expect(order.items.first.examName, 'NFS');
      expect(order.items.first.normalizedStatus, 'REQUESTED');
      expect(order.hasStructuredItems, isTrue);
      expect(order.normalizedStatus, 'REQUESTED');
      expect(order.progressIndex, 0);
      expect(order.nextOperationalStatus, 'SAMPLE_COLLECTED');
      expect(order.targetOrganizationId, 'org-lab-1');
    });

    test('keeps legacy exams readable when structured items are absent', () {
      final payload = orderJson()..remove('items');
      final order = PatientLabOrderSummary.fromJson(payload);

      expect(order.exams, ['NFS', 'CRP']);
      expect(order.items.map((item) => item.examName), ['NFS', 'CRP']);
      expect(order.items.every((item) => item.id.isEmpty), isTrue);
      expect(order.hasStructuredItems, isFalse);
    });

    test('maps result metadata and links it to one exam item', () {
      final result = PatientLabResultSummary.fromJson(resultJson());

      expect(result.resultNumber, 'EXAM-RES-20260808-000001');
      expect(result.examRequestNumber, 'EXAM-REQ-20260808-000001');
      expect(result.labOrderItemId, 'item-crp');
      expect(result.examName, 'CRP');
      expect(result.status, 'VALIDATED');
      expect(result.analyte, 'CRP');
      expect(result.value, '22');
      expect(result.unit, 'mg/L');
      expect(result.referenceRange, '< 5');
      expect(result.interpretation, 'ELEVE');
      expect(result.conclusion, 'Syndrome inflammatoire biologique');
      expect(result.validatorName, 'Dr Biologiste');
      expect(result.hasPdf, isTrue);
      expect(result.version, 2);
    });

    test('keeps payment statuses before the sampling step', () {
      final awaiting = PatientLabOrderSummary.fromJson(
        orderJson(status: 'AWAITING_PAYMENT'),
      );
      final paid = PatientLabOrderSummary.fromJson(orderJson(status: 'PAID'));

      expect(awaiting.progressIndex, 0);
      expect(awaiting.nextOperationalStatus, isNull);
      expect(paid.progressIndex, 0);
      expect(paid.nextOperationalStatus, 'SAMPLE_COLLECTED');
    });
  });

  group('LabOrderApi', () {
    test('creates a request with canonical exam type and priority', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => jsonResponse(201, orderJson()),
      );
      final api = buildApi(adapter);

      await api.createOrder(
        patientId: 'patient-1',
        examType: 'laboratoire',
        exams: const [' NFS ', 'CRP'],
        priority: 'normale',
        reason: 'Fatigue persistante',
      );

      final request = adapter.requests.single;
      expect(request.method, 'POST');
      expect(request.path, '/api/lab-orders');
      final data = Map<String, dynamic>.from(request.data as Map);
      expect(data['patientId'], 'patient-1');
      expect(data['examType'], 'LABORATOIRE');
      expect(data['priority'], 'NORMALE');
      expect(data['exams'], ['NFS', 'CRP']);
      expect(data['reason'], 'Fatigue persistante');
      expect(data.containsKey('targetOrganizationId'), isFalse);
      expect(data.containsKey('visitId'), isFalse);
    });

    test(
      'updates legacy order status through its dedicated endpoint',
      () async {
        final adapter = QueueHttpClientAdapter(
          (_, _) => jsonResponse(200, orderJson(status: 'SAMPLE_COLLECTED')),
        );
        final api = buildApi(adapter);

        final order = await api.updateStatus(
          orderId: 'order-1',
          status: 'sample_collected',
        );

        final request = adapter.requests.single;
        expect(request.method, 'PATCH');
        expect(request.path, '/api/lab-orders/order-1/status');
        expect(
          Map<String, dynamic>.from(request.data as Map),
          containsPair('status', 'SAMPLE_COLLECTED'),
        );
        expect(order.normalizedStatus, 'SAMPLE_COLLECTED');
      },
    );

    test('updates one exam without targeting the whole request', () async {
      final payload = orderJson();
      final items = List<Map<String, dynamic>>.from(payload['items'] as List);
      items[1] = <String, dynamic>{...items[1], 'status': 'SAMPLE_COLLECTED'};
      payload['items'] = items;
      final adapter = QueueHttpClientAdapter(
        (_, _) => jsonResponse(200, payload),
      );
      final api = buildApi(adapter);

      final order = await api.updateItemStatus(
        orderId: 'order-1',
        itemId: 'item-crp',
        status: 'sample_collected',
      );

      final request = adapter.requests.single;
      expect(request.method, 'PATCH');
      expect(request.path, '/api/lab-orders/order-1/items/item-crp/status');
      expect(
        Map<String, dynamic>.from(request.data as Map),
        containsPair('status', 'SAMPLE_COLLECTED'),
      );
      expect(order.items.first.normalizedStatus, 'REQUESTED');
      expect(order.items.last.normalizedStatus, 'SAMPLE_COLLECTED');
    });

    test('downloads result PDF bytes from the canonical endpoint', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => ResponseBody.fromBytes(
          const [0x25, 0x50, 0x44, 0x46, 0x2D],
          200,
          headers: {
            Headers.contentTypeHeader: ['application/pdf'],
          },
        ),
      );
      final api = buildApi(adapter);

      final bytes = await api.downloadResultPdf('result-1');

      expect(adapter.requests.single.method, 'GET');
      expect(
        adapter.requests.single.path,
        '/api/lab-orders/results/result-1/pdf',
      );
      expect(bytes.take(4), [0x25, 0x50, 0x44, 0x46]);
    });
  });
}

LabOrderApi buildApi(QueueHttpClientAdapter adapter) {
  final dio = Dio(BaseOptions(baseUrl: 'https://api.test'))
    ..httpClientAdapter = adapter;
  return LabOrderApi(
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

Map<String, dynamic> orderJson({String status = 'REQUESTED'}) {
  final itemStatus = switch (status) {
    'SAMPLE_COLLECTED' ||
    'IN_PROGRESS' ||
    'RESULT_AVAILABLE' ||
    'VALIDATED' ||
    'CANCELLED' => status,
    _ => 'REQUESTED',
  };
  return <String, dynamic>{
    'id': 'order-1',
    'examRequestNumber': 'EXAM-REQ-20260808-000001',
    'patientId': 'patient-1',
    'patientName': 'Patiente Test',
    'visitId': 'visit-1',
    'requesterPractitionerId': 'doctor-1',
    'requesterPractitionerName': 'Dr Test',
    'sourceOrganizationId': 'org-clinic-1',
    'targetOrganizationId': 'org-lab-1',
    'examType': 'LABORATOIRE',
    'exams': ['NFS', 'CRP'],
    'items': [
      {'id': 'item-nfs', 'examName': 'NFS', 'status': itemStatus},
      {'id': 'item-crp', 'examName': 'CRP', 'status': itemStatus},
    ],
    'reason': 'Fatigue persistante',
    'priority': 'NORMALE',
    'status': status,
    'createdAt': '2026-08-08T10:00:00Z',
  };
}

Map<String, dynamic> resultJson() {
  return <String, dynamic>{
    'id': 'result-1',
    'resultNumber': 'EXAM-RES-20260808-000001',
    'examRequestNumber': 'EXAM-REQ-20260808-000001',
    'labOrderItemId': 'item-crp',
    'examName': 'CRP',
    'patientId': 'patient-1',
    'validatorName': 'Dr Biologiste',
    'status': 'VALIDATED',
    'validatorUserId': 'bio-1',
    'conclusion': 'Syndrome inflammatoire biologique',
    'documentId': 'document-1',
    'version': 2,
    'parentResultId': 'result-v1',
    'analyteName': 'CRP',
    'value': '22',
    'unit': 'mg/L',
    'referenceRange': '< 5',
    'interpretation': 'ELEVE',
    'comment': 'À corréler au contexte clinique',
    'pdfFilePath': '/storage/lab-result.pdf',
    'sampleCollectedAt': '2026-08-08T08:00:00Z',
    'resultAt': '2026-08-08T09:30:00Z',
    'validatedAt': '2026-08-08T09:45:00Z',
    'createdAt': '2026-08-08T09:45:00Z',
  };
}
