import 'package:flutter/material.dart';

abstract final class AppDesignTokens {
  // Brand colors — DESIGN.md is the source of truth.
  static const Color brandNight = Color(0xFF0A1D3D);
  static const Color brandCyan = Color(0xFF0B91B2);
  static const Color brandCyanDark = Color(0xFF22A8C8);
  static const Color brandGreen = Color(0xFF16A34A);

  // Light theme.
  static const Color lightBackground = Color(0xFFF7FAFC);
  static const Color lightSurface = Color(0xFFFFFFFF);
  static const Color lightText = brandNight;
  static const Color lightSecondary = Color(0xFF40556F);
  static const Color lightMuted = Color(0xFF64748B);
  static const Color lightOutline = Color(0xFFD8E5E8);
  static const Color lightInput = Color(0xFFF1F5F9);

  // Dark theme.
  static const Color darkBackground = Color(0xFF071124);
  static const Color darkSurface = Color(0xFF111C31);
  static const Color darkText = Color(0xFFF4F8FC);
  static const Color darkSecondary = Color(0xFFC6D2E1);
  static const Color darkMuted = Color(0xFF94A3B8);
  static const Color darkOutline = Color(0xFF21314B);
  static const Color darkInput = Color(0xFF0D182B);

  // Semantic colors.
  static const Color success = brandGreen;
  static const Color warning = Color(0xFFD97706);
  static const Color error = Color(0xFFDC2626);
  static const Color errorDark = Color(0xFFFCA5A5);
  static const Color info = Color(0xFF2563EB);
  static const Color onStrongColor = Color(0xFFFFFFFF);

  // Spacing.
  static const double spaceXs = 4;
  static const double spaceSm = 8;
  static const double spaceMd = 16;
  static const double spaceLg = 24;
  static const double spaceXl = 32;
  static const double space2xl = 48;

  // Radius.
  static const double radiusNone = 0;
  static const double radiusXs = 2;
  static const double radiusSm = 4;
  static const double radiusMd = 6;
  static const double radiusLg = 8;

  // Structure and touch targets.
  static const double borderWidth = 1;
  static const double minTouchTarget = 44;

  // Typography scale from DESIGN.md. Font assets are intentionally not loaded
  // from a remote source; local Montserrat/Inter assets can be wired later.
  static const double headlineLargeSize = 32;
  static const double headlineMediumSize = 24;
  static const double bodyLargeSize = 18;
  static const double bodyMediumSize = 16;
  static const double bodySmallSize = 14;
  static const double labelLargeSize = 14;
  static const double labelMediumSize = 12;
  static const double labelSmallSize = 11;

  static const List<BoxShadow> lightPanelShadow = <BoxShadow>[
    BoxShadow(color: Color(0x120A1D3D), blurRadius: 10, offset: Offset(0, 3)),
  ];

  static const List<BoxShadow> darkPanelShadow = <BoxShadow>[
    BoxShadow(color: Color(0x42000000), blurRadius: 12, offset: Offset(0, 4)),
  ];
}

enum AppSemanticTone { neutral, success, warning, error, info }
