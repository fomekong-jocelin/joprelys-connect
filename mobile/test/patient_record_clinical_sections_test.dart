import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/auth/domain/effective_access.dart';
import 'package:joprelys_mobile/features/dashboard/domain/patient_history.dart';
import 'package:joprelys_mobile/features/dashboard/domain/patient_record.dart';
import 'package:joprelys_mobile/features/dashboard/presentation/widgets/patient_record_consultations_section.dart';
import 'package:joprelys_mobile/features/dashboard/presentation/widgets/patient_record_laboratory_section.dart';
import 'package:joprelys_mobile/l10n/app_localizations.dart';

void main() {
  testWidgets(
    'consultation card exposes full SOAP and prescription on expand',
    (tester) async {
      _usePhoneViewport(tester);
      final record = _record(
        visits: [
          PastVisitSummary(
            id: 'consultation-1',
            visitNumber: 'VIS-20260808-000001',
            date: DateTime.utc(2026, 8, 8),
            practitionerName: 'Dr Test',
            chiefComplaint: 'Fatigue depuis trois semaines',
            symptoms: 'Fatigue depuis trois semaines et toux sèche.',
            clinicalExam: 'Léger sifflement du côté droit.',
            diagnosis: '',
            conclusion: 'Bilan complémentaire demandé.',
            advice: 'Hydratation et repos.',
            followUp: 'Contrôle après les résultats.',
            status: 'BROUILLON',
            prescriptionItems: const [
              PatientPrescriptionItemSummary(
                drugName: 'Inhalateur',
                duration: '7 jours',
              ),
            ],
            temperature: 36.8,
            systolic: 120,
            diastolic: 80,
            pulse: 72,
          ),
        ],
      );

      await tester.pumpWidget(
        _TestApp(
          child: PatientRecordConsultationsDetailSection(record: record),
        ),
      );

      expect(find.text('VIS-20260808-000001'), findsOneWidget);
      expect(find.text('Dr Test'), findsOneWidget);
      expect(
        find.text('Fatigue depuis trois semaines et toux sèche.'),
        findsOneWidget,
      );
      expect(tester.takeException(), isNull);

      await tester.tap(
        find.text('Fatigue depuis trois semaines et toux sèche.'),
      );
      await tester.pumpAndSettle();

      expect(find.text('Léger sifflement du côté droit.'), findsOneWidget);
      expect(find.text('Bilan complémentaire demandé.'), findsOneWidget);
      expect(find.text('Hydratation et repos.'), findsOneWidget);
      expect(find.text('Contrôle après les résultats.'), findsOneWidget);
      expect(find.text('Inhalateur'), findsOneWidget);
      expect(find.text('7 jours'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('lab request links results in one compact tracking card', (
    tester,
  ) async {
    _usePhoneViewport(tester);
    final record = _record(
      labOrders: [
        PatientLabOrderSummary(
          id: 'order-1',
          number: 'EXAM-REQ-20260808-000001',
          status: 'RESULT_AVAILABLE',
          exams: const [
            'Numération complète',
            'Bilan inflammatoire',
            'Radiographie du thorax',
          ],
          createdAt: DateTime.utc(2026, 8, 8),
          priority: 'URGENTE',
          practitioner: 'Dr Test',
          examType: 'AUTRE',
          reason: 'Fatigue persistante',
        ),
      ],
      labResults: [
        PatientLabResultSummary(
          id: 'result-1',
          resultNumber: 'EXAM-RES-20260808-000001',
          examRequestNumber: 'EXAM-REQ-20260808-000001',
          status: 'DRAFT',
          analyte: 'CRP',
          value: '22',
          unit: 'mg/L',
          referenceRange: '< 5',
          interpretation: 'ELEVE',
          conclusion: 'Syndrome inflammatoire biologique',
          validatorName: 'Dr Biologiste',
          resultAt: DateTime.utc(2026, 8, 8, 10),
        ),
      ],
    );

    await tester.pumpWidget(
      _TestApp(
        child: PatientRecordLaboratoryDetailSection(
          record: record,
          access: _readOnlyAccess,
          onChanged: () {},
        ),
      ),
    );

    expect(find.text('EXAM-REQ-20260808-000001'), findsOneWidget);
    expect(find.text('Résultat disponible'), findsWidgets);
    expect(find.text('Numération complète'), findsOneWidget);
    expect(find.text('Bilan inflammatoire'), findsOneWidget);
    expect(find.text('Radiographie du thorax'), findsNothing);
    expect(find.text('1 résultat'), findsWidgets);
    expect(find.text('Prescrit'), findsOneWidget);
    expect(find.text('Prélevé'), findsOneWidget);
    expect(find.text('Analyse'), findsOneWidget);
    expect(find.text('Résultat'), findsOneWidget);
    expect(find.text('Validé'), findsOneWidget);
    expect(tester.takeException(), isNull);

    await tester.tap(find.text('EXAM-REQ-20260808-000001'));
    await tester.pumpAndSettle();

    expect(find.text('Radiographie du thorax'), findsOneWidget);
    expect(find.text('Fatigue persistante'), findsOneWidget);
    expect(find.text('EXAM-RES-20260808-000001'), findsOneWidget);
    expect(find.text('CRP'), findsOneWidget);
    expect(find.text('22 mg/L'), findsOneWidget);
    expect(find.text('Valeurs de référence: < 5'), findsOneWidget);
    expect(find.text('Syndrome inflammatoire biologique'), findsOneWidget);
    expect(find.text('Dr Biologiste'), findsOneWidget);
    expect(find.text('Prescrire un examen'), findsNothing);
    expect(find.text('Annuler la demande'), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets('lab create action follows LAB_ORDER_CREATE permission', (
    tester,
  ) async {
    _usePhoneViewport(tester);
    await tester.pumpWidget(
      _TestApp(
        child: PatientRecordLaboratoryDetailSection(
          record: _record(),
          access: const EffectiveAccess(
            userId: 'doctor-1',
            permissions: {'LAB_ORDER_READ', 'LAB_ORDER_CREATE'},
          ),
          onChanged: () {},
        ),
      ),
    );

    expect(find.text('Prescrire un examen'), findsOneWidget);
    expect(
      find.text('Aucune demande d’examen n’est enregistrée pour ce patient.'),
      findsOneWidget,
    );
    expect(tester.takeException(), isNull);
  });
}

const _readOnlyAccess = EffectiveAccess(
  userId: 'doctor-1',
  permissions: {'LAB_ORDER_READ'},
);

void _usePhoneViewport(WidgetTester tester) {
  tester.view.devicePixelRatio = 1;
  tester.view.physicalSize = const Size(390, 844);
  addTearDown(tester.view.resetDevicePixelRatio);
  addTearDown(tester.view.resetPhysicalSize);
}

PatientRecordBundle _record({
  List<PastVisitSummary> visits = const [],
  List<PatientLabOrderSummary> labOrders = const [],
  List<PatientLabResultSummary> labResults = const [],
}) {
  return PatientRecordBundle(
    identity: const PatientIdentity(
      id: 'patient-1',
      fullName: 'Patient Test',
      globalPatientNumber: 'DPU-001',
      localPatientNumber: 'PAT-001',
      status: 'ACTIVE',
    ),
    history: PatientMedicalHistory(
      patientId: 'patient-1',
      patientName: 'Patient Test',
      patientDpu: 'DPU-001',
      pastVisits: visits,
    ),
    labOrders: labOrders,
    labResults: labResults,
  );
}

class _TestApp extends StatelessWidget {
  const _TestApp({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return ProviderScope(
      child: MaterialApp(
        locale: const Locale('fr'),
        supportedLocales: AppLocalizations.supportedLocales,
        localizationsDelegates: const [
          AppLocalizations.delegate,
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        home: Scaffold(body: SizedBox.expand(child: child)),
      ),
    );
  }
}
