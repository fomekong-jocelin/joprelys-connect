import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../auth/domain/effective_access.dart';
import '../../../foundation/presentation/mobile_workspace_localizations.dart';
import '../../data/patient_record_api.dart';
import '../../domain/patient_record.dart';
import 'patient_record_section.dart';
import 'patient_record_sections.dart';

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
      final record = await ref.read(patientRecordApiProvider).getRecord(
            target: widget.target,
            access: widget.access,
          );
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
      await ref.read(patientRecordApiProvider).activateEmergencyAccess(
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
        _emergencyError = AppLocalizations.of(context).recordEmergencyAccessError;
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
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w900,
              ),
        ),
        actions: [
          IconButton(
            tooltip: l10n.recordRefresh,
            onPressed: _loading ? null : _load,
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      body: SafeArea(
        top: false,
        child: _loading
            ? _RecordLoading(label: l10n.recordLoading)
            : _consentRequired
                ? _ConsentRequired(
                    controller: _emergencyReasonController,
                    loading: _emergencyLoading,
                    error: _emergencyError,
                    onSubmit: _activateEmergencyAccess,
                  )
                : _error != null || _record == null
                    ? _RecordError(
                        message: _friendlyError(l10n),
                        onRetry: _load,
                      )
                    : _RecordContent(
                        record: _record!,
                        sections: _availableSections,
                        selected: _section,
                        onSectionChanged: (section) {
                          setState(() => _section = section);
                        },
                      ),
      ),
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

class _RecordContent extends StatelessWidget {
  const _RecordContent({
    required this.record,
    required this.sections,
    required this.selected,
    required this.onSectionChanged,
  });

  final PatientRecordBundle record;
  final List<PatientRecordSection> sections;
  final PatientRecordSection selected;
  final ValueChanged<PatientRecordSection> onSectionChanged;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Column(
      children: [
        PatientRecordHero(record: record),
        Material(
          color: colors.surface,
          child: SizedBox(
            height: 58,
            child: ListView.separated(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              scrollDirection: Axis.horizontal,
              itemCount: sections.length,
              separatorBuilder: (_, _) => const SizedBox(width: 8),
              itemBuilder: (context, index) {
                final section = sections[index];
                return _SectionChip(
                  section: section,
                  selected: section == selected,
                  onTap: () => onSectionChanged(section),
                );
              },
            ),
          ),
        ),
        Divider(height: 1, color: colors.outlineVariant.withValues(alpha: 0.5)),
        Expanded(
          child: AnimatedSwitcher(
            duration: const Duration(milliseconds: 220),
            child: PatientRecordSectionView(
              key: ValueKey(selected),
              section: selected,
              record: record,
            ),
          ),
        ),
      ],
    );
  }
}

class _SectionChip extends StatelessWidget {
  const _SectionChip({
    required this.section,
    required this.selected,
    required this.onTap,
  });

  final PatientRecordSection section;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    final (label, icon) = switch (section) {
      PatientRecordSection.overview => (
        l10n.recordOverview,
        Icons.grid_view_rounded,
      ),
      PatientRecordSection.medical => (
        l10n.recordMedical,
        Icons.health_and_safety_rounded,
      ),
      PatientRecordSection.consultations => (
        l10n.recordConsultations,
        Icons.history_rounded,
      ),
      PatientRecordSection.laboratory => (
        l10n.recordLaboratory,
        Icons.science_rounded,
      ),
      PatientRecordSection.hospitalizations => (
        l10n.recordHospitalizations,
        Icons.local_hospital_rounded,
      ),
      PatientRecordSection.audit => (
        l10n.recordAudit,
        Icons.shield_outlined,
      ),
    };

    return Semantics(
      selected: selected,
      button: true,
      label: label,
      child: Material(
        color: selected
            ? colors.primaryContainer
            : colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  icon,
                  size: 18,
                  color: selected
                      ? colors.onPrimaryContainer
                      : colors.onSurfaceVariant,
                ),
                const SizedBox(width: 7),
                Text(
                  label,
                  maxLines: 1,
                  style: Theme.of(context).textTheme.labelLarge?.copyWith(
                        fontWeight: FontWeight.w800,
                        color: selected
                            ? colors.onPrimaryContainer
                            : colors.onSurfaceVariant,
                      ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _RecordLoading extends StatelessWidget {
  const _RecordLoading({required this.label});
  final String label;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const CircularProgressIndicator(),
          const SizedBox(height: 16),
          Text(label, style: Theme.of(context).textTheme.bodyMedium),
        ],
      ),
    );
  }
}

class _RecordError extends StatelessWidget {
  const _RecordError({required this.message, required this.onRetry});
  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.cloud_off_rounded, size: 48, color: colors.error),
            const SizedBox(height: 16),
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 18),
            FilledButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh_rounded),
              label: Text(l10n.recordRefresh),
            ),
          ],
        ),
      ),
    );
  }
}

class _ConsentRequired extends StatelessWidget {
  const _ConsentRequired({
    required this.controller,
    required this.loading,
    required this.error,
    required this.onSubmit,
  });

  final TextEditingController controller;
  final bool loading;
  final String? error;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 520),
          child: Card(
            elevation: 0,
            color: colors.surfaceContainerLow,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
              side: BorderSide(color: colors.outlineVariant),
            ),
            child: Padding(
              padding: const EdgeInsets.all(22),
              child: Column(
                children: [
                  Container(
                    width: 64,
                    height: 64,
                    decoration: BoxDecoration(
                      color: colors.errorContainer,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      Icons.lock_person_rounded,
                      color: colors.onErrorContainer,
                      size: 32,
                    ),
                  ),
                  const SizedBox(height: 18),
                  Text(
                    l10n.recordAccessDeniedTitle,
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w900,
                        ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    l10n.recordAccessDeniedBody,
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: colors.onSurfaceVariant,
                        ),
                  ),
                  const SizedBox(height: 22),
                  TextField(
                    controller: controller,
                    minLines: 3,
                    maxLines: 5,
                    textCapitalization: TextCapitalization.sentences,
                    decoration: InputDecoration(
                      labelText: l10n.recordEmergencyReasonLabel,
                      hintText: l10n.recordEmergencyReasonHint,
                      alignLabelWithHint: true,
                    ),
                  ),
                  if (error != null) ...[
                    const SizedBox(height: 12),
                    Text(
                      error!,
                      style: TextStyle(color: colors.error),
                      textAlign: TextAlign.center,
                    ),
                  ],
                  const SizedBox(height: 18),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      onPressed: loading ? null : onSubmit,
                      icon: loading
                          ? const SizedBox.square(
                              dimension: 18,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.emergency_share_rounded),
                      label: Text(l10n.recordEmergencyAccess),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
