import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../domain/active_visit.dart';
import '../dashboard_localizations.dart';
import 'consultation_notes_sheet.dart';
import 'patient_vitals_sheet.dart';

class ActiveQueueSection extends StatelessWidget {
  const ActiveQueueSection({
    required this.queue,
    required this.onRefresh,
    super.key,
  });

  final AsyncValue<List<ActiveVisit>> queue;
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final visits = queue.value;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _SectionHeader(
          title: l10n.dashboardQueueTitle,
          subtitle: visits == null
              ? l10n.dashboardQueueSubtitleLoading
              : l10n.dashboardQueueSubtitle(visits.length),
          loading: queue.isLoading,
          refreshTooltip: l10n.dashboardQueueRefresh,
          onRefresh: onRefresh,
        ),
        const SizedBox(height: 12),
        queue.when(
          data: (data) => _QueueContent(visits: data, onRefresh: onRefresh),
          loading: () => const _QueueLoading(),
          error: (error, stackTrace) => _QueueError(onRetry: onRefresh),
        ),
      ],
    );
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({
    required this.title,
    required this.subtitle,
    required this.loading,
    required this.refreshTooltip,
    required this.onRefresh,
  });

  final String title;
  final String subtitle;
  final bool loading;
  final String refreshTooltip;
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                  letterSpacing: -0.2,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                subtitle,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: colors.onSurfaceVariant,
                  height: 1.25,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: 12),
        Tooltip(
          message: refreshTooltip,
          child: Material(
            color: colors.primary.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
            child: InkWell(
              onTap: loading ? null : () => onRefresh(),
              borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
              child: SizedBox.square(
                dimension: AppDesignTokens.minTouchTarget,
                child: Center(
                  child: loading
                      ? SizedBox.square(
                          dimension: 18,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: colors.primary,
                          ),
                        )
                      : Icon(
                          Icons.refresh_rounded,
                          size: 21,
                          color: colors.primary,
                        ),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _QueueContent extends StatelessWidget {
  const _QueueContent({required this.visits, required this.onRefresh});

  final List<ActiveVisit> visits;
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    if (visits.isEmpty) {
      return const _QueueEmpty();
    }

    final withVitals = visits.where((visit) => visit.hasVitals).length;
    final withoutVitals = visits.length - withVitals;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _QueueSummary(
          total: visits.length,
          withVitals: withVitals,
          withoutVitals: withoutVitals,
        ),
        const SizedBox(height: AppDesignTokens.spaceMd),
        for (var index = 0; index < visits.length; index++) ...[
          _ActiveVisitCard(
            visit: visits[index],
            onRefresh: () => onRefresh(),
            onTap: () => PatientVitalsSheet.show(
              context,
              visit: visits[index],
              onSaved: onRefresh,
            ),
          ),
          if (index != visits.length - 1)
            const SizedBox(height: AppDesignTokens.spaceSm),
        ],
      ],
    );
  }
}

class _QueueSummary extends StatelessWidget {
  const _QueueSummary({
    required this.total,
    required this.withVitals,
    required this.withoutVitals,
  });

  final int total;
  final int withVitals;
  final int withoutVitals;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;

    return LayoutBuilder(
      builder: (context, constraints) {
        const gap = 8.0;
        final columns = constraints.maxWidth < 340 ? 2 : 3;
        final itemWidth =
            (constraints.maxWidth - gap * (columns - 1)) / columns;

        return Wrap(
          spacing: gap,
          runSpacing: gap,
          children: [
            SizedBox(
              width: itemWidth,
              child: _MetricCard(
                value: total,
                label: l10n.dashboardQueueTotalCompact,
                accent: colors.primary,
                icon: Icons.people_alt_outlined,
              ),
            ),
            SizedBox(
              width: itemWidth,
              child: _MetricCard(
                value: withVitals,
                label: l10n.dashboardQueueWithVitalsCompact,
                accent: AppDesignTokens.success,
                icon: Icons.check_circle_outline_rounded,
              ),
            ),
            SizedBox(
              width: itemWidth,
              child: _MetricCard(
                value: withoutVitals,
                label: l10n.dashboardQueueWithoutVitals,
                accent: AppDesignTokens.warning,
                icon: Icons.hourglass_empty_rounded,
              ),
            ),
          ],
        );
      },
    );
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({
    required this.value,
    required this.label,
    required this.accent,
    this.icon,
  });

  final int value;
  final String label;
  final Color accent;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 12),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        border: Border.all(
          color: accent.withValues(alpha: 0.3),
          width: 1,
        ),
        boxShadow: Theme.of(context).brightness == Brightness.dark
            ? AppDesignTokens.darkPanelShadow
            : AppDesignTokens.lightPanelShadow,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (icon != null) ...[
                Icon(icon, size: 15, color: accent),
                const SizedBox(width: 5),
              ],
              Text(
                '$value',
                style: theme.textTheme.titleMedium?.copyWith(
                  color: accent,
                  fontWeight: FontWeight.w900,
                  height: 1,
                ),
              ),
            ],
          ),
          const SizedBox(height: 5),
          Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            textAlign: TextAlign.center,
            style: theme.textTheme.labelSmall?.copyWith(
              color: colors.onSurfaceVariant,
              fontWeight: FontWeight.w700,
              fontSize: 11,
            ),
          ),
        ],
      ),
    );
  }
}

class _ActiveVisitCard extends StatelessWidget {
  const _ActiveVisitCard({
    required this.visit,
    this.onTap,
    this.onRefresh,
  });

  final ActiveVisit visit;
  final VoidCallback? onTap;
  final VoidCallback? onRefresh;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final localArrival = visit.queueSince.toLocal();
    final arrivalLabel = DateFormat.Hm(l10n.localeName).format(localArrival);
    final careUnit = [
      if (visit.service?.isNotEmpty == true) visit.service!,
      if (visit.orientation.isNotEmpty && visit.orientation != visit.service)
        visit.orientation,
    ].join(' · ');
    final statusColor = visit.hasVitals
        ? AppDesignTokens.success
        : AppDesignTokens.warning;

    return Container(
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        border: Border.all(
          color: colors.outline.withValues(alpha: 0.6),
          width: 1,
        ),
        boxShadow: theme.brightness == Brightness.dark
            ? AppDesignTokens.darkPanelShadow
            : AppDesignTokens.lightPanelShadow,
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    CircleAvatar(
                      radius: 20,
                      backgroundColor: colors.primary.withValues(alpha: 0.12),
                      foregroundColor: colors.primary,
                      child: Text(
                        _initials(visit.patientName),
                        style: theme.textTheme.labelLarge?.copyWith(
                          color: colors.primary,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(
                            visit.patientName,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.w800,
                              height: 1.15,
                            ),
                          ),
                          const SizedBox(height: 3),
                          Text(
                            l10n.dashboardQueueVisitReference(
                              visit.visitNumber,
                              visit.patientDpu,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.labelSmall?.copyWith(
                              color: colors.onSurfaceVariant,
                              fontSize: 11,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 8),
                    _VitalsBadge(hasVitals: visit.hasVitals),
                  ],
                ),
                const SizedBox(height: 12),
                Text(
                  visit.reason,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.bodyMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                    height: 1.3,
                  ),
                ),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    _InfoChip(
                      icon: Icons.schedule_rounded,
                      label: l10n.dashboardQueueArrivedAt(arrivalLabel),
                    ),
                    if (careUnit.isNotEmpty)
                      _InfoChip(
                        icon: Icons.medical_services_outlined,
                        label: careUnit,
                      ),
                    _ActionChip(
                      icon: Icons.assignment_outlined,
                      label: l10n.consultationNotesTitle,
                      onTap: () => ConsultationNotesSheet.show(
                        context,
                        visit: visit,
                        onSaved: onRefresh,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  String _initials(String name) {
    final parts = name
        .trim()
        .split(RegExp(r'\s+'))
        .where((part) => part.isNotEmpty)
        .take(2)
        .toList(growable: false);
    if (parts.isEmpty) {
      return '?';
    }
    return parts.map((part) => part[0].toUpperCase()).join();
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

    return ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 270),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
        decoration: BoxDecoration(
          color: colors.surfaceContainerHighest.withValues(alpha: 0.55),
          borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
          border: Border.all(
            color: colors.outline.withValues(alpha: 0.4),
            width: 1,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 14, color: colors.onSurfaceVariant),
            const SizedBox(width: 5),
            Flexible(
              child: Text(
                label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.labelSmall?.copyWith(
                  color: colors.onSurfaceVariant,
                  fontWeight: FontWeight.w600,
                  fontSize: 11,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ActionChip extends StatelessWidget {
  const _ActionChip({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Material(
      color: colors.primary.withValues(alpha: 0.1),
      borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
            border: Border.all(
              color: colors.primary.withValues(alpha: 0.3),
              width: 1,
            ),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 14, color: colors.primary),
              const SizedBox(width: 5),
              Text(
                label,
                style: theme.textTheme.labelSmall?.copyWith(
                  color: colors.primary,
                  fontWeight: FontWeight.w700,
                  fontSize: 11,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _VitalsBadge extends StatelessWidget {
  const _VitalsBadge({required this.hasVitals});

  final bool hasVitals;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final accent = hasVitals ? AppDesignTokens.success : AppDesignTokens.warning;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: accent.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(
          color: accent.withValues(alpha: 0.35),
          width: 1,
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            hasVitals
                ? Icons.check_circle_outline_rounded
                : Icons.hourglass_empty_rounded,
            size: 13,
            color: accent,
          ),
          const SizedBox(width: 4),
          Text(
            hasVitals
                ? l10n.dashboardQueueVitalsReadyCompact
                : l10n.dashboardQueueVitalsPending,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: accent,
              fontWeight: FontWeight.w800,
              fontSize: 11,
            ),
          ),
        ],
      ),
    );
  }
}

class _QueueLoading extends StatelessWidget {
  const _QueueLoading();

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;

    return Container(
      constraints: const BoxConstraints(minHeight: 132),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      child: const CircularProgressIndicator(),
    );
  }
}

class _QueueEmpty extends StatelessWidget {
  const _QueueEmpty();

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.all(AppDesignTokens.spaceLg),
      decoration: BoxDecoration(
        color: colors.primary.withValues(alpha: 0.07),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            Icons.check_circle_outline_rounded,
            size: 36,
            color: colors.primary,
          ),
          const SizedBox(height: AppDesignTokens.spaceMd),
          Text(
            l10n.dashboardQueueEmptyTitle,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: AppDesignTokens.spaceSm),
          Text(
            l10n.dashboardQueueEmptyBody,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: colors.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}

class _QueueError extends StatelessWidget {
  const _QueueError({required this.onRetry});

  final Future<void> Function() onRetry;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.all(AppDesignTokens.spaceLg),
      decoration: BoxDecoration(
        color: colors.errorContainer,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.cloud_off_rounded, color: colors.onErrorContainer),
          const SizedBox(height: AppDesignTokens.spaceSm),
          Text(
            l10n.dashboardQueueLoadError,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: colors.onErrorContainer,
            ),
          ),
          const SizedBox(height: AppDesignTokens.spaceMd),
          AppButton(
            label: l10n.dashboardQueueRetry,
            icon: Icons.refresh_rounded,
            variant: AppButtonVariant.secondary,
            onPressed: () => onRetry(),
          ),
        ],
      ),
    );
  }
}
