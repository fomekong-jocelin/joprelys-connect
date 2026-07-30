import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/app_config.dart';
import '../../../../core/i18n/locale_controller.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../core/theme/theme_controller.dart';
import '../../../../features/auth/application/auth_controller.dart';
import '../../../../features/auth/domain/professional_session.dart';
import '../../../../features/auth/presentation/widgets/auth_error_message.dart';
import '../../../../features/dashboard/application/active_queue_controller.dart';
import '../../../../features/dashboard/domain/active_visit.dart';
import '../../../../features/dashboard/presentation/dashboard_localizations.dart';
import '../../../../features/dashboard/presentation/widgets/active_queue_section.dart';
import '../../../../l10n/app_localizations.dart';
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

class _ProfessionalHome extends ConsumerWidget {
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
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    final preferredName = _preferredName(session.name);
    final queueCount = queue.value?.length;

    return Scaffold(
      body: DecoratedBox(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              colors.primary.withValues(alpha: 0.1),
              theme.scaffoldBackgroundColor,
              theme.scaffoldBackgroundColor,
            ],
            stops: const [0, 0.24, 1],
          ),
        ),
        child: SafeArea(
          child: RefreshIndicator(
            onRefresh: onQueueRefresh,
            child: SingleChildScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(16, 10, 16, 28),
              child: Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 520),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _DashboardTopBar(
                        session: session,
                        onProfilePressed: () => _openProfessionalSheet(
                          context,
                          session: session,
                          loading: loading,
                          authError: authError,
                          onBiometricsPressed: onBiometricsPressed,
                          onLogoutPressed: onLogoutPressed,
                        ),
                      ),
                      const SizedBox(height: AppDesignTokens.spaceXl),
                      Text(
                        l10n.dashboardWorkspaceLabel,
                        style: theme.textTheme.labelMedium?.copyWith(
                          color: colors.primary,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 1.2,
                        ),
                      ),
                      const SizedBox(height: AppDesignTokens.spaceSm),
                      Text(
                        l10n.dashboardGreeting(preferredName),
                        style: theme.textTheme.headlineMedium?.copyWith(
                          fontWeight: FontWeight.w900,
                          letterSpacing: -0.7,
                          height: 1.15,
                        ),
                      ),
                      const SizedBox(height: AppDesignTokens.spaceSm),
                      Text(
                        queueCount == null
                            ? l10n.foundationWelcomeSubtitle
                            : l10n.dashboardOverviewSubtitle(queueCount),
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: colors.onSurfaceVariant,
                          height: 1.45,
                        ),
                      ),
                      if (authError != null) ...[
                        const SizedBox(height: AppDesignTokens.spaceMd),
                        _InlineError(message: authError!),
                      ],
                      const SizedBox(height: AppDesignTokens.spaceXl),
                      ActiveQueueSection(
                        queue: queue,
                        onRefresh: onQueueRefresh,
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  String _preferredName(String fullName) {
    final parts = fullName
        .trim()
        .split(RegExp(r'\s+'))
        .where((part) => part.isNotEmpty)
        .toList(growable: false);
    if (parts.isEmpty) {
      return fullName;
    }
    if (parts.length == 1) {
      return _titleCase(parts.first);
    }
    final first = parts.first;
    final firstLooksLikeSurname =
        first == first.toUpperCase() && first != first.toLowerCase();
    return _titleCase(firstLooksLikeSurname ? parts.last : first);
  }

  String _titleCase(String value) {
    if (value.isEmpty) {
      return value;
    }
    return '${value[0].toUpperCase()}${value.substring(1).toLowerCase()}';
  }

  void _openProfessionalSheet(
    BuildContext context, {
    required ProfessionalSession session,
    required bool loading,
    required String? authError,
    required VoidCallback onBiometricsPressed,
    required VoidCallback onLogoutPressed,
  }) {
    final l10n = AppLocalizations.of(context);
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        final sheetTheme = Theme.of(sheetContext);
        return FractionallySizedBox(
          heightFactor: 0.86,
          child: Container(
            decoration: BoxDecoration(
              color: sheetTheme.colorScheme.surface,
              borderRadius: const BorderRadius.vertical(
                top: Radius.circular(AppDesignTokens.radiusLg),
              ),
            ),
            child: SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Center(
                    child: Container(
                      width: 42,
                      height: 4,
                      decoration: BoxDecoration(
                        color: sheetTheme.colorScheme.outlineVariant,
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ),
                  const SizedBox(height: AppDesignTokens.spaceMd),
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          l10n.dashboardProfileTitle,
                          style: sheetTheme.textTheme.titleLarge?.copyWith(
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                      ),
                      IconButton(
                        tooltip: l10n.dashboardClose,
                        onPressed: () => Navigator.of(sheetContext).pop(),
                        icon: const Icon(Icons.close_rounded),
                      ),
                    ],
                  ),
                  const SizedBox(height: AppDesignTokens.spaceMd),
                  ProfessionalIdentityCard(session: session),
                  const SizedBox(height: AppDesignTokens.spaceMd),
                  ProfessionalSecurityCard(
                    enabled: session.biometricEnabled,
                    loading: loading,
                    onPressed: () {
                      Navigator.of(sheetContext).pop();
                      onBiometricsPressed();
                    },
                  ),
                  if (authError != null) ...[
                    const SizedBox(height: AppDesignTokens.spaceMd),
                    _InlineError(message: authError),
                  ],
                  const SizedBox(height: AppDesignTokens.spaceLg),
                  AppButton(
                    label: l10n.authLogout,
                    icon: Icons.logout_rounded,
                    variant: AppButtonVariant.destructiveSecondary,
                    expand: true,
                    loading: loading,
                    onPressed: loading
                        ? null
                        : () {
                            Navigator.of(sheetContext).pop();
                            onLogoutPressed();
                          },
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class _DashboardTopBar extends ConsumerWidget {
  const _DashboardTopBar({
    required this.session,
    required this.onProfilePressed,
  });

  final ProfessionalSession session;
  final VoidCallback onProfilePressed;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final locale = ref.watch(appLocaleProvider);
    final themeMode = ref.watch(themeModeProvider);
    final colors = Theme.of(context).colorScheme;

    return Row(
      children: [
        Container(
          width: 38,
          height: 38,
          padding: const EdgeInsets.all(5),
          decoration: BoxDecoration(
            color: colors.surface,
            borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
            boxShadow: Theme.of(context).brightness == Brightness.dark
                ? AppDesignTokens.darkPanelShadow
                : AppDesignTokens.lightPanelShadow,
          ),
          child: Image.asset(
            AppConfig.logoIconAsset,
            fit: BoxFit.contain,
            filterQuality: FilterQuality.high,
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                AppConfig.appShortName,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w900,
                  height: 1.05,
                ),
              ),
              Text(
                AppConfig.productName,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: colors.onSurfaceVariant,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
        ),
        PopupMenuButton<String>(
          tooltip: l10n.foundationLanguageTitle,
          initialValue: locale.languageCode,
          onSelected: (value) {
            final controller = ref.read(appLocaleProvider.notifier);
            value == 'fr' ? controller.useFrench() : controller.useEnglish();
          },
          itemBuilder: (context) => [
            PopupMenuItem(value: 'fr', child: Text(l10n.languageFrench)),
            PopupMenuItem(value: 'en', child: Text(l10n.languageEnglish)),
          ],
          child: _ToolbarControl(label: locale.languageCode.toUpperCase()),
        ),
        const SizedBox(width: 6),
        PopupMenuButton<ThemeMode>(
          tooltip: l10n.foundationThemeTitle,
          initialValue: themeMode,
          onSelected: ref.read(themeModeProvider.notifier).setMode,
          itemBuilder: (context) => [
            PopupMenuItem(
              value: ThemeMode.system,
              child: _ThemeMenuItem(
                icon: Icons.brightness_auto_outlined,
                label: l10n.themeSystem,
              ),
            ),
            PopupMenuItem(
              value: ThemeMode.light,
              child: _ThemeMenuItem(
                icon: Icons.light_mode_outlined,
                label: l10n.themeLight,
              ),
            ),
            PopupMenuItem(
              value: ThemeMode.dark,
              child: _ThemeMenuItem(
                icon: Icons.dark_mode_outlined,
                label: l10n.themeDark,
              ),
            ),
          ],
          child: _ToolbarControl(icon: _themeIcon(themeMode)),
        ),
        const SizedBox(width: 6),
        Tooltip(
          message: l10n.dashboardProfileTooltip,
          child: Material(
            color: colors.primary,
            shape: const CircleBorder(),
            child: InkWell(
              onTap: onProfilePressed,
              customBorder: const CircleBorder(),
              child: SizedBox.square(
                dimension: 40,
                child: Center(
                  child: Text(
                    _initials(session.name),
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      color: AppDesignTokens.onStrongColor,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }

  IconData _themeIcon(ThemeMode mode) => switch (mode) {
    ThemeMode.light => Icons.light_mode_outlined,
    ThemeMode.dark => Icons.dark_mode_outlined,
    ThemeMode.system => Icons.brightness_auto_outlined,
  };

  String _initials(String name) {
    final parts = name
        .trim()
        .split(RegExp(r'\s+'))
        .where((part) => part.isNotEmpty)
        .toList(growable: false);
    if (parts.isEmpty) {
      return '?';
    }
    if (parts.length == 1) {
      return parts.first[0].toUpperCase();
    }
    return '${parts.first[0]}${parts.last[0]}'.toUpperCase();
  }
}

class _ToolbarControl extends StatelessWidget {
  const _ToolbarControl({this.label, this.icon})
    : assert(label != null || icon != null);

  final String? label;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      constraints: const BoxConstraints(
        minWidth: 40,
        minHeight: AppDesignTokens.minTouchTarget,
      ),
      padding: const EdgeInsets.symmetric(horizontal: 10),
      decoration: BoxDecoration(
        color: colors.surface.withValues(alpha: 0.76),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      alignment: Alignment.center,
      child: icon == null
          ? Text(
              label!,
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                fontWeight: FontWeight.w900,
              ),
            )
          : Icon(icon, size: 19),
    );
  }
}

class _ThemeMenuItem extends StatelessWidget {
  const _ThemeMenuItem({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 18),
        const SizedBox(width: AppDesignTokens.spaceSm),
        Text(label),
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
        style: Theme.of(context).textTheme.bodySmall?.copyWith(
          color: colors.onErrorContainer,
        ),
      ),
    );
  }
}
