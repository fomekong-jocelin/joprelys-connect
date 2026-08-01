import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/app_config.dart';
import '../../../../core/i18n/locale_controller.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../core/theme/theme_controller.dart';
import '../../../../features/auth/domain/professional_session.dart';
import '../../../../features/dashboard/presentation/dashboard_localizations.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_brand_lockup.dart';

class ProfessionalAppBar extends ConsumerWidget implements PreferredSizeWidget {
  const ProfessionalAppBar({
    required this.session,
    required this.onProfilePressed,
    super.key,
  });

  static const double _height = 64;

  final ProfessionalSession session;
  final VoidCallback onProfilePressed;

  @override
  Size get preferredSize => const Size.fromHeight(_height + 1);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;

    return AppBar(
      key: const ValueKey('professional-fixed-app-bar'),
      automaticallyImplyLeading: false,
      toolbarHeight: _height,
      titleSpacing: AppDesignTokens.spaceMd,
      elevation: 0,
      scrolledUnderElevation: 1,
      surfaceTintColor: Colors.transparent,
      backgroundColor: colors.surface,
      title: const _BrandTitle(),
      actions: [
        _LanguageAction(
          l10n: l10n,
          languageCode: ref.watch(appLocaleProvider).languageCode,
          onSelected: (value) => _setLocale(ref, value),
        ),
        _ThemeAction(
          l10n: l10n,
          themeMode: ref.watch(themeModeProvider),
          onSelected: ref.read(themeModeProvider.notifier).setMode,
        ),
        _ProfileAction(
          name: session.name,
          tooltip: l10n.dashboardProfileTooltip,
          onPressed: onProfilePressed,
        ),
      ],
      bottom: _AppBarDivider(color: colors.outlineVariant),
    );
  }

  void _setLocale(WidgetRef ref, String value) {
    final controller = ref.read(appLocaleProvider.notifier);
    value == 'fr' ? controller.useFrench() : controller.useEnglish();
  }
}

class _BrandTitle extends StatelessWidget {
  const _BrandTitle();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        const AppBrandMark(size: 40),
        const SizedBox(width: AppDesignTokens.spaceSm),
        Flexible(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                AppConfig.appShortName,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w900,
                  height: 1.05,
                ),
              ),
              Text(
                AppConfig.productName,
                maxLines: 1,
                style: theme.textTheme.labelSmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                  fontWeight: FontWeight.w700,
                  height: 1.1,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _LanguageAction extends StatelessWidget {
  const _LanguageAction({
    required this.l10n,
    required this.languageCode,
    required this.onSelected,
  });

  final AppLocalizations l10n;
  final String languageCode;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton<String>(
      tooltip: l10n.foundationLanguageTitle,
      initialValue: languageCode,
      onSelected: onSelected,
      itemBuilder: (context) => [
        PopupMenuItem(value: 'fr', child: Text(l10n.languageFrench)),
        PopupMenuItem(value: 'en', child: Text(l10n.languageEnglish)),
      ],
      child: _AppBarControl(label: languageCode.toUpperCase()),
    );
  }
}

class _ThemeAction extends StatelessWidget {
  const _ThemeAction({
    required this.l10n,
    required this.themeMode,
    required this.onSelected,
  });

  final AppLocalizations l10n;
  final ThemeMode themeMode;
  final ValueChanged<ThemeMode> onSelected;

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton<ThemeMode>(
      tooltip: l10n.foundationThemeTitle,
      initialValue: themeMode,
      onSelected: onSelected,
      itemBuilder: (context) => [
        _item(
          ThemeMode.system,
          Icons.brightness_auto_outlined,
          l10n.themeSystem,
        ),
        _item(ThemeMode.light, Icons.light_mode_outlined, l10n.themeLight),
        _item(ThemeMode.dark, Icons.dark_mode_outlined, l10n.themeDark),
      ],
      child: _AppBarControl(icon: _themeIcon(themeMode)),
    );
  }

  PopupMenuItem<ThemeMode> _item(ThemeMode value, IconData icon, String label) {
    return PopupMenuItem(
      value: value,
      child: _ThemeMenuItem(icon: icon, label: label),
    );
  }
}

class _ProfileAction extends StatelessWidget {
  const _ProfileAction({
    required this.name,
    required this.tooltip,
    required this.onPressed,
  });

  final String name;
  final String tooltip;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Padding(
      padding: const EdgeInsets.only(right: AppDesignTokens.spaceSm),
      child: Tooltip(
        message: tooltip,
        child: Material(
          color: theme.colorScheme.primary,
          shape: const CircleBorder(),
          child: InkWell(
            onTap: onPressed,
            customBorder: const CircleBorder(),
            child: SizedBox.square(
              dimension: AppDesignTokens.minTouchTarget,
              child: Center(
                child: Text(
                  _initials(name),
                  style: theme.textTheme.labelLarge?.copyWith(
                    color: AppDesignTokens.onStrongColor,
                    fontWeight: FontWeight.w900,
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

class _AppBarControl extends StatelessWidget {
  const _AppBarControl({this.label, this.icon})
    : assert(label != null || icon != null);

  final String? label;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return SizedBox.square(
      dimension: AppDesignTokens.minTouchTarget,
      child: Center(
        child: icon == null
            ? Text(
                label!,
                style: theme.textTheme.labelMedium?.copyWith(
                  color: theme.colorScheme.onSurface,
                  fontWeight: FontWeight.w900,
                ),
              )
            : Icon(icon, size: 20, color: theme.colorScheme.onSurface),
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

class _AppBarDivider extends StatelessWidget implements PreferredSizeWidget {
  const _AppBarDivider({required this.color});

  final Color color;

  @override
  Size get preferredSize => const Size.fromHeight(1);

  @override
  Widget build(BuildContext context) {
    return Divider(height: 1, color: color.withValues(alpha: 0.55));
  }
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
  if (parts.isEmpty) return '?';
  if (parts.length == 1) return parts.first[0].toUpperCase();
  return '${parts.first[0]}${parts.last[0]}'.toUpperCase();
}
