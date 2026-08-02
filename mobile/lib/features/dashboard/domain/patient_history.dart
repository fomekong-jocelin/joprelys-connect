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
      systolic: (json['systolic'] as num?)?.toInt(),
      diastolic: (json['diastolic'] as num?)?.toInt(),
      pulse: (json['pulse'] as num?)?.toInt(),
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

String? _optionalString(Object? value) {
  if (value == null) return null;
  final text = value.toString().trim();
  return text.isEmpty ? null : text;
}
