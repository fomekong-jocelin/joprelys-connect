import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/core/theme/app_design_tokens.dart';
import 'package:joprelys_mobile/core/theme/app_theme.dart';
import 'package:joprelys_mobile/shared/widgets/app_badge.dart';
import 'package:joprelys_mobile/shared/widgets/app_button.dart';
import 'package:joprelys_mobile/shared/widgets/app_card.dart';
import 'package:joprelys_mobile/shared/widgets/app_confirm_dialog.dart';
import 'package:joprelys_mobile/shared/widgets/app_empty_state.dart';
import 'package:joprelys_mobile/shared/widgets/app_error_state.dart';
import 'package:joprelys_mobile/shared/widgets/app_loading_state.dart';
import 'package:joprelys_mobile/shared/widgets/app_page_header.dart';
import 'package:joprelys_mobile/shared/widgets/app_text_field.dart';

void main() {
  test('light and dark themes map the Joprelys semantic colors', () {
    expect(AppTheme.light.colorScheme.primary, AppDesignTokens.brandCyan);
    expect(
      AppTheme.light.scaffoldBackgroundColor,
      AppDesignTokens.lightBackground,
    );
    expect(AppTheme.dark.colorScheme.primary, AppDesignTokens.brandCyanDark);
    expect(
      AppTheme.dark.scaffoldBackgroundColor,
      AppDesignTokens.darkBackground,
    );
  });

  test('theme geometry keeps sober Joprelys radii', () {
    final inputBorder =
        AppTheme.light.inputDecorationTheme.enabledBorder!
            as OutlineInputBorder;
    expect(inputBorder.borderRadius.topLeft.x, AppDesignTokens.radiusSm);

    final cardShape = AppTheme.light.cardTheme.shape! as RoundedRectangleBorder;
    final cardRadius = cardShape.borderRadius as BorderRadius;
    expect(cardRadius.topLeft.x, AppDesignTokens.radiusLg);
  });

  testWidgets('shared design-system widgets render together', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light,
        home: Scaffold(
          body: SingleChildScrollView(
            child: Column(
              children: [
                const AppPageHeader(
                  title: 'Foundation',
                  subtitle: 'Shared UI primitives',
                ),
                AppButton(label: 'Primary', onPressed: () {}),
                AppButton(
                  label: 'Secondary',
                  onPressed: () {},
                  variant: AppButtonVariant.secondary,
                ),
                const AppTextField(label: 'Field'),
                const AppCard(child: Text('Card')),
                const AppBadge(label: 'Info', tone: AppSemanticTone.info),
                const AppLoadingState(label: 'Loading'),
                const AppEmptyState(title: 'Empty', message: 'Nothing here'),
                AppErrorState(
                  title: 'Error',
                  message: 'Retry later',
                  retryLabel: 'Retry',
                  onRetry: () {},
                ),
              ],
            ),
          ),
        ),
      ),
    );

    await tester.pump();

    expect(find.text('Primary'), findsOneWidget);
    expect(find.text('Secondary'), findsOneWidget);
    expect(find.text('Field'), findsOneWidget);
    expect(find.text('Card'), findsOneWidget);
    expect(find.text('Info'), findsOneWidget);
    expect(find.text('Loading'), findsOneWidget);
    expect(find.text('Empty'), findsOneWidget);
    expect(find.text('Error'), findsOneWidget);

    final primaryButtonSize = tester.getSize(find.byType(ElevatedButton).first);
    expect(
      primaryButtonSize.height,
      greaterThanOrEqualTo(AppDesignTokens.minTouchTarget),
    );
  });

  testWidgets('confirmation dialog returns the explicit choice', (
    tester,
  ) async {
    bool? result;

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light,
        home: Builder(
          builder: (context) => Scaffold(
            body: AppButton(
              label: 'Open',
              onPressed: () async {
                result = await AppConfirmDialog.show(
                  context,
                  title: 'Confirm',
                  message: 'Continue?',
                  cancelLabel: 'Cancel',
                  confirmLabel: 'Confirm',
                );
              },
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('Open'));
    await tester.pumpAndSettle();
    expect(find.byType(AppConfirmDialog), findsOneWidget);

    await tester.tap(find.widgetWithText(AppButton, 'Confirm'));
    await tester.pumpAndSettle();

    expect(result, isTrue);
  });
}
