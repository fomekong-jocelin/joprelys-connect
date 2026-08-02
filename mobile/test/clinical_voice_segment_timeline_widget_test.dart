import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/core/config/app_config.dart';
import 'package:joprelys_mobile/features/dashboard/application/clinical_voice_state.dart';
import 'package:joprelys_mobile/features/dashboard/presentation/widgets/clinical_voice_segment_timeline.dart';
import 'package:joprelys_mobile/l10n/app_localizations.dart';

void main() {
  testWidgets('segment actions remain usable on a narrow mobile screen', (
    tester,
  ) async {
    tester.view.devicePixelRatio = 1;
    tester.view.physicalSize = const Size(320, 700);
    addTearDown(tester.view.resetDevicePixelRatio);
    addTearDown(tester.view.resetPhysicalSize);

    String? savedText;
    String? deletedId;

    await tester.pumpWidget(
      MaterialApp(
        locale: const Locale('fr'),
        supportedLocales: AppConfig.supportedLocales,
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        home: Scaffold(
          body: SingleChildScrollView(
            padding: const EdgeInsets.all(12),
            child: ClinicalTranscriptTimeline(
              segments: const <ClinicalTranscriptSegment>[
                ClinicalTranscriptSegment(
                  id: 'segment-1',
                  offset: Duration(seconds: 12),
                  text:
                      'Le patient présente une toux sèche persistante surtout le soir.',
                ),
              ],
              partialTranscript: '',
              partialOffset: Duration.zero,
              editable: true,
              onSegmentChanged: (segmentId, text) async {
                savedText = text;
                return true;
              },
              onSegmentDeleted: (segmentId) async {
                deletedId = segmentId;
                return true;
              },
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byTooltip('Corriger'), findsOneWidget);
    expect(find.byTooltip('Supprimer'), findsOneWidget);

    await tester.tap(find.byTooltip('Corriger'));
    await tester.pumpAndSettle();
    await tester.enterText(
      find.byType(TextField),
      'Toux sèche corrigée et persistante le soir.',
    );
    await tester.tap(find.text('Enregistrer'));
    await tester.pumpAndSettle();

    expect(savedText, 'Toux sèche corrigée et persistante le soir.');
    expect(tester.takeException(), isNull);

    await tester.tap(find.byTooltip('Supprimer'));
    await tester.pump();

    expect(deletedId, 'segment-1');
    expect(tester.takeException(), isNull);
  });
}
