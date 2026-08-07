import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_client_providers.dart';

abstract interface class ClinicalVoiceStructuredGateway {
  Future<void> savePrescriptionDraft({
    required String consultationId,
    required String prescriptionJson,
  });

  Future<void> saveLabOrders({
    required String patientId,
    required String visitId,
    required String labOrdersJson,
  });
}

final clinicalVoiceStructuredApiProvider =
    Provider<ClinicalVoiceStructuredGateway>((ref) {
      return ClinicalVoiceStructuredApi(ref.watch(apiClientProvider));
    });

final class ClinicalVoiceStructuredApi
    implements ClinicalVoiceStructuredGateway {
  const ClinicalVoiceStructuredApi(this._client);

  final ApiClient _client;

  @override
  Future<void> savePrescriptionDraft({
    required String consultationId,
    required String prescriptionJson,
  }) async {
    final items = _decodePrescriptionItems(prescriptionJson);
    if (items.isEmpty) return;
    await _client.post<void>(
      '/api/consultations/$consultationId/prescription',
      data: <String, dynamic>{'items': items},
    );
  }

  @override
  Future<void> saveLabOrders({
    required String patientId,
    required String visitId,
    required String labOrdersJson,
  }) async {
    final exams = _decodeLabOrders(labOrdersJson);
    if (exams.isEmpty) return;
    await _client.post<void>(
      '/api/lab-orders',
      data: <String, dynamic>{
        'patientId': patientId,
        'visitId': visitId,
        'examType': 'AUTRE',
        'exams': exams,
      },
    );
  }

  List<Map<String, dynamic>> _decodePrescriptionItems(String raw) {
    final decoded = jsonDecode(raw);
    if (decoded is! List) {
      throw const FormatException('Invalid clinical voice prescription');
    }

    return decoded
        .map<Map<String, dynamic>>((value) {
          if (value is! Map) {
            throw const FormatException(
              'Invalid clinical voice prescription item',
            );
          }
          final source = Map<String, dynamic>.from(value);
          final drugName = _requiredText(source['drugName'], 'drugName');
          final dosage = _optionalText(source['dosage'], 'dosage') ?? '';
          return <String, dynamic>{
            'drugName': drugName,
            'dosage': dosage,
            if (_optionalText(source['posology'], 'posology') case final value?)
              'posology': value,
            if (_optionalText(source['duration'], 'duration') case final value?)
              'duration': value,
            if (_optionalText(source['quantity'], 'quantity') case final value?)
              'quantity': value,
            if (_optionalText(source['instructions'], 'instructions')
                case final value?)
              'instructions': value,
            if (_optionalText(source['form'], 'form') case final value?)
              'form': value,
            if (_optionalText(source['route'], 'route') case final value?)
              'route': value,
            if (_optionalText(source['frequency'], 'frequency')
                case final value?)
              'frequency': value,
            if (source['substitutionAllowed'] case final bool value)
              'substitutionAllowed': value,
          };
        })
        .toList(growable: false);
  }

  List<String> _decodeLabOrders(String raw) {
    final decoded = jsonDecode(raw);
    if (decoded is! List) {
      throw const FormatException('Invalid clinical voice lab orders');
    }
    return decoded
        .map<String>((value) {
          if (value is! String || value.trim().isEmpty) {
            throw const FormatException('Invalid clinical voice lab order');
          }
          return value.trim();
        })
        .toList(growable: false);
  }

  String _requiredText(Object? value, String field) {
    final text = _optionalText(value, field);
    if (text == null) {
      throw FormatException('Missing clinical voice field: $field');
    }
    return text;
  }

  String? _optionalText(Object? value, String field) {
    if (value == null) return null;
    if (value is! String) {
      throw FormatException('Invalid clinical voice field: $field');
    }
    final normalized = value.trim();
    return normalized.isEmpty ? null : normalized;
  }
}
