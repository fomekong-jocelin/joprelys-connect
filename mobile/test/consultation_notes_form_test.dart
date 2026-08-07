import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/presentation/widgets/consultation_note_form_controllers.dart';
import 'package:joprelys_mobile/features/dashboard/presentation/widgets/consultation_notes_form.dart';
import 'package:joprelys_mobile/l10n/app_localizations.dart';

void main() {
  testWidgets('requires history but allows a draft without diagnosis', (
    tester,
  ) async {
    final formKey = GlobalKey<FormState>();
    final controllers = ConsultationNoteFormControllers();
    addTearDown(controllers.dispose);

    await tester.pumpWidget(
      _TestApp(
        locale: const Locale('fr'),
        child: ConsultationNotesForm(
          formKey: formKey,
          controllers: controllers,
        ),
      ),
    );

    expect(find.byType(TextFormField), findsNWidgets(6));
    expect(formKey.currentState?.validate(), isFalse);
    await tester.pump();
    expect(find.text('L’histoire de la maladie est requise.'), findsOneWidget);
    expect(find.text('Le diagnostic est requis.'), findsNothing);

    await tester.enterText(
      find.byType(TextFormField).at(0),
      'Fatigue depuis trois semaines et toux sèche.',
    );

    expect(formKey.currentState?.validate(), isTrue);
    expect(
      controllers.toConsultationNote().symptoms,
      'Fatigue depuis trois semaines et toux sèche.',
    );
    expect(controllers.toConsultationNote().diagnosis, isEmpty);
  });

  testWidgets('renders the same SOAP sections in English', (tester) async {
    final formKey = GlobalKey<FormState>();
    final controllers = ConsultationNoteFormControllers();
    addTearDown(controllers.dispose);

    await tester.pumpWidget(
      _TestApp(
        locale: const Locale('en'),
        child: ConsultationNotesForm(
          formKey: formKey,
          controllers: controllers,
        ),
      ),
    );

    expect(find.textContaining('S — Subjective'), findsOneWidget);
    expect(find.textContaining('O — Objective'), findsOneWidget);
    expect(find.textContaining('A — Assessment'), findsOneWidget);
    expect(find.textContaining('P — Plan'), findsOneWidget);
  });
}

class _TestApp extends StatelessWidget {
  const _TestApp({required this.locale, required this.child});

  final Locale locale;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      locale: locale,
      supportedLocales: AppLocalizations.supportedLocales,
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: Scaffold(body: SingleChildScrollView(child: child)),
    );
  }
}
