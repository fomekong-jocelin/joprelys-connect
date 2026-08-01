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

  void populate(ConsultationNote note) {
    symptoms.text = note.symptoms ?? '';
    clinicalExam.text = note.clinicalExam ?? '';
    diagnosis.text = note.diagnosis ?? '';
    conclusion.text = note.conclusion ?? '';
    advice.text = note.advice ?? '';
    followUp.text = note.followUp ?? '';
  }

  /// Applique uniquement dans les champs encore vides.
  ///
  /// Une proposition vocale ne peut donc jamais écraser silencieusement une
  /// saisie manuelle plus récente. Le remplacement explicite fera l'objet d'un
  /// véritable écran de diff dans une incision séparée.
  bool applyAcceptedToEmptyFields(ConsultationNote note) {
    var changed = false;
    changed = _fillWhenEmpty(symptoms, note.symptoms) || changed;
    changed = _fillWhenEmpty(clinicalExam, note.clinicalExam) || changed;
    changed = _fillWhenEmpty(diagnosis, note.diagnosis) || changed;
    changed = _fillWhenEmpty(conclusion, note.conclusion) || changed;
    changed = _fillWhenEmpty(advice, note.advice) || changed;
    changed = _fillWhenEmpty(followUp, note.followUp) || changed;
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
    return draft;
  }

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

  bool _fillWhenEmpty(TextEditingController controller, String? value) {
    final proposed = value?.trim();
    if (controller.text.trim().isNotEmpty ||
        proposed == null ||
        proposed.isEmpty) {
      return false;
    }
    controller.text = proposed;
    return true;
  }

  void _putWhenPresent(Map<String, String> target, String field, String value) {
    final trimmed = value.trim();
    if (trimmed.isNotEmpty) {
      target[field] = trimmed;
    }
  }
}
