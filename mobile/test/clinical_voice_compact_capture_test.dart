import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/application/clinical_voice_state.dart';
import 'package:joprelys_mobile/features/dashboard/domain/consultation_note.dart';
import 'package:joprelys_mobile/features/dashboard/domain/patient_vitals.dart';
import 'package:joprelys_mobile/features/dashboard/presentation/widgets/clinical_voice_progressive_assistant_sections.dart';
import 'package:joprelys_mobile/l10n/app_localizations.dart';

void main() {
  testWidgets(
    'listening capture hides empty transcript chrome and reconnect banner',
    (tester) async {
      await tester.pumpWidget(
        _Harness(
          state: const RealtimeSpeechState(
            status: SpeechStatus.listening,
            stage: ClinicalVoiceStage.capture,
            transcript: '',
            segments: [],
            partialTranscript: '',
            partialOffset: Duration.zero,
            vitals: PatientVitals(),
            note: ConsultationNote(),
            revisions: [],
            errorMessage: 'Connexion vocale interrompue. Reconnexion (4/6)…',
          ),
        ),
      );

      expect(find.text('La dictée apparaîtra ici'), findsNothing);
      expect(
        find.textContaining('Chaque passage peut être relu'),
        findsNothing,
      );
      expect(find.textContaining('Reconnexion (4/6)'), findsNothing);
    },
  );

  testWidgets('listening capture shows transcript directly below microphone', (
    tester,
  ) async {
    const transcript = 'Bonjour madame, décrivez-moi ce qui vous amène.';
    await tester.pumpWidget(
      _Harness(
        state: const RealtimeSpeechState(
          status: SpeechStatus.listening,
          stage: ClinicalVoiceStage.capture,
          transcript: transcript,
          segments: [],
          partialTranscript: transcript,
          partialOffset: Duration(seconds: 2),
          vitals: PatientVitals(),
          note: ConsultationNote(),
          revisions: [],
        ),
      ),
    );

    expect(find.text(transcript), findsOneWidget);
    expect(find.text('La dictée apparaîtra ici'), findsNothing);
    expect(find.textContaining('Chaque passage peut être relu'), findsNothing);
  });
}

class _Harness extends StatefulWidget {
  const _Harness({required this.state});

  final RealtimeSpeechState state;

  @override
  State<_Harness> createState() => _HarnessState();
}

class _HarnessState extends State<_Harness> with TickerProviderStateMixin {
  late final AnimationController _haloController;
  late final AnimationController _waveController;

  @override
  void initState() {
    super.initState();
    _haloController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1800),
    );
    _waveController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 850),
    );
  }

  @override
  void dispose() {
    _haloController.dispose();
    _waveController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      locale: const Locale('fr'),
      supportedLocales: AppLocalizations.supportedLocales,
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: Scaffold(
        body: SizedBox(
          width: 390,
          height: 800,
          child: ClinicalVoiceProgressiveCaptureBody(
            state: widget.state,
            haloController: _haloController,
            waveController: _waveController,
            onToggleListening: () {},
            onClearAll: () {},
            onFinalize: () {},
            onRetrySynchronization: () {},
            onSegmentChanged: (_, _) async => true,
            onSegmentDeleted: (_) async => true,
          ),
        ),
      ),
    );
  }
}
