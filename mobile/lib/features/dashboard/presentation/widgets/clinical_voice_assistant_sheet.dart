import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../../../shared/widgets/app_text_field.dart';
import '../../application/clinical_dictation_parser.dart';
import '../../application/clinical_speech_service.dart';
import '../../domain/active_visit.dart';
import '../dashboard_localizations.dart';

class ClinicalVoiceAssistantSheet extends ConsumerStatefulWidget {
  const ClinicalVoiceAssistantSheet({
    required this.visit,
    required this.onExtracted,
    super.key,
  });

  final ActiveVisit visit;
  final ValueChanged<DictationParseResult> onExtracted;

  static Future<void> show(
    BuildContext context, {
    required ActiveVisit visit,
    required ValueChanged<DictationParseResult> onExtracted,
  }) {
    return showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => Padding(
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
        ),
        child: FractionallySizedBox(
          heightFactor: 0.90,
          child: ClinicalVoiceAssistantSheet(
            visit: visit,
            onExtracted: onExtracted,
          ),
        ),
      ),
    );
  }

  @override
  ConsumerState<ClinicalVoiceAssistantSheet> createState() =>
      _ClinicalVoiceAssistantSheetState();
}

class _ClinicalVoiceAssistantSheetState
    extends ConsumerState<ClinicalVoiceAssistantSheet>
    with TickerProviderStateMixin {
  final _speechService = ClinicalSpeechService();
  final _dictationController = TextEditingController();

  late AnimationController _haloController;
  late AnimationController _waveController;

  List<String> _getPresetDictations(AppLocalizations l10n) => [
        l10n.assistantPresetDictation1,
        l10n.assistantPresetDictation2,
        l10n.assistantPresetDictation3,
      ];

  @override
  void initState() {
    super.initState();
    _haloController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2800),
    )..repeat();

    _waveController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat();

    // Lancement immédiat de l'écoute vocale en direct
    _speechService.startRealtimeListening();
    _speechService.addListener(_onSpeechStateChanged);
  }

  @override
  void dispose() {
    _speechService.removeListener(_onSpeechStateChanged);
    _speechService.dispose();
    _dictationController.dispose();
    _haloController.dispose();
    _waveController.dispose();
    super.dispose();
  }

  void _onSpeechStateChanged() {
    final state = _speechService.value;
    if (_dictationController.text != state.transcript) {
      _dictationController.text = state.transcript;
    }
  }

  void _onTextChanged(String text) {
    _speechService.updateTranscript(text);
  }

  void _toggleListening() {
    if (_speechService.value.status == SpeechStatus.listening) {
      _speechService.stopListening();
    } else {
      _speechService.startRealtimeListening();
    }
  }

  void _applyPreset(String text) {
    _speechService.startRealtimeListening();
    _speechService.updateTranscript(text);
  }

  void _applyResult() {
    final state = _speechService.value;
    final result = DictationParseResult(
      vitals: state.vitals,
      note: state.note,
    );
    widget.onExtracted(result);
    Navigator.of(context).pop();
  }

  String _formattedReference(String visitNum, String rawDpu) {
    var cleaned = rawDpu.trim();
    cleaned = cleaned.replaceAll(
      RegExp(r'^(DPU[\s\-]*)+', caseSensitive: false),
      'DPU-',
    );
    if (!cleaned.toUpperCase().startsWith('DPU-')) {
      cleaned = 'DPU-$cleaned';
    }
    final nonBreakingDpu = cleaned.replaceAll('-', '\u2011');
    return '$visitNum · $nonBreakingDpu';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);

    return ValueListenableBuilder<RealtimeSpeechState>(
      valueListenable: _speechService,
      builder: (context, state, child) {
        final isListening = state.status == SpeechStatus.listening;

        return Container(
          decoration: BoxDecoration(
            color: colors.surface,
            borderRadius: const BorderRadius.vertical(
              top: Radius.circular(AppDesignTokens.radiusLg),
            ),
          ),
          child: Column(
            children: [
              const SizedBox(height: 12),
              Center(
                child: Container(
                  width: 42,
                  height: 4,
                  decoration: BoxDecoration(
                    color: colors.outlineVariant,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              // En-tête de la modale avec identité du patient
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Icon(Icons.mic_rounded, color: colors.primary, size: 22),
                              const SizedBox(width: 6),
                              Text(
                                l10n.assistantTitle,
                                style: theme.textTheme.titleLarge?.copyWith(
                                  fontWeight: FontWeight.w900,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 2),
                          Text(
                            '${widget.visit.patientName} (${_formattedReference(widget.visit.visitNumber, widget.visit.patientDpu)})',
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: colors.onSurfaceVariant,
                              height: 1.3,
                            ),
                          ),
                        ],
                      ),
                    ),
                    IconButton(
                      onPressed: () => Navigator.of(context).pop(),
                      icon: const Icon(Icons.close_rounded),
                    ),
                  ],
                ),
              ),
              const Divider(height: 1),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // Surface d'écoute Web-aligned (VoiceListeningSurfaceComponent)
                      _WebAlignedVoiceListeningSurface(
                        active: isListening,
                        soundLevel: state.soundLevel,
                        haloController: _haloController,
                        waveController: _waveController,
                        badgeText: isListening
                            ? l10n.assistantSecuredRecording
                            : l10n.assistantPaused,
                        statusText: isListening
                            ? l10n.assistantListeningStatusText
                            : l10n.assistantTapToListen,
                        tipText: l10n.assistantTipNaturalDictation,
                        stopLabel: l10n.assistantStopDictationButton,
                        startLabel: l10n.assistantStartDictation,
                        onToggleListening: _toggleListening,
                      ),
                      const SizedBox(height: 16),
                      // Puces de dictée rapide pour tests instantanés
                      SingleChildScrollView(
                        scrollDirection: Axis.horizontal,
                        child: Row(
                          children: [
                            for (final text in _getPresetDictations(l10n)) ...[
                              Padding(
                                padding: const EdgeInsets.only(right: 8),
                                child: ActionChip(
                                  avatar: const Icon(
                                    Icons.auto_awesome_rounded,
                                    size: 14,
                                  ),
                                  label: Text(
                                    text,
                                    style: theme.textTheme.labelSmall?.copyWith(
                                      fontWeight: FontWeight.w700,
                                    ),
                                  ),
                                  backgroundColor:
                                      colors.primary.withValues(alpha: 0.1),
                                  side: BorderSide(
                                    color: colors.primary.withValues(alpha: 0.3),
                                  ),
                                  onPressed: () => _applyPreset(text),
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),
                      // Zone de transcription vocale en direct
                      AppTextField(
                        label: l10n.assistantLiveTranscript,
                        hint: l10n.assistantDictationHint,
                        controller: _dictationController,
                        onChanged: _onTextChanged,
                        maxLines: 3,
                      ),
                      const SizedBox(height: 16),
                      // Synthèse d'extraction des constantes en direct
                      if (!state.vitals.isEmpty || state.note.subjective != null) ...[
                        Container(
                          width: double.infinity,
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(
                            color: colors.primary.withValues(alpha: 0.08),
                            borderRadius: BorderRadius.circular(
                              AppDesignTokens.radiusLg,
                            ),
                            border: Border.all(
                              color: colors.primary.withValues(alpha: 0.3),
                            ),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                l10n.assistantExtractedSummary,
                                style: theme.textTheme.titleMedium?.copyWith(
                                  fontWeight: FontWeight.w800,
                                  color: colors.primary,
                                ),
                              ),
                              const SizedBox(height: 10),
                              if (!state.vitals.isEmpty) ...[
                                Wrap(
                                  spacing: 8,
                                  runSpacing: 8,
                                  children: [
                                    if (state.vitals.temperature != null)
                                      _VitalExtractPill(
                                        label: 'T°: ${state.vitals.temperature}°C',
                                      ),
                                    if (state.vitals.systolic != null &&
                                        state.vitals.diastolic != null)
                                      _VitalExtractPill(
                                        label:
                                            'TA: ${state.vitals.systolic}/${state.vitals.diastolic} mmHg',
                                      ),
                                    if (state.vitals.pulse != null)
                                      _VitalExtractPill(
                                        label: 'Pouls: ${state.vitals.pulse} bpm',
                                      ),
                                    if (state.vitals.spo2 != null)
                                      _VitalExtractPill(
                                        label: 'SpO2: ${state.vitals.spo2}%',
                                      ),
                                    if (state.vitals.weight != null)
                                      _VitalExtractPill(
                                        label: 'Poids: ${state.vitals.weight} kg',
                                      ),
                                    if (state.vitals.height != null)
                                      _VitalExtractPill(
                                        label: 'Taille: ${state.vitals.height} cm',
                                      ),
                                    if (state.vitals.glycemia != null)
                                      _VitalExtractPill(
                                        label:
                                            'Glycémie: ${state.vitals.glycemia} g/L',
                                      ),
                                    if (state.vitals.painScale != null)
                                      _VitalExtractPill(
                                        label:
                                            'Douleur: EVA ${state.vitals.painScale}/10',
                                      ),
                                  ],
                                ),
                              ],
                            ],
                          ),
                        ),
                        const SizedBox(height: 16),
                        AppButton(
                          label: l10n.assistantApplyLiveVitals,
                          icon: Icons.check_circle_rounded,
                          expand: true,
                          onPressed: _applyResult,
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _WebAlignedVoiceListeningSurface extends StatelessWidget {
  const _WebAlignedVoiceListeningSurface({
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
                  Icon(Icons.auto_awesome_rounded, color: primaryColor, size: 18),
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
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  side: BorderSide(
                    color: active ? AppDesignTokens.error : primaryColor,
                  ),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
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
                      final size = 124.0 + (math.sin(val * math.pi * 2) * 8.0) + levelBonus;
                      final opacity = 0.2 + (math.sin(val * math.pi * 2) * 0.15);

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
                      final size = 104.0 + (math.sin(val * math.pi * 2) * 6.0) + levelBonus;
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
                            : [
                                colors.surfaceContainerHighest,
                                colors.surface,
                              ],
                      ),
                      boxShadow: active
                          ? [
                              BoxShadow(
                                color: primaryColor.withValues(alpha: 0.38),
                                blurRadius: 20 + (soundLevel * 0.5).clamp(0.0, 15.0),
                                spreadRadius: 4 + (soundLevel * 0.2).clamp(0.0, 8.0),
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

      final y1 = midY + math.sin((normX * 4 * math.pi) + phase) * baseAmp * envelope;
      final y2 =
          midY + math.sin((normX * 6 * math.pi) - (phase * 1.4)) * (baseAmp * 0.7) * envelope;

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

class _VitalExtractPill extends StatelessWidget {
  const _VitalExtractPill({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: colors.primary.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(
          color: colors.primary.withValues(alpha: 0.4),
        ),
      ),
      child: Text(
        label,
        style: theme.textTheme.labelSmall?.copyWith(
          color: colors.primary,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}
