import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../features/auth/application/auth_controller.dart';
import '../../../../features/auth/domain/professional_session.dart';
import '../../../../features/auth/presentation/widgets/auth_error_message.dart';
import '../../../../features/auth/presentation/widgets/auth_preferences_bar.dart';
import '../../../../features/dashboard/application/active_queue_controller.dart';
import '../../../../features/dashboard/domain/active_visit.dart';
import '../../../../features/dashboard/presentation/widgets/active_queue_section.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_brand_lockup.dart';
import '../../../../shared/widgets/app_button.dart';
import '../widgets/professional_identity_card.dart';
import '../widgets/professional_security_card.dart';

class FoundationPage extends ConsumerWidget {
  const FoundationPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final auth = ref.watch(authControllerProvider);
    final authState = auth.value;
    final session = authState?.session;

    if (session == null) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    final queue = ref.watch(activeQueueControllerProvider);

    return _ProfessionalHome(
      session: session,
      queue: queue,
      authError: authErrorMessage(l10n, authState?.errorCode),
      loading: auth.isLoading,
      onQueueRefresh: ref
          .read(activeQueueControllerProvider.notifier)
          .refreshQueue,
      onBiometricsPressed: session.biometricEnabled
          ? ref.read(authControllerProvider.notifier).disableBiometrics
          : () => ref
                .read(authControllerProvider.notifier)
                .enableBiometrics(reason: l10n.authBiometricEnableReason),
      onLogoutPressed: ref.read(authControllerProvider.notifier).logout,
    );
  }
}

class _ProfessionalHome extends StatelessWidget {
  const _ProfessionalHome({
    required this.session,
    required this.queue,
    required this.loading,
    required this.onQueueRefresh,
    required this.onBiometricsPressed,
    required this.onLogoutPressed,
    this.authError,
  });

  final ProfessionalSession session;
  final AsyncValue<List<ActiveVisit>> queue;
  final String? authError;
  final bool loading;
  final Future<void> Function() onQueueRefresh;
  final VoidCallback onBiometricsPressed;
  final VoidCallback onLogoutPressed;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;

    return Scaffold(
      body: DecoratedBox(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              colors.primary.withValues(alpha: 0.08),
              colors.surface,
              colors.surface,
            ],
            stops: const [0, 0.3, 1],
          ),
        ),
        child: SafeArea(
          child: RefreshIndicator(
            onRefresh: onQueueRefresh,
            child: SingleChildScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
              child: Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 480),
                  child: _ProfessionalHomeContent(
                    session: session,
                    queue: queue,
                    authError: authError,
                    loading: loading,
                    onQueueRefresh: onQueueRefresh,
                    onBiometricsPressed: onBiometricsPressed,
                    onLogoutPressed: onLogoutPressed,
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _ProfessionalHomeContent extends StatelessWidget {
  const _ProfessionalHomeContent({
    required this.session,
    required this.queue,
    required this.loading,
    required this.onQueueRefresh,
    required this.onBiometricsPressed,
    required this.onLogoutPressed,
    this.authError,
  });

  final ProfessionalSession session;
  final AsyncValue<List<ActiveVisit>> queue;
  final String? authError;
  final bool loading;
  final Future<void> Function() onQueueRefresh;
  final VoidCallback onBiometricsPressed;
  final VoidCallback onLogoutPressed;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const AuthPreferencesBar(),
        const SizedBox(height: AppDesignTokens.spaceLg),
        const Align(
          alignment: Alignment.center,
          child: AppBrandLockup(logoWidth: 112, compact: true),
        ),
        const SizedBox(height: AppDesignTokens.spaceLg),
        Text(
          l10n.foundationWelcomeTitle(session.name),
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.headlineMedium,
        ),
        const SizedBox(height: AppDesignTokens.spaceSm),
        Text(
          l10n.foundationWelcomeSubtitle,
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: AppDesignTokens.spaceXl),
        ActiveQueueSection(queue: queue, onRefresh: onQueueRefresh),
        const SizedBox(height: AppDesignTokens.spaceXl),
        Divider(color: Theme.of(context).colorScheme.outlineVariant),
        const SizedBox(height: AppDesignTokens.spaceMd),
        ProfessionalIdentityCard(session: session),
        const SizedBox(height: AppDesignTokens.spaceMd),
        ProfessionalSecurityCard(
          enabled: session.biometricEnabled,
          loading: loading,
          onPressed: onBiometricsPressed,
        ),
        if (authError != null) ...[
          const SizedBox(height: AppDesignTokens.spaceMd),
          _InlineError(message: authError!),
        ],
        const SizedBox(height: AppDesignTokens.spaceMd),
        AppButton(
          label: l10n.authLogout,
          icon: Icons.logout,
          variant: AppButtonVariant.destructiveSecondary,
          expand: true,
          loading: loading,
          onPressed: loading ? null : onLogoutPressed,
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
    return Text(
      message,
      textAlign: TextAlign.center,
      style: Theme.of(
        context,
      ).textTheme.bodySmall?.copyWith(color: colors.error),
    );
  }
}
