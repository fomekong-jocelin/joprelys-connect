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

    test('keeps screenshot passages 28 to 30 as one growing passage', () {
      var current = 'un peu quand je monte les escaliers';

      final passage29 = mergeClinicalSpeechHypothesis(
        current,
        "un peu quand je monte les escaliers j'ai",
        currentFinalized: true,
      );
      expect(passage29.startsNewSegment, isFalse);
      current = passage29.text;

      final passage30 = mergeClinicalSpeechHypothesis(
        current,
        "un peu quand je monte les escaliers j'ai besoin de reprendre",
        currentFinalized: true,
      );
      expect(passage30.startsNewSegment, isFalse);
      expect(
        passage30.text,
        "un peu quand je monte les escaliers j'ai besoin de reprendre",
      );
    });

    test('keeps screenshot passages 49 to 51 as one growing passage', () {
      var current =
          "pouvez-vous vous asseoir sur la table d'examen je vais d'abord prendre votre tension votre";

      final passage50 = mergeClinicalSpeechHypothesis(
        current,
        "pouvez-vous vous asseoir sur la table d'examen je vais d'abord prendre votre tension votre température",
        currentFinalized: true,
      );
      expect(passage50.startsNewSegment, isFalse);
      current = passage50.text;

      final passage51 = mergeClinicalSpeechHypothesis(
        current,
        "pouvez-vous vous asseoir sur la table d'examen je vais d'abord prendre votre tension votre température et votre",
        currentFinalized: true,
      );
      expect(passage51.startsNewSegment, isFalse);
      expect(
        passage51.text,
        "pouvez-vous vous asseoir sur la table d'examen je vais d'abord prendre votre tension votre température et votre",
      );
    });

    test('does not lose words when Android resets a non-final partial', () {
      final result = mergeClinicalSpeechHypothesis(
        'température est normal votre tension est à 12 8 et votre saturation en',
        'oxygène',
        currentFinalized: false,
      );

      expect(result.startsNewSegment, isFalse);
      expect(
        result.text,
        'température est normal votre tension est à 12 8 et votre saturation en oxygène',
      );
    });

    test('accepts a small correction inside a non-final short hypothesis', () {
      final result = mergeClinicalSpeechHypothesis(
        'oui',
        'non',
        currentFinalized: false,
      );

      expect(result.startsNewSegment, isFalse);
      expect(result.text, 'non');
    });

    test('starts a new segment for a finalized short answer', () {
      final result = mergeClinicalSpeechHypothesis(
        'avez-vous une douleur',
        'non',
        currentFinalized: true,
      );

      expect(result.startsNewSegment, isTrue);
      expect(result.text, 'non');
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
