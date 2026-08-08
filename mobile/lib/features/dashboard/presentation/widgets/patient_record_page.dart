import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../auth/domain/effective_access.dart';
import '../../../foundation/presentation/mobile_workspace_localizations.dart';
import '../../data/patient_record_api.dart';
import '../../domain/patient_record.dart';
import 'patient_record_navigation.dart';
import 'patient_record_section.dart';
import 'patient_record_states.dart';

export 'patient_record_section.dart';

class PatientRecordPage extends ConsumerStatefulWidget {
  const PatientRecordPage({
    required this.target,
    required this.access,
    super.key,
  });

  final PatientRecordTarget target;
  final EffectiveAccess access;

  static Future<void> show(
    BuildContext context, {
    required PatientRecordTarget target,
    required EffectiveAccess access,
  }) {
    return Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        fullscreenDialog: true,
        builder: (_) => PatientRecordPage(target: target, access: access),
      ),
    );
  }

  @override
  ConsumerState<PatientRecordPage> createState() => _PatientRecordPageState();
}

class _PatientRecordPageState extends ConsumerState<PatientRecordPage> {
  PatientRecordBundle? _record;
  PatientRecordSection _section = PatientRecordSection.overview;
  bool _loading = true;
  bool _consentRequired = false;
  bool _emergencyLoading = false;
  String? _error;
  String? _emergencyError;
  final _emergencyReasonController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _emergencyReasonController.dispose();
    super.dispose();
  }

  List<PatientRecordSection> get _availableSections {
    final sections = <PatientRecordSection>[PatientRecordSection.overview];
    if (widget.access.hasPermission('CLINICAL_READ')) {
      sections.addAll(const [
        PatientRecordSection.medical,
        PatientRecordSection.consultations,
        PatientRecordSection.prescriptions,
      ]);
    }
    if (widget.access.hasPermission('LAB_ORDER_READ')) {
      sections.add(PatientRecordSection.laboratory);
    }
    if (widget.access.hasPermission('HOSPITALIZATION_READ')) {
      sections.add(PatientRecordSection.hospitalizations);
    }
    if (widget.access.hasPermission('AUDIT_READ')) {
      sections.add(PatientRecordSection.audit);
    }
    return sections;
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _emergencyLoading = false;
      _error = null;
      _consentRequired = false;
      _emergencyError = null;
    });
    try {
      final record = await ref
          .read(patientRecordApiProvider)
          .getRecord(target: widget.target, access: widget.access);
      if (!mounted) return;
      setState(() {
        _record = record;
        _loading = false;
      });
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _consentRequired = error.statusCode == 403;
        _error = error.statusCode == 403 ? null : error.message;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = AppLocalizations.of(context).recordLoadError;
      });
    }
  }

  Future<void> _activateEmergencyAccess() async {
    final reason = _emergencyReasonController.text.trim();
    if (reason.isEmpty || _emergencyLoading) return;
    setState(() {
      _emergencyLoading = true;
      _emergencyError = null;
    });
    try {
      await ref
          .read(patientRecordApiProvider)
          .activateEmergencyAccess(
            patientId: widget.target.patientId,
            reason: reason,
          );
      if (!mounted) return;
      _emergencyReasonController.clear();
      await _load();
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _emergencyLoading = false;
        _emergencyError = error.message;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _emergencyLoading = false;
        _emergencyError = AppLocalizations.of(
          context,
        ).recordEmergencyAccessError;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;

    return Scaffold(
      backgroundColor: colors.surfaceContainerLowest,
      appBar: AppBar(
        surfaceTintColor: Colors.transparent,
        backgroundColor: colors.surface,
        leading: IconButton(
          tooltip: l10n.recordClose,
          onPressed: () => Navigator.of(context).pop(),
          icon: const Icon(Icons.arrow_back_rounded),
        ),
        titleSpacing: 0,
        title: Text(
          l10n.recordTitle,
          style: Theme.of(
            context,
          ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w900),
        ),
        actions: [
          IconButton(
            tooltip: l10n.recordRefresh,
            onPressed: _loading ? null : _load,
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      body: SafeArea(top: false, child: _body(l10n)),
    );
  }

  Widget _body(AppLocalizations l10n) {
    if (_loading) {
      return PatientRecordLoading(label: l10n.recordLoading);
    }
    if (_consentRequired) {
      return PatientConsentRequired(
        controller: _emergencyReasonController,
        loading: _emergencyLoading,
        error: _emergencyError,
        onSubmit: _activateEmergencyAccess,
      );
    }
    final record = _record;
    if (_error != null || record == null) {
      return PatientRecordError(message: _friendlyError(l10n), onRetry: _load);
    }
    return PatientRecordContent(
      record: record,
      sections: _availableSections,
      selected: _section,
      access: widget.access,
      onRecordChanged: _load,
      onSectionChanged: (section) {
        setState(() => _section = section);
      },
    );
  }

  String _friendlyError(AppLocalizations l10n) {
    final error = _error?.trim();
    if (error == null || error.isEmpty || error.startsWith('ApiException')) {
      return l10n.recordLoadError;
    }
    return error;
  }
}
