import 'dart:convert';

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
    for (final controller in <TextEditingController>[
      _tempController,
      _weightController,
      _heightController,
      _pulseController,
      _sysController,
      _diaController,
      _spo2Controller,
      _glycemiaController,
      _respController,
      _painController,
    ]) {
      controller.dispose();
    }
    super.dispose();
  }

  Future<void> _fetchExistingVitals() async {
    try {
      final existing = await ref.read(vitalsApiProvider).getVitals(widget.visit.id);
      if (existing != null && mounted) _populateFields(existing);
    } catch (_) {
      // Un formulaire vide reste utilisable si aucune constante n'est disponible.
    } finally {
      if (mounted) setState(() => _fetchingInitial = false);
    }
  }

  void _populateFields(PatientVitals vitals) {
    _setValue(_tempController, vitals.temperature);
    _setValue(_weightController, vitals.weight);
    _setValue(_heightController, vitals.height);
    _setValue(_pulseController, vitals.pulse);
    _setValue(_sysController, vitals.systolic);
    _setValue(_diaController, vitals.diastolic);
    _setValue(_spo2Controller, vitals.spo2);
    _setValue(_glycemiaController, vitals.glycemia);
    _setValue(_respController, vitals.respiratoryRate);
    _setValue(_painController, vitals.painScale);
    _recalculateBmi();
  }

  void _setValue(TextEditingController controller, num? value) {
    if (value != null) controller.text = value.toString();
  }

  void _recalculateBmi() {
    final weight = double.tryParse(_weightController.text.trim());
    final height = double.tryParse(_heightController.text.trim());
    final next = weight != null && height != null && height > 0
        ? weight / ((height / 100) * (height / 100))
        : null;
    if (next != _calculatedBmi && mounted) {
      setState(() => _calculatedBmi = next);
    }
  }

  PatientVitals _currentVitals() {
    return PatientVitals(
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
  }

  Future<void> _submit() async {
    if (_formKey.currentState?.validate() != true) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await ref.read(vitalsApiProvider).saveVitals(
        widget.visit.id,
        _currentVitals(),
      );
      if (!mounted) return;
      Navigator.of(context).pop();
      widget.onSaved();
    } catch (error) {
      if (mounted) {
        setState(() {
          _loading = false;
          _error = error.toString();
        });
      }
    }
  }

  void _launchAssistant() {
    final current = _currentVitals();
    ClinicalVoiceAssistantSheet.show(
      context,
      visit: widget.visit,
      initialDraft: current.isEmpty
          ? const <String, String>{}
          : <String, String>{'vitals': jsonEncode(current.toJson())},
      onExtracted: (result) {
        final changed = _fillEmptyVitals(result.vitals);
        if (!mounted) return;
        _recalculateBmi();
        final isFrench = Localizations.localeOf(context).languageCode != 'en';
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              changed > 0
                  ? (isFrench
                        ? '$changed constante(s) acceptée(s) ont rempli uniquement les champs vides.'
                        : '$changed accepted vital(s) filled empty fields only.')
                  : (isFrench
                        ? 'Aucun champ vide à compléter. Les constantes saisies ont été conservées.'
                        : 'No empty field to fill. Existing vitals were preserved.'),
            ),
          ),
        );
      },
    );
  }

  int _fillEmptyVitals(PatientVitals vitals) {
    var changed = 0;
    changed += _fillWhenEmpty(_tempController, vitals.temperature);
    changed += _fillWhenEmpty(_weightController, vitals.weight);
    changed += _fillWhenEmpty(_heightController, vitals.height);
    changed += _fillWhenEmpty(_pulseController, vitals.pulse);
    changed += _fillWhenEmpty(_sysController, vitals.systolic);
    changed += _fillWhenEmpty(_diaController, vitals.diastolic);
    changed += _fillWhenEmpty(_spo2Controller, vitals.spo2);
    changed += _fillWhenEmpty(_glycemiaController, vitals.glycemia);
    changed += _fillWhenEmpty(_respController, vitals.respiratoryRate);
    changed += _fillWhenEmpty(_painController, vitals.painScale);
    return changed;
  }

  int _fillWhenEmpty(TextEditingController controller, num? value) {
    if (controller.text.trim().isNotEmpty || value == null) return 0;
    controller.text = value.toString();
    return 1;
  }

  String _formattedReference(String visitNum, String rawDpu) {
    var cleaned = rawDpu.trim().replaceAll(
      RegExp(r'^(DPU[\s\-]*)+', caseSensitive: false),
      'DPU-',
    );
    if (!cleaned.toUpperCase().startsWith('DPU-')) cleaned = 'DPU-$cleaned';
    return '$visitNum · ${cleaned.replaceAll('-', '\u2011')}';
  }

  Widget _numberField({
    required String label,
    required String hint,
    required TextEditingController controller,
    bool decimal = false,
  }) {
    return AppTextField(
      label: label,
      hint: hint,
      controller: controller,
      keyboardType: TextInputType.numberWithOptions(decimal: decimal),
    );
  }

  Widget _fieldRow(Widget first, Widget second) {
    return Row(
      children: [
        Expanded(child: first),
        const SizedBox(width: 12),
        Expanded(child: second),
      ],
    );
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
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Text(
                                    l10n.vitalsBmiLabel,
                                    style: theme.textTheme.labelMedium?.copyWith(
                                      fontWeight: FontWeight.w700,
                                      color: colors.primary,
                                    ),
                                  ),
                                  Text(
                                    '${_calculatedBmi!.toStringAsFixed(1)} kg/m²',
                                    style: theme.textTheme.titleMedium?.copyWith(
                                      fontWeight: FontWeight.w900,
                                      color: colors.primary,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(height: 16),
                          ],
                          _fieldRow(
                            _numberField(
                              label: l10n.vitalsTemperatureLabel,
                              hint: l10n.vitalsTemperatureHint,
                              controller: _tempController,
                              decimal: true,
                            ),
                            _numberField(
                              label: l10n.vitalsPulseLabel,
                              hint: l10n.vitalsPulseHint,
                              controller: _pulseController,
                            ),
                          ),
                          const SizedBox(height: 12),
                          _fieldRow(
                            _numberField(
                              label: l10n.vitalsWeightLabel,
                              hint: l10n.vitalsWeightHint,
                              controller: _weightController,
                              decimal: true,
                            ),
                            _numberField(
                              label: l10n.vitalsHeightLabel,
                              hint: l10n.vitalsHeightHint,
                              controller: _heightController,
                            ),
                          ),
                          const SizedBox(height: 12),
                          _fieldRow(
                            _numberField(
                              label: l10n.vitalsSystolicLabel,
                              hint: l10n.vitalsSystolicHint,
                              controller: _sysController,
                            ),
                            _numberField(
                              label: l10n.vitalsDiastolicLabel,
                              hint: l10n.vitalsDiastolicHint,
                              controller: _diaController,
                            ),
                          ),
                          const SizedBox(height: 12),
                          _fieldRow(
                            _numberField(
                              label: l10n.vitalsSpo2Label,
                              hint: l10n.vitalsSpo2Hint,
                              controller: _spo2Controller,
                            ),
                            _numberField(
                              label: l10n.vitalsGlycemiaLabel,
                              hint: l10n.vitalsGlycemiaHint,
                              controller: _glycemiaController,
                              decimal: true,
                            ),
                          ),
                          const SizedBox(height: 12),
                          _fieldRow(
                            _numberField(
                              label: l10n.vitalsRespiratoryRateLabel,
                              hint: l10n.vitalsRespiratoryRateHint,
                              controller: _respController,
                            ),
                            _numberField(
                              label: l10n.vitalsPainScaleLabel,
                              hint: l10n.vitalsPainScaleHint,
                              controller: _painController,
                            ),
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
