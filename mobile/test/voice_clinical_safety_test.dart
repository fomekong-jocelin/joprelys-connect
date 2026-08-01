import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/data/clinical_voice_ai_api.dart';
import 'package:joprelys_mobile/features/dashboard/domain/consultation_note.dart';
import 'package:joprelys_mobile/features/dashboard/presentation/widgets/consultation_note_form_controllers.dart';

void main() {
  test('accepted voice content never overwrites a manual consultation field', () {
    final controllers = ConsultationNoteFormControllers();
    addTearDown(controllers.dispose);

    controllers.symptoms.text = 'Saisie manuelle récente';

    final changed = controllers.applyAcceptedToEmptyFields(
      const ConsultationNote(
        symptoms: 'Proposition vocale obsolète',
        diagnosis: 'Paludisme simple',
      ),
    );

    expect(changed, isTrue);
    expect(controllers.symptoms.text, 'Saisie manuelle récente');
    expect(controllers.diagnosis.text, 'Paludisme simple');
    expect(controllers.toAiDraft()['symptoms'], 'Saisie manuelle récente');
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
              'proposedValue': jsonEncode({
                'temperature': 38.5,
                'pulse': 92,
              }),
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

  test('only server-accepted proposals unlock form application', () {
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
}
