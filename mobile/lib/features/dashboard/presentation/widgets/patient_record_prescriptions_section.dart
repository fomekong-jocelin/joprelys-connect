import 'package:flutter/material.dart';

import '../../../../core/i18n/app_locale_formatters.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../auth/domain/effective_access.dart';
import '../../domain/patient_history.dart';
import '../../domain/patient_record.dart';
import '../prescription_localizations.dart';
import 'prescription_editor_sheet.dart';

class PatientRecordPrescriptionsSection extends StatelessWidget {
  const PatientRecordPrescriptionsSection({
    required this.record,
    required this.access,
    required this.onChanged,
    super.key,
  });

  final PatientRecordBundle record;
  final EffectiveAccess access;
  final VoidCallback onChanged;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final visits = record.history.pastVisits
        .where(
          (visit) =>
              visit.prescriptionNumber?.trim().isNotEmpty == true ||
              visit.prescriptionItems.isNotEmpty,
        )
        .toList(growable: false);

    if (visits.isEmpty) {
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [_EmptyState(message: l10n.prescriptionHistoryEmpty)],
      );
    }

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: visits.length,
      separatorBuilder: (_, _) => const SizedBox(height: 10),
      itemBuilder: (context, index) {
        final visit = visits[index];
        return _PrescriptionHistoryCard(
          visit: visit,
          canWrite: access.hasPermission('CLINICAL_WRITE'),
          onChanged: onChanged,
        );
      },
    );
  }
}

class _PrescriptionHistoryCard extends StatelessWidget {
  const _PrescriptionHistoryCard({
    required this.visit,
    required this.canWrite,
    required this.onChanged,
  });

  final PastVisitSummary visit;
  final bool canWrite;
  final VoidCallback onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context);
    final status = _statusLabel(l10n, visit.prescriptionStatus);
    final canOpen = visit.consultationId.trim().isNotEmpty;

    return Material(
      color: colors.surfaceContainerLow,
      borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      child: InkWell(
        onTap: canOpen ? () => _open(context) : null,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        child: Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            border: Border.all(color: colors.outlineVariant),
            borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      visit.prescriptionNumber ?? l10n.prescriptionTitle,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  _StatusTag(
                    label: status,
                    status: visit.prescriptionStatus ?? '',
                  ),
                ],
              ),
              const SizedBox(height: 5),
              Text(
                '${AppLocaleFormatters.formatDate(visit.date, locale)} · ${visit.practitionerName}',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: colors.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 10),
              Text(
                l10n.prescriptionItemsCount(visit.prescriptionItems.length),
                style: theme.textTheme.labelMedium?.copyWith(
                  color: colors.primary,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 6),
              for (final item in visit.prescriptionItems.take(2))
                Padding(
                  padding: const EdgeInsets.only(bottom: 4),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Icon(
                        Icons.medication_outlined,
                        size: 16,
                        color: colors.onSurfaceVariant,
                      ),
                      const SizedBox(width: 7),
                      Expanded(
                        child: Text(
                          _itemSummary(item),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.bodyMedium,
                        ),
                      ),
                    ],
                  ),
                ),
              if (visit.prescriptionItems.length > 2)
                Text(
                  '+${visit.prescriptionItems.length - 2}',
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: colors.onSurfaceVariant,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              if (canOpen) ...[
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    Text(
                      l10n.prescriptionOpen,
                      style: theme.textTheme.labelMedium?.copyWith(
                        color: colors.primary,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(width: 4),
                    Icon(
                      Icons.chevron_right_rounded,
                      size: 18,
                      color: colors.primary,
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _open(BuildContext context) {
    return PrescriptionEditorSheet.show(
      context,
      consultationId: visit.consultationId,
      reference: visit.visitNumber,
      canWrite: canWrite,
      onChanged: onChanged,
    );
  }
}

class _StatusTag extends StatelessWidget {
  const _StatusTag({required this.label, required this.status});

  final String label;
  final String status;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final tone = switch (status.toUpperCase()) {
      'ACTIVE' => AppDesignTokens.success,
      'DRAFT' => AppDesignTokens.warning,
      'CANCELLED' => colors.error,
      _ => colors.onSurfaceVariant,
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 4),
      decoration: BoxDecoration(
        color: tone.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(color: tone.withValues(alpha: 0.3)),
      ),
      child: Text(
        label,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: tone,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 28),
      child: Column(
        children: [
          Icon(Icons.medication_outlined, color: colors.onSurfaceVariant),
          const SizedBox(height: 8),
          Text(
            message,
            textAlign: TextAlign.center,
            style: TextStyle(color: colors.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

String _itemSummary(PatientPrescriptionItemSummary item) {
  final details = <String>[
    item.drugName,
    if (item.dosage?.trim().isNotEmpty == true) item.dosage!.trim(),
    if (item.posology?.trim().isNotEmpty == true) item.posology!.trim(),
  ];
  return details.join(' · ');
}

String _statusLabel(AppLocalizations l10n, String? raw) {
  return switch (raw?.trim().toUpperCase()) {
    'DRAFT' => l10n.prescriptionStatusDraft,
    'ACTIVE' => l10n.prescriptionStatusActive,
    'CANCELLED' => l10n.prescriptionStatusCancelled,
    'EXPIRED' => l10n.prescriptionStatusExpired,
    _ => l10n.prescriptionStatusUnknown,
  };
}
