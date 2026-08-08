import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/domain/prescription.dart';
import 'package:joprelys_mobile/features/dashboard/presentation/widgets/prescription_item_form.dart';
import 'package:joprelys_mobile/l10n/app_localizations.dart';

void main() {
  testWidgets('draft medication stays compact and accepts an empty dosage', (
    tester,
  ) async {
    final formKey = GlobalKey<FormState>();
    final controllers = PrescriptionItemControllers.fromItem(
      const PrescriptionItem(drugName: 'Inhalateur'),
    );
    addTearDown(controllers.dispose);

    await tester.binding.setSurfaceSize(const Size(390, 800));
    addTearDown(() => tester.binding.setSurfaceSize(null));

    await tester.pumpWidget(
      _Harness(
        formKey: formKey,
        controllers: controllers,
        requireDosage: false,
      ),
    );

    expect(find.text('Inhalateur'), findsWidgets);
    expect(find.text('Détails complémentaires'), findsOneWidget);
    expect(find.text('Forme'), findsNothing);
    expect(formKey.currentState?.validate(), isTrue);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'finalization requires dosage and secondary fields stay progressive',
    (tester) async {
      final formKey = GlobalKey<FormState>();
      final controllers = PrescriptionItemControllers.fromItem(
        const PrescriptionItem(drugName: 'Inhalateur'),
      );
      addTearDown(controllers.dispose);

      await tester.binding.setSurfaceSize(const Size(390, 800));
      addTearDown(() => tester.binding.setSurfaceSize(null));

      await tester.pumpWidget(
        _Harness(
          formKey: formKey,
          controllers: controllers,
          requireDosage: true,
        ),
      );

      expect(formKey.currentState?.validate(), isFalse);
      await tester.pump();
      expect(
        find.text('Complétez le dosage avant de finaliser.'),
        findsOneWidget,
      );

      await tester.tap(find.text('Détails complémentaires'));
      await tester.pumpAndSettle();
      expect(find.text('Forme'), findsOneWidget);
      expect(find.text('Voie'), findsOneWidget);
      expect(find.text('Fréquence'), findsOneWidget);
      expect(find.text('Instructions'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );
}

class _Harness extends StatelessWidget {
  const _Harness({
    required this.formKey,
    required this.controllers,
    required this.requireDosage,
  });

  final GlobalKey<FormState> formKey;
  final PrescriptionItemControllers controllers;
  final bool requireDosage;

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
        body: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Form(
            key: formKey,
            child: PrescriptionItemForm(
              index: 0,
              controllers: controllers,
              editable: true,
              requireDosage: requireDosage,
              initiallyExpanded: true,
              onChanged: () {},
              onRemove: () {},
            ),
          ),
        ),
      ),
    );
  }
}
