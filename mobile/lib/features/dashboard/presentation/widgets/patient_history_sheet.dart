import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../l10n/app_localizations.dart';
import '../../../auth/application/effective_access_controller.dart';
import '../../../foundation/presentation/mobile_workspace_localizations.dart';
import '../../domain/active_visit.dart';
import '../../domain/patient_record.dart';
import 'patient_record_page.dart';

/// Compatibility entry point used by active-queue cards.
///
/// The former two-tab bottom sheet has been replaced by the same full-screen,
/// permission-aware patient record used by the Patients destination.
class PatientHistorySheet extends StatelessWidget {
  const PatientHistorySheet({required this.visit, super.key});

  final ActiveVisit visit;

  static Future<void> show(BuildContext context, {required ActiveVisit visit}) {
    return Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        fullscreenDialog: true,
        builder: (_) => _PatientRecordAccessGate(visit: visit),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return _PatientRecordAccessGate(visit: visit);
  }
}

class _PatientRecordAccessGate extends ConsumerWidget {
  const _PatientRecordAccessGate({required this.visit});

  final ActiveVisit visit;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final access = ref.watch(effectiveAccessProvider);
    return access.when(
      loading: () =>
          const Scaffold(body: Center(child: CircularProgressIndicator())),
      error: (error, stackTrace) => Scaffold(
        appBar: AppBar(
          leading: IconButton(
            onPressed: () => Navigator.of(context).pop(),
            icon: const Icon(Icons.arrow_back_rounded),
          ),
          title: Text(AppLocalizations.of(context).recordTitle),
        ),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: FilledButton.icon(
              onPressed: () => refreshEffectiveAccess(ref),
              icon: const Icon(Icons.refresh_rounded),
              label: Text(AppLocalizations.of(context).recordRefresh),
            ),
          ),
        ),
      ),
      data: (effectiveAccess) => PatientRecordPage(
        target: PatientRecordTarget(
          patientId: visit.patientId,
          patientName: visit.patientName,
          patientDpu: visit.patientDpu,
          activeVisit: visit,
        ),
        access: effectiveAccess,
      ),
    );
  }
}
