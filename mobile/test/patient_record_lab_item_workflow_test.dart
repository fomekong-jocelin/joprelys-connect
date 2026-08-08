import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/auth/domain/effective_access.dart';
import 'package:joprelys_mobile/features/dashboard/domain/patient_history.dart';
import 'package:joprelys_mobile/features/dashboard/domain/patient_record.dart';
import 'package:joprelys_mobile/features/dashboard/presentation/widgets/patient_record_laboratory_item_section.dart';
import 'package:joprelys_mobile/l10n/app_localizations.dart';

void main() {
  testWidgets('one request exposes independent exam states and linked result', (
    tester,
  ) async {
    tester.view.devicePixelRatio = 1;
    tester.view.physicalSize = const Size(390, 844);
    addTearDown(tester.view.resetDevicePixelRatio);
    addTearDown(tester.view.resetPhysicalSize);

    final record = PatientRecordBundle(
      identity: const PatientIdentity(
        id: 'patient-1',
        fullName: 'Patient Test',
        globalPatientNumber: 'DPU-001',
        localPatientNumber: 'PAT-001',
        status: 'ACTIVE',
      ),
      history: const PatientMedicalHistory(
        patientId: 'patient-1',
        patientName: 'Patient Test',
        patientDpu: 'DPU-001',
      ),
      labOrders: [
        PatientLabOrderSummary(
          id: 'order-1',
          number: 'EXAM-REQ-20260808-000001',
          status: 'RESULT_AVAILABLE',
          exams: const ['NFS', 'CRP', 'Radiographie du thorax'],
          items: const [
            PatientLabOrderItemSummary(
              id: 'item-nfs',
              examName: 'NFS',
              status: 'VALIDATED',
            ),
            PatientLabOrderItemSummary(
              id: 'item-crp',
              examName: 'CRP',
              status: 'IN_PROGRESS',
            ),
            PatientLabOrderItemSummary(
              id: 'item-radio',
              examName: 'Radiographie du thorax',
              status: 'REQUESTED',
            ),
          ],
          practitioner: 'Dr Test',
          reason: 'Fatigue persistante',
          createdAt: DateTime(2026, 8, 8),
        ),
      ],
      labResults: const [
        PatientLabResultSummary(
          id: 'result-1',
          resultNumber: 'EXAM-RES-20260808-000001',
          examRequestNumber: 'EXAM-REQ-20260808-000001',
          labOrderItemId: 'item-nfs',
          examName: 'NFS',
          status: 'VALIDATED',
          analyte: 'Hémoglobine',
          value: '13.4',
          unit: 'g/dL',
        ),
      ],
    );

    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          locale: const Locale('fr'),
          supportedLocales: AppLocalizations.supportedLocales,
          localizationsDelegates: const [
            AppLocalizations.delegate,
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
          home: Scaffold(
            body: PatientRecordLaboratoryItemSection(
              record: record,
              access: const EffectiveAccess(
                userId: 'doctor-1',
                permissions: {'LAB_ORDER_READ'},
              ),
              onChanged: _noop,
            ),
          ),
        ),
      ),
    );

    expect(find.text('NFS'), findsOneWidget);
    expect(find.text('CRP'), findsOneWidget);
    expect(find.text('Validé'), findsWidgets);
    expect(find.text('En analyse'), findsWidgets);
    expect(find.text('Radiographie du thorax'), findsNothing);
    expect(tester.takeException(), isNull);

    await tester.tap(find.text('EXAM-REQ-20260808-000001'));
    await tester.pumpAndSettle();

    expect(find.text('Radiographie du thorax'), findsOneWidget);
    expect(find.text('Hémoglobine'), findsOneWidget);
    expect(find.text('13.4 g/dL'), findsOneWidget);
    expect(find.text('Fatigue persistante'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}

void _noop() {}
