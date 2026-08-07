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
          final item = <String, dynamic>{
            'drugName': _requiredText(source['drugName'], 'drugName'),
            'dosage': _optionalText(source['dosage'], 'dosage') ?? '',
          };
          _putOptionalText(item, source, 'posology');
          _putOptionalText(item, source, 'duration');
          _putOptionalText(item, source, 'quantity');
          _putOptionalText(item, source, 'instructions');
          _putOptionalText(item, source, 'form');
          _putOptionalText(item, source, 'route');
          _putOptionalText(item, source, 'frequency');
          if (source['substitutionAllowed'] case final bool value) {
            item['substitutionAllowed'] = value;
          }
          return item;
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

  void _putOptionalText(
    Map<String, dynamic> target,
    Map<String, dynamic> source,
    String field,
  ) {
    final value = _optionalText(source[field], field);
    if (value != null) {
      target[field] = value;
    }
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
