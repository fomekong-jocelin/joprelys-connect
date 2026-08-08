import 'package:flutter/foundation.dart';

@immutable
final class PrescriptionItem {
  const PrescriptionItem({
    required this.drugName,
    this.id,
    this.dosage = '',
    this.posology = '',
    this.duration = '',
    this.quantity = '',
    this.instructions = '',
    this.form = '',
    this.route = '',
    this.frequency = '',
    this.sortOrder = 0,
    this.substitutionAllowed = true,
  });

  final String? id;
  final String drugName;
  final String dosage;
  final String posology;
  final String duration;
  final String quantity;
  final String instructions;
  final String form;
  final String route;
  final String frequency;
  final int sortOrder;
  final bool substitutionAllowed;

  factory PrescriptionItem.fromJson(Map<String, dynamic> json) {
    return PrescriptionItem(
      id: _optionalText(json['id']),
      drugName: _text(json['drugName']),
      dosage: _text(json['dosage']),
      posology: _text(json['posology']),
      duration: _text(json['duration']),
      quantity: _text(json['quantity']),
      instructions: _text(json['instructions']),
      form: _text(json['form']),
      route: _text(json['route']),
      frequency: _text(json['frequency']),
      sortOrder: _int(json['sortOrder']),
      substitutionAllowed: json['substitutionAllowed'] != false,
    );
  }

  bool get hasRequiredDraftFields => drugName.trim().isNotEmpty;
  bool get isCompleteForFinalization =>
      drugName.trim().isNotEmpty && dosage.trim().isNotEmpty;

  Map<String, dynamic> toSaveJson() {
    return <String, dynamic>{
      'drugName': drugName.trim(),
      'dosage': dosage.trim(),
      if (posology.trim().isNotEmpty) 'posology': posology.trim(),
      if (duration.trim().isNotEmpty) 'duration': duration.trim(),
      if (quantity.trim().isNotEmpty) 'quantity': quantity.trim(),
      if (instructions.trim().isNotEmpty) 'instructions': instructions.trim(),
      if (form.trim().isNotEmpty) 'form': form.trim(),
      if (route.trim().isNotEmpty) 'route': route.trim(),
      if (frequency.trim().isNotEmpty) 'frequency': frequency.trim(),
      'substitutionAllowed': substitutionAllowed,
    };
  }
}

@immutable
final class Prescription {
  const Prescription({
    required this.id,
    required this.consultationId,
    required this.items,
    required this.status,
    this.prescriptionNumber,
    this.expiresAt,
    this.transmissionStatus,
    this.transmittedAt,
    this.issuedAt,
    this.documentId,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String consultationId;
  final List<PrescriptionItem> items;
  final String status;
  final String? prescriptionNumber;
  final DateTime? expiresAt;
  final String? transmissionStatus;
  final DateTime? transmittedAt;
  final DateTime? issuedAt;
  final String? documentId;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  factory Prescription.fromJson(Map<String, dynamic> json) {
    final items = <PrescriptionItem>[];
    final rawItems = json['items'];
    if (rawItems is List) {
      items.addAll(
        rawItems.whereType<Map>().map(
          (item) => PrescriptionItem.fromJson(Map<String, dynamic>.from(item)),
        ),
      );
    }
    items.sort((a, b) => a.sortOrder.compareTo(b.sortOrder));

    return Prescription(
      id: _text(json['id']),
      consultationId: _text(json['consultationId']),
      items: List<PrescriptionItem>.unmodifiable(items),
      status: _text(json['status']).toUpperCase(),
      prescriptionNumber: _optionalText(json['prescriptionNumber']),
      expiresAt: _date(json['expiresAt']),
      transmissionStatus: _optionalText(
        json['transmissionStatus'],
      )?.toUpperCase(),
      transmittedAt: _date(json['transmittedAt']),
      issuedAt: _date(json['issuedAt']),
      documentId: _optionalText(json['documentId']),
      createdAt: _date(json['createdAt']),
      updatedAt: _date(json['updatedAt']),
    );
  }

  bool get isDraft => status == 'DRAFT';
  bool get isActive => status == 'ACTIVE';
  bool get isCancelled => status == 'CANCELLED';
  bool get isExpired => status == 'EXPIRED';
  bool get isTransmitted => transmissionStatus == 'TRANSMITTED';
  bool get canFinalize =>
      isDraft &&
      items.isNotEmpty &&
      items.every((item) => item.isCompleteForFinalization);
}

String _text(Object? value) => value?.toString().trim() ?? '';

String? _optionalText(Object? value) {
  final valueText = _text(value);
  return valueText.isEmpty ? null : valueText;
}

int _int(Object? value) {
  if (value is num) return value.toInt();
  return int.tryParse(value?.toString() ?? '') ?? 0;
}

DateTime? _date(Object? value) {
  if (value == null) return null;
  return DateTime.tryParse(value.toString())?.toLocal();
}
