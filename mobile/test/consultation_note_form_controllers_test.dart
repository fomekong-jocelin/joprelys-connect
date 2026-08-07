import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/domain/consultation_note.dart';
import 'package:joprelys_mobile/features/dashboard/presentation/widgets/consultation_note_form_controllers.dart';

void main() {
  test('accepted voice extras survive until explicit save orchestration', () {
    final controllers = ConsultationNoteFormControllers();
    addTearDown(controllers.dispose);

    controllers.applyAcceptedDraft(
      const ConsultationNote(
        symptoms: 'Toux sèche',
        prescriptions: '[{"drugName":"inhalateur"}]',
        labOrders: '["numération complète","radiographie du thorax"]',
      ),
    );

    expect(controllers.acceptedPrescriptions, '[{"drugName":"inhalateur"}]');
    expect(
      controllers.acceptedLabOrders,
      '["numération complète","radiographie du thorax"]',
    );
    expect(controllers.hasAcceptedStructuredExtras, isTrue);
    expect(
      controllers.toAiDraft(),
      containsPair('prescription', '[{"drugName":"inhalateur"}]'),
    );
    expect(
      controllers.toAiDraft(),
      containsPair(
        'labOrders',
        '["numération complète","radiographie du thorax"]',
      ),
    );

    final soap = controllers.toConsultationNote();
    expect(soap.symptoms, 'Toux sèche');
    expect(soap.prescriptions, isNull);
    expect(soap.labOrders, isNull);
  });

  test('a newly accepted result can clear stale structured extras', () {
    final controllers = ConsultationNoteFormControllers();
    addTearDown(controllers.dispose);

    controllers.applyAcceptedDraft(
      const ConsultationNote(
        symptoms: 'Toux sèche',
        prescriptions: '[{"drugName":"inhalateur"}]',
        labOrders: '["radiographie du thorax"]',
      ),
    );
    controllers.applyAcceptedDraft(
      const ConsultationNote(symptoms: 'Toux sèche'),
    );

    expect(controllers.acceptedPrescriptions, isNull);
    expect(controllers.acceptedLabOrders, isNull);
  });
}
