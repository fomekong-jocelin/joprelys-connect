import 'package:flutter/widgets.dart';

import '../../domain/consultation_note.dart';

final class ConsultationNoteFormControllers {
  ConsultationNoteFormControllers();

  final symptoms = TextEditingController();
  final clinicalExam = TextEditingController();
  final diagnosis = TextEditingController();
  final conclusion = TextEditingController();
  final advice = TextEditingController();
  final followUp = TextEditingController();

  String? _acceptedPrescriptions;
  String? _acceptedLabOrders;

  String? get acceptedPrescriptions => _acceptedPrescriptions;
  String? get acceptedLabOrders => _acceptedLabOrders;

  bool get hasAcceptedStructuredExtras =>
      _isPresent(_acceptedPrescriptions) || _isPresent(_acceptedLabOrders);

  void populate(ConsultationNote note) {
    symptoms.text = note.symptoms ?? '';
    clinicalExam.text = note.clinicalExam ?? '';
    diagnosis.text = note.diagnosis ?? '';
    conclusion.text = note.conclusion ?? '';
    advice.text = note.advice ?? '';
    followUp.text = note.followUp ?? '';
    _acceptedPrescriptions = _normalized(note.prescriptions);
    _acceptedLabOrders = _normalized(note.labOrders);
  }

  /// Remplace le brouillon courant par le résultat IA explicitement accepté.
  bool applyAcceptedDraft(ConsultationNote note) {
    var changed = false;
    changed = _replace(symptoms, note.symptoms) || changed;
    changed = _replace(clinicalExam, note.clinicalExam) || changed;
    changed = _replace(diagnosis, note.diagnosis) || changed;
    changed = _replace(conclusion, note.conclusion) || changed;
    changed = _replace(advice, note.advice) || changed;
    changed = _replace(followUp, note.followUp) || changed;

    final prescriptions = _normalized(note.prescriptions);
    final labOrders = _normalized(note.labOrders);
    if (_acceptedPrescriptions != prescriptions) {
      _acceptedPrescriptions = prescriptions;
      changed = true;
    }
    if (_acceptedLabOrders != labOrders) {
      _acceptedLabOrders = labOrders;
      changed = true;
    }
    return changed;
  }

  Map<String, String> toAiDraft() {
    final draft = <String, String>{};
    _putWhenPresent(draft, 'symptoms', symptoms.text);
    _putWhenPresent(draft, 'clinicalExam', clinicalExam.text);
    _putWhenPresent(draft, 'diagnosis', diagnosis.text);
    _putWhenPresent(draft, 'conclusion', conclusion.text);
    _putWhenPresent(draft, 'advice', advice.text);
    _putWhenPresent(draft, 'followUp', followUp.text);
    _putWhenPresent(draft, 'prescription', _acceptedPrescriptions ?? '');
    _putWhenPresent(draft, 'labOrders', _acceptedLabOrders ?? '');
    return draft;
  }

  /// Le contrat consultation reste strictement SOAP. Les extras structurés sont
  /// persistés séparément par ClinicalVoiceAcceptedResultPersistence.
  ConsultationNote toConsultationNote() {
    return ConsultationNote(
      symptoms: symptoms.text.trim(),
      clinicalExam: clinicalExam.text.trim(),
      diagnosis: diagnosis.text.trim(),
      conclusion: conclusion.text.trim(),
      advice: advice.text.trim(),
      followUp: followUp.text.trim(),
    );
  }

  void dispose() {
    symptoms.dispose();
    clinicalExam.dispose();
    diagnosis.dispose();
    conclusion.dispose();
    advice.dispose();
    followUp.dispose();
  }

  bool _replace(TextEditingController controller, String? value) {
    final replacement = value?.trim() ?? '';
    if (controller.text == replacement) return false;
    controller.text = replacement;
    return true;
  }

  void _putWhenPresent(Map<String, String> target, String field, String value) {
    final trimmed = value.trim();
    if (trimmed.isNotEmpty) {
      target[field] = trimmed;
    }
  }

  String? _normalized(String? value) {
    final normalized = value?.trim() ?? '';
    return normalized.isEmpty ? null : normalized;
  }

  bool _isPresent(String? value) => value != null && value.trim().isNotEmpty;
}
