import '../domain/consultation_note.dart';
import '../domain/patient_vitals.dart';

final class DictationParseResult {
  const DictationParseResult({
    required this.vitals,
    required this.note,
  });

  final PatientVitals vitals;
  final ConsultationNote note;
}

final class ClinicalDictationParser {
  const ClinicalDictationParser();

  DictationParseResult parse(String text) {
    if (text.trim().isEmpty) {
      return const DictationParseResult(
        vitals: PatientVitals(),
        note: ConsultationNote(),
      );
    }

    final lower = text.toLowerCase();

    // 1. Temperature (°C)
    double? temp;
    final tempMatch = RegExp(
      r'(?:température|temp|t°?)\s*:?\s*(\d{2}(?:[\.,]\d)?)',
      caseSensitive: false,
    ).firstMatch(lower);
    if (tempMatch != null) {
      temp = double.tryParse(tempMatch.group(1)!.replaceAll(',', '.'));
    }

    // 2. Tension Arterielle (Systolique / Diastolique)
    int? sys;
    int? dia;
    final taMatch = RegExp(
      r'(?:tension|ta|bp)\s*:?\s*(\d{2,3})[\s\/\u2011\-]+(?:sur\s+)?(\d{2,3})',
      caseSensitive: false,
    ).firstMatch(lower);
    if (taMatch != null) {
      sys = int.tryParse(taMatch.group(1)!);
      dia = int.tryParse(taMatch.group(2)!);
    }

    // 3. Pouls (bpm)
    int? pulse;
    final pulseMatch = RegExp(
      r'(?:pouls|fc|pulse|bpm)\s*:?\s*(\d{2,3})',
      caseSensitive: false,
    ).firstMatch(lower);
    if (pulseMatch != null) {
      pulse = int.tryParse(pulseMatch.group(1)!);
    }

    // 4. SpO2 (%)
    int? spo2;
    final spo2Match = RegExp(
      r'(?:spo2|sat|saturation)\s*:?\s*(\d{2,3})%?',
      caseSensitive: false,
    ).firstMatch(lower);
    if (spo2Match != null) {
      spo2 = int.tryParse(spo2Match.group(1)!);
    }

    // 5. Poids (kg)
    double? weight;
    final weightMatch = RegExp(
      r'(?:poids|weight)\s*:?\s*(\d{2,3}(?:[\.,]\d)?)\s*(?:kg)?',
      caseSensitive: false,
    ).firstMatch(lower);
    if (weightMatch != null) {
      weight = double.tryParse(weightMatch.group(1)!.replaceAll(',', '.'));
    }

    // 6. Taille (cm)
    int? height;
    final heightMatch = RegExp(
      r'(?:taille|height)\s*:?\s*(\d{2,3})\s*(?:cm)?',
      caseSensitive: false,
    ).firstMatch(lower);
    if (heightMatch != null) {
      height = int.tryParse(heightMatch.group(1)!);
    }

    // 7. Glycémie (g/L)
    double? glycemia;
    final glyMatch = RegExp(
      r'(?:glycémie|glycemie|dextro)\s*:?\s*(\d+(?:[\.,]\d+)?)',
      caseSensitive: false,
    ).firstMatch(lower);
    if (glyMatch != null) {
      glycemia = double.tryParse(glyMatch.group(1)!.replaceAll(',', '.'));
    }

    // 8. Fréquence respiratoire (c/min)
    int? respRate;
    final respMatch = RegExp(
      r'(?:fréquence respiratoire|freq resp|fr)\s*:?\s*(\d{1,2})',
      caseSensitive: false,
    ).firstMatch(lower);
    if (respMatch != null) {
      respRate = int.tryParse(respMatch.group(1)!);
    }

    // 9. Douleur (EVA)
    int? pain;
    final painMatch = RegExp(
      r'(?:douleur|eva)\s*:?\s*(\d{1,2})',
      caseSensitive: false,
    ).firstMatch(lower);
    if (painMatch != null) {
      pain = int.tryParse(painMatch.group(1)!);
    }

    final vitals = PatientVitals(
      temperature: temp,
      weight: weight,
      height: height,
      pulse: pulse,
      systolic: sys,
      diastolic: dia,
      spo2: spo2,
      glycemia: glycemia,
      respiratoryRate: respRate,
      painScale: pain,
    );

    // Extraction des notes cliniques (Subjectif/Objective/Assessment/Plan)
    final note = ConsultationNote(
      subjective: text,
      objective: _buildObjectiveSummary(vitals),
      assessment: lower.contains('fièvre') || lower.contains('fievre')
          ? 'Syndrome fébrile à évaluer'
          : null,
      plan: 'Prendre les constantes de contrôle, surveillance clinique.',
    );

    return DictationParseResult(vitals: vitals, note: note);
  }

  String _buildObjectiveSummary(PatientVitals vitals) {
    final parts = <String>[];
    if (vitals.temperature != null) parts.add('T°: ${vitals.temperature}°C');
    if (vitals.systolic != null && vitals.diastolic != null) {
      parts.add('TA: ${vitals.systolic}/${vitals.diastolic} mmHg');
    }
    if (vitals.pulse != null) parts.add('Pouls: ${vitals.pulse} bpm');
    if (vitals.spo2 != null) parts.add('SpO2: ${vitals.spo2}%');
    return parts.join(' | ');
  }
}
