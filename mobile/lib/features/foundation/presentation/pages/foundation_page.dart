import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/app_config.dart';
import '../../../../core/i18n/locale_controller.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../core/theme/theme_controller.dart';
import '../../../../features/auth/application/auth_controller.dart';
import '../../../../features/auth/presentation/widgets/auth_error_message.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_badge.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../../../shared/widgets/app_card.dart';

class FoundationPage extends ConsumerWidget {
  const FoundationPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final locale = ref.watch(appLocaleProvider);
    final localeController = ref.read(appLocaleProvider.notifier);
    final themeMode = ref.watch(themeModeProvider);
    final themeController = ref.read(themeModeProvider.notifier);
    final auth = ref.watch(authControllerProvider);
    final authState = auth.value;
    final session = authState?.session;
    final authError = authErrorMessage(l10n, authState?.errorCode);

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(AppDesignTokens.spaceLg),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 520),
              child: AppCard(
                child: Semantics(
                  label: AppConfig.appName,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        Icons.health_and_safety_outlined,
                        size: 56,
                        color: Theme.of(context).colorScheme.primary,
                      ),
                      const SizedBox(height: AppDesignTokens.spaceMd),
                      Text(
                        AppConfig.appName,
                        textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.headlineMedium,
                      ),
                      const SizedBox(height: AppDesignTokens.spaceSm),
                      AppBadge(
                        label: l10n.foundationBadge,
                        tone: AppSemanticTone.info,
                      ),
                      const SizedBox(height: AppDesignTokens.spaceLg),
                      if (session == null)
                        const CircularProgressIndicator()
                      else ...[
                        Text(
                          l10n.authSignedInAs(session.name),
                          textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                        const SizedBox(height: AppDesignTokens.spaceXs),
                        Text(
                          session.email,
                          textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.bodyMedium,
                        ),
                        const SizedBox(height: AppDesignTokens.spaceXs),
                        Text(
                          l10n.authRoleLabel(session.role),
                          textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                        const SizedBox(height: AppDesignTokens.spaceMd),
                        AppButton(
                          label: session.biometricEnabled
                              ? l10n.authDisableBiometrics
                              : l10n.authEnableBiometrics,
                          icon: Icons.fingerprint,
                          variant: AppButtonVariant.secondary,
                          expand: true,
                          loading: auth.isLoading,
                          onPressed: auth.isLoading
                              ? null
                              : session.biometricEnabled
                              ? ref
                                    .read(authControllerProvider.notifier)
                                    .disableBiometrics
                              : () => ref
                                    .read(authControllerProvider.notifier)
                                    .enableBiometrics(
                                      reason: l10n.authBiometricEnableReason,
                                    ),
                        ),
                        const SizedBox(height: AppDesignTokens.spaceSm),
                        AppButton(
                          label: l10n.authLogout,
                          icon: Icons.logout,
                          variant: AppButtonVariant.destructive,
                          expand: true,
                          loading: auth.isLoading,
                          onPressed: auth.isLoading
                              ? null
                              : ref
                                    .read(authControllerProvider.notifier)
                                    .logout,
                        ),
                      ],
                      if (authError != null) ...[
                        const SizedBox(height: AppDesignTokens.spaceMd),
                        Text(
                          authError,
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            color: Theme.of(context).colorScheme.error,
                          ),
                        ),
                      ],
                      const SizedBox(height: AppDesignTokens.spaceLg),
                      Text(
                        l10n.foundationLanguageTitle,
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: AppDesignTokens.spaceSm),
                      Wrap(
                        alignment: WrapAlignment.center,
                        spacing: AppDesignTokens.spaceSm,
                        runSpacing: AppDesignTokens.spaceSm,
                        children: [
                          AppButton(
                            label: l10n.languageFrench,
                            onPressed: localeController.useFrench,
                            variant: locale.languageCode == 'fr'
                                ? AppButtonVariant.primary
                                : AppButtonVariant.secondary,
                          ),
                          AppButton(
                            label: l10n.languageEnglish,
                            onPressed: localeController.useEnglish,
                            variant: locale.languageCode == 'en'
                                ? AppButtonVariant.primary
                                : AppButtonVariant.secondary,
                          ),
                        ],
                      ),
                      const SizedBox(height: AppDesignTokens.spaceLg),
                      Text(
                        l10n.foundationThemeTitle,
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: AppDesignTokens.spaceSm),
                      Wrap(
                        alignment: WrapAlignment.center,
                        spacing: AppDesignTokens.spaceSm,
                        runSpacing: AppDesignTokens.spaceSm,
                        children: [
                          AppButton(
                            label: l10n.themeSystem,
                            onPressed: themeController.useSystem,
                            variant: themeMode == ThemeMode.system
                                ? AppButtonVariant.primary
                                : AppButtonVariant.secondary,
                          ),
                          AppButton(
                            label: l10n.themeLight,
                            onPressed: themeController.useLight,
                            variant: themeMode == ThemeMode.light
                                ? AppButtonVariant.primary
                                : AppButtonVariant.secondary,
                          ),
                          AppButton(
                            label: l10n.themeDark,
                            onPressed: themeController.useDark,
                            variant: themeMode == ThemeMode.dark
                                ? AppButtonVariant.primary
                                : AppButtonVariant.secondary,
                          ),
                        ],
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
}
