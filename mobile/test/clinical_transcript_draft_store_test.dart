import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/application/clinical_voice_state.dart';
import 'package:joprelys_mobile/features/dashboard/data/clinical_transcript_draft_store.dart';

void main() {
  group('clinical transcript draft codec', () {
    test('round-trips v3 segments, offsets and the live passage', () {
      final draft = ClinicalTranscriptDraft(
        segments: const <ClinicalTranscriptSegment>[
          ClinicalTranscriptSegment(
            id: 'segment-1',
            offset: Duration(seconds: 4),
            text: 'Le patient tousse depuis trois jours',
          ),
          ClinicalTranscriptSegment(
            id: 'segment-2',
            offset: Duration(seconds: 12),
            text: 'Il ne présente pas de fièvre',
          ),
        ],
        partialTranscript: 'la saturation est à 98 pour cent',
        partialOffset: const Duration(seconds: 18),
        explicitlyCleared: false,
      );

      final encoded = encodeClinicalTranscriptDraft(draft);
      final decoded = Map<String, dynamic>.from(
        jsonDecode(encoded) as Map,
      );
      final restored = decodeClinicalTranscriptDraft(encoded);

      expect(decoded['version'], clinicalTranscriptDraftVersion);
      expect(restored.explicitlyCleared, isFalse);
      expect(restored.segments, hasLength(2));
      expect(restored.segments.first.id, 'segment-1');
      expect(restored.segments.first.offset, const Duration(seconds: 4));
      expect(restored.segments.last.text, 'Il ne présente pas de fièvre');
      expect(restored.partialTranscript, 'la saturation est à 98 pour cent');
      expect(restored.partialOffset, const Duration(seconds: 18));
    });

    test('preserves an explicit empty draft after delete all', () {
      const draft = ClinicalTranscriptDraft(
        segments: <ClinicalTranscriptSegment>[],
        partialTranscript: '',
        partialOffset: Duration.zero,
        explicitlyCleared: true,
      );

      final restored = decodeClinicalTranscriptDraft(
        encodeClinicalTranscriptDraft(draft),
      );

      expect(restored.hasContent, isFalse);
      expect(restored.explicitlyCleared, isTrue);
    });

    test('rejects a legacy v2 draft so stale text cannot be restored', () {
      expect(
        () => decodeClinicalTranscriptDraft(
          jsonEncode(<String, Object?>{
            'version': 2,
            'explicitlyCleared': false,
            'partialTranscript': '',
            'partialOffsetMs': 0,
            'segments': <Map<String, Object?>>[
              <String, Object?>{
                'id': 'old-segment',
                'offsetMs': 0,
                'text': 'Ancien transcript déjà appliqué',
              },
            ],
          }),
        ),
        throwsFormatException,
      );
    });

    test('rejects a non-object payload', () {
      expect(
        () => decodeClinicalTranscriptDraft('["invalid"]'),
        throwsFormatException,
      );
    });
  });
}
