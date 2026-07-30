final class ActiveVisit {
  const ActiveVisit({
    required this.id,
    required this.visitNumber,
    required this.patientId,
    required this.patientName,
    required this.patientDpu,
    required this.reason,
    required this.orientation,
    required this.status,
    required this.createdAt,
    this.service,
    this.mainPractitionerId,
    this.arrivalAt,
    this.closedAt,
    this.vitals,
  });

  final String id;
  final String visitNumber;
  final String patientId;
  final String patientName;
  final String patientDpu;
  final String reason;
  final String orientation;
  final String? service;
  final String? mainPractitionerId;
  final String status;
  final DateTime? arrivalAt;
  final DateTime createdAt;
  final DateTime? closedAt;
  final VisitVitals? vitals;

  DateTime get queueSince => arrivalAt ?? createdAt;
  bool get hasVitals => vitals != null;

  factory ActiveVisit.fromJson(Map<String, dynamic> json) {
    return ActiveVisit(
      id: _requiredString(json, 'id'),
      visitNumber: _requiredString(json, 'visitNumber'),
      patientId: _requiredString(json, 'patientId'),
      patientName: _requiredString(json, 'patientName'),
      patientDpu: _requiredString(json, 'patientDpu'),
      reason: _requiredString(json, 'reason'),
      orientation: _requiredString(json, 'orientation'),
      service: _optionalString(json, 'service'),
      mainPractitionerId: _optionalString(json, 'mainPractitionerId'),
      status: _requiredString(json, 'status'),
      arrivalAt: _optionalDate(json, 'arrivalAt'),
      createdAt: _requiredDate(json, 'createdAt'),
      closedAt: _optionalDate(json, 'closedAt'),
      vitals: VisitVitals.tryParse(json['vitals']),
    );
  }

  static String _requiredString(Map<String, dynamic> json, String key) {
    final value = json[key];
    if (value is! String || value.trim().isEmpty) {
      throw FormatException('Invalid active visit field: $key');
    }
    return value.trim();
  }

  static String? _optionalString(Map<String, dynamic> json, String key) {
    final value = json[key];
    if (value == null) {
      return null;
    }
    if (value is! String) {
      throw FormatException('Invalid active visit field: $key');
    }
    final normalized = value.trim();
    return normalized.isEmpty ? null : normalized;
  }

  static DateTime _requiredDate(Map<String, dynamic> json, String key) {
    final value = _requiredString(json, key);
    final parsed = DateTime.tryParse(value);
    if (parsed == null) {
      throw FormatException('Invalid active visit date: $key');
    }
    return parsed.toUtc();
  }

  static DateTime? _optionalDate(Map<String, dynamic> json, String key) {
    final value = json[key];
    if (value == null) {
      return null;
    }
    if (value is! String) {
      throw FormatException('Invalid active visit date: $key');
    }
    final normalized = value.trim();
    if (normalized.isEmpty) {
      return null;
    }
    final parsed = DateTime.tryParse(normalized);
    if (parsed == null) {
      throw FormatException('Invalid active visit date: $key');
    }
    return parsed.toUtc();
  }
}

final class VisitVitals {
  const VisitVitals({
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

  final double? temperature;
  final double? weight;
  final double? height;
  final double? pulse;
  final double? systolic;
  final double? diastolic;
  final double? spo2;
  final double? glycemia;
  final double? respiratoryRate;
  final double? painScale;
  final double? bmi;

  static VisitVitals? tryParse(Object? value) {
    if (value == null) {
      return null;
    }
    if (value is! Map) {
      throw const FormatException('Invalid active visit vitals');
    }
    final json = Map<String, dynamic>.from(value);
    return VisitVitals(
      temperature: _number(json['temperature']),
      weight: _number(json['weight']),
      height: _number(json['height']),
      pulse: _number(json['pulse']),
      systolic: _number(json['systolic']),
      diastolic: _number(json['diastolic']),
      spo2: _number(json['spo2']),
      glycemia: _number(json['glycemia']),
      respiratoryRate: _number(json['respiratoryRate']),
      painScale: _number(json['painScale']),
      bmi: _number(json['bmi']),
    );
  }

  static double? _number(Object? value) {
    if (value == null) {
      return null;
    }
    if (value is num) {
      return value.toDouble();
    }
    throw const FormatException('Invalid active visit vital value');
  }
}
