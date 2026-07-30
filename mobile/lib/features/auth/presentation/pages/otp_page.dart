import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../../../shared/widgets/app_text_field.dart';
import '../../application/auth_controller.dart';
import '../widgets/auth_error_message.dart';
import '../widgets/auth_shell.dart';

class OtpPage extends ConsumerStatefulWidget {
  const OtpPage({super.key});

  @override
  ConsumerState<OtpPage> createState() => _OtpPageState();
}

class _OtpPageState extends ConsumerState<OtpPage> {
  final _otpController = TextEditingController();
  String? _localError;

  @override
  void dispose() {
    _otpController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final auth = ref.watch(authControllerProvider);
    final authState = auth.value;
    final email = authState?.pendingEmail ?? '';
    final error = _localError ?? authErrorMessage(l10n, authState?.errorCode);

    return AuthShell(
      icon: Icons.verified_user_outlined,
      title: l10n.authOtpTitle,
      subtitle: l10n.authOtpSubtitle(email),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          AppTextField(
            controller: _otpController,
            label: l10n.authOtpLabel,
            keyboardType: TextInputType.number,
            textInputAction: TextInputAction.done,
            enabled: !auth.isLoading,
            prefixIcon: const Icon(Icons.password_outlined),
          ),
          if (error != null) ...[
            const SizedBox(height: AppDesignTokens.spaceMd),
            Text(
              error,
              textAlign: TextAlign.center,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ],
          const SizedBox(height: AppDesignTokens.spaceLg),
          AppButton(
            label: l10n.authVerifyOtp,
            icon: Icons.check_circle_outline,
            expand: true,
            loading: auth.isLoading,
            onPressed: auth.isLoading ? null : _submit,
          ),
          const SizedBox(height: AppDesignTokens.spaceSm),
          AppButton(
            label: l10n.authBackToLogin,
            variant: AppButtonVariant.secondary,
            expand: true,
            onPressed: auth.isLoading
                ? null
                : ref.read(authControllerProvider.notifier).cancelOtp,
          ),
        ],
      ),
    );
  }

  Future<void> _submit() async {
    final l10n = AppLocalizations.of(context);
    final otp = _otpController.text.trim();
    if (otp.isEmpty) {
      setState(() => _localError = l10n.authOtpRequired);
      return;
    }
    setState(() => _localError = null);
    await ref.read(authControllerProvider.notifier).verifyOtp(otp);
    _otpController.clear();
  }
}
