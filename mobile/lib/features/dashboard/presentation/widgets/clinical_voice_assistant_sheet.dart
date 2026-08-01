import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'clinical_voice_listening_surface.dart';
import 'clinical_voice_transcript_widgets.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
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
    final result = DictationParseResult(vitals: state.vitals, note: state.note);
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
                              Icon(
                                Icons.mic_rounded,
                                color: colors.primary,
                                size: 22,
                              ),
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
                      ClinicalVoiceListeningSurface(
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
                                  backgroundColor: colors.primary.withValues(
                                    alpha: 0.1,
                                  ),
                                  side: BorderSide(
                                    color: colors.primary.withValues(
                                      alpha: 0.3,
                                    ),
                                  ),
                                  onPressed: () => _applyPreset(text),
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),
                      // Zone de transcription vocale en direct (Web-aligned with timestamp & Corriger action)
                      ClinicalTranscriptCard(
                        transcript: state.transcript,
                        controller: _dictationController,
                        onChanged: _onTextChanged,
                        label: l10n.assistantLiveTranscript,
                        hint: l10n.assistantDictationHint,
                      ),
                      const SizedBox(height: 16),
                      // Synthèse d'extraction des constantes en direct
                      if (!state.vitals.isEmpty ||
                          state.note.symptoms != null) ...[
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
                                      VitalExtractPill(
                                        label:
                                            'T°: ${state.vitals.temperature}°C',
                                      ),
                                    if (state.vitals.systolic != null &&
                                        state.vitals.diastolic != null)
                                      VitalExtractPill(
                                        label:
                                            'TA: ${state.vitals.systolic}/${state.vitals.diastolic} mmHg',
                                      ),
                                    if (state.vitals.pulse != null)
                                      VitalExtractPill(
                                        label:
                                            'Pouls: ${state.vitals.pulse} bpm',
                                      ),
                                    if (state.vitals.spo2 != null)
                                      VitalExtractPill(
                                        label: 'SpO2: ${state.vitals.spo2}%',
                                      ),
                                    if (state.vitals.weight != null)
                                      VitalExtractPill(
                                        label:
                                            'Poids: ${state.vitals.weight} kg',
                                      ),
                                    if (state.vitals.height != null)
                                      VitalExtractPill(
                                        label:
                                            'Taille: ${state.vitals.height} cm',
                                      ),
                                    if (state.vitals.glycemia != null)
                                      VitalExtractPill(
                                        label:
                                            'Glycémie: ${state.vitals.glycemia} g/L',
                                      ),
                                    if (state.vitals.painScale != null)
                                      VitalExtractPill(
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
