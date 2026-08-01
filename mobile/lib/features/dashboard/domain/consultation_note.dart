import 'package:flutter/foundation.dart';

@immutable
final class ConsultationNote {
  const ConsultationNote({
    this.symptoms,
    this.clinicalExam,
    this.diagnosis,
    this.conclusion,
    this.advice,
    this.followUp,
    this.updatedAt,
  });

  factory ConsultationNote.fromJson(Map<String, dynamic> json) {
    return ConsultationNote(
      symptoms: json['symptoms'] as String?,
      clinicalExam: json['clinicalExam'] as String?,
      diagnosis: json['diagnosis'] as String?,
      conclusion: json['conclusion'] as String?,
      advice: json['advice'] as String?,
      followUp: json['followUp'] as String?,
      updatedAt: json['updatedAt'] != null
          ? DateTime.tryParse(json['updatedAt'] as String)
          : null,
    );
  }

  final String? symptoms;
  final String? clinicalExam;
  final String? diagnosis;
  final String? conclusion;
  final String? advice;
  final String? followUp;
  final DateTime? updatedAt;

  bool get isEmpty =>
      _isBlank(symptoms) &&
      _isBlank(clinicalExam) &&
      _isBlank(diagnosis) &&
      _isBlank(conclusion) &&
      _isBlank(advice) &&
      _isBlank(followUp);

  Map<String, dynamic> toJson() {
    return {
      if (symptoms != null) 'symptoms': symptoms,
      if (clinicalExam != null) 'clinicalExam': clinicalExam,
      if (diagnosis != null) 'diagnosis': diagnosis,
      if (conclusion != null) 'conclusion': conclusion,
      if (advice != null) 'advice': advice,
      if (followUp != null) 'followUp': followUp,
    };
  }

  ConsultationNote copyWith({
    String? symptoms,
    String? clinicalExam,
    String? diagnosis,
    String? conclusion,
    String? advice,
    String? followUp,
    DateTime? updatedAt,
  }) {
    return ConsultationNote(
      symptoms: symptoms ?? this.symptoms,
      clinicalExam: clinicalExam ?? this.clinicalExam,
      diagnosis: diagnosis ?? this.diagnosis,
      conclusion: conclusion ?? this.conclusion,
      advice: advice ?? this.advice,
      followUp: followUp ?? this.followUp,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  static bool _isBlank(String? value) => value == null || value.trim().isEmpty;
}
