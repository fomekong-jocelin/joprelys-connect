import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_client_providers.dart';
import '../../../core/network/api_request_policy.dart';
import '../domain/patient_record.dart';

abstract interface class LabOrderGateway {
  Future<PatientLabOrderSummary> createOrder({
    required String patientId,
    required String examType,
    required List<String> exams,
    required String priority,
    String? visitId,
    String? targetOrganizationId,
    String? reason,
  });

  Future<PatientLabOrderSummary> updateStatus({
    required String orderId,
    required String status,
  });

  Future<List<int>> downloadResultPdf(String resultId);
}

final labOrderApiProvider = Provider<LabOrderGateway>((ref) {
  return LabOrderApi(ref.watch(apiClientProvider));
});

final class LabOrderApi implements LabOrderGateway {
  const LabOrderApi(this._client);

  final ApiClient _client;

  @override
  Future<PatientLabOrderSummary> createOrder({
    required String patientId,
    required String examType,
    required List<String> exams,
    required String priority,
    String? visitId,
    String? targetOrganizationId,
    String? reason,
  }) async {
    final response = await _client.post<dynamic>(
      '/api/lab-orders',
      data: <String, dynamic>{
        'patientId': patientId,
        if (visitId?.trim().isNotEmpty == true) 'visitId': visitId!.trim(),
        if (targetOrganizationId?.trim().isNotEmpty == true)
          'targetOrganizationId': targetOrganizationId!.trim(),
        'examType': examType.trim().toUpperCase(),
        'exams': exams
            .map((exam) => exam.trim())
            .where((exam) => exam.isNotEmpty)
            .toList(growable: false),
        if (reason?.trim().isNotEmpty == true) 'reason': reason!.trim(),
        'priority': priority.trim().toUpperCase(),
      },
    );
    return _orderFromResponse(response.data);
  }

  @override
  Future<PatientLabOrderSummary> updateStatus({
    required String orderId,
    required String status,
  }) async {
    final response = await _client.patch<dynamic>(
      '/api/lab-orders/$orderId/status',
      data: <String, dynamic>{'status': status.trim().toUpperCase()},
    );
    return _orderFromResponse(response.data);
  }

  @override
  Future<List<int>> downloadResultPdf(String resultId) async {
    final response = await _client.request<List<int>>(
      '/api/lab-orders/results/$resultId/pdf',
      method: 'GET',
      policy: const ApiRequestPolicy.protectedRead(),
      responseType: ResponseType.bytes,
    );
    final bytes = response.data;
    if (bytes == null || bytes.isEmpty) {
      throw const FormatException('Invalid lab result PDF response');
    }
    return List<int>.unmodifiable(bytes);
  }

  PatientLabOrderSummary _orderFromResponse(Object? data) {
    if (data is! Map) {
      throw const FormatException('Invalid lab order response');
    }
    return PatientLabOrderSummary.fromJson(Map<String, dynamic>.from(data));
  }
}
