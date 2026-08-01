import 'package:flutter/foundation.dart';

enum AllergySeverity { low, moderate, severe }

@immutable
final class MedicalAntecedent {
  const MedicalAntecedent({
    required this.type,
    required this.description,
    this.diagnosedYear,
  });

  final String type; // MEDICAL, SURGICAL, FAMILY
  final String description;
  final int? diagnosedYear;

  factory MedicalAntecedent.fromJson(Map<String, dynamic> json) {
    return MedicalAntecedent(
      type: json['type'] as String? ?? 'MEDICAL',
      description: json['description'] as String? ?? '',
      diagnosedYear: json['diagnosedYear'] as int?,
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
    final sevStr = (json['severity'] as String? ?? 'LOW').toUpperCase();
    final severity = switch (sevStr) {
      'SEVERE' => AllergySeverity.severe,
      'MODERATE' => AllergySeverity.moderate,
      _ => AllergySeverity.low,
    };

    return PatientAllergy(
      allergen: json['allergen'] as String? ?? '',
      severity: severity,
      reaction: json['reaction'] as String?,
    );
  }

  Map<String, dynamic> toJson() => {
    'allergen': allergen,
    'severity': severity.name.toUpperCase(),
    if (reaction != null) 'reaction': reaction,
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
    this.temperature,
    this.systolic,
    this.diastolic,
    this.pulse,
  });

  final String id;
  final String visitNumber;
  final DateTime date;
  final String practitionerName;
  final String chiefComplaint;
  final double? temperature;
  final int? systolic;
  final int? diastolic;
  final int? pulse;

  factory PastVisitSummary.fromJson(Map<String, dynamic> json) {
    return PastVisitSummary(
      id: json['id'] as String? ?? '',
      visitNumber: json['visitNumber'] as String? ?? '',
      date: DateTime.tryParse(json['date'] as String? ?? '') ?? DateTime.now(),
      practitionerName: json['practitionerName'] as String? ?? '',
      chiefComplaint: json['chiefComplaint'] as String? ?? '',
      temperature: (json['temperature'] as num?)?.toDouble(),
      systolic: json['systolic'] as int?,
      diastolic: json['diastolic'] as int?,
      pulse: json['pulse'] as int?,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'visitNumber': visitNumber,
    'date': date.toIso8601String(),
    'practitionerName': practitionerName,
    'chiefComplaint': chiefComplaint,
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
            ?.map((e) => MedicalAntecedent.fromJson(e as Map<String, dynamic>))
            .toList() ??
        [];

    final allergiesList =
        (json['allergies'] as List<dynamic>?)
            ?.map((e) => PatientAllergy.fromJson(e as Map<String, dynamic>))
            .toList() ??
        [];

    final visitsList =
        (json['pastVisits'] as List<dynamic>?)
            ?.map((e) => PastVisitSummary.fromJson(e as Map<String, dynamic>))
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
    'antecedents': antecedents.map((e) => e.toJson()).toList(),
    'allergies': allergies.map((e) => e.toJson()).toList(),
    'pastVisits': pastVisits.map((e) => e.toJson()).toList(),
  };
}
