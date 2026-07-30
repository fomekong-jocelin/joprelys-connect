import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/domain/consultation_note.dart';

void main() {
  group('ConsultationNote', () {
    test('serializes and deserializes JSON correctly', () {
      final json = <String, dynamic>{
        'subjective': 'Anamnèse patient avec douleurs thoraciques',
        'objective': 'Auscultation pulmonaire normale, ECG normal',
        'assessment': 'Syndrome douloureux thoracique atypique',
        'plan': 'NFS, Troponine, Surveillance 4h',
        'updatedAt': '2026-07-30T22:00:00.000Z',
      };

      final note = ConsultationNote.fromJson(json);
      expect(note.subjective, 'Anamnèse patient avec douleurs thoraciques');
      expect(note.objective, 'Auscultation pulmonaire normale, ECG normal');
      expect(note.assessment, 'Syndrome douloureux thoracique atypique');
      expect(note.plan, 'NFS, Troponine, Surveillance 4h');
      expect(note.isEmpty, false);

      final serialized = note.toJson();
      expect(serialized['subjective'], 'Anamnèse patient avec douleurs thoraciques');
      expect(serialized['plan'], 'NFS, Troponine, Surveillance 4h');
    });

    test('detects empty notes correctly', () {
      const emptyNote = ConsultationNote();
      expect(emptyNote.isEmpty, true);

      const filledNote = ConsultationNote(subjective: 'Fièvre isolée');
      expect(filledNote.isEmpty, false);
    });
  });
}
