import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_badge.dart';
import '../../../../shared/widgets/app_card.dart';
import '../../../auth/domain/professional_session.dart';

class ProfessionalIdentityCard extends StatelessWidget {
  const ProfessionalIdentityCard({required this.session, super.key});

  final ProfessionalSession session;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;

    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            l10n.foundationIdentityTitle,
            style: Theme.of(
              context,
            ).textTheme.labelMedium?.copyWith(color: colors.onSurfaceVariant),
          ),
          const SizedBox(height: AppDesignTokens.spaceMd),
          Row(
            children: [
              CircleAvatar(
                radius: 26,
                backgroundColor: colors.primaryContainer,
                foregroundColor: colors.onPrimaryContainer,
                child: Text(
                  _initials(session.name),
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    color: colors.onPrimaryContainer,
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
                      session.name,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: AppDesignTokens.spaceXs),
                    Text(
                      session.email,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: AppDesignTokens.spaceMd),
          AppBadge(label: session.role, tone: AppSemanticTone.info),
        ],
      ),
    );
  }

  String _initials(String name) {
    final parts = name.trim().split(RegExp(r'\s+'));
    return parts
        .where((part) => part.isNotEmpty)
        .take(2)
        .map((part) => part.characters.first.toUpperCase())
        .join();
  }
}
