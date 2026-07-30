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
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
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
          child: _ThemeControl(
            icon: _themeIcon(themeMode),
            tooltip: l10n.foundationThemeTitle,
          ),
        ),
        _LanguageSegment(
          activeLanguageCode: locale.languageCode,
          frenchLabel: l10n.languageFrench,
          englishLabel: l10n.languageEnglish,
          onFrenchSelected: ref.read(appLocaleProvider.notifier).useFrench,
          onEnglishSelected: ref.read(appLocaleProvider.notifier).useEnglish,
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

class _ThemeControl extends StatelessWidget {
  const _ThemeControl({required this.icon, required this.tooltip});

  final IconData icon;
  final String tooltip;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      width: AppDesignTokens.minTouchTarget,
      height: AppDesignTokens.minTouchTarget,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: colors.surface.withValues(alpha: 0.92),
        border: Border.all(color: colors.outlineVariant),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
        boxShadow: Theme.of(context).brightness == Brightness.dark
            ? AppDesignTokens.darkPanelShadow
            : AppDesignTokens.lightPanelShadow,
      ),
      child: Tooltip(message: tooltip, child: Icon(icon, size: 20)),
    );
  }
}

class _LanguageSegment extends StatelessWidget {
  const _LanguageSegment({
    required this.activeLanguageCode,
    required this.frenchLabel,
    required this.englishLabel,
    required this.onFrenchSelected,
    required this.onEnglishSelected,
  });

  final String activeLanguageCode;
  final String frenchLabel;
  final String englishLabel;
  final VoidCallback onFrenchSelected;
  final VoidCallback onEnglishSelected;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(AppDesignTokens.spaceXs),
      decoration: BoxDecoration(
        color: colors.surface.withValues(alpha: 0.92),
        border: Border.all(color: colors.outlineVariant),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
        boxShadow: Theme.of(context).brightness == Brightness.dark
            ? AppDesignTokens.darkPanelShadow
            : AppDesignTokens.lightPanelShadow,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          _LanguageButton(
            code: 'FR',
            tooltip: frenchLabel,
            selected: activeLanguageCode == 'fr',
            onPressed: onFrenchSelected,
          ),
          const SizedBox(width: AppDesignTokens.spaceXs),
          _LanguageButton(
            code: 'EN',
            tooltip: englishLabel,
            selected: activeLanguageCode == 'en',
            onPressed: onEnglishSelected,
          ),
        ],
      ),
    );
  }
}

class _LanguageButton extends StatelessWidget {
  const _LanguageButton({
    required this.code,
    required this.tooltip,
    required this.selected,
    required this.onPressed,
  });

  final String code;
  final String tooltip;
  final bool selected;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Semantics(
      selected: selected,
      button: true,
      label: tooltip,
      child: InkWell(
        onTap: onPressed,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 150),
          constraints: const BoxConstraints(minWidth: 48, minHeight: 36),
          padding: const EdgeInsets.symmetric(
            horizontal: AppDesignTokens.spaceSm,
          ),
          decoration: BoxDecoration(
            color: selected ? colors.surface : Colors.transparent,
            border: Border.all(
              color: selected ? colors.primary : Colors.transparent,
            ),
            borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
          ),
          child: Center(
            child: Text(
              code,
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                color: selected ? colors.primary : colors.onSurfaceVariant,
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
        ),
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
