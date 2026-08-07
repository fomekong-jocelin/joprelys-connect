import 'package:flutter/material.dart';

import '../../../../core/i18n/app_locale_formatters.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../foundation/presentation/mobile_workspace_localizations.dart';
import '../../domain/patient_history.dart';
import '../../domain/patient_record.dart';
import '../dashboard_localizations.dart';

class PatientRecordConsultationsDetailSection extends StatelessWidget {
  const PatientRecordConsultationsDetailSection({
    required this.record,
    super.key,
  });

  final PatientRecordBundle record;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    if (record.history.pastVisits.isEmpty) {
      return _EmptySection(message: l10n.recordNoConsultations);
    }

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: record.history.pastVisits.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (context, index) => _ConsultationDetailCard(
        visit: record.history.pastVisits[index],
      ),
    );
  }
}

class PatientRecordLaboratoryDetailSection extends StatelessWidget {
  const PatientRecordLaboratoryDetailSection({
    required this.record,
    super.key,
  });

  final PatientRecordBundle record;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _SectionTitle(icon: Icons.biotech_rounded, title: l10n.recordLabResults),
        const SizedBox(height: 10),
        if (record.labResults.isEmpty)
          _InlineEmpty(message: l10n.recordNoLabOrders)
        else
          for (final result in record.labResults) ...[
            _LabResultCard(result: result),
            const SizedBox(height: 10),
          ],
        const SizedBox(height: 14),
        _SectionTitle(
          icon: Icons.assignment_outlined,
          title: l10n.recordLabOrders,
        ),
        const SizedBox(height: 10),
        if (record.labOrders.isEmpty)
          _InlineEmpty(message: l10n.recordNoLabOrders)
        else
          for (final order in record.labOrders) ...[
            _LabOrderDetailCard(order: order),
            const SizedBox(height: 10),
          ],
      ],
    );
  }
}

class _ConsultationDetailCard extends StatelessWidget {
  const _ConsultationDetailCard({required this.visit});

  final PastVisitSummary visit;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context);
    final isFrench = l10n.localeName.startsWith('fr');
    final summary = _firstNonBlank([visit.diagnosis, visit.symptoms]);

    return Container(
      decoration: BoxDecoration(
        color: colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
        border: Border.all(color: colors.outlineVariant),
      ),
      clipBehavior: Clip.antiAlias,
      child: ExpansionTile(
        tilePadding: const EdgeInsets.fromLTRB(14, 10, 10, 10),
        childrenPadding: const EdgeInsets.fromLTRB(14, 0, 14, 16),
        maintainState: true,
        shape: const Border(),
        collapsedShape: const Border(),
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                _Badge(
                  label: visit.visitNumber.isEmpty
                      ? l10n.recordConsultations
                      : visit.visitNumber,
                ),
                const SizedBox(width: 8),
                if (visit.status.trim().isNotEmpty)
                  _Badge(
                    label: _statusLabel(visit.status, isFrench: isFrench),
                    neutral: true,
                  ),
                const Spacer(),
                Text(
                  AppLocaleFormatters.formatDate(visit.date, locale),
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: colors.onSurfaceVariant,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              summary.isEmpty ? l10n.recordUnknown : summary,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: theme.textTheme.titleSmall?.copyWith(
                fontWeight: FontWeight.w900,
                height: 1.3,
              ),
            ),
            const SizedBox(height: 6),
            Row(
              children: [
                Icon(
                  Icons.medical_services_outlined,
                  size: 16,
                  color: colors.onSurfaceVariant,
                ),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    visit.practitionerName.isEmpty
                        ? l10n.recordUnknown
                        : visit.practitionerName,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: colors.onSurfaceVariant,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
        children: [
          if (_hasVitals(visit)) ...[
            Align(
              alignment: Alignment.centerLeft,
              child: Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  if (visit.temperature != null)
                    _InfoChip(
                      icon: Icons.thermostat_rounded,
                      label:
                          '${l10n.recordTemperature} ${visit.temperature!.toStringAsFixed(1)} °C',
                    ),
                  if (visit.systolic != null && visit.diastolic != null)
                    _InfoChip(
                      icon: Icons.monitor_heart_outlined,
                      label:
                          '${l10n.recordBloodPressure} ${visit.systolic}/${visit.diastolic}',
                    ),
                  if (visit.pulse != null)
                    _InfoChip(
                      icon: Icons.favorite_outline_rounded,
                      label: '${l10n.recordPulse} ${visit.pulse} bpm',
                    ),
                ],
              ),
            ),
            const SizedBox(height: 14),
          ],
          Divider(color: colors.outlineVariant.withValues(alpha: 0.7)),
          const SizedBox(height: 6),
          _ClinicalField(
            label: l10n.consultationSubjectiveLabel,
            value: visit.symptoms,
            emptyValue: l10n.recordUnknown,
          ),
          if (visit.clinicalExam.trim().isNotEmpty)
            _ClinicalField(
              label: l10n.consultationObjectiveLabel,
              value: visit.clinicalExam,
            ),
          _ClinicalField(
            label: l10n.consultationDiagnosisLabel,
            value: visit.diagnosis,
            emptyValue: l10n.recordUnknown,
          ),
          if (visit.conclusion.trim().isNotEmpty)
            _ClinicalField(
              label: l10n.consultationConclusionLabel,
              value: visit.conclusion,
            ),
          if (visit.advice.trim().isNotEmpty)
            _ClinicalField(
              label: l10n.consultationAdviceLabel,
              value: visit.advice,
            ),
          if (visit.followUp.trim().isNotEmpty)
            _ClinicalField(
              label: l10n.consultationFollowUpLabel,
              value: visit.followUp,
            ),
          if (visit.prescriptionItems.isNotEmpty) ...[
            const SizedBox(height: 2),
            _SectionTitle(
              icon: Icons.medication_outlined,
              title: isFrench ? 'Prescription' : 'Prescription',
            ),
            const SizedBox(height: 8),
            for (final item in visit.prescriptionItems)
              _PrescriptionItem(item: item),
          ],
        ],
      ),
    );
  }

  bool _hasVitals(PastVisitSummary value) {
    return value.temperature != null ||
        value.systolic != null ||
        value.diastolic != null ||
        value.pulse != null;
  }
}

class _PrescriptionItem extends StatelessWidget {
  const _PrescriptionItem({required this.item});

  final PatientPrescriptionItemSummary item;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final details = <String>[
      item.dosage,
      item.posology,
      item.frequency,
      item.duration,
      item.route,
      item.form,
      item.quantity,
    ].whereType<String>().where((value) => value.trim().isNotEmpty).toList();

    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: colors.surfaceContainerHighest.withValues(alpha: 0.45),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            item.drugName,
            style: const TextStyle(fontWeight: FontWeight.w900),
          ),
          if (details.isNotEmpty) ...[
            const SizedBox(height: 3),
            Text(details.join(' · ')),
          ],
          if (item.instructions?.trim().isNotEmpty == true) ...[
            const SizedBox(height: 3),
            Text(
              item.instructions!,
              style: TextStyle(color: colors.onSurfaceVariant),
            ),
          ],
        ],
      ),
    );
  }
}

class _LabOrderDetailCard extends StatelessWidget {
  const _LabOrderDetailCard({required this.order});

  final PatientLabOrderSummary order;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context);
    final isFrench = l10n.localeName.startsWith('fr');

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
        border: Border.all(color: colors.outlineVariant),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: _Badge(
                    label: order.number.isEmpty
                        ? l10n.recordLabOrders
                        : order.number,
                  ),
                ),
              ),
              if (order.createdAt != null)
                Text(
                  AppLocaleFormatters.formatDate(order.createdAt!, locale),
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: colors.onSurfaceVariant,
                    fontWeight: FontWeight.w700,
                  ),
                ),
            ],
          ),
          const SizedBox(height: 10),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              if (order.status.trim().isNotEmpty)
                _Badge(
                  label: _statusLabel(order.status, isFrench: isFrench),
                  neutral: true,
                ),
              if (order.priority?.trim().isNotEmpty == true)
                _Badge(
                  label:
                      '${isFrench ? 'Priorité' : 'Priority'}: ${order.priority}',
                  neutral: true,
                ),
              if (order.examType?.trim().isNotEmpty == true)
                _Badge(
                  label: '${isFrench ? 'Type' : 'Type'}: ${order.examType}',
                  neutral: true,
                ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            isFrench ? 'Examens prescrits' : 'Requested examinations',
            style: theme.textTheme.labelLarge?.copyWith(
              fontWeight: FontWeight.w900,
              color: colors.primary,
            ),
          ),
          const SizedBox(height: 6),
          if (order.exams.isEmpty)
            Text(l10n.recordUnknown)
          else
            for (final exam in order.exams)
              Padding(
                padding: const EdgeInsets.only(bottom: 6),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(
                      Icons.check_circle_outline_rounded,
                      size: 18,
                      color: colors.primary,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        exam,
                        style: theme.textTheme.bodyMedium?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
          if (order.reason?.trim().isNotEmpty == true) ...[
            const SizedBox(height: 6),
            _MetadataRow(
              label: isFrench ? 'Motif' : 'Reason',
              value: order.reason!,
            ),
          ],
          if (order.practitioner?.trim().isNotEmpty == true) ...[
            const SizedBox(height: 6),
            _MetadataRow(
              label: l10n.recordPractitioner,
              value: order.practitioner!,
            ),
          ],
        ],
      ),
    );
  }
}

class _LabResultCard extends StatelessWidget {
  const _LabResultCard({required this.result});

  final PatientLabResultSummary result;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
        border: Border.all(color: colors.outlineVariant),
      ),
      child: Row(
        children: [
          Icon(Icons.analytics_outlined, color: colors.primary),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  result.analyte,
                  style: const TextStyle(fontWeight: FontWeight.w900),
                ),
                if (result.referenceRange?.trim().isNotEmpty == true)
                  Text(
                    result.referenceRange!,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: colors.onSurfaceVariant,
                    ),
                  ),
              ],
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                '${result.value}${result.unit == null ? '' : ' ${result.unit}'}',
                style: theme.textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w900,
                ),
              ),
              if (result.interpretation?.trim().isNotEmpty == true)
                _Badge(label: result.interpretation!, neutral: true),
            ],
          ),
        ],
      ),
    );
  }
}

class _ClinicalField extends StatelessWidget {
  const _ClinicalField({
    required this.label,
    required this.value,
    this.emptyValue,
  });

  final String label;
  final String value;
  final String? emptyValue;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final content = value.trim().isEmpty ? emptyValue : value.trim();
    if (content == null || content.isEmpty) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: theme.textTheme.labelMedium?.copyWith(
              color: colors.primary,
              fontWeight: FontWeight.w900,
            ),
          ),
          const SizedBox(height: 3),
          Text(content, style: theme.textTheme.bodyMedium?.copyWith(height: 1.4)),
        ],
      ),
    );
  }
}

class _MetadataRow extends StatelessWidget {
  const _MetadataRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '$label : ',
          style: theme.textTheme.bodySmall?.copyWith(fontWeight: FontWeight.w900),
        ),
        Expanded(child: Text(value, style: theme.textTheme.bodySmall)),
      ],
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle({required this.icon, required this.title});

  final IconData icon;
  final String title;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Row(
      children: [
        Icon(icon, color: colors.primary, size: 22),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            title,
            style: theme.textTheme.titleMedium?.copyWith(
              color: colors.primary,
              fontWeight: FontWeight.w900,
            ),
          ),
        ),
      ],
    );
  }
}

class _Badge extends StatelessWidget {
  const _Badge({required this.label, this.neutral = false});

  final String label;
  final bool neutral;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
      decoration: BoxDecoration(
        color: neutral
            ? colors.surfaceContainerHighest
            : colors.primaryContainer,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      child: Text(
        label,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: theme.textTheme.labelSmall?.copyWith(
          color: neutral ? colors.onSurfaceVariant : colors.onPrimaryContainer,
          fontWeight: FontWeight.w900,
        ),
      ),
    );
  }
}

class _InfoChip extends StatelessWidget {
  const _InfoChip({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      decoration: BoxDecoration(
        color: colors.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 16, color: colors.onSurfaceVariant),
          const SizedBox(width: 5),
          Text(
            label,
            style: theme.textTheme.labelMedium?.copyWith(
              fontWeight: FontWeight.w800,
            ),
          ),
        ],
      ),
    );
  }
}

class _EmptySection extends StatelessWidget {
  const _EmptySection({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [_InlineEmpty(message: message)],
    );
  }
}

class _InlineEmpty extends StatelessWidget {
  const _InlineEmpty({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
        border: Border.all(color: colors.outlineVariant),
      ),
      child: Text(
        message,
        textAlign: TextAlign.center,
        style: theme.textTheme.bodyMedium?.copyWith(
          color: colors.onSurfaceVariant,
          fontStyle: FontStyle.italic,
        ),
      ),
    );
  }
}

String _firstNonBlank(Iterable<String> values) {
  for (final value in values) {
    final trimmed = value.trim();
    if (trimmed.isNotEmpty) return trimmed;
  }
  return '';
}

String _statusLabel(String raw, {required bool isFrench}) {
  return switch (raw.trim().toUpperCase()) {
    'BROUILLON' || 'DRAFT' => isFrench ? 'Brouillon' : 'Draft',
    'PENDING' || 'REQUESTED' => isFrench ? 'En attente' : 'Pending',
    'ACTIVE' => isFrench ? 'Actif' : 'Active',
    'COMPLETED' || 'DONE' => isFrench ? 'Terminé' : 'Completed',
    'CANCELLED' || 'CANCELED' => isFrench ? 'Annulé' : 'Cancelled',
    final value => value,
  };
}
