import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/i18n/locale_controller.dart';
import '../../../../core/theme/theme_controller.dart';
import '../../../../l10n/app_localizations.dart';

class AuthPreferencesBar extends ConsumerWidget {
  const AuthPreferencesBar({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final locale = ref.watch(appLocaleProvider);
    final themeMode = ref.watch(themeModeProvider);

    return Wrap(
      alignment: WrapAlignment.center,
      crossAxisAlignment: WrapCrossAlignment.center,
      spacing: 4,
      children: [
        TextButton(
          onPressed: ref.read(appLocaleProvider.notifier).useFrench,
          child: Text(
            l10n.languageFrench,
            style: locale.languageCode == 'fr'
                ? const TextStyle(fontWeight: FontWeight.w700)
                : null,
          ),
        ),
        TextButton(
          onPressed: ref.read(appLocaleProvider.notifier).useEnglish,
          child: Text(
            l10n.languageEnglish,
            style: locale.languageCode == 'en'
                ? const TextStyle(fontWeight: FontWeight.w700)
                : null,
          ),
        ),
        PopupMenuButton<ThemeMode>(
          tooltip: l10n.foundationThemeTitle,
          initialValue: themeMode,
          onSelected: ref.read(themeModeProvider.notifier).setMode,
          itemBuilder: (context) => [
            PopupMenuItem(
              value: ThemeMode.system,
              child: Text(l10n.themeSystem),
            ),
            PopupMenuItem(
              value: ThemeMode.light,
              child: Text(l10n.themeLight),
            ),
            PopupMenuItem(
              value: ThemeMode.dark,
              child: Text(l10n.themeDark),
            ),
          ],
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.contrast, size: 18),
                const SizedBox(width: 6),
                Text(l10n.foundationThemeTitle),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
