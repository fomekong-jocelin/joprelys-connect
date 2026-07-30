import 'package:flutter/foundation.dart';

@immutable
final class ConsultationNote {
  const ConsultationNote({
    this.subjective,
    this.objective,
    this.assessment,
    this.plan,
    this.updatedAt,
  });

  factory ConsultationNote.fromJson(Map<String, dynamic> json) {
    return ConsultationNote(
      subjective: json['subjective'] as String?,
      objective: json['objective'] as String?,
      assessment: json['assessment'] as String?,
      plan: json['plan'] as String?,
      updatedAt: json['updatedAt'] != null
          ? DateTime.tryParse(json['updatedAt'] as String)
          : null,
    );
  }

  final String? subjective;
  final String? objective;
  final String? assessment;
  final String? plan;
  final DateTime? updatedAt;

  bool get isEmpty =>
      (subjective == null || subjective!.trim().isEmpty) &&
      (objective == null || objective!.trim().isEmpty) &&
      (assessment == null || assessment!.trim().isEmpty) &&
      (plan == null || plan!.trim().isEmpty);

  Map<String, dynamic> toJson() {
    return {
      if (subjective != null) 'subjective': subjective,
      if (objective != null) 'objective': objective,
      if (assessment != null) 'assessment': assessment,
      if (plan != null) 'plan': plan,
    };
  }

  ConsultationNote copyWith({
    String? subjective,
    String? objective,
    String? assessment,
    String? plan,
    DateTime? updatedAt,
  }) {
    return ConsultationNote(
      subjective: subjective ?? this.subjective,
      objective: objective ?? this.objective,
      assessment: assessment ?? this.assessment,
      plan: plan ?? this.plan,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}
