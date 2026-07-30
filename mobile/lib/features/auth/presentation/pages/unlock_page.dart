import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../application/auth_controller.dart';
import '../widgets/auth_error_message.dart';
import '../widgets/auth_shell.dart';

class UnlockPage extends ConsumerWidget {
  const UnlockPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final auth = ref.watch(authControllerProvider);
    final authState = auth.value;
    final session = authState?.session;
    final error = authErrorMessage(l10n, authState?.errorCode);

    return AuthShell(
      icon: Icons.fingerprint,
      title: l10n.authUnlockTitle,
      subtitle: l10n.authUnlockSubtitle(session?.name ?? ''),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (error != null) ...[
            Text(
              error,
              textAlign: TextAlign.center,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
            const SizedBox(height: AppDesignTokens.spaceMd),
          ],
          AppButton(
            label: l10n.authUnlockAction,
            icon: Icons.fingerprint,
            expand: true,
            loading: auth.isLoading,
            onPressed: auth.isLoading
                ? null
                : () => ref
                      .read(authControllerProvider.notifier)
                      .unlock(reason: l10n.authBiometricUnlockReason),
          ),
          const SizedBox(height: AppDesignTokens.spaceSm),
          AppButton(
            label: l10n.authLogout,
            variant: AppButtonVariant.secondary,
            expand: true,
            onPressed: auth.isLoading
                ? null
                : ref.read(authControllerProvider.notifier).logout,
          ),
        ],
      ),
    );
  }
}
