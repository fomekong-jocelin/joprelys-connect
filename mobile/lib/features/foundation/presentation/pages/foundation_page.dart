import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../features/auth/application/auth_controller.dart';
import '../../../../features/auth/domain/professional_session.dart';
import '../../../../features/auth/presentation/widgets/auth_error_message.dart';
import '../../../../features/dashboard/application/active_queue_controller.dart';
import '../../../../features/dashboard/domain/active_visit.dart';
import '../../../../features/dashboard/presentation/widgets/active_queue_section.dart';
import '../../../../l10n/app_localizations.dart';
import '../widgets/professional_app_bar.dart';
import '../widgets/professional_dashboard_intro.dart';
import '../widgets/professional_profile_sheet.dart';

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

    return _ProfessionalHome(
      session: session,
      queue: ref.watch(activeQueueControllerProvider),
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
    return Scaffold(
      appBar: ProfessionalAppBar(
        session: session,
        onProfilePressed: () => _showProfile(context),
      ),
      body: _DashboardScrollBody(
        sessionName: session.name,
        queue: queue,
        authError: authError,
        onRefresh: onQueueRefresh,
      ),
    );
  }

  void _showProfile(BuildContext context) {
    ProfessionalProfileSheet.show(
      context,
      session: session,
      loading: loading,
      authError: authError,
      onBiometricsPressed: onBiometricsPressed,
      onLogoutPressed: onLogoutPressed,
    );
  }
}

class _DashboardScrollBody extends StatelessWidget {
  const _DashboardScrollBody({
    required this.sessionName,
    required this.queue,
    required this.onRefresh,
    this.authError,
  });

  final String sessionName;
  final AsyncValue<List<ActiveVisit>> queue;
  final String? authError;
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: RefreshIndicator(
        onRefresh: onRefresh,
        child: SingleChildScrollView(
          key: const ValueKey('professional-dashboard-scroll'),
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 28),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 520),
              child: _DashboardContent(
                sessionName: sessionName,
                queue: queue,
                authError: authError,
                onRefresh: onRefresh,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _DashboardContent extends StatelessWidget {
  const _DashboardContent({
    required this.sessionName,
    required this.queue,
    required this.onRefresh,
    this.authError,
  });

  final String sessionName;
  final AsyncValue<List<ActiveVisit>> queue;
  final String? authError;
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ProfessionalDashboardIntro(
          sessionName: sessionName,
          queueCount: queue.value?.length,
        ),
        if (authError != null) ...[
          const SizedBox(height: AppDesignTokens.spaceMd),
          _InlineError(message: authError!),
        ],
        const SizedBox(height: AppDesignTokens.spaceLg),
        ActiveQueueSection(queue: queue, onRefresh: onRefresh),
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
