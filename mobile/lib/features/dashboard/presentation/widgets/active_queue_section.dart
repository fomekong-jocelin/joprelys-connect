import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../domain/active_visit.dart';
import '../dashboard_localizations.dart';

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
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    l10n.dashboardQueueTitle,
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.w800,
                      letterSpacing: -0.3,
                    ),
                  ),
                  const SizedBox(height: AppDesignTokens.spaceXs),
                  Text(
                    visits == null
                        ? l10n.dashboardQueueSubtitleLoading
                        : l10n.dashboardQueueSubtitle(visits.length),
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
            _RefreshAction(
              loading: queue.isLoading,
              tooltip: l10n.dashboardQueueRefresh,
              onPressed: onRefresh,
            ),
          ],
        ),
        const SizedBox(height: AppDesignTokens.spaceMd),
        queue.when(
          data: (data) => _QueueContent(visits: data),
          loading: () => const _QueueLoading(),
          error: (error, stackTrace) => _QueueError(onRetry: onRefresh),
        ),
      ],
    );
  }
}

class _RefreshAction extends StatelessWidget {
  const _RefreshAction({
    required this.loading,
    required this.tooltip,
    required this.onPressed,
  });

  final bool loading;
  final String tooltip;
  final Future<void> Function() onPressed;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Tooltip(
      message: tooltip,
      child: Material(
        color: colors.primary.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        child: InkWell(
          onTap: loading ? null : () => onPressed(),
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
                  : Icon(Icons.refresh_rounded, size: 21, color: colors.primary),
            ),
          ),
        ),
      ),
    );
  }
}

class _QueueContent extends StatelessWidget {
  const _QueueContent({required this.visits});

  final List<ActiveVisit> visits;

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
        ...visits.map(
          (visit) => Padding(
            padding: const EdgeInsets.only(bottom: AppDesignTokens.spaceSm),
            child: _ActiveVisitCard(visit: visit),
          ),
        ),
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

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppDesignTokens.spaceMd,
        vertical: 14,
      ),
      decoration: BoxDecoration(
        color: colors.primary.withValues(alpha: 0.075),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      child: Row(
        children: [
          Expanded(
            child: _MetricItem(
              value: total,
              label: l10n.dashboardQueueTotalCompact,
              accent: colors.primary,
            ),
          ),
          _MetricDivider(color: colors.outlineVariant),
          Expanded(
            child: _MetricItem(
              value: withVitals,
              label: l10n.dashboardQueueWithVitalsCompact,
              accent: AppDesignTokens.success,
            ),
          ),
          _MetricDivider(color: colors.outlineVariant),
          Expanded(
            child: _MetricItem(
              value: withoutVitals,
              label: l10n.dashboardQueueWithoutVitals,
              accent: AppDesignTokens.warning,
            ),
          ),
        ],
      ),
    );
  }
}

class _MetricItem extends StatelessWidget {
  const _MetricItem({
    required this.value,
    required this.label,
    required this.accent,
  });

  final int value;
  final String label;
  final Color accent;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          '$value',
          style: Theme.of(context).textTheme.titleLarge?.copyWith(
            color: accent,
            fontWeight: FontWeight.w900,
            height: 1,
          ),
        ),
        const SizedBox(height: 6),
        Text(
          label,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.labelSmall?.copyWith(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
            fontWeight: FontWeight.w700,
          ),
        ),
      ],
    );
  }
}

class _MetricDivider extends StatelessWidget {
  const _MetricDivider({required this.color});

  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 1,
      height: 34,
      margin: const EdgeInsets.symmetric(horizontal: AppDesignTokens.spaceSm),
      color: color.withValues(alpha: 0.7),
    );
  }
}

class _ActiveVisitCard extends StatelessWidget {
  const _ActiveVisitCard({required this.visit});

  final ActiveVisit visit;

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
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        boxShadow: theme.brightness == Brightness.dark
            ? const [
                BoxShadow(
                  color: Color(0x28000000),
                  blurRadius: 14,
                  offset: Offset(0, 5),
                ),
              ]
            : const [
                BoxShadow(
                  color: Color(0x0D0A1D3D),
                  blurRadius: 14,
                  offset: Offset(0, 5),
                ),
              ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(width: 4, color: statusColor),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(AppDesignTokens.spaceMd),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        CircleAvatar(
                          radius: 20,
                          backgroundColor: colors.primary.withValues(alpha: 0.11),
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
                            children: [
                              Text(
                                visit.patientName,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: theme.textTheme.titleMedium?.copyWith(
                                  fontWeight: FontWeight.w800,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                visit.reason,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: theme.textTheme.bodySmall?.copyWith(
                                  color: colors.onSurfaceVariant,
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: AppDesignTokens.spaceSm),
                        _VitalsBadge(hasVitals: visit.hasVitals),
                      ],
                    ),
                    const SizedBox(height: 14),
                    Text(
                      l10n.dashboardQueueVisitReference(
                        visit.visitNumber,
                        visit.patientDpu,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.labelSmall?.copyWith(
                        color: colors.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Wrap(
                      spacing: 14,
                      runSpacing: 8,
                      children: [
                        _Metadata(
                          icon: Icons.schedule_rounded,
                          label: l10n.dashboardQueueArrivedAt(arrivalLabel),
                        ),
                        if (careUnit.isNotEmpty)
                          _Metadata(
                            icon: Icons.medical_services_outlined,
                            label: careUnit,
                          ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
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

class _Metadata extends StatelessWidget {
  const _Metadata({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 15, color: colors.onSurfaceVariant),
        const SizedBox(width: 5),
        ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 230),
          child: Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.labelMedium?.copyWith(
              color: colors.onSurfaceVariant,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ],
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
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
      decoration: BoxDecoration(
        color: accent.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
      ),
      child: Text(
        hasVitals
            ? l10n.dashboardQueueVitalsReadyCompact
            : l10n.dashboardQueueVitalsPending,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: accent,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _QueueLoading extends StatelessWidget {
  const _QueueLoading();

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Column(
      children: List.generate(
        2,
        (index) => Container(
          height: 118,
          margin: const EdgeInsets.only(bottom: AppDesignTokens.spaceSm),
          decoration: BoxDecoration(
            color: colors.surface,
            borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
          ),
          alignment: Alignment.center,
          child: index == 0
              ? const CircularProgressIndicator(strokeWidth: 2)
              : null,
        ),
      ),
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
        color: colors.primary.withValues(alpha: 0.065),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: colors.primary.withValues(alpha: 0.12),
              shape: BoxShape.circle,
            ),
            child: Icon(Icons.check_rounded, color: colors.primary),
          ),
          const SizedBox(width: AppDesignTokens.spaceMd),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  l10n.dashboardQueueEmptyTitle,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: AppDesignTokens.spaceXs),
                Text(
                  l10n.dashboardQueueEmptyBody,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
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
        children: [
          Icon(Icons.cloud_off_outlined, color: colors.onErrorContainer),
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
