import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/core/network/api_client.dart';
import 'package:joprelys_mobile/core/network/api_exception_mapper.dart';
import 'package:joprelys_mobile/features/dashboard/data/prescription_api.dart';
import 'package:joprelys_mobile/features/dashboard/domain/prescription.dart';

import 'support/queue_http_client_adapter.dart';

void main() {
  group('Prescription domain', () {
    test('draft allows an empty dosage but finalization does not', () {
      const item = PrescriptionItem(drugName: 'Inhalateur', dosage: '');

      expect(item.hasRequiredDraftFields, isTrue);
      expect(item.isCompleteForFinalization, isFalse);
      expect(item.toSaveJson(), containsPair('dosage', ''));
    });

    test('preserves the complete structured medication payload', () {
      final item = PrescriptionItem.fromJson(<String, dynamic>{
        'id': 'item-1',
        'drugName': 'Amoxicilline',
        'dosage': '500 mg',
        'posology': '1 gélule matin et soir',
        'duration': '7 jours',
        'quantity': '14 gélules',
        'instructions': 'Après le repas',
        'sortOrder': 2,
        'form': 'Gélule',
        'route': 'Orale',
        'frequency': '2 fois par jour',
        'substitutionAllowed': false,
      });

      expect(item.drugName, 'Amoxicilline');
      expect(item.form, 'Gélule');
      expect(item.route, 'Orale');
      expect(item.frequency, '2 fois par jour');
      expect(item.substitutionAllowed, isFalse);
      expect(item.isCompleteForFinalization, isTrue);
    });
  });

  group('PrescriptionApi', () {
    test('maps 204 to no prescription', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => ResponseBody.fromString('', 204),
      );
      final api = buildApi(adapter);

      expect(await api.getForConsultation('consultation-1'), isNull);
      expect(
        adapter.requests.single.path,
        '/api/consultations/consultation-1/prescription',
      );
    });

    test('loads and sorts a prescription from the canonical endpoint', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => jsonResponse(200, prescriptionJson()),
      );
      final api = buildApi(adapter);

      final prescription = await api.getForConsultation('consultation-1');

      expect(prescription?.status, 'DRAFT');
      expect(prescription?.items, hasLength(2));
      expect(prescription?.items.first.drugName, 'Inhalateur');
      expect(prescription?.canFinalize, isFalse);
    });

    test('saves a draft without inventing a missing dosage', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => jsonResponse(200, prescriptionJson()),
      );
      final api = buildApi(adapter);

      await api.saveDraft(
        consultationId: 'consultation-1',
        items: const [PrescriptionItem(drugName: 'Inhalateur')],
      );

      final request = adapter.requests.single;
      expect(request.method, 'POST');
      expect(
        request.path,
        '/api/consultations/consultation-1/prescription',
      );
      final data = Map<String, dynamic>.from(request.data as Map);
      final items = List<dynamic>.from(data['items'] as List);
      final item = Map<String, dynamic>.from(items.single as Map);
      expect(item, containsPair('drugName', 'Inhalateur'));
      expect(item, containsPair('dosage', ''));
      expect(item.containsKey('frequency'), isFalse);
    });

    test('uses dedicated lifecycle endpoints', () async {
      final adapter = QueueHttpClientAdapter(
        (_, _) => jsonResponse(200, prescriptionJson(status: 'ACTIVE')),
      );
      final api = buildApi(adapter);

      await api.finalize('prescription-1');
      expect(adapter.requests.single.method, 'POST');
      expect(
        adapter.requests.single.path,
        '/api/prescriptions/prescription-1/finalize',
      );

      adapter.enqueue((_, _) => jsonResponse(200, prescriptionJson(status: 'CANCELLED')));
      await api.cancel('prescription-1');
      expect(adapter.requests[1].method, 'PATCH');
      expect(
        adapter.requests[1].path,
        '/api/prescriptions/prescription-1/cancel',
      );

      adapter.enqueue((_, _) => jsonResponse(200, prescriptionJson(status: 'ACTIVE')));
      await api.transmit('prescription-1');
      expect(adapter.requests[2].method, 'POST');
      expect(
        adapter.requests[2].path,
        '/api/prescriptions/prescription-1/transmit',
      );
    });
  });
}

PrescriptionApi buildApi(QueueHttpClientAdapter adapter) {
  final dio = Dio(BaseOptions(baseUrl: 'https://api.test'))
    ..httpClientAdapter = adapter;
  return PrescriptionApi(
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

Map<String, dynamic> prescriptionJson({String status = 'DRAFT'}) {
  return <String, dynamic>{
    'id': 'prescription-1',
    'consultationId': 'consultation-1',
    'prescriptionNumber': 'ORD-20260808-000001',
    'status': status,
    'expiresAt': '2026-11-06T10:00:00Z',
    'transmissionStatus': null,
    'createdAt': '2026-08-08T10:00:00Z',
    'updatedAt': '2026-08-08T10:00:00Z',
    'items': [
      {
        'id': 'item-2',
        'drugName': 'Paracétamol',
        'dosage': '500 mg',
        'sortOrder': 2,
        'substitutionAllowed': true,
      },
      {
        'id': 'item-1',
        'drugName': 'Inhalateur',
        'dosage': '',
        'sortOrder': 1,
        'substitutionAllowed': true,
      },
    ],
  };
}
