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
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [_EmptyCard(message: l10n.recordNoConsultations)],
      );
    }

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: record.history.pastVisits.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (context, index) => _ConsultationCard(
        visit: record.history.pastVisits[index],
      ),
    );
  }
}

class _ConsultationCard extends StatelessWidget {
  const _ConsultationCard({required this.visit});

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
                Expanded(
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: _Badge(
                      label: visit.visitNumber.isEmpty
                          ? l10n.recordConsultations
                          : visit.visitNumber,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  AppLocaleFormatters.formatDate(visit.date, locale),
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: colors.onSurfaceVariant,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
            if (visit.status.trim().isNotEmpty) ...[
              const SizedBox(height: 7),
              Align(
                alignment: Alignment.centerLeft,
                child: _Badge(
                  label: _statusLabel(visit.status, isFrench: isFrench),
                  neutral: true,
                ),
              ),
            ],
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
}

class _PrescriptionItem extends StatelessWidget {
  const _PrescriptionItem({required this.item});

  final PatientPrescriptionItemSummary item;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final details = <String?>[
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
        Icon(icon, color: colors.primary, size: 20),
        const SizedBox(width: 7),
        Text(
          title,
          style: theme.textTheme.labelLarge?.copyWith(
            color: colors.primary,
            fontWeight: FontWeight.w900,
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
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 7),
      decoration: BoxDecoration(
        color: colors.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 15, color: colors.onSurfaceVariant),
          const SizedBox(width: 5),
          Text(label, style: const TextStyle(fontWeight: FontWeight.w800)),
        ],
      ),
    );
  }
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
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
        style: TextStyle(
          color: colors.onSurfaceVariant,
          fontStyle: FontStyle.italic,
        ),
      ),
    );
  }
}

bool _hasVitals(PastVisitSummary value) {
  return value.temperature != null ||
      value.systolic != null ||
      value.diastolic != null ||
      value.pulse != null;
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
    'ACTIVE' => isFrench ? 'Actif' : 'Active',
    'COMPLETED' || 'DONE' => isFrench ? 'Terminé' : 'Completed',
    final value => value,
  };
}
