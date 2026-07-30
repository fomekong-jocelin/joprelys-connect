import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/i18n/locale_controller.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../core/theme/theme_controller.dart';
import '../../../../l10n/app_localizations.dart';

class AuthPreferencesBar extends ConsumerWidget {
  const AuthPreferencesBar({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final locale = ref.watch(appLocaleProvider);
    final themeMode = ref.watch(themeModeProvider);

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        PopupMenuButton<String>(
          tooltip: l10n.foundationLanguageTitle,
          onSelected: (value) {
            final controller = ref.read(appLocaleProvider.notifier);
            value == 'fr' ? controller.useFrench() : controller.useEnglish();
          },
          itemBuilder: (context) => [
            PopupMenuItem(
              value: 'fr',
              child: Row(
                children: [
                  const Text('🇫🇷'),
                  const SizedBox(width: AppDesignTokens.spaceSm),
                  Text(l10n.languageFrench),
                ],
              ),
            ),
            PopupMenuItem(
              value: 'en',
              child: Row(
                children: [
                  const Text('🇬🇧'),
                  const SizedBox(width: AppDesignTokens.spaceSm),
                  Text(l10n.languageEnglish),
                ],
              ),
            ),
          ],
          child: _PreferenceControl(
            label: locale.languageCode.toUpperCase(),
            leading: Text(locale.languageCode == 'fr' ? '🇫🇷' : '🇬🇧'),
          ),
        ),
        const SizedBox(width: AppDesignTokens.spaceSm),
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
          child: _PreferenceControl(
            label: l10n.foundationThemeTitle,
            leading: Icon(_themeIcon(themeMode), size: 18),
            compact: true,
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
}

class _PreferenceControl extends StatelessWidget {
  const _PreferenceControl({
    required this.label,
    required this.leading,
    this.compact = false,
  });

  final String label;
  final Widget leading;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      constraints: const BoxConstraints(minHeight: AppDesignTokens.minTouchTarget),
      padding: EdgeInsets.symmetric(
        horizontal: compact ? AppDesignTokens.spaceSm : 12,
        vertical: AppDesignTokens.spaceSm,
      ),
      decoration: BoxDecoration(
        color: colors.surface.withValues(alpha: 0.78),
        border: Border.all(color: colors.outlineVariant),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          leading,
          if (!compact) ...[
            const SizedBox(width: 6),
            Text(
              label,
              style: Theme.of(context).textTheme.labelLarge,
            ),
          ],
          const SizedBox(width: 2),
          const Icon(Icons.arrow_drop_down, size: 18),
        ],
      ),
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
