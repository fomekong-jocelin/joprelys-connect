import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/app_config.dart';
import '../../../../core/i18n/locale_controller.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../core/theme/theme_controller.dart';
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
