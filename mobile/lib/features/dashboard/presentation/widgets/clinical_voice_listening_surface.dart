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
    final primaryColor = colors.primary;

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        border: Border.all(
          color: colors.outline.withValues(alpha: 0.5),
          width: 1,
        ),
      ),
      child: Column(
        children: [
          // En-tête de la surface avec Sparkles & Bouton Arrêter/Démarrer
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Icon(
                    Icons.auto_awesome_rounded,
                    color: primaryColor,
                    size: 18,
                  ),
                  const SizedBox(width: 6),
                  Text(
                    badgeText,
                    style: theme.textTheme.labelMedium?.copyWith(
                      color: primaryColor,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ],
              ),
              OutlinedButton.icon(
                onPressed: onToggleListening,
                style: OutlinedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 6,
                  ),
                  side: BorderSide(
                    color: active ? AppDesignTokens.error : primaryColor,
                  ),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(
                      AppDesignTokens.radiusSm,
                    ),
                  ),
                ),
                icon: Icon(
                  active ? Icons.stop_rounded : Icons.mic_rounded,
                  size: 16,
                  color: active ? AppDesignTokens.error : primaryColor,
                ),
                label: Text(
                  active ? stopLabel : startLabel,
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: active ? AppDesignTokens.error : primaryColor,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          // Surface visuelle centrale Web (Canvas Wave + Triple Halos + Micro Orb)
          SizedBox(
            height: 160,
            width: double.infinity,
            child: Stack(
              alignment: Alignment.center,
              children: [
                // Canvas des ondes sinusoïdales (dynamiquement réactives au niveau décibel réel)
                Positioned.fill(
                  child: AnimatedBuilder(
                    animation: waveController,
                    builder: (context, child) {
                      return CustomPaint(
                        painter: _SineWavePainter(
                          phase: waveController.value * math.pi * 2,
                          color: primaryColor,
                          active: active,
                          soundLevel: soundLevel,
                        ),
                      );
                    },
                  ),
                ),
                // Halo Extérieur
                if (active)
                  AnimatedBuilder(
                    animation: haloController,
                    builder: (context, child) {
                      final val = haloController.value;
                      final levelBonus = (soundLevel * 0.8).clamp(0.0, 20.0);
                      final size =
                          124.0 +
                          (math.sin(val * math.pi * 2) * 8.0) +
                          levelBonus;
                      final opacity =
                          0.2 + (math.sin(val * math.pi * 2) * 0.15);

                      return Container(
                        width: size,
                        height: size,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          border: Border.all(
                            color: primaryColor.withValues(alpha: opacity),
                            width: 1.5,
                          ),
                        ),
                      );
                    },
                  ),
                // Halo Intermédiaire
                if (active)
                  AnimatedBuilder(
                    animation: haloController,
                    builder: (context, child) {
                      final val = (haloController.value + 0.4) % 1.0;
                      final levelBonus = (soundLevel * 0.5).clamp(0.0, 14.0);
                      final size =
                          104.0 +
                          (math.sin(val * math.pi * 2) * 6.0) +
                          levelBonus;
                      final opacity = 0.3 + (math.sin(val * math.pi * 2) * 0.2);

                      return Container(
                        width: size,
                        height: size,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          border: Border.all(
                            color: primaryColor.withValues(alpha: opacity),
                            width: 1.5,
                          ),
                        ),
                      );
                    },
                  ),
                // Halo Intérieur
                Container(
                  width: 84,
                  height: 84,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: primaryColor.withValues(alpha: active ? 0.18 : 0.08),
                  ),
                ),
                // Micro Orb Central (Identique au composant voice-micro Web)
                GestureDetector(
                  onTap: onToggleListening,
                  child: Container(
                    width: 66,
                    height: 66,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: active
                            ? [
                                primaryColor,
                                primaryColor.withValues(alpha: 0.85),
                              ]
                            : [colors.surfaceContainerHighest, colors.surface],
                      ),
                      boxShadow: active
                          ? [
                              BoxShadow(
                                color: primaryColor.withValues(alpha: 0.38),
                                blurRadius:
                                    20 + (soundLevel * 0.5).clamp(0.0, 15.0),
                                spreadRadius:
                                    4 + (soundLevel * 0.2).clamp(0.0, 8.0),
                              ),
                            ]
                          : [
                              BoxShadow(
                                color: colors.shadow.withValues(alpha: 0.1),
                                blurRadius: 8,
                              ),
                            ],
                      border: Border.all(
                        color: active
                            ? primaryColor.withValues(alpha: 0.8)
                            : colors.outline.withValues(alpha: 0.4),
                        width: 1,
                      ),
                    ),
                    child: Icon(
                      Icons.mic_rounded,
                      size: 34,
                      color: active ? Colors.white : colors.onSurfaceVariant,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 8),
          Text(
            statusText,
            textAlign: TextAlign.center,
            style: theme.textTheme.titleSmall?.copyWith(
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            tipText,
            textAlign: TextAlign.center,
            style: theme.textTheme.bodySmall?.copyWith(
              color: colors.onSurfaceVariant,
              fontSize: 11,
              height: 1.3,
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
    required this.active,
    required this.soundLevel,
  });

  final double phase;
  final Color color;
  final bool active;
  final double soundLevel;

  @override
  void paint(Canvas canvas, Size size) {
    if (!active) return;

    final baseAmp = (soundLevel > 0) ? (14.0 + (soundLevel * 1.2)) : 16.0;

    final paint1 = Paint()
      ..color = color.withValues(alpha: 0.45)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.0;

    final paint2 = Paint()
      ..color = color.withValues(alpha: 0.25)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.5;

    final path1 = Path();
    final path2 = Path();

    final midY = size.height / 2;
    path1.moveTo(0, midY);
    path2.moveTo(0, midY);

    for (double x = 0; x <= size.width; x += 2) {
      final normX = x / size.width;
      final envelope = math.sin(normX * math.pi); // 0 aux bords, 1 au centre

      final y1 =
          midY + math.sin((normX * 4 * math.pi) + phase) * baseAmp * envelope;
      final y2 =
          midY +
          math.sin((normX * 6 * math.pi) - (phase * 1.4)) *
              (baseAmp * 0.7) *
              envelope;

      path1.lineTo(x, y1);
      path2.lineTo(x, y2);
    }

    canvas.drawPath(path1, paint1);
    canvas.drawPath(path2, paint2);
  }

  @override
  bool shouldRepaint(_SineWavePainter oldDelegate) {
    return oldDelegate.phase != phase ||
        oldDelegate.active != active ||
        oldDelegate.soundLevel != soundLevel;
  }
}
