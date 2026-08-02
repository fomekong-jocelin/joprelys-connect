import 'dart:convert';

import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/data/clinical_voice_ai_api.dart';
import 'package:joprelys_mobile/features/dashboard/domain/consultation_note.dart';
import 'package:joprelys_mobile/features/dashboard/domain/patient_vitals.dart';
import 'package:joprelys_mobile/features/dashboard/presentation/widgets/consultation_note_form_controllers.dart';
import 'package:joprelys_mobile/features/dashboard/presentation/widgets/vitals_form_merge.dart';

void main() {
  test('accepted SOAP draft replaces a stale value from a previous capture', () {
    final controllers = ConsultationNoteFormControllers();
    addTearDown(controllers.dispose);

    controllers.symptoms.text = 'Mal de tête depuis trois semaines';
    controllers.diagnosis.text = 'Céphalée à explorer';

    final changed = controllers.applyAcceptedDraft(
      const ConsultationNote(
        symptoms: 'Fièvre et frissons apparus ce matin',
        diagnosis: 'Syndrome fébrile à explorer',
      ),
    );

    expect(changed, isTrue);
    expect(controllers.symptoms.text, 'Fièvre et frissons apparus ce matin');
    expect(controllers.diagnosis.text, 'Syndrome fébrile à explorer');
    expect(
      controllers.toAiDraft()['symptoms'],
      isNot('Mal de tête depuis trois semaines'),
    );
  });

  test('pending AI proposals stay separate from the accepted draft', () {
    final state = ClinicalAiState.fromJson({
      'draft': {'symptoms': 'Fièvre depuis trois jours'},
      'transcript': 'Le patient présente une fièvre depuis trois jours.',
      'assistantMessage': 'Deux éléments sont proposés.',
      'needsClarification': false,
      'revisions': [
        {
          'id': 'revision-1',
          'sequence': 1,
          'status': 'PENDING',
          'proposals': [
            {
              'id': 'proposal-1',
              'field': 'diagnosis',
              'operation': 'SET',
              'previousValue': null,
              'proposedValue': 'Syndrome fébrile',
              'reason': 'Formulation clinique fidèle',
              'uncertainty': 'LOW',
              'status': 'PENDING',
            },
            {
              'id': 'proposal-2',
              'field': 'vitals',
              'operation': 'SET',
              'previousValue': null,
              'proposedValue': jsonEncode({'temperature': 38.5, 'pulse': 92}),
              'reason': 'Valeurs explicitement dictées',
              'uncertainty': 'LOW',
              'status': 'PENDING',
            },
          ],
        },
      ],
    });

    expect(state.hasPendingProposals, isTrue);
    expect(state.noteFrom().diagnosis, isNull);
    expect(state.noteFrom(includePending: true).diagnosis, 'Syndrome fébrile');
    expect(state.vitalsFrom().isEmpty, isTrue);
    expect(state.vitalsFrom(includePending: true).temperature, 38.5);
    expect(state.vitalsFrom(includePending: true).pulse, 92);
  });

  test('server-accepted proposals remain applicable', () {
    final state = ClinicalAiState.fromJson({
      'draft': {
        'symptoms': 'Céphalée aiguë depuis trois jours',
        'diagnosis': 'Céphalée aiguë à explorer',
      },
      'transcript': 'Céphalée aiguë depuis trois jours.',
      'needsClarification': false,
      'revisions': [
        {
          'id': 'revision-1',
          'sequence': 1,
          'status': 'DECIDED',
          'proposals': [
            {
              'id': 'proposal-1',
              'field': 'diagnosis',
              'operation': 'SET',
              'previousValue': null,
              'proposedValue': 'Céphalée aiguë à explorer',
              'reason': 'Accepté par le médecin',
              'uncertainty': 'LOW',
              'status': 'ACCEPTED',
            },
          ],
        },
      ],
    });

    expect(state.hasPendingProposals, isFalse);
    expect(state.hasAcceptedChanges, isTrue);
    expect(state.noteFrom().diagnosis, 'Céphalée aiguë à explorer');
  });

  test('partial AI vitals never erase other explicitly dictated values', () {
    const explicit = PatientVitals(
      temperature: 38.5,
      pulse: 92,
      systolic: 150,
      diastolic: 90,
    );
    const ai = PatientVitals(pulse: 88);

    final merged = explicit.mergePrefer(ai);

    expect(merged.temperature, 38.5);
    expect(merged.pulse, 88);
    expect(merged.systolic, 150);
    expect(merged.diastolic, 90);
  });

  test('an explicitly confirmed vital replaces the displayed form value', () {
    final controller = TextEditingController(text: '37');
    addTearDown(controller.dispose);

    final changed = applyAcceptedVitalValue(controller, 38.5);

    expect(changed, 1);
    expect(controller.text, '38.5');
  });

  test('AI session payload restores a pending reviewed transcript', () {
    final state = ClinicalAiState.fromJson({
      'draft': <String, String>{},
      'pendingTranscript': 'Température trente-huit virgule cinq.',
      'transcriptStatus': 'PENDING_REVIEW',
      'revisions': <Object>[],
      'needsClarification': false,
    });

    expect(state.pendingTranscript, 'Température trente-huit virgule cinq.');
    expect(state.transcriptStatus, 'PENDING_REVIEW');
  });
}
