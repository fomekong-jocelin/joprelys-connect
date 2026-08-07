import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/core/config/app_config.dart';
import 'package:joprelys_mobile/features/dashboard/data/consultation_api.dart';
import 'package:joprelys_mobile/features/dashboard/domain/active_visit.dart';
import 'package:joprelys_mobile/features/dashboard/domain/consultation_note.dart';
import 'package:joprelys_mobile/features/dashboard/presentation/widgets/consultation_notes_sheet.dart';
import 'package:joprelys_mobile/l10n/app_localizations.dart';

void main() {
  testWidgets(
    'SOAP header stays fixed while the reason scrolls with the form',
    (tester) async {
      tester.view.devicePixelRatio = 1;
      tester.view.physicalSize = const Size(393, 852);
      addTearDown(tester.view.resetDevicePixelRatio);
      addTearDown(tester.view.resetPhysicalSize);

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            consultationApiProvider.overrideWithValue(
              const _FakeConsultationGateway(),
            ),
          ],
          child: MaterialApp(
            locale: const Locale('fr'),
            supportedLocales: AppConfig.supportedLocales,
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            home: Scaffold(body: ConsultationNotesSheet(visit: _visit)),
          ),
        ),
      );
      await tester.pumpAndSettle();

      final header = find.byKey(const ValueKey('consultation-fixed-header'));
      final reason = find.byKey(
        const ValueKey('consultation-scrollable-reason'),
      );
      final headerTop = tester.getTopLeft(header);
      final reasonTop = tester.getTopLeft(reason);

      await tester.drag(
        find.byType(SingleChildScrollView),
        const Offset(0, -320),
      );
      await tester.pumpAndSettle();

      expect(tester.getTopLeft(header), headerTop);
      expect(tester.getTopLeft(reason).dy, lessThan(reasonTop.dy));
    },
  );
}

final _visit = ActiveVisit(
  id: 'visit-1',
  visitNumber: 'VIS-20260801-001',
  patientId: 'patient-1',
  patientName: 'Patient Test',
  patientDpu: 'DPU-TEST-001',
  reason: 'Motif de consultation',
  orientation: 'Médecine générale',
  status: 'ACTIVE',
  createdAt: DateTime.utc(2026, 8, 1, 12),
);

final class _FakeConsultationGateway implements ConsultationGateway {
  const _FakeConsultationGateway();

  @override
  Future<ConsultationNote?> getConsultationNote(String visitId) async => null;

  @override
  Future<SavedConsultationNote> saveConsultationNote(
    String visitId,
    ConsultationNote note,
  ) async {
    return SavedConsultationNote(consultationId: 'consultation-1', note: note);
  }
}
