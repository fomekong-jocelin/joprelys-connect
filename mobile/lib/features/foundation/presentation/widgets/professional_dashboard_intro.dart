import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../features/dashboard/presentation/dashboard_localizations.dart';
import '../../../../l10n/app_localizations.dart';

class ProfessionalDashboardIntro extends StatelessWidget {
  const ProfessionalDashboardIntro({
    required this.sessionName,
    required this.queueCount,
    super.key,
  });

  final String sessionName;
  final int? queueCount;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _WorkspaceBadge(),
        const SizedBox(height: AppDesignTokens.spaceSm),
        Text(
          l10n.dashboardGreeting(_preferredName(sessionName)),
          key: const ValueKey('professional-dashboard-greeting'),
          style: theme.textTheme.headlineMedium?.copyWith(
            fontWeight: FontWeight.w900,
            letterSpacing: -0.5,
            height: 1.15,
          ),
        ),
        const SizedBox(height: AppDesignTokens.spaceXs),
        Text(
          queueCount == null
              ? l10n.foundationWelcomeSubtitle
              : l10n.dashboardOverviewSubtitle(queueCount!),
          style: theme.textTheme.bodyMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
            height: 1.4,
          ),
        ),
      ],
    );
  }
}

class _WorkspaceBadge extends StatelessWidget {
  const _WorkspaceBadge();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppDesignTokens.spaceSm,
        vertical: AppDesignTokens.spaceXs,
      ),
      decoration: BoxDecoration(
        color: colors.primary.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(color: colors.primary.withValues(alpha: 0.25)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.local_hospital_rounded, size: 13, color: colors.primary),
          const SizedBox(width: 5),
          Text(
            AppLocalizations.of(context).dashboardWorkspaceLabel,
            style: theme.textTheme.labelSmall?.copyWith(
              color: colors.primary,
              fontWeight: FontWeight.w800,
              letterSpacing: 1,
            ),
          ),
        ],
      ),
    );
  }
}

String _preferredName(String fullName) {
  final parts = fullName
      .trim()
      .split(RegExp(r'\s+'))
      .where((part) => part.isNotEmpty)
      .toList(growable: false);
  if (parts.isEmpty) return fullName;
  if (parts.length == 1) return _titleCase(parts.first);

  final first = parts.first;
  final firstLooksLikeSurname =
      first == first.toUpperCase() && first != first.toLowerCase();
  return _titleCase(firstLooksLikeSurname ? parts.last : first);
}

String _titleCase(String value) {
  if (value.isEmpty) return value;
  return '${value[0].toUpperCase()}${value.substring(1).toLowerCase()}';
}
