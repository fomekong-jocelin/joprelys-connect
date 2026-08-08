import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_client_providers.dart';
import '../domain/prescription.dart';

abstract interface class PrescriptionGateway {
  Future<Prescription?> getForConsultation(String consultationId);

  Future<Prescription> saveDraft({
    required String consultationId,
    required List<PrescriptionItem> items,
  });

  Future<Prescription> finalize(String prescriptionId);
  Future<Prescription> cancel(String prescriptionId);
  Future<Prescription> transmit(String prescriptionId);
}

final prescriptionApiProvider = Provider<PrescriptionGateway>((ref) {
  return PrescriptionApi(ref.watch(apiClientProvider));
});

final class PrescriptionApi implements PrescriptionGateway {
  const PrescriptionApi(this._client);

  final ApiClient _client;

  @override
  Future<Prescription?> getForConsultation(String consultationId) async {
    final response = await _client.get<dynamic>(
      '/api/consultations/$consultationId/prescription',
    );
    final data = response.data;
    if (response.statusCode == 204 || data == null) return null;
    if (data is! Map) {
      throw const FormatException('Invalid prescription response');
    }
    return Prescription.fromJson(Map<String, dynamic>.from(data));
  }

  @override
  Future<Prescription> saveDraft({
    required String consultationId,
    required List<PrescriptionItem> items,
  }) async {
    final response = await _client.post<dynamic>(
      '/api/consultations/$consultationId/prescription',
      data: <String, dynamic>{
        'items': items.map((item) => item.toSaveJson()).toList(growable: false),
      },
    );
    return _prescriptionFromResponse(response.data);
  }

  @override
  Future<Prescription> finalize(String prescriptionId) async {
    final response = await _client.post<dynamic>(
      '/api/prescriptions/$prescriptionId/finalize',
    );
    return _prescriptionFromResponse(response.data);
  }

  @override
  Future<Prescription> cancel(String prescriptionId) async {
    final response = await _client.patch<dynamic>(
      '/api/prescriptions/$prescriptionId/cancel',
    );
    return _prescriptionFromResponse(response.data);
  }

  @override
  Future<Prescription> transmit(String prescriptionId) async {
    final response = await _client.post<dynamic>(
      '/api/prescriptions/$prescriptionId/transmit',
    );
    return _prescriptionFromResponse(response.data);
  }

  Prescription _prescriptionFromResponse(Object? data) {
    if (data is! Map) {
      throw const FormatException('Invalid prescription response');
    }
    return Prescription.fromJson(Map<String, dynamic>.from(data));
  }
}
