import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../features/auth/domain/professional_session.dart';
import '../../../../features/dashboard/presentation/dashboard_localizations.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import 'professional_identity_card.dart';
import 'professional_security_card.dart';

class ProfessionalProfileSheet extends StatelessWidget {
  const ProfessionalProfileSheet({
    required this.session,
    required this.loading,
    required this.onBiometricsPressed,
    required this.onLogoutPressed,
    this.authError,
    super.key,
  });

  final ProfessionalSession session;
  final bool loading;
  final String? authError;
  final VoidCallback onBiometricsPressed;
  final VoidCallback onLogoutPressed;

  static Future<void> show(
    BuildContext context, {
    required ProfessionalSession session,
    required bool loading,
    required String? authError,
    required VoidCallback onBiometricsPressed,
    required VoidCallback onLogoutPressed,
  }) {
    return showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) => FractionallySizedBox(
        heightFactor: 0.86,
        child: ProfessionalProfileSheet(
          session: session,
          loading: loading,
          authError: authError,
          onBiometricsPressed: onBiometricsPressed,
          onLogoutPressed: onLogoutPressed,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(AppDesignTokens.radiusLg),
        ),
      ),
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
        child: _ProfileContent(
          session: session,
          loading: loading,
          authError: authError,
          onBiometricsPressed: onBiometricsPressed,
          onLogoutPressed: onLogoutPressed,
        ),
      ),
    );
  }
}

class _ProfileContent extends StatelessWidget {
  const _ProfileContent({
    required this.session,
    required this.loading,
    required this.onBiometricsPressed,
    required this.onLogoutPressed,
    this.authError,
  });

  final ProfessionalSession session;
  final bool loading;
  final String? authError;
  final VoidCallback onBiometricsPressed;
  final VoidCallback onLogoutPressed;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const _DragHandle(),
        const SizedBox(height: AppDesignTokens.spaceMd),
        const _ProfileHeader(),
        const SizedBox(height: AppDesignTokens.spaceMd),
        ProfessionalIdentityCard(session: session),
        const SizedBox(height: AppDesignTokens.spaceMd),
        ProfessionalSecurityCard(
          enabled: session.biometricEnabled,
          loading: loading,
          onPressed: () => _closeThen(context, onBiometricsPressed),
        ),
        if (authError != null) ...[
          const SizedBox(height: AppDesignTokens.spaceMd),
          _InlineError(message: authError!),
        ],
        const SizedBox(height: AppDesignTokens.spaceLg),
        AppButton(
          label: AppLocalizations.of(context).authLogout,
          icon: Icons.logout_rounded,
          variant: AppButtonVariant.destructiveSecondary,
          expand: true,
          loading: loading,
          onPressed: loading
              ? null
              : () => _closeThen(context, onLogoutPressed),
        ),
      ],
    );
  }

  void _closeThen(BuildContext context, VoidCallback action) {
    Navigator.of(context).pop();
    action();
  }
}

class _DragHandle extends StatelessWidget {
  const _DragHandle();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        width: 42,
        height: 4,
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.outlineVariant,
          borderRadius: BorderRadius.circular(2),
        ),
      ),
    );
  }
}

class _ProfileHeader extends StatelessWidget {
  const _ProfileHeader();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    return Row(
      children: [
        Expanded(
          child: Text(
            l10n.dashboardProfileTitle,
            style: theme.textTheme.titleLarge?.copyWith(
              fontWeight: FontWeight.w900,
            ),
          ),
        ),
        IconButton(
          tooltip: l10n.dashboardClose,
          onPressed: () => Navigator.of(context).pop(),
          icon: const Icon(Icons.close_rounded),
        ),
      ],
    );
  }
}

class _InlineError extends StatelessWidget {
  const _InlineError({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.errorContainer,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      child: Text(
        message,
        textAlign: TextAlign.center,
        style: Theme.of(
          context,
        ).textTheme.bodySmall?.copyWith(color: colors.onErrorContainer),
      ),
    );
  }
}
