import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_client_providers.dart';
import '../../../core/network/api_exception.dart';
import '../../auth/domain/effective_access.dart';
import '../domain/patient_history.dart';
import '../domain/patient_record.dart';

abstract interface class PatientRecordGateway {
  Future<PatientRecordBundle> getRecord({
    required PatientRecordTarget target,
    required EffectiveAccess access,
  });

  Future<void> activateEmergencyAccess({
    required String patientId,
    required String reason,
  });
}

final patientRecordApiProvider = Provider<PatientRecordGateway>((ref) {
  return PatientRecordApi(ref.watch(apiClientProvider));
});

final class PatientRecordApi implements PatientRecordGateway {
  const PatientRecordApi(this._client);

  final ApiClient _client;

  @override
  Future<PatientRecordBundle> getRecord({
    required PatientRecordTarget target,
    required EffectiveAccess access,
  }) async {
    final patientPayload = await _getMap('/api/patients/${target.patientId}');
    final identity = PatientIdentity.fromJson(patientPayload, fallback: target);

    final canReadClinical = access.hasPermission('CLINICAL_READ');
    final canReadLabs = access.hasPermission('LAB_ORDER_READ');
    final canReadHospitalizations = access.hasPermission(
      'HOSPITALIZATION_READ',
    );
    final canReadAudit = access.hasPermission('AUDIT_READ');

    final results = await Future.wait<List<dynamic>>([
      if (canReadClinical)
        _safeList('/api/patients/${target.patientId}/allergies')
      else
        Future.value(const <dynamic>[]),
      if (canReadClinical)
        _safeList('/api/patients/${target.patientId}/medical-history')
      else
        Future.value(const <dynamic>[]),
      if (canReadClinical)
        _safeList('/api/patients/${target.patientId}/consultations')
      else
        Future.value(const <dynamic>[]),
      if (canReadClinical)
        _safeList('/api/patients/${target.patientId}/vaccinations')
      else
        Future.value(const <dynamic>[]),
      if (canReadLabs)
        _safeList('/api/lab-orders/patient/${target.patientId}')
      else
        Future.value(const <dynamic>[]),
      if (canReadLabs)
        _safeList('/api/lab-orders/patient/${target.patientId}/results')
      else
        Future.value(const <dynamic>[]),
      if (canReadHospitalizations)
        _safeList('/api/hospitalizations/patient/${target.patientId}')
      else
        Future.value(const <dynamic>[]),
      if (canReadAudit)
        _safeList('/api/audit/patients/${target.patientId}')
      else
        Future.value(const <dynamic>[]),
    ]);

    final allergies = _maps(results[0])
        .map(PatientAllergy.fromJson)
        .where((item) => item.allergen.isNotEmpty)
        .toList(growable: false);
    final antecedents = _maps(results[1])
        .map(MedicalAntecedent.fromJson)
        .where((item) => item.description.isNotEmpty)
        .toList(growable: false);
    final visits = _maps(results[2]).map(_pastVisit).toList(growable: false)
      ..sort((left, right) => right.date.compareTo(left.date));

    return PatientRecordBundle(
      identity: identity,
      history: PatientMedicalHistory(
        patientId: target.patientId,
        patientName: identity.fullName,
        patientDpu: identity.globalPatientNumber,
        antecedents: antecedents,
        allergies: allergies,
        pastVisits: visits,
      ),
      vaccinations: _maps(results[3])
          .map(PatientVaccinationSummary.fromJson)
          .where((item) => item.label.isNotEmpty)
          .toList(growable: false),
      labOrders: _maps(
        results[4],
      ).map(PatientLabOrderSummary.fromJson).toList(growable: false),
      labResults: _maps(
        results[5],
      ).map(PatientLabResultSummary.fromJson).toList(growable: false),
      hospitalizations: _maps(
        results[6],
      ).map(PatientHospitalizationSummary.fromJson).toList(growable: false),
      auditLogs: _maps(
        results[7],
      ).map(PatientAuditSummary.fromJson).toList(growable: false),
    );
  }

  @override
  Future<void> activateEmergencyAccess({
    required String patientId,
    required String reason,
  }) async {
    await _client.post<void>(
      '/api/patients/$patientId/emergency-access',
      data: {'reason': reason.trim()},
    );
  }

  Future<Map<String, dynamic>> _getMap(String path) async {
    final response = await _client.get<dynamic>(path);
    final payload = response.data;
    if (payload is Map<String, dynamic>) {
      return payload;
    }
    if (payload is Map) {
      return payload.map((key, value) => MapEntry(key.toString(), value));
    }
    throw const ApiException(
      kind: ApiFailureKind.malformedResponse,
      code: 'PATIENT_RECORD_INVALID',
      message: 'PATIENT_RECORD_INVALID',
    );
  }

  Future<List<dynamic>> _safeList(String path) async {
    try {
      final response = await _client.get<dynamic>(path);
      return response.data is List
          ? List<dynamic>.from(response.data as List)
          : const <dynamic>[];
    } on ApiException catch (error) {
      if (error.statusCode == 404) {
        return const <dynamic>[];
      }
      rethrow;
    }
  }

  Iterable<Map<String, dynamic>> _maps(List<dynamic> values) sync* {
    for (final value in values) {
      if (value is Map<String, dynamic>) {
        yield value;
      } else if (value is Map) {
        yield value.map((key, item) => MapEntry(key.toString(), item));
      }
    }
  }

  PastVisitSummary _pastVisit(Map<String, dynamic> map) {
    final vitals = map['vitals'] is Map
        ? Map<String, dynamic>.from(map['vitals'] as Map)
        : const <String, dynamic>{};
    final rawDate = map['createdAt'] ?? map['date'] ?? map['consultedAt'];
    return PastVisitSummary(
      id: (map['visitId'] ?? map['id'] ?? '').toString(),
      visitNumber: (map['visitNumber'] ?? '').toString(),
      date:
          DateTime.tryParse(rawDate?.toString() ?? '')?.toLocal() ??
          DateTime.fromMillisecondsSinceEpoch(0),
      practitionerName: (map['doctorName'] ?? map['practitionerName'] ?? '')
          .toString(),
      chiefComplaint:
          (map['diagnosis'] ?? map['symptoms'] ?? map['reason'] ?? '')
              .toString(),
      temperature: (vitals['temperature'] as num?)?.toDouble(),
      systolic: (vitals['systolic'] as num?)?.toInt(),
      diastolic: (vitals['diastolic'] as num?)?.toInt(),
      pulse: (vitals['pulse'] as num?)?.toInt(),
    );
  }
}
