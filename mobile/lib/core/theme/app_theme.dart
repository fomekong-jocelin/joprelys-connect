import 'package:flutter/material.dart';

import 'app_design_tokens.dart';

abstract final class AppTheme {
  static ThemeData get light => _build(
    brightness: Brightness.light,
    primary: AppDesignTokens.brandCyan,
    background: AppDesignTokens.lightBackground,
    surface: AppDesignTokens.lightSurface,
    text: AppDesignTokens.lightText,
    secondary: AppDesignTokens.lightSecondary,
    muted: AppDesignTokens.lightMuted,
    outline: AppDesignTokens.lightOutline,
    input: AppDesignTokens.lightInput,
  );

  static ThemeData get dark => _build(
    brightness: Brightness.dark,
    primary: AppDesignTokens.brandCyanDark,
    background: AppDesignTokens.darkBackground,
    surface: AppDesignTokens.darkSurface,
    text: AppDesignTokens.darkText,
    secondary: AppDesignTokens.darkSecondary,
    muted: AppDesignTokens.darkMuted,
    outline: AppDesignTokens.darkOutline,
    input: AppDesignTokens.darkInput,
  );

  static ThemeData _build({
    required Brightness brightness,
    required Color primary,
    required Color background,
    required Color surface,
    required Color text,
    required Color secondary,
    required Color muted,
    required Color outline,
    required Color input,
  }) {
    final colorScheme =
        ColorScheme.fromSeed(
          seedColor: primary,
          brightness: brightness,
        ).copyWith(
          primary: primary,
          onPrimary: AppDesignTokens.onStrongColor,
          secondary: secondary,
          onSecondary: brightness == Brightness.light
              ? AppDesignTokens.onStrongColor
              : AppDesignTokens.brandNight,
          surface: surface,
          onSurface: text,
          error: brightness == Brightness.light
              ? AppDesignTokens.error
              : AppDesignTokens.errorDark,
          onError: AppDesignTokens.onStrongColor,
          errorContainer: brightness == Brightness.light
              ? const Color(0xFFFEE2E2)
              : const Color(0xFF3B1219),
          onErrorContainer: brightness == Brightness.light
              ? const Color(0xFF991B1B)
              : const Color(0xFFFCA5A5),
          outline: outline,
        );

    final textTheme = _textTheme(text: text, muted: muted);
    final compactShape = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
    );
    final cardShape = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      side: BorderSide(color: outline, width: AppDesignTokens.borderWidth),
    );

    return ThemeData(
      brightness: brightness,
      useMaterial3: true,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: background,
      textTheme: textTheme,
      dividerTheme: DividerThemeData(
        color: outline,
        thickness: AppDesignTokens.borderWidth,
        space: AppDesignTokens.spaceMd,
      ),
      cardTheme: CardThemeData(
        color: surface,
        elevation: 1,
        shadowColor: brightness == Brightness.light
            ? AppDesignTokens.lightPanelShadow.first.color
            : AppDesignTokens.darkPanelShadow.first.color,
        surfaceTintColor: surface,
        shape: cardShape,
        margin: EdgeInsets.zero,
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: surface,
        elevation: 2,
        surfaceTintColor: surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        ),
        titleTextStyle: textTheme.titleLarge,
        contentTextStyle: textTheme.bodyMedium,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: input,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: AppDesignTokens.spaceMd,
          vertical: 12,
        ),
        border: _inputBorder(outline),
        enabledBorder: _inputBorder(outline),
        focusedBorder: _inputBorder(primary, width: 2),
        errorBorder: _inputBorder(AppDesignTokens.error),
        focusedErrorBorder: _inputBorder(AppDesignTokens.error, width: 2),
        labelStyle: textTheme.bodySmall?.copyWith(color: muted),
        hintStyle: textTheme.bodyMedium?.copyWith(color: muted),
        errorStyle: textTheme.bodySmall?.copyWith(color: AppDesignTokens.error),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ButtonStyle(
          minimumSize: const WidgetStatePropertyAll(
            Size(0, AppDesignTokens.minTouchTarget),
          ),
          padding: const WidgetStatePropertyAll(
            EdgeInsets.symmetric(
              horizontal: AppDesignTokens.spaceMd,
              vertical: AppDesignTokens.spaceSm,
            ),
          ),
          shape: WidgetStatePropertyAll(compactShape),
          elevation: const WidgetStatePropertyAll(0),
          textStyle: WidgetStatePropertyAll(textTheme.labelLarge),
          backgroundColor: WidgetStateProperty.resolveWith((states) {
            if (states.contains(WidgetState.disabled)) {
              return muted;
            }
            return primary;
          }),
          foregroundColor: const WidgetStatePropertyAll(
            AppDesignTokens.onStrongColor,
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: ButtonStyle(
          minimumSize: const WidgetStatePropertyAll(
            Size(0, AppDesignTokens.minTouchTarget),
          ),
          padding: const WidgetStatePropertyAll(
            EdgeInsets.symmetric(
              horizontal: AppDesignTokens.spaceMd,
              vertical: AppDesignTokens.spaceSm,
            ),
          ),
          shape: WidgetStatePropertyAll(compactShape),
          textStyle: WidgetStatePropertyAll(textTheme.labelLarge),
          foregroundColor: WidgetStatePropertyAll(primary),
          side: WidgetStatePropertyAll(
            BorderSide(color: outline, width: AppDesignTokens.borderWidth),
          ),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: ButtonStyle(
          minimumSize: const WidgetStatePropertyAll(
            Size(0, AppDesignTokens.minTouchTarget),
          ),
          shape: WidgetStatePropertyAll(compactShape),
          textStyle: WidgetStatePropertyAll(textTheme.labelLarge),
          foregroundColor: WidgetStatePropertyAll(primary),
        ),
      ),
    );
  }

  static OutlineInputBorder _inputBorder(Color color, {double width = 1}) {
    return OutlineInputBorder(
      borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
      borderSide: BorderSide(color: color, width: width),
    );
  }

  static TextTheme _textTheme({required Color text, required Color muted}) {
    return TextTheme(
      headlineLarge: TextStyle(
        color: text,
        fontSize: AppDesignTokens.headlineLargeSize,
        fontWeight: FontWeight.w800,
        height: 1.25,
      ),
      headlineMedium: TextStyle(
        color: text,
        fontSize: AppDesignTokens.headlineMediumSize,
        fontWeight: FontWeight.w700,
        height: 1.33,
      ),
      titleLarge: TextStyle(
        color: text,
        fontSize: 20,
        fontWeight: FontWeight.w700,
        height: 1.3,
      ),
      titleMedium: TextStyle(
        color: text,
        fontSize: AppDesignTokens.bodyMediumSize,
        fontWeight: FontWeight.w600,
        height: 1.4,
      ),
      bodyLarge: TextStyle(
        color: text,
        fontSize: AppDesignTokens.bodyLargeSize,
        fontWeight: FontWeight.w400,
        height: 1.55,
      ),
      bodyMedium: TextStyle(
        color: text,
        fontSize: AppDesignTokens.bodyMediumSize,
        fontWeight: FontWeight.w400,
        height: 1.5,
      ),
      bodySmall: TextStyle(
        color: muted,
        fontSize: AppDesignTokens.bodySmallSize,
        fontWeight: FontWeight.w400,
        height: 1.43,
      ),
      labelLarge: TextStyle(
        color: text,
        fontSize: AppDesignTokens.labelLargeSize,
        fontWeight: FontWeight.w600,
        height: 1.43,
      ),
      labelMedium: TextStyle(
        color: text,
        fontSize: AppDesignTokens.labelMediumSize,
        fontWeight: FontWeight.w600,
        height: 1.33,
      ),
      labelSmall: TextStyle(
        color: muted,
        fontSize: AppDesignTokens.labelSmallSize,
        fontWeight: FontWeight.w600,
        height: 1.45,
      ),
    );
  }
}
