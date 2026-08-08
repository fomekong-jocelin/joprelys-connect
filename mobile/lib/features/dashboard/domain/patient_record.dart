import 'package:flutter/foundation.dart';

import 'active_visit.dart';
import 'patient_history.dart';

@immutable
final class PatientRecordTarget {
  const PatientRecordTarget({
    required this.patientId,
    required this.patientName,
    required this.patientDpu,
    this.activeVisit,
  });

  final String patientId;
  final String patientName;
  final String patientDpu;
  final ActiveVisit? activeVisit;
}

@immutable
final class PatientIdentity {
  const PatientIdentity({
    required this.id,
    required this.fullName,
    required this.globalPatientNumber,
    required this.localPatientNumber,
    required this.status,
    this.gender,
    this.birthDate,
    this.phone,
    this.city,
    this.bloodGroup,
    this.emergencyAccessActive = false,
  });

  final String id;
  final String fullName;
  final String globalPatientNumber;
  final String localPatientNumber;
  final String status;
  final String? gender;
  final DateTime? birthDate;
  final String? phone;
  final String? city;
  final String? bloodGroup;
  final bool emergencyAccessActive;

  factory PatientIdentity.fromJson(
    Map<String, dynamic> json, {
    required PatientRecordTarget fallback,
  }) {
    final fullName = _firstString(json, const ['fullName', 'displayName']);
    final global = _firstString(json, const [
      'globalPatientNumber',
      'dpu',
      'patientDpu',
    ]);
    final local = _firstString(json, const [
      'localPatientNumber',
      'temporaryPatientNumber',
    ]);

    return PatientIdentity(
      id: _firstString(json, const ['id']) ?? fallback.patientId,
      fullName: fullName ?? fallback.patientName,
      globalPatientNumber: global ?? fallback.patientDpu,
      localPatientNumber: local ?? '',
      status: _firstString(json, const ['status']) ?? 'ACTIVE',
      gender: _firstString(json, const ['gender', 'sex']),
      birthDate: _date(json['birthDate']),
      phone: _firstString(json, const ['phone', 'phoneNumber']),
      city: _firstString(json, const ['city', 'addressCity']),
      bloodGroup: _firstString(json, const ['bloodGroup']),
      emergencyAccessActive: json['emergencyAccessActive'] == true,
    );
  }
}

@immutable
final class PatientVaccinationSummary {
  const PatientVaccinationSummary({
    required this.label,
    this.date,
    this.status,
  });

  final String label;
  final DateTime? date;
  final String? status;

  factory PatientVaccinationSummary.fromJson(Map<String, dynamic> json) {
    return PatientVaccinationSummary(
      label: _firstString(json, const ['vaccineName', 'vaccine', 'name']) ?? '',
      date: _date(json['administrationDate'] ?? json['date']),
      status: _firstString(json, const ['status']),
    );
  }
}

@immutable
final class PatientLabOrderItemSummary {
  const PatientLabOrderItemSummary({
    required this.id,
    required this.examName,
    required this.status,
    this.sampleCollectedAt,
    this.resultAt,
    this.validatedAt,
  });

  final String id;
  final String examName;
  final String status;
  final DateTime? sampleCollectedAt;
  final DateTime? resultAt;
  final DateTime? validatedAt;

  factory PatientLabOrderItemSummary.fromJson(Map<String, dynamic> json) {
    return PatientLabOrderItemSummary(
      id: _firstString(json, const ['id']) ?? '',
      examName: _firstString(json, const ['examName', 'name']) ?? '',
      status: _firstString(json, const ['status']) ?? 'REQUESTED',
      sampleCollectedAt: _date(json['sampleCollectedAt']),
      resultAt: _date(json['resultAt']),
      validatedAt: _date(json['validatedAt']),
    );
  }

  String get normalizedStatus => status.trim().toUpperCase();
  bool get isCancelled => normalizedStatus == 'CANCELLED';
  bool get isValidated => normalizedStatus == 'VALIDATED';
  bool get isTerminal => isCancelled || isValidated;

  int get progressIndex {
    return switch (normalizedStatus) {
      'SAMPLE_COLLECTED' => 1,
      'IN_PROGRESS' => 2,
      'RESULT_AVAILABLE' => 3,
      'VALIDATED' => 4,
      'CANCELLED' => -1,
      _ => 0,
    };
  }

  String? get nextOperationalStatus {
    return switch (normalizedStatus) {
      'REQUESTED' || 'PAID' => 'SAMPLE_COLLECTED',
      'SAMPLE_COLLECTED' => 'IN_PROGRESS',
      _ => null,
    };
  }
}

@immutable
final class PatientLabOrderSummary {
  const PatientLabOrderSummary({
    required this.id,
    required this.number,
    required this.status,
    required this.exams,
    this.items = const [],
    this.patientId,
    this.visitId,
    this.requesterPractitionerId,
    this.sourceOrganizationId,
    this.targetOrganizationId,
    this.createdAt,
    this.priority,
    this.practitioner,
    this.examType,
    this.reason,
  });

  final String id;
  final String number;
  final String status;
  final List<String> exams;
  final List<PatientLabOrderItemSummary> items;
  final String? patientId;
  final String? visitId;
  final String? requesterPractitionerId;
  final String? sourceOrganizationId;
  final String? targetOrganizationId;
  final DateTime? createdAt;
  final String? priority;
  final String? practitioner;
  final String? examType;
  final String? reason;

  factory PatientLabOrderSummary.fromJson(Map<String, dynamic> json) {
    final rawExams = json['exams'];
    final exams = rawExams is List
        ? rawExams
              .map((item) => item.toString().trim())
              .where((item) => item.isNotEmpty)
              .toList(growable: false)
        : const <String>[];
    final status = _firstString(json, const ['status']) ?? '';
    final rawItems = json['items'];
    final structuredItems = rawItems is List
        ? rawItems
              .whereType<Map>()
              .map(
                (item) => PatientLabOrderItemSummary.fromJson(
                  Map<String, dynamic>.from(item),
                ),
              )
              .where((item) => item.examName.isNotEmpty)
              .toList(growable: false)
        : const <PatientLabOrderItemSummary>[];
    final items = structuredItems.isNotEmpty
        ? structuredItems
        : exams
              .map(
                (exam) => PatientLabOrderItemSummary(
                  id: '',
                  examName: exam,
                  status: status,
                ),
              )
              .toList(growable: false);

    return PatientLabOrderSummary(
      id: _firstString(json, const ['id']) ?? '',
      number:
          _firstString(json, const ['examRequestNumber', 'orderNumber']) ?? '',
      patientId: _firstString(json, const ['patientId']),
      visitId: _firstString(json, const ['visitId']),
      requesterPractitionerId: _firstString(json, const [
        'requesterPractitionerId',
      ]),
      sourceOrganizationId: _firstString(json, const ['sourceOrganizationId']),
      targetOrganizationId: _firstString(json, const ['targetOrganizationId']),
      status: status,
      exams: exams.isNotEmpty
          ? exams
          : items.map((item) => item.examName).toList(growable: false),
      items: items,
      createdAt: _date(json['createdAt']),
      priority: _firstString(json, const ['priority']),
      practitioner: _firstString(json, const [
        'requesterPractitionerName',
        'practitionerName',
      ]),
      examType: _firstString(json, const ['examType', 'type']),
      reason: _firstString(json, const ['reason', 'clinicalReason']),
    );
  }

  String get normalizedStatus => status.trim().toUpperCase();
  bool get isCancelled => normalizedStatus == 'CANCELLED';
  bool get isValidated => normalizedStatus == 'VALIDATED';
  bool get isTerminal => isCancelled || isValidated;
  bool get isActive => !isTerminal;
  bool get hasStructuredItems => items.any((item) => item.id.isNotEmpty);

  int get progressIndex {
    return switch (normalizedStatus) {
      'SAMPLE_COLLECTED' => 1,
      'IN_PROGRESS' => 2,
      'RESULT_AVAILABLE' => 3,
      'VALIDATED' => 4,
      'CANCELLED' => -1,
      _ => 0,
    };
  }

  String? get nextOperationalStatus {
    return switch (normalizedStatus) {
      'REQUESTED' || 'PAID' => 'SAMPLE_COLLECTED',
      'SAMPLE_COLLECTED' => 'IN_PROGRESS',
      _ => null,
    };
  }
}

@immutable
final class PatientLabResultSummary {
  const PatientLabResultSummary({
    required this.id,
    required this.analyte,
    required this.value,
    this.resultNumber = '',
    this.examRequestNumber = '',
    this.labOrderItemId,
    this.examName,
    this.status = '',
    this.unit,
    this.interpretation,
    this.referenceRange,
    this.validatorName,
    this.conclusion,
    this.documentId,
    this.version = 1,
    this.parentResultId,
    this.comment,
    this.pdfFilePath,
    this.sampleCollectedAt,
    this.resultAt,
    this.validatedAt,
    this.createdAt,
  });

  final String id;
  final String resultNumber;
  final String examRequestNumber;
  final String? labOrderItemId;
  final String? examName;
  final String status;
  final String analyte;
  final String value;
  final String? unit;
  final String? interpretation;
  final String? referenceRange;
  final String? validatorName;
  final String? conclusion;
  final String? documentId;
  final int version;
  final String? parentResultId;
  final String? comment;
  final String? pdfFilePath;
  final DateTime? sampleCollectedAt;
  final DateTime? resultAt;
  final DateTime? validatedAt;
  final DateTime? createdAt;

  factory PatientLabResultSummary.fromJson(Map<String, dynamic> json) {
    return PatientLabResultSummary(
      id: _firstString(json, const ['id']) ?? '',
      resultNumber: _firstString(json, const ['resultNumber']) ?? '',
      examRequestNumber:
          _firstString(json, const ['examRequestNumber', 'orderNumber']) ?? '',
      labOrderItemId: _firstString(json, const ['labOrderItemId']),
      examName: _firstString(json, const ['examName']),
      status: _firstString(json, const ['status']) ?? '',
      analyte: _firstString(json, const ['analyteName', 'name']) ?? '',
      value: (json['value'] ?? '').toString(),
      unit: _firstString(json, const ['unit']),
      interpretation: _firstString(json, const ['interpretation']),
      referenceRange: _firstString(json, const ['referenceRange']),
      validatorName: _firstString(json, const ['validatorName']),
      conclusion: _firstString(json, const ['conclusion']),
      documentId: _firstString(json, const ['documentId']),
      version: _int(json['version'], fallback: 1),
      parentResultId: _firstString(json, const ['parentResultId']),
      comment: _firstString(json, const ['comment']),
      pdfFilePath: _firstString(json, const ['pdfFilePath']),
      sampleCollectedAt: _date(json['sampleCollectedAt']),
      resultAt: _date(json['resultAt']),
      validatedAt: _date(json['validatedAt']),
      createdAt: _date(json['createdAt']),
    );
  }

  bool get hasPdf =>
      pdfFilePath?.trim().isNotEmpty == true ||
      documentId?.trim().isNotEmpty == true;

  String get normalizedInterpretation =>
      interpretation?.trim().toUpperCase() ?? '';

  bool get isCritical =>
      normalizedInterpretation == 'CRITICAL' ||
      normalizedInterpretation == 'CRITIQUE';
}

@immutable
final class PatientHospitalizationSummary {
  const PatientHospitalizationSummary({
    required this.id,
    required this.status,
    this.admittedAt,
    this.dischargedAt,
    this.service,
    this.reason,
  });

  final String id;
  final String status;
  final DateTime? admittedAt;
  final DateTime? dischargedAt;
  final String? service;
  final String? reason;

  factory PatientHospitalizationSummary.fromJson(Map<String, dynamic> json) {
    return PatientHospitalizationSummary(
      id: _firstString(json, const ['id']) ?? '',
      status: _firstString(json, const ['status']) ?? '',
      admittedAt: _date(
        json['admissionDate'] ?? json['admittedAt'] ?? json['createdAt'],
      ),
      dischargedAt: _date(json['dischargeDate'] ?? json['dischargedAt']),
      service: _firstString(json, const [
        'serviceName',
        'departmentName',
        'service',
      ]),
      reason: _firstString(json, const [
        'admissionReason',
        'reason',
        'diagnosis',
      ]),
    );
  }
}

@immutable
final class PatientAuditSummary {
  const PatientAuditSummary({
    required this.id,
    required this.action,
    required this.status,
    this.actor,
    this.createdAt,
    this.resourceType,
    this.reason,
  });

  final String id;
  final String action;
  final String status;
  final String? actor;
  final DateTime? createdAt;
  final String? resourceType;
  final String? reason;

  factory PatientAuditSummary.fromJson(Map<String, dynamic> json) {
    return PatientAuditSummary(
      id: _firstString(json, const ['id']) ?? '',
      action: _firstString(json, const ['action']) ?? '',
      status: _firstString(json, const ['status']) ?? '',
      actor: _firstString(json, const ['actorName', 'userName']),
      createdAt: _date(json['createdAt']),
      resourceType: _firstString(json, const ['resourceType']),
      reason: _firstString(json, const ['reason', 'details']),
    );
  }
}

@immutable
final class PatientRecordBundle {
  const PatientRecordBundle({
    required this.identity,
    required this.history,
    this.vaccinations = const [],
    this.labOrders = const [],
    this.labResults = const [],
    this.hospitalizations = const [],
    this.auditLogs = const [],
  });

  final PatientIdentity identity;
  final PatientMedicalHistory history;
  final List<PatientVaccinationSummary> vaccinations;
  final List<PatientLabOrderSummary> labOrders;
  final List<PatientLabResultSummary> labResults;
  final List<PatientHospitalizationSummary> hospitalizations;
  final List<PatientAuditSummary> auditLogs;
}

String? _firstString(Map<String, dynamic> json, List<String> keys) {
  for (final key in keys) {
    final value = json[key];
    if (value == null) {
      continue;
    }
    final text = value.toString().trim();
    if (text.isNotEmpty) {
      return text;
    }
  }
  return null;
}

int _int(Object? value, {int fallback = 0}) {
  if (value is num) return value.toInt();
  return int.tryParse(value?.toString() ?? '') ?? fallback;
}

DateTime? _date(Object? value) {
  if (value == null) {
    return null;
  }
  return DateTime.tryParse(value.toString())?.toLocal();
}
