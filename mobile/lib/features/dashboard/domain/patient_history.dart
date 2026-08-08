import 'package:flutter/foundation.dart';

enum AllergySeverity { low, moderate, severe }

@immutable
final class MedicalAntecedent {
  const MedicalAntecedent({
    required this.type,
    required this.description,
    this.diagnosedYear,
  });

  final String type;
  final String description;
  final int? diagnosedYear;

  factory MedicalAntecedent.fromJson(Map<String, dynamic> json) {
    final rawYear = json['diagnosedYear'] ?? json['year'];
    return MedicalAntecedent(
      type: (json['type'] ?? json['category'] ?? 'MEDICAL').toString(),
      description:
          (json['description'] ?? json['condition'] ?? json['label'] ?? '')
              .toString(),
      diagnosedYear: rawYear is num
          ? rawYear.toInt()
          : int.tryParse(rawYear?.toString() ?? ''),
    );
  }

  Map<String, dynamic> toJson() => {
    'type': type,
    'description': description,
    if (diagnosedYear != null) 'diagnosedYear': diagnosedYear,
  };
}

@immutable
final class PatientAllergy {
  const PatientAllergy({
    required this.allergen,
    required this.severity,
    this.reaction,
  });

  final String allergen;
  final AllergySeverity severity;
  final String? reaction;

  factory PatientAllergy.fromJson(Map<String, dynamic> json) {
    final severityValue = (json['severity'] ?? 'LOW').toString().toUpperCase();
    final severity = switch (severityValue) {
      'SEVERE' || 'HIGH' || 'CRITICAL' => AllergySeverity.severe,
      'MODERATE' || 'MEDIUM' => AllergySeverity.moderate,
      _ => AllergySeverity.low,
    };

    return PatientAllergy(
      allergen: (json['allergen'] ?? json['substance'] ?? json['name'] ?? '')
          .toString(),
      severity: severity,
      reaction: _optionalString(json['reaction'] ?? json['reactionType']),
    );
  }

  Map<String, dynamic> toJson() => {
    'allergen': allergen,
    'severity': severity.name.toUpperCase(),
    if (reaction != null) 'reaction': reaction,
  };
}

@immutable
final class PatientPrescriptionItemSummary {
  const PatientPrescriptionItemSummary({
    required this.drugName,
    this.dosage,
    this.posology,
    this.duration,
    this.quantity,
    this.instructions,
    this.form,
    this.route,
    this.frequency,
  });

  final String drugName;
  final String? dosage;
  final String? posology;
  final String? duration;
  final String? quantity;
  final String? instructions;
  final String? form;
  final String? route;
  final String? frequency;

  factory PatientPrescriptionItemSummary.fromJson(Map<String, dynamic> json) {
    return PatientPrescriptionItemSummary(
      drugName: _string(json['drugName'] ?? json['name']),
      dosage: _optionalString(json['dosage']),
      posology: _optionalString(json['posology']),
      duration: _optionalString(json['duration']),
      quantity: _optionalString(json['quantity']),
      instructions: _optionalString(json['instructions']),
      form: _optionalString(json['form']),
      route: _optionalString(json['route']),
      frequency: _optionalString(json['frequency']),
    );
  }

  Map<String, dynamic> toJson() => {
    'drugName': drugName,
    if (dosage != null) 'dosage': dosage,
    if (posology != null) 'posology': posology,
    if (duration != null) 'duration': duration,
    if (quantity != null) 'quantity': quantity,
    if (instructions != null) 'instructions': instructions,
    if (form != null) 'form': form,
    if (route != null) 'route': route,
    if (frequency != null) 'frequency': frequency,
  };
}

@immutable
final class PastVisitSummary {
  const PastVisitSummary({
    required this.id,
    required this.visitNumber,
    required this.date,
    required this.practitionerName,
    required this.chiefComplaint,
    this.symptoms = '',
    this.clinicalExam = '',
    this.diagnosis = '',
    this.conclusion = '',
    this.advice = '',
    this.followUp = '',
    this.status = '',
    this.documentNumber = '',
    this.prescriptionNumber,
    this.prescriptionStatus,
    this.prescriptionItems = const [],
    this.temperature,
    this.systolic,
    this.diastolic,
    this.pulse,
  });

  final String id;
  final String visitNumber;
  final DateTime date;
  final String practitionerName;

  /// Résumé court conservé pour les anciennes vues/cartes.
  final String chiefComplaint;

  /// Champs SOAP détaillés retournés par l'historique consultation backend.
  final String symptoms;
  final String clinicalExam;
  final String diagnosis;
  final String conclusion;
  final String advice;
  final String followUp;
  final String status;
  final String documentNumber;
  final String? prescriptionNumber;
  final String? prescriptionStatus;
  final List<PatientPrescriptionItemSummary> prescriptionItems;

  final double? temperature;
  final int? systolic;
  final int? diastolic;
  final int? pulse;

  factory PastVisitSummary.fromJson(Map<String, dynamic> json) {
    final rawItems = json['prescriptionItems'];
    final symptoms = _string(json['symptoms'] ?? json['reason']);
    final diagnosis = _string(json['diagnosis']);
    return PastVisitSummary(
      id: _string(json['visitId'] ?? json['id']),
      visitNumber: _string(json['visitNumber']),
      date:
          DateTime.tryParse(
            (json['createdAt'] ?? json['date'] ?? json['consultedAt'] ?? '')
                .toString(),
          )?.toLocal() ??
          DateTime.fromMillisecondsSinceEpoch(0),
      practitionerName: _string(json['doctorName'] ?? json['practitionerName']),
      chiefComplaint: _firstNonBlank([diagnosis, symptoms]),
      symptoms: symptoms,
      clinicalExam: _string(json['clinicalExam']),
      diagnosis: diagnosis,
      conclusion: _string(json['conclusion']),
      advice: _string(json['advice']),
      followUp: _string(json['followUp']),
      status: _string(json['status']),
      documentNumber: _string(json['documentNumber']),
      prescriptionNumber: _optionalString(json['prescriptionNumber']),
      prescriptionStatus: _optionalString(json['prescriptionStatus']),
      prescriptionItems: rawItems is List
          ? rawItems
                .whereType<Map>()
                .map(
                  (item) => PatientPrescriptionItemSummary.fromJson(
                    Map<String, dynamic>.from(item),
                  ),
                )
                .where((item) => item.drugName.isNotEmpty)
                .toList(growable: false)
          : const <PatientPrescriptionItemSummary>[],
      temperature: _number(json, 'temperature')?.toDouble(),
      systolic: _number(json, 'systolic')?.toInt(),
      diastolic: _number(json, 'diastolic')?.toInt(),
      pulse: _number(json, 'pulse')?.toInt(),
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'visitNumber': visitNumber,
    'date': date.toIso8601String(),
    'practitionerName': practitionerName,
    'chiefComplaint': chiefComplaint,
    'symptoms': symptoms,
    'clinicalExam': clinicalExam,
    'diagnosis': diagnosis,
    'conclusion': conclusion,
    'advice': advice,
    'followUp': followUp,
    'status': status,
    'documentNumber': documentNumber,
    if (prescriptionNumber != null) 'prescriptionNumber': prescriptionNumber,
    if (prescriptionStatus != null) 'prescriptionStatus': prescriptionStatus,
    'prescriptionItems': prescriptionItems
        .map((item) => item.toJson())
        .toList(),
    if (temperature != null) 'temperature': temperature,
    if (systolic != null) 'systolic': systolic,
    if (diastolic != null) 'diastolic': diastolic,
    if (pulse != null) 'pulse': pulse,
  };
}

@immutable
final class PatientMedicalHistory {
  const PatientMedicalHistory({
    required this.patientId,
    required this.patientName,
    required this.patientDpu,
    this.antecedents = const [],
    this.allergies = const [],
    this.pastVisits = const [],
  });

  final String patientId;
  final String patientName;
  final String patientDpu;
  final List<MedicalAntecedent> antecedents;
  final List<PatientAllergy> allergies;
  final List<PastVisitSummary> pastVisits;

  factory PatientMedicalHistory.fromJson(Map<String, dynamic> json) {
    final antecedentsList =
        (json['antecedents'] as List<dynamic>?)
            ?.whereType<Map>()
            .map(
              (item) =>
                  MedicalAntecedent.fromJson(Map<String, dynamic>.from(item)),
            )
            .toList() ??
        [];

    final allergiesList =
        (json['allergies'] as List<dynamic>?)
            ?.whereType<Map>()
            .map(
              (item) =>
                  PatientAllergy.fromJson(Map<String, dynamic>.from(item)),
            )
            .toList() ??
        [];

    final visitsList =
        (json['pastVisits'] as List<dynamic>?)
            ?.whereType<Map>()
            .map(
              (item) =>
                  PastVisitSummary.fromJson(Map<String, dynamic>.from(item)),
            )
            .toList() ??
        [];

    return PatientMedicalHistory(
      patientId: json['patientId'] as String? ?? '',
      patientName: json['patientName'] as String? ?? '',
      patientDpu: json['patientDpu'] as String? ?? '',
      antecedents: antecedentsList,
      allergies: allergiesList,
      pastVisits: visitsList,
    );
  }

  Map<String, dynamic> toJson() => {
    'patientId': patientId,
    'patientName': patientName,
    'patientDpu': patientDpu,
    'antecedents': antecedents.map((item) => item.toJson()).toList(),
    'allergies': allergies.map((item) => item.toJson()).toList(),
    'pastVisits': pastVisits.map((item) => item.toJson()).toList(),
  };
}

num? _number(Map<String, dynamic> json, String key) {
  final direct = json[key];
  if (direct is num) return direct;
  final vitals = json['vitals'];
  if (vitals is Map) {
    final value = vitals[key];
    if (value is num) return value;
    return num.tryParse(value?.toString() ?? '');
  }
  return num.tryParse(direct?.toString() ?? '');
}

String _firstNonBlank(Iterable<String> values) {
  for (final value in values) {
    final trimmed = value.trim();
    if (trimmed.isNotEmpty) return trimmed;
  }
  return '';
}

String _string(Object? value) => value?.toString().trim() ?? '';

String? _optionalString(Object? value) {
  if (value == null) return null;
  final text = value.toString().trim();
  return text.isEmpty ? null : text;
}
