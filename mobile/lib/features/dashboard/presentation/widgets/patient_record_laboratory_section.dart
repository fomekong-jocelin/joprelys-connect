import 'package:flutter/material.dart';

import '../../../../core/i18n/app_locale_formatters.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../foundation/presentation/mobile_workspace_localizations.dart';
import '../../domain/patient_record.dart';

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
          _EmptyCard(message: l10n.recordNoLabOrders)
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
          _EmptyCard(message: l10n.recordNoLabOrders)
        else
          for (final order in record.labOrders) ...[
            _LabOrderCard(order: order),
            const SizedBox(height: 10),
          ],
      ],
    );
  }
}

class _LabOrderCard extends StatelessWidget {
  const _LabOrderCard({required this.order});

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
              const SizedBox(width: 8),
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
                  label: 'Type: ${order.examType}',
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
          const SizedBox(height: 7),
          if (order.exams.isEmpty)
            Text(l10n.recordUnknown)
          else
            for (final exam in order.exams)
              Padding(
                padding: const EdgeInsets.only(bottom: 7),
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
            const SizedBox(height: 5),
            _Metadata(
              label: isFrench ? 'Motif' : 'Reason',
              value: order.reason!,
            ),
          ],
          if (order.practitioner?.trim().isNotEmpty == true) ...[
            const SizedBox(height: 6),
            _Metadata(
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

class _Metadata extends StatelessWidget {
  const _Metadata({required this.label, required this.value});

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

String _statusLabel(String raw, {required bool isFrench}) {
  return switch (raw.trim().toUpperCase()) {
    'PENDING' || 'REQUESTED' => isFrench ? 'En attente' : 'Pending',
    'COMPLETED' || 'DONE' => isFrench ? 'Terminé' : 'Completed',
    'CANCELLED' || 'CANCELED' => isFrench ? 'Annulé' : 'Cancelled',
    final value => value,
  };
}
