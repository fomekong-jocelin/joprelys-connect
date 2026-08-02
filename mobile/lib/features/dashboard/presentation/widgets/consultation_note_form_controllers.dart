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

  /// Remplace le brouillon SOAP courant par le brouillon IA explicitement accepté.
  ///
  /// Le résultat transmis par l'assistant représente l'état SOAP complet construit
  /// à partir du brouillon initial et des décisions du praticien. Les champs non
  /// modifiés sont donc conservés dans ce résultat, tandis qu'une modification ou
  /// un effacement accepté doit réellement remplacer l'ancienne valeur affichée.
  bool applyAcceptedDraft(ConsultationNote note) {
    var changed = false;
    changed = _replace(symptoms, note.symptoms) || changed;
    changed = _replace(clinicalExam, note.clinicalExam) || changed;
    changed = _replace(diagnosis, note.diagnosis) || changed;
    changed = _replace(conclusion, note.conclusion) || changed;
    changed = _replace(advice, note.advice) || changed;
    changed = _replace(followUp, note.followUp) || changed;
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
}
