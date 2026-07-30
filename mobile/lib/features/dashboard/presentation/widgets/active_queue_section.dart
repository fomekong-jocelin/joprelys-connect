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
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  const SizedBox(height: AppDesignTokens.spaceXs),
                  Text(
                    queue.value == null
                        ? l10n.dashboardQueueSubtitleLoading
                        : l10n.dashboardQueueSubtitle(queue.value!.length),
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
            IconButton(
              tooltip: l10n.dashboardQueueRefresh,
              onPressed: queue.isLoading ? null : () => onRefresh(),
              icon: const Icon(Icons.refresh),
            ),
          ],
        ),
        const SizedBox(height: AppDesignTokens.spaceMd),
        queue.when(
          data: (visits) => _QueueContent(visits: visits),
          loading: () => const _QueueLoading(),
          error: (error, stackTrace) => _QueueError(onRetry: onRefresh),
        ),
      ],
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

    return Row(
      children: [
        Expanded(
          child: _SummaryTile(
            value: total,
            label: l10n.dashboardQueueTotal,
            icon: Icons.groups_2_outlined,
            tone: _SummaryTone.primary,
          ),
        ),
        const SizedBox(width: AppDesignTokens.spaceSm),
        Expanded(
          child: _SummaryTile(
            value: withVitals,
            label: l10n.dashboardQueueWithVitals,
            icon: Icons.monitor_heart_outlined,
            tone: _SummaryTone.success,
          ),
        ),
        const SizedBox(width: AppDesignTokens.spaceSm),
        Expanded(
          child: _SummaryTile(
            value: withoutVitals,
            label: l10n.dashboardQueueWithoutVitals,
            icon: Icons.pending_actions_outlined,
            tone: _SummaryTone.warning,
          ),
        ),
      ],
    );
  }
}

enum _SummaryTone { primary, success, warning }

class _SummaryTile extends StatelessWidget {
  const _SummaryTile({
    required this.value,
    required this.label,
    required this.icon,
    required this.tone,
  });

  final int value;
  final String label;
  final IconData icon;
  final _SummaryTone tone;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final accent = switch (tone) {
      _SummaryTone.primary => colors.primary,
      _SummaryTone.success => AppDesignTokens.success,
      _SummaryTone.warning => AppDesignTokens.warning,
    };

    return Container(
      constraints: const BoxConstraints(minHeight: 104),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: accent.withValues(alpha: 0.08),
        border: Border.all(color: accent.withValues(alpha: 0.24)),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 21, color: accent),
          const Spacer(),
          Text(
            '$value',
            style: Theme.of(context).textTheme.titleLarge?.copyWith(
              color: accent,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: AppDesignTokens.spaceXs),
          Text(
            label,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.labelSmall,
          ),
        ],
      ),
    );
  }
}

class _ActiveVisitCard extends StatelessWidget {
  const _ActiveVisitCard({required this.visit});

  final ActiveVisit visit;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    final localArrival = visit.queueSince.toLocal();
    final arrivalLabel = DateFormat.Hm(l10n.localeName).format(localArrival);
    final secondary = [
      if (visit.service?.isNotEmpty == true) visit.service!,
      if (visit.orientation.isNotEmpty) visit.orientation,
    ].join(' · ');

    return Container(
      padding: const EdgeInsets.all(AppDesignTokens.spaceMd),
      decoration: BoxDecoration(
        color: colors.surface,
        border: Border.all(color: colors.outlineVariant),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        boxShadow: Theme.of(context).brightness == Brightness.dark
            ? AppDesignTokens.darkPanelShadow
            : AppDesignTokens.lightPanelShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              CircleAvatar(
                radius: 21,
                backgroundColor: colors.primary.withValues(alpha: 0.12),
                foregroundColor: colors.primary,
                child: Text(
                  _initials(visit.patientName),
                  style: Theme.of(context).textTheme.labelLarge?.copyWith(
                    color: colors.primary,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
              const SizedBox(width: AppDesignTokens.spaceMd),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      visit.patientName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: AppDesignTokens.spaceXs),
                    Text(
                      l10n.dashboardQueueVisitReference(
                        visit.visitNumber,
                        visit.patientDpu,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: AppDesignTokens.spaceSm),
              _VitalsBadge(hasVitals: visit.hasVitals),
            ],
          ),
          const SizedBox(height: AppDesignTokens.spaceMd),
          Text(
            visit.reason,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
          if (secondary.isNotEmpty) ...[
            const SizedBox(height: AppDesignTokens.spaceXs),
            Text(
              secondary,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
          const SizedBox(height: AppDesignTokens.spaceMd),
          Row(
            children: [
              Icon(
                Icons.schedule_outlined,
                size: 17,
                color: colors.onSurfaceVariant,
              ),
              const SizedBox(width: AppDesignTokens.spaceXs),
              Text(
                l10n.dashboardQueueArrivedAt(arrivalLabel),
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
          ),
        ],
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
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
      ),
      child: Text(
        hasVitals
            ? l10n.dashboardQueueVitalsReady
            : l10n.dashboardQueueVitalsPending,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: accent,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _QueueLoading extends StatelessWidget {
  const _QueueLoading();

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 168,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        border: Border.all(color: Theme.of(context).colorScheme.outlineVariant),
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
        color: colors.primary.withValues(alpha: 0.06),
        border: Border.all(color: colors.primary.withValues(alpha: 0.18)),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      child: Column(
        children: [
          Icon(Icons.event_available_outlined, size: 38, color: colors.primary),
          const SizedBox(height: AppDesignTokens.spaceMd),
          Text(
            l10n.dashboardQueueEmptyTitle,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: AppDesignTokens.spaceSm),
          Text(
            l10n.dashboardQueueEmptyBody,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodySmall,
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
        border: Border.all(color: colors.error.withValues(alpha: 0.3)),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      child: Column(
        children: [
          Icon(Icons.cloud_off_outlined, color: colors.onErrorContainer),
          const SizedBox(height: AppDesignTokens.spaceSm),
          Text(
            l10n.dashboardQueueLoadError,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: colors.onErrorContainer,
            ),
          ),
          const SizedBox(height: AppDesignTokens.spaceMd),
          AppButton(
            label: l10n.dashboardQueueRetry,
            icon: Icons.refresh,
            variant: AppButtonVariant.secondary,
            onPressed: () => onRetry(),
          ),
        ],
      ),
    );
  }
}
