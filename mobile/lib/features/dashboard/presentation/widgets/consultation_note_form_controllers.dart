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

  void applyExtracted(ConsultationNote note) {
    _replaceWhenPresent(symptoms, note.symptoms);
    _replaceWhenPresent(clinicalExam, note.clinicalExam);
    _replaceWhenPresent(diagnosis, note.diagnosis);
    _replaceWhenPresent(conclusion, note.conclusion);
    _replaceWhenPresent(advice, note.advice);
    _replaceWhenPresent(followUp, note.followUp);
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

  void _replaceWhenPresent(TextEditingController controller, String? value) {
    if (value != null && value.trim().isNotEmpty) {
      controller.text = value;
    }
  }
}
