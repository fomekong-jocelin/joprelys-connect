import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/application/clinical_speech_hypothesis.dart';

void main() {
  group('progressive clinical speech hypotheses', () {
    test('keeps the longest cumulative hypothesis', () {
      final first = mergeClinicalSpeechHypothesis(
        'Le patient présente une toux sèche persistante',
        'Le patient présente une toux sèche',
      );

      expect(first.startsNewSegment, isFalse);
      expect(first.text, 'Le patient présente une toux sèche persistante');
    });

    test('extends the current hypothesis without losing previous words', () {
      final result = mergeClinicalSpeechHypothesis(
        'Le patient présente une toux sèche',
        'Le patient présente une toux sèche surtout le soir',
      );

      expect(result.startsNewSegment, isFalse);
      expect(result.text, 'Le patient présente une toux sèche surtout le soir');
    });

    test('merges an overlapping recognition window', () {
      final result = mergeClinicalSpeechHypothesis(
        'Il travaille dans un entrepôt avec beaucoup de poussière',
        'beaucoup de poussière surtout quand on déplace les cartons',
      );

      expect(result.startsNewSegment, isFalse);
      expect(
        result.text,
        'Il travaille dans un entrepôt avec beaucoup de poussière surtout quand on déplace les cartons',
      );
    });

    test('starts a new segment for a genuinely different passage', () {
      final result = mergeClinicalSpeechHypothesis(
        'La toux est plus importante le soir',
        'À l auscultation il existe un sifflement à droite',
      );

      expect(result.startsNewSegment, isTrue);
      expect(result.text, 'À l auscultation il existe un sifflement à droite');
    });

    test('removes a cumulative replay of committed speech', () {
      expect(
        stripCommittedClinicalTranscriptPrefix(
          'Le patient tousse depuis trois jours. Il ne présente pas de fièvre',
          'Le patient tousse depuis trois jours.',
        ),
        'Il ne présente pas de fièvre',
      );
    });

    test('ignores an exact replay of the committed transcript', () {
      expect(
        stripCommittedClinicalTranscriptPrefix(
          'Le patient tousse depuis trois jours.',
          'Le patient tousse depuis trois jours.',
        ),
        isEmpty,
      );
    });
  });
}
