import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../../../shared/widgets/app_card.dart';

class ProfessionalSecurityCard extends StatelessWidget {
  const ProfessionalSecurityCard({
    required this.enabled,
    required this.loading,
    required this.onPressed,
    super.key,
  });

  final bool enabled;
  final bool loading;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;

    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: AppDesignTokens.minTouchTarget,
                height: AppDesignTokens.minTouchTarget,
                decoration: BoxDecoration(
                  color: colors.primaryContainer,
                  borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
                ),
                child: Icon(
                  enabled
                      ? Icons.verified_user_outlined
                      : Icons.shield_outlined,
                  color: colors.onPrimaryContainer,
                ),
              ),
              const SizedBox(width: AppDesignTokens.spaceMd),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      l10n.foundationSecurityTitle,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: AppDesignTokens.spaceXs),
                    Text(
                      enabled
                          ? l10n.foundationBiometricEnabled
                          : l10n.foundationBiometricDisabled,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: AppDesignTokens.spaceMd),
          AppButton(
            label: enabled
                ? l10n.authDisableBiometrics
                : l10n.authEnableBiometrics,
            icon: Icons.fingerprint,
            variant: AppButtonVariant.secondary,
            expand: true,
            loading: loading,
            onPressed: loading ? null : onPressed,
          ),
        ],
      ),
    );
  }
}
