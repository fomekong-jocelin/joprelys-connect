import 'dart:convert';

String formatClinicalValue(
  String field,
  String? value, {
  required bool isFrench,
}) {
  final raw = value?.trim();
  if (raw == null || raw.isEmpty) {
    return isFrench ? 'Vide' : 'Empty';
  }
  if (!const {'vitals', 'prescription', 'labOrders'}.contains(field)) {
    return raw;
  }

  try {
    final decoded = jsonDecode(raw);
    return switch (field) {
      'prescription' => _formatPrescription(decoded, isFrench) ?? raw,
      'labOrders' => _formatLabOrders(decoded) ?? raw,
      'vitals' => _formatVitals(decoded, isFrench) ?? raw,
      _ => raw,
    };
  } catch (_) {
    return raw;
  }
}

String? _formatPrescription(Object? decoded, bool isFrench) {
  final items = decoded is List ? decoded : <Object?>[decoded];
  final lines = <String>[];
  for (final item in items) {
    if (item is! Map) {
      if (item != null && item.toString().trim().isNotEmpty) {
        lines.add(item.toString().trim());
      }
      continue;
    }

    final medication = Map<String, dynamic>.from(item);
    final name = _text(medication['drugName']);
    final details = <String>[
      if (_text(medication['dosage']) case final dosage?)
        '${isFrench ? 'Dosage' : 'Dosage'} : $dosage',
      if (_text(medication['form']) case final form?)
        '${isFrench ? 'Forme' : 'Form'} : $form',
      if (_text(medication['posology']) case final posology?)
        '${isFrench ? 'Posologie' : 'Directions'} : $posology',
      if (_text(medication['frequency']) case final frequency?)
        '${isFrench ? 'Fréquence' : 'Frequency'} : $frequency',
      if (_text(medication['route']) case final route?)
        '${isFrench ? 'Voie' : 'Route'} : $route',
      if (_text(medication['duration']) case final duration?)
        '${isFrench ? 'Durée' : 'Duration'} : $duration',
      if (_text(medication['quantity']) case final quantity?)
        '${isFrench ? 'Quantité' : 'Quantity'} : $quantity',
      if (_text(medication['instructions']) case final instructions?)
        '${isFrench ? 'Instructions' : 'Instructions'} : $instructions',
      if (medication['substitutionAllowed'] case final bool allowed)
        '${isFrench ? 'Substitution' : 'Substitution'} : '
            '${allowed ? (isFrench ? 'autorisée' : 'allowed') : (isFrench ? 'non autorisée' : 'not allowed')}',
    ];

    final title = name ?? (isFrench ? 'Médicament' : 'Medication');
    lines.add(<String>[
      title,
      for (final detail in details) '• $detail',
    ].join('\n'));
  }
  return lines.isEmpty ? null : lines.join('\n\n');
}

String? _formatLabOrders(Object? decoded) {
  if (decoded is! List) return null;
  final items = decoded
      .map(_text)
      .whereType<String>()
      .map((item) => '• $item')
      .toList(growable: false);
  return items.isEmpty ? null : items.join('\n');
}

String? _formatVitals(Object? decoded, bool isFrench) {
  if (decoded is! Map) return null;
  final labels = <String, String>{
    'temperature': isFrench ? 'Température' : 'Temperature',
    'weight': isFrench ? 'Poids' : 'Weight',
    'height': isFrench ? 'Taille' : 'Height',
    'pulse': isFrench ? 'Pouls' : 'Pulse',
    'systolic': isFrench ? 'Systolique' : 'Systolic',
    'diastolic': isFrench ? 'Diastolique' : 'Diastolic',
    'spo2': 'SpO₂',
    'glycemia': isFrench ? 'Glycémie' : 'Glycemia',
    'respiratoryRate': isFrench
        ? 'Fréquence respiratoire'
        : 'Respiratory rate',
    'painScale': isFrench ? 'Douleur' : 'Pain scale',
  };
  final items = decoded.entries
      .where((entry) => entry.value != null)
      .map((entry) => '${labels[entry.key.toString()] ?? entry.key}: ${entry.value}')
      .toList(growable: false);
  return items.isEmpty ? null : items.join(' · ');
}

String? _text(Object? value) {
  if (value is! String) return null;
  final trimmed = value.trim();
  return trimmed.isEmpty ? null : trimmed;
}
