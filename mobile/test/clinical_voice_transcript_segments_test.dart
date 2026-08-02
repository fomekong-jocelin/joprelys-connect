import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/application/clinical_voice_state.dart';
import 'package:joprelys_mobile/features/dashboard/presentation/widgets/clinical_voice_segment_timeline.dart';

void main() {
  group('clinical transcript segments', () {
    test('rebuilds the reviewed transcript in chronological order', () {
      final transcript = clinicalTranscriptFromSegments(
        const <ClinicalTranscriptSegment>[
          ClinicalTranscriptSegment(
            id: 'segment-1',
            offset: Duration(seconds: 2),
            text: 'Le patient présente une fièvre',
          ),
          ClinicalTranscriptSegment(
            id: 'segment-2',
            offset: Duration(seconds: 14),
            text: 'La température est à 38,5 degrés',
          ),
        ],
      );

      expect(
        transcript,
        'Le patient présente une fièvre. La température est à 38,5 degrés',
      );
    });

    test('ignores blank segments after an edit', () {
      final transcript = clinicalTranscriptFromSegments(
        const <ClinicalTranscriptSegment>[
          ClinicalTranscriptSegment(
            id: 'segment-1',
            offset: Duration.zero,
            text: '   ',
          ),
          ClinicalTranscriptSegment(
            id: 'segment-2',
            offset: Duration(seconds: 5),
            text: 'Auscultation pulmonaire normale',
          ),
        ],
      );

      expect(transcript, 'Auscultation pulmonaire normale');
    });

    test('formats segment offsets for short and long consultations', () {
      expect(formatClinicalTranscriptOffset(Duration.zero), '00:00');
      expect(
        formatClinicalTranscriptOffset(const Duration(minutes: 4, seconds: 9)),
        '04:09',
      );
      expect(
        formatClinicalTranscriptOffset(
          const Duration(hours: 1, minutes: 2, seconds: 3),
        ),
        '01:02:03',
      );
    });
  });
}
