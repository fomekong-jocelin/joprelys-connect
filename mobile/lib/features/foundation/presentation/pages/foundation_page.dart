import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../features/auth/application/auth_controller.dart';
import '../../../../features/auth/application/effective_access_controller.dart';
import '../../../../features/auth/domain/effective_access.dart';
import '../../../../features/auth/domain/professional_session.dart';
import '../../../../features/auth/presentation/widgets/auth_error_message.dart';
import '../../../../features/dashboard/application/active_queue_controller.dart';
import '../../../../features/dashboard/domain/active_visit.dart';
import '../../../../features/dashboard/presentation/pages/patients_page.dart';
import '../../../../features/dashboard/presentation/widgets/active_queue_section.dart';
import '../../../../l10n/app_localizations.dart';
import '../mobile_workspace_localizations.dart';
import '../widgets/professional_app_bar.dart';
import '../widgets/professional_dashboard_intro.dart';
import '../widgets/professional_profile_sheet.dart';
import 'workspace_profile_page.dart';

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

    final access = ref.watch(effectiveAccessProvider);
    return access.when(
      loading: () => Scaffold(
        appBar: ProfessionalAppBar(
          session: session,
          onProfilePressed: () => _showProfile(
            context,
            session: session,
            loading: auth.isLoading,
            authError: authErrorMessage(l10n, authState?.errorCode),
            onBiometricsPressed: session.biometricEnabled
                ? ref.read(authControllerProvider.notifier).disableBiometrics
                : () => ref
                      .read(authControllerProvider.notifier)
                      .enableBiometrics(reason: l10n.authBiometricEnableReason),
            onLogoutPressed: ref.read(authControllerProvider.notifier).logout,
          ),
        ),
        body: const Center(child: CircularProgressIndicator()),
      ),
      error: (error, stackTrace) => Scaffold(
        appBar: ProfessionalAppBar(
          session: session,
          onProfilePressed: () => _showProfile(
            context,
            session: session,
            loading: auth.isLoading,
            authError: authErrorMessage(l10n, authState?.errorCode),
            onBiometricsPressed: session.biometricEnabled
                ? ref.read(authControllerProvider.notifier).disableBiometrics
                : () => ref
                      .read(authControllerProvider.notifier)
                      .enableBiometrics(reason: l10n.authBiometricEnableReason),
            onLogoutPressed: ref.read(authControllerProvider.notifier).logout,
          ),
        ),
        body: _AccessLoadError(onRetry: () => refreshEffectiveAccess(ref)),
      ),
      data: (effectiveAccess) {
        final canViewQueue = effectiveAccess.hasPermission('VISIT_READ');
        final queue = canViewQueue
            ? ref.watch(activeQueueControllerProvider)
            : const AsyncValue<List<ActiveVisit>>.data(<ActiveVisit>[]);
        return _ProfessionalWorkspace(
          session: session,
          access: effectiveAccess,
          queue: queue,
          canViewQueue: canViewQueue,
          authError: authErrorMessage(l10n, authState?.errorCode),
          loading: auth.isLoading,
          onQueueRefresh: canViewQueue
              ? ref.read(activeQueueControllerProvider.notifier).refreshQueue
              : () async {},
          onBiometricsPressed: session.biometricEnabled
              ? ref.read(authControllerProvider.notifier).disableBiometrics
              : () => ref
                    .read(authControllerProvider.notifier)
                    .enableBiometrics(reason: l10n.authBiometricEnableReason),
          onLogoutPressed: ref.read(authControllerProvider.notifier).logout,
        );
      },
    );
  }

  static void _showProfile(
    BuildContext context, {
    required ProfessionalSession session,
    required bool loading,
    required String? authError,
    required VoidCallback onBiometricsPressed,
    required VoidCallback onLogoutPressed,
  }) {
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

class _ProfessionalWorkspace extends StatefulWidget {
  const _ProfessionalWorkspace({
    required this.session,
    required this.access,
    required this.queue,
    required this.canViewQueue,
    required this.loading,
    required this.onQueueRefresh,
    required this.onBiometricsPressed,
    required this.onLogoutPressed,
    this.authError,
  });

  final ProfessionalSession session;
  final EffectiveAccess access;
  final AsyncValue<List<ActiveVisit>> queue;
  final bool canViewQueue;
  final String? authError;
  final bool loading;
  final Future<void> Function() onQueueRefresh;
  final VoidCallback onBiometricsPressed;
  final VoidCallback onLogoutPressed;

  @override
  State<_ProfessionalWorkspace> createState() => _ProfessionalWorkspaceState();
}

class _ProfessionalWorkspaceState extends State<_ProfessionalWorkspace> {
  int _selectedIndex = 0;

  List<_WorkspaceDestination> _destinations(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final destinations = <_WorkspaceDestination>[
      _WorkspaceDestination(
        label: l10n.navHome,
        icon: Icons.home_outlined,
        selectedIcon: Icons.home_rounded,
        child: _DashboardScrollBody(
          sessionName: widget.session.name,
          queue: widget.queue,
          canViewQueue: widget.canViewQueue,
          authError: widget.authError,
          onRefresh: widget.onQueueRefresh,
        ),
      ),
    ];

    if (widget.access.hasPermission('PATIENT_READ')) {
      destinations.add(
        _WorkspaceDestination(
          label: l10n.navPatients,
          icon: Icons.people_alt_outlined,
          selectedIcon: Icons.people_alt_rounded,
          child: PatientsPage(access: widget.access),
        ),
      );
    }

    destinations.add(
      _WorkspaceDestination(
        label: l10n.navProfile,
        icon: Icons.person_outline_rounded,
        selectedIcon: Icons.person_rounded,
        child: WorkspaceProfilePage(
          session: widget.session,
          access: widget.access,
          onOpenAccount: () => _showProfile(context),
        ),
      ),
    );
    return destinations;
  }

  @override
  Widget build(BuildContext context) {
    final destinations = _destinations(context);
    final safeIndex = _selectedIndex.clamp(0, destinations.length - 1).toInt();
    final pages = destinations
        .map((item) => item.child)
        .toList(growable: false);
    final colors = Theme.of(context).colorScheme;

    return LayoutBuilder(
      builder: (context, constraints) {
        final useRail = constraints.maxWidth >= 760;
        final content = IndexedStack(index: safeIndex, children: pages);

        if (useRail) {
          return Scaffold(
            appBar: ProfessionalAppBar(
              session: widget.session,
              onProfilePressed: () => _showProfile(context),
            ),
            body: Row(
              children: [
                SafeArea(
                  top: false,
                  child: NavigationRail(
                    selectedIndex: safeIndex,
                    onDestinationSelected: _select,
                    labelType: NavigationRailLabelType.all,
                    groupAlignment: -0.75,
                    destinations: destinations
                        .map(
                          (item) => NavigationRailDestination(
                            icon: Icon(item.icon),
                            selectedIcon: Icon(item.selectedIcon),
                            label: Text(item.label),
                          ),
                        )
                        .toList(growable: false),
                  ),
                ),
                VerticalDivider(
                  width: 1,
                  color: colors.outlineVariant.withValues(alpha: 0.7),
                ),
                Expanded(child: content),
              ],
            ),
          );
        }

        return Scaffold(
          appBar: ProfessionalAppBar(
            session: widget.session,
            onProfilePressed: () => _showProfile(context),
          ),
          body: content,
          bottomNavigationBar: NavigationBar(
            selectedIndex: safeIndex,
            onDestinationSelected: _select,
            labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
            destinations: destinations
                .map(
                  (item) => NavigationDestination(
                    icon: Icon(item.icon),
                    selectedIcon: Icon(item.selectedIcon),
                    label: item.label,
                  ),
                )
                .toList(growable: false),
          ),
        );
      },
    );
  }

  void _select(int index) {
    if (index == _selectedIndex) return;
    setState(() => _selectedIndex = index);
  }

  void _showProfile(BuildContext context) {
    ProfessionalProfileSheet.show(
      context,
      session: widget.session,
      loading: widget.loading,
      authError: widget.authError,
      onBiometricsPressed: widget.onBiometricsPressed,
      onLogoutPressed: widget.onLogoutPressed,
    );
  }
}

class _WorkspaceDestination {
  const _WorkspaceDestination({
    required this.label,
    required this.icon,
    required this.selectedIcon,
    required this.child,
  });

  final String label;
  final IconData icon;
  final IconData selectedIcon;
  final Widget child;
}

class _DashboardScrollBody extends StatelessWidget {
  const _DashboardScrollBody({
    required this.sessionName,
    required this.queue,
    required this.canViewQueue,
    required this.onRefresh,
    this.authError,
  });

  final String sessionName;
  final AsyncValue<List<ActiveVisit>> queue;
  final bool canViewQueue;
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
              constraints: const BoxConstraints(maxWidth: 620),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  ProfessionalDashboardIntro(
                    sessionName: sessionName,
                    queueCount: canViewQueue ? queue.value?.length : null,
                  ),
                  if (authError != null) ...[
                    const SizedBox(height: AppDesignTokens.spaceMd),
                    _InlineError(message: authError!),
                  ],
                  if (canViewQueue) ...[
                    const SizedBox(height: AppDesignTokens.spaceLg),
                    ActiveQueueSection(queue: queue, onRefresh: onRefresh),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _AccessLoadError extends StatelessWidget {
  const _AccessLoadError({required this.onRetry});
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.admin_panel_settings_outlined,
              size: 48,
              color: colors.error,
            ),
            const SizedBox(height: 16),
            Text(l10n.recordLoadError, textAlign: TextAlign.center),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh_rounded),
              label: Text(l10n.recordRefresh),
            ),
          ],
        ),
      ),
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
