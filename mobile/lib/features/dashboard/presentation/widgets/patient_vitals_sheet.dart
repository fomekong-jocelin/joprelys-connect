import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../../../shared/widgets/app_text_field.dart';
import '../../data/vitals_api.dart';
import '../../domain/active_visit.dart';
import '../../domain/patient_vitals.dart';
import '../dashboard_localizations.dart';
import 'clinical_voice_assistant_sheet.dart';

class PatientVitalsSheet extends ConsumerStatefulWidget {
  const PatientVitalsSheet({
    required this.visit,
    required this.onSaved,
    super.key,
  });

  final ActiveVisit visit;
  final VoidCallback onSaved;

  static Future<void> show(
    BuildContext context, {
    required ActiveVisit visit,
    required VoidCallback onSaved,
  }) {
    return showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) => FractionallySizedBox(
        heightFactor: 0.9,
        child: PatientVitalsSheet(visit: visit, onSaved: onSaved),
      ),
    );
  }

  @override
  ConsumerState<PatientVitalsSheet> createState() => _PatientVitalsSheetState();
}

class _PatientVitalsSheetState extends ConsumerState<PatientVitalsSheet> {
  final _formKey = GlobalKey<FormState>();

  final _tempController = TextEditingController();
  final _weightController = TextEditingController();
  final _heightController = TextEditingController();
  final _pulseController = TextEditingController();
  final _sysController = TextEditingController();
  final _diaController = TextEditingController();
  final _spo2Controller = TextEditingController();
  final _glycemiaController = TextEditingController();
  final _respController = TextEditingController();
  final _painController = TextEditingController();

  bool _loading = false;
  bool _fetchingInitial = true;
  String? _error;
  double? _calculatedBmi;

  @override
  void initState() {
    super.initState();
    _fetchExistingVitals();
    _weightController.addListener(_recalculateBmi);
    _heightController.addListener(_recalculateBmi);
  }

  @override
  void dispose() {
    _tempController.dispose();
    _weightController.dispose();
    _heightController.dispose();
    _pulseController.dispose();
    _sysController.dispose();
    _diaController.dispose();
    _spo2Controller.dispose();
    _glycemiaController.dispose();
    _respController.dispose();
    _painController.dispose();
    super.dispose();
  }

  Future<void> _fetchExistingVitals() async {
    try {
      final gateway = ref.read(vitalsApiProvider);
      final existing = await gateway.getVitals(widget.visit.id);
      if (existing != null && mounted) {
        _populateFields(existing);
      }
    } catch (_) {
      // Keep empty form if initial fetch fails or has no vitals yet
    } finally {
      if (mounted) {
        setState(() => _fetchingInitial = false);
      }
    }
  }

  void _populateFields(PatientVitals vitals) {
    if (vitals.temperature != null) {
      _tempController.text = vitals.temperature.toString();
    }
    if (vitals.weight != null) {
      _weightController.text = vitals.weight.toString();
    }
    if (vitals.height != null) {
      _heightController.text = vitals.height.toString();
    }
    if (vitals.pulse != null) {
      _pulseController.text = vitals.pulse.toString();
    }
    if (vitals.systolic != null) {
      _sysController.text = vitals.systolic.toString();
    }
    if (vitals.diastolic != null) {
      _diaController.text = vitals.diastolic.toString();
    }
    if (vitals.spo2 != null) {
      _spo2Controller.text = vitals.spo2.toString();
    }
    if (vitals.glycemia != null) {
      _glycemiaController.text = vitals.glycemia.toString();
    }
    if (vitals.respiratoryRate != null) {
      _respController.text = vitals.respiratoryRate.toString();
    }
    if (vitals.painScale != null) {
      _painController.text = vitals.painScale.toString();
    }
    _recalculateBmi();
  }

  void _recalculateBmi() {
    final w = double.tryParse(_weightController.text.trim());
    final h = double.tryParse(_heightController.text.trim());
    if (w != null && h != null && h > 0) {
      final hMeters = h / 100.0;
      setState(() => _calculatedBmi = w / (hMeters * hMeters));
    } else {
      if (_calculatedBmi != null) {
        setState(() => _calculatedBmi = null);
      }
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _loading = true;
      _error = null;
    });

    final payload = PatientVitals(
      temperature: double.tryParse(_tempController.text.trim()),
      weight: double.tryParse(_weightController.text.trim()),
      height: int.tryParse(_heightController.text.trim()),
      pulse: int.tryParse(_pulseController.text.trim()),
      systolic: int.tryParse(_sysController.text.trim()),
      diastolic: int.tryParse(_diaController.text.trim()),
      spo2: int.tryParse(_spo2Controller.text.trim()),
      glycemia: double.tryParse(_glycemiaController.text.trim()),
      respiratoryRate: int.tryParse(_respController.text.trim()),
      painScale: int.tryParse(_painController.text.trim()),
    );

    try {
      final gateway = ref.read(vitalsApiProvider);
      await gateway.saveVitals(widget.visit.id, payload);
      if (mounted) {
        Navigator.of(context).pop();
        widget.onSaved();
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _loading = false;
          _error = e.toString();
        });
      }
    }
  }

  void _launchAssistant() {
    ClinicalVoiceAssistantSheet.show(
      context,
      visit: widget.visit,
      onExtracted: (result) {
        final v = result.vitals;
        if (v.temperature != null)
          _tempController.text = v.temperature.toString();
        if (v.pulse != null) _pulseController.text = v.pulse.toString();
        if (v.weight != null) _weightController.text = v.weight.toString();
        if (v.height != null) _heightController.text = v.height.toString();
        if (v.systolic != null) _sysController.text = v.systolic.toString();
        if (v.diastolic != null) _diaController.text = v.diastolic.toString();
        if (v.spo2 != null) _spo2Controller.text = v.spo2.toString();
        if (v.glycemia != null)
          _glycemiaController.text = v.glycemia.toString();
        if (v.respiratoryRate != null)
          _respController.text = v.respiratoryRate.toString();
        if (v.painScale != null) _painController.text = v.painScale.toString();
        _recalculateBmi();
      },
    );
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
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.visit.patientName,
                        style: theme.textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        _formattedReference(
                          widget.visit.visitNumber,
                          widget.visit.patientDpu,
                        ),
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: colors.onSurfaceVariant,
                          height: 1.3,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  tooltip: l10n.assistantTitle,
                  onPressed: _launchAssistant,
                  icon: Icon(Icons.mic_rounded, color: colors.primary),
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
            child: _fetchingInitial
                ? const Center(child: CircularProgressIndicator())
                : SingleChildScrollView(
                    padding: const EdgeInsets.all(16),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          if (_calculatedBmi != null) ...[
                            Container(
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: colors.primary.withValues(alpha: 0.1),
                                borderRadius: BorderRadius.circular(
                                  AppDesignTokens.radiusSm,
                                ),
                                border: Border.all(
                                  color: colors.primary.withValues(alpha: 0.3),
                                ),
                              ),
                              child: Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Text(
                                    l10n.vitalsBmiLabel,
                                    style: theme.textTheme.labelMedium
                                        ?.copyWith(
                                          fontWeight: FontWeight.w700,
                                          color: colors.primary,
                                        ),
                                  ),
                                  Text(
                                    '${_calculatedBmi!.toStringAsFixed(1)} kg/m²',
                                    style: theme.textTheme.titleMedium
                                        ?.copyWith(
                                          fontWeight: FontWeight.w900,
                                          color: colors.primary,
                                        ),
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(height: 16),
                          ],
                          Row(
                            children: [
                              Expanded(
                                child: AppTextField(
                                  label: l10n.vitalsTemperatureLabel,
                                  hint: l10n.vitalsTemperatureHint,
                                  controller: _tempController,
                                  keyboardType:
                                      const TextInputType.numberWithOptions(
                                        decimal: true,
                                      ),
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: AppTextField(
                                  label: l10n.vitalsPulseLabel,
                                  hint: l10n.vitalsPulseHint,
                                  controller: _pulseController,
                                  keyboardType: TextInputType.number,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),
                          Row(
                            children: [
                              Expanded(
                                child: AppTextField(
                                  label: l10n.vitalsWeightLabel,
                                  hint: l10n.vitalsWeightHint,
                                  controller: _weightController,
                                  keyboardType:
                                      const TextInputType.numberWithOptions(
                                        decimal: true,
                                      ),
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: AppTextField(
                                  label: l10n.vitalsHeightLabel,
                                  hint: l10n.vitalsHeightHint,
                                  controller: _heightController,
                                  keyboardType: TextInputType.number,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),
                          Row(
                            children: [
                              Expanded(
                                child: AppTextField(
                                  label: l10n.vitalsSystolicLabel,
                                  hint: l10n.vitalsSystolicHint,
                                  controller: _sysController,
                                  keyboardType: TextInputType.number,
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: AppTextField(
                                  label: l10n.vitalsDiastolicLabel,
                                  hint: l10n.vitalsDiastolicHint,
                                  controller: _diaController,
                                  keyboardType: TextInputType.number,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),
                          Row(
                            children: [
                              Expanded(
                                child: AppTextField(
                                  label: l10n.vitalsSpo2Label,
                                  hint: l10n.vitalsSpo2Hint,
                                  controller: _spo2Controller,
                                  keyboardType: TextInputType.number,
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: AppTextField(
                                  label: l10n.vitalsGlycemiaLabel,
                                  hint: l10n.vitalsGlycemiaHint,
                                  controller: _glycemiaController,
                                  keyboardType:
                                      const TextInputType.numberWithOptions(
                                        decimal: true,
                                      ),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),
                          Row(
                            children: [
                              Expanded(
                                child: AppTextField(
                                  label: l10n.vitalsRespiratoryRateLabel,
                                  hint: l10n.vitalsRespiratoryRateHint,
                                  controller: _respController,
                                  keyboardType: TextInputType.number,
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: AppTextField(
                                  label: l10n.vitalsPainScaleLabel,
                                  hint: l10n.vitalsPainScaleHint,
                                  controller: _painController,
                                  keyboardType: TextInputType.number,
                                ),
                              ),
                            ],
                          ),
                          if (_error != null) ...[
                            const SizedBox(height: 12),
                            Text(
                              _error!,
                              style: theme.textTheme.bodySmall?.copyWith(
                                color: colors.error,
                              ),
                            ),
                          ],
                          const SizedBox(height: 24),
                          AppButton(
                            label: l10n.vitalsSaveButton,
                            icon: Icons.save_rounded,
                            expand: true,
                            loading: _loading,
                            onPressed: _loading ? null : _submit,
                          ),
                        ],
                      ),
                    ),
                  ),
          ),
        ],
      ),
    );
  }
}
