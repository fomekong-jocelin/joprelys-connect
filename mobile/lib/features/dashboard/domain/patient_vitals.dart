import 'package:flutter/foundation.dart';

@immutable
class PatientVitals {
  const PatientVitals({
    this.temperature,
    this.weight,
    this.height,
    this.pulse,
    this.systolic,
    this.diastolic,
    this.spo2,
    this.glycemia,
    this.respiratoryRate,
    this.painScale,
    this.bmi,
  });

  factory PatientVitals.fromJson(Map<String, dynamic> json) {
    return PatientVitals(
      temperature: (json['temperature'] as num?)?.toDouble(),
      weight: (json['weight'] as num?)?.toDouble(),
      height: (json['height'] as num?)?.toInt(),
      pulse: (json['pulse'] as num?)?.toInt(),
      systolic: (json['systolic'] as num?)?.toInt(),
      diastolic: (json['diastolic'] as num?)?.toInt(),
      spo2: (json['spo2'] as num?)?.toInt(),
      glycemia: (json['glycemia'] as num?)?.toDouble(),
      respiratoryRate: (json['respiratoryRate'] as num?)?.toInt(),
      painScale: (json['painScale'] as num?)?.toInt(),
      bmi: (json['bmi'] as num?)?.toDouble(),
    );
  }

  final double? temperature;
  final double? weight;
  final int? height;
  final int? pulse;
  final int? systolic;
  final int? diastolic;
  final int? spo2;
  final double? glycemia;
  final int? respiratoryRate;
  final int? painScale;
  final double? bmi;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      if (temperature != null) 'temperature': temperature,
      if (weight != null) 'weight': weight,
      if (height != null) 'height': height,
      if (pulse != null) 'pulse': pulse,
      if (systolic != null) 'systolic': systolic,
      if (diastolic != null) 'diastolic': diastolic,
      if (spo2 != null) 'spo2': spo2,
      if (glycemia != null) 'glycemia': glycemia,
      if (respiratoryRate != null) 'respiratoryRate': respiratoryRate,
      if (painScale != null) 'painScale': painScale,
    };
  }

  /// Keep every explicit value and let the preferred source replace only the
  /// fields it actually contains.
  PatientVitals mergePrefer(PatientVitals preferred) {
    return PatientVitals(
      temperature: preferred.temperature ?? temperature,
      weight: preferred.weight ?? weight,
      height: preferred.height ?? height,
      pulse: preferred.pulse ?? pulse,
      systolic: preferred.systolic ?? systolic,
      diastolic: preferred.diastolic ?? diastolic,
      spo2: preferred.spo2 ?? spo2,
      glycemia: preferred.glycemia ?? glycemia,
      respiratoryRate: preferred.respiratoryRate ?? respiratoryRate,
      painScale: preferred.painScale ?? painScale,
      bmi: preferred.bmi ?? bmi,
    );
  }

  double? get calculatedBmi {
    if (bmi != null) return bmi;
    if (weight != null && height != null && height! > 0) {
      final heightInMeters = height! / 100.0;
      return weight! / (heightInMeters * heightInMeters);
    }
    return null;
  }

  bool get isEmpty =>
      temperature == null &&
      weight == null &&
      height == null &&
      pulse == null &&
      systolic == null &&
      diastolic == null &&
      spo2 == null &&
      glycemia == null &&
      respiratoryRate == null &&
      painScale == null;
}
