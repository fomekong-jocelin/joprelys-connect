import 'package:flutter/material.dart';

import '../../core/config/app_config.dart';
import '../../core/theme/app_design_tokens.dart';

class AppBrandLockup extends StatelessWidget {
  const AppBrandLockup({this.logoWidth = 120, this.compact = false, super.key});

  final double logoWidth;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final brightness = theme.brightness;

    return Semantics(
      label: AppConfig.appName,
      image: true,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          _ThemeAwareBrandAsset(
            asset: brightness == Brightness.dark
                ? AppConfig.logoOnDarkAsset
                : AppConfig.logoOnLightAsset,
            width: logoWidth,
            brightness: brightness,
            key: ValueKey('app-brand-wordmark-${brightness.name}'),
          ),
          SizedBox(
            width: compact ? AppDesignTokens.spaceSm : AppDesignTokens.spaceMd,
          ),
          Text(
            AppConfig.productName,
            style:
                (compact
                        ? theme.textTheme.titleLarge
                        : theme.textTheme.headlineMedium)
                    ?.copyWith(
                      color: colors.onSurface,
                      fontWeight: FontWeight.w800,
                      letterSpacing: -0.5,
                    ),
          ),
        ],
      ),
    );
  }
}

class AppBrandMark extends StatelessWidget {
  const AppBrandMark({this.size = 32, super.key});

  final double size;

  @override
  Widget build(BuildContext context) {
    final brightness = Theme.of(context).brightness;

    return Semantics(
      label: AppConfig.appName,
      image: true,
      child: _ThemeAwareBrandAsset(
        asset: brightness == Brightness.dark
            ? AppConfig.logoIconOnDarkAsset
            : AppConfig.logoIconOnLightAsset,
        width: size,
        height: size,
        brightness: brightness,
        key: ValueKey('app-brand-mark-${brightness.name}'),
      ),
    );
  }
}

class _ThemeAwareBrandAsset extends StatelessWidget {
  const _ThemeAwareBrandAsset({
    required this.asset,
    required this.width,
    required this.brightness,
    this.height,
    super.key,
  });

  final String asset;
  final double width;
  final double? height;
  final Brightness brightness;

  @override
  Widget build(BuildContext context) {
    final image = Image.asset(
      asset,
      width: width,
      height: height,
      fit: BoxFit.contain,
      alignment: Alignment.centerLeft,
      filterQuality: FilterQuality.high,
      gaplessPlayback: true,
    );

    if (brightness == Brightness.light) {
      return image;
    }

    return ColorFiltered(colorFilter: _onDarkBrandFilter, child: image);
  }
}

// The official transparent PNG uses #001D4A and #009730. This matrix maps
// only the navy strokes to white while preserving the brand green and alpha.
// It avoids the rectangular background baked into the communication asset.
const ColorFilter _onDarkBrandFilter = ColorFilter.matrix(<double>[
  0,
  -2.090164,
  0,
  0,
  315.614754,
  0,
  -0.852459,
  0,
  0,
  279.721311,
  0,
  -1.696721,
  0,
  0,
  304.196721,
  0,
  0,
  0,
  1,
  0,
]);
