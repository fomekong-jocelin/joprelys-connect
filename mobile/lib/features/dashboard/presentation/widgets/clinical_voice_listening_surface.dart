import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_tokens.dart';

class ClinicalVoiceListeningSurface extends StatelessWidget {
  const ClinicalVoiceListeningSurface({
    required this.active,
    required this.soundLevel,
    required this.haloController,
    required this.waveController,
    required this.badgeText,
    required this.statusText,
    required this.tipText,
    required this.stopLabel,
    required this.startLabel,
    required this.onToggleListening,
    super.key,
  });

  final bool active;
  final double soundLevel;
  final AnimationController haloController;
  final AnimationController waveController;
  final String badgeText;
  final String statusText;
  final String tipText;
  final String stopLabel;
  final String startLabel;
  final VoidCallback onToggleListening;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final actionColor = active ? colors.error : colors.primary;
    final actionLabel = active ? stopLabel : startLabel;
    final reactiveBlur = active ? soundLevel * 0.12 : 0.0;
    final reactiveSpread = active ? soundLevel * 0.04 : 0.0;

    return AnimatedContainer(
      duration: const Duration(milliseconds: 220),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: active
            ? colors.primaryContainer.withValues(alpha: 0.16)
            : colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
        border: Border.all(
          color: active
              ? colors.primary.withValues(alpha: 0.5)
              : colors.outlineVariant,
        ),
      ),
      child: Column(
        children: [
          Row(
            children: [
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: active
                      ? colors.primary
                      : colors.primaryContainer.withValues(alpha: 0.7),
                ),
                child: Icon(
                  active ? Icons.graphic_eq_rounded : Icons.mic_none_rounded,
                  color: active ? colors.onPrimary : colors.primary,
                ),
              ),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      badgeText,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.labelLarge?.copyWith(
                        color: colors.primary,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      statusText,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: colors.onSurfaceVariant,
                        height: 1.3,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          AnimatedContainer(
            duration: const Duration(milliseconds: 220),
            height: active ? 118 : 92,
            width: double.infinity,
            child: Stack(
              alignment: Alignment.center,
              children: [
                if (active)
                  Positioned.fill(
                    child: AnimatedBuilder(
                      animation: waveController,
                      builder: (context, child) {
                        return CustomPaint(
                          painter: _SineWavePainter(
                            phase: waveController.value * math.pi * 2,
                            color: colors.primary,
                            soundLevel: soundLevel,
                          ),
                        );
                      },
                    ),
                  ),
                AnimatedBuilder(
                  animation: haloController,
                  builder: (context, child) {
                    final pulse = active
                        ? math.sin(haloController.value * math.pi * 2)
                        : 0.0;
                    final reactive = active ? soundLevel * 0.18 : 0.0;
                    return Container(
                      width: 82 + pulse * 6 + reactive,
                      height: 82 + pulse * 6 + reactive,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: actionColor.withValues(alpha: 0.1),
                        border: Border.all(
                          color: actionColor.withValues(
                            alpha: active
                                ? 0.28 + pulse.abs() * 0.18
                                : 0.22,
                          ),
                        ),
                      ),
                    );
                  },
                ),
                Semantics(
                  button: true,
                  label: actionLabel,
                  child: Tooltip(
                    message: actionLabel,
                    child: Material(
                      color: Colors.transparent,
                      child: InkResponse(
                        onTap: onToggleListening,
                        radius: 48,
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 180),
                          width: 66,
                          height: 66,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: actionColor,
                            boxShadow: [
                              BoxShadow(
                                color: actionColor.withValues(alpha: 0.25),
                                blurRadius: 14 + reactiveBlur,
                                spreadRadius: 2 + reactiveSpread,
                              ),
                            ],
                          ),
                          child: Icon(
                            active ? Icons.stop_rounded : Icons.mic_rounded,
                            size: 32,
                            color: active ? colors.onError : colors.onPrimary,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerLeft,
            child: Text(
              tipText,
              style: theme.textTheme.bodySmall?.copyWith(
                color: colors.onSurfaceVariant,
                height: 1.35,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _SineWavePainter extends CustomPainter {
  _SineWavePainter({
    required this.phase,
    required this.color,
    required this.soundLevel,
  });

  final double phase;
  final Color color;
  final double soundLevel;

  @override
  void paint(Canvas canvas, Size size) {
    final reactiveLevel = soundLevel.clamp(0.0, 100.0);
    final baseAmplitude = 8.0 + reactiveLevel * 0.22;

    final primaryPaint = Paint()
      ..color = color.withValues(alpha: 0.52)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.2;
    final secondaryPaint = Paint()
      ..color = color.withValues(alpha: 0.24)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.4;

    final primaryPath = Path();
    final secondaryPath = Path();
    final middle = size.height / 2;
    primaryPath.moveTo(0, middle);
    secondaryPath.moveTo(0, middle);

    for (double x = 0; x <= size.width; x += 2) {
      final normalizedX = x / size.width;
      final envelope = math.sin(normalizedX * math.pi);
      final primaryY =
          middle +
          math.sin(normalizedX * 4 * math.pi + phase) *
              baseAmplitude *
              envelope;
      final secondaryY =
          middle +
          math.sin(normalizedX * 7 * math.pi - phase * 1.35) *
              baseAmplitude *
              0.55 *
              envelope;
      primaryPath.lineTo(x, primaryY);
      secondaryPath.lineTo(x, secondaryY);
    }

    canvas.drawPath(primaryPath, primaryPaint);
    canvas.drawPath(secondaryPath, secondaryPaint);
  }

  @override
  bool shouldRepaint(_SineWavePainter oldDelegate) {
    return oldDelegate.phase != phase ||
        oldDelegate.soundLevel != soundLevel ||
        oldDelegate.color != color;
  }
}
