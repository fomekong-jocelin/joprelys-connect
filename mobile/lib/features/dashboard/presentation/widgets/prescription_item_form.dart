import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../domain/prescription.dart';
import '../prescription_localizations.dart';

final class PrescriptionItemControllers {
  PrescriptionItemControllers.fromItem(PrescriptionItem item)
    : drugName = TextEditingController(text: item.drugName),
      dosage = TextEditingController(text: item.dosage),
      posology = TextEditingController(text: item.posology),
      duration = TextEditingController(text: item.duration),
      quantity = TextEditingController(text: item.quantity),
      instructions = TextEditingController(text: item.instructions),
      form = TextEditingController(text: item.form),
      route = TextEditingController(text: item.route),
      frequency = TextEditingController(text: item.frequency),
      substitutionAllowed = item.substitutionAllowed;

  PrescriptionItemControllers.empty()
    : this.fromItem(const PrescriptionItem(drugName: ''));

  final TextEditingController drugName;
  final TextEditingController dosage;
  final TextEditingController posology;
  final TextEditingController duration;
  final TextEditingController quantity;
  final TextEditingController instructions;
  final TextEditingController form;
  final TextEditingController route;
  final TextEditingController frequency;
  bool substitutionAllowed;

  PrescriptionItem toItem({required int sortOrder}) {
    return PrescriptionItem(
      drugName: drugName.text.trim(),
      dosage: dosage.text.trim(),
      posology: posology.text.trim(),
      duration: duration.text.trim(),
      quantity: quantity.text.trim(),
      instructions: instructions.text.trim(),
      form: form.text.trim(),
      route: route.text.trim(),
      frequency: frequency.text.trim(),
      sortOrder: sortOrder,
      substitutionAllowed: substitutionAllowed,
    );
  }

  void dispose() {
    drugName.dispose();
    dosage.dispose();
    posology.dispose();
    duration.dispose();
    quantity.dispose();
    instructions.dispose();
    form.dispose();
    route.dispose();
    frequency.dispose();
  }
}

class PrescriptionItemForm extends StatefulWidget {
  const PrescriptionItemForm({
    required this.index,
    required this.controllers,
    required this.editable,
    required this.requireDosage,
    required this.onChanged,
    required this.onRemove,
    this.initiallyExpanded = false,
    super.key,
  });

  final int index;
  final PrescriptionItemControllers controllers;
  final bool editable;
  final bool requireDosage;
  final bool initiallyExpanded;
  final VoidCallback onChanged;
  final VoidCallback onRemove;

  @override
  State<PrescriptionItemForm> createState() => _PrescriptionItemFormState();
}

class _PrescriptionItemFormState extends State<PrescriptionItemForm> {
  late bool _expanded;
  bool _detailsExpanded = false;

  @override
  void initState() {
    super.initState();
    _expanded = widget.initiallyExpanded;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    final drug = widget.controllers.drugName.text.trim();
    final dosage = widget.controllers.dosage.text.trim();
    final posology = widget.controllers.posology.text.trim();

    return Container(
      decoration: BoxDecoration(
        color: colors.surface,
        border: Border.all(color: colors.outlineVariant),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        children: [
          InkWell(
            onTap: () => setState(() => _expanded = !_expanded),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(12, 10, 8, 10),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          drug.isEmpty
                              ? l10n.prescriptionMedicationIndex(
                                  widget.index + 1,
                                )
                              : drug,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.titleSmall?.copyWith(
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        if (dosage.isNotEmpty || posology.isNotEmpty) ...[
                          const SizedBox(height: 2),
                          Text(
                            [
                              if (dosage.isNotEmpty) dosage,
                              if (posology.isNotEmpty) posology,
                            ].join(' · '),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: colors.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                  if (widget.editable)
                    IconButton(
                      tooltip: l10n.prescriptionRemoveMedication,
                      visualDensity: VisualDensity.compact,
                      onPressed: widget.onRemove,
                      icon: Icon(
                        Icons.delete_outline_rounded,
                        color: colors.error,
                        size: 20,
                      ),
                    ),
                  Icon(
                    _expanded
                        ? Icons.expand_less_rounded
                        : Icons.expand_more_rounded,
                    color: colors.onSurfaceVariant,
                  ),
                ],
              ),
            ),
          ),
          if (_expanded) ...[
            Divider(height: 1, color: colors.outlineVariant),
            Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _field(
                    controller: widget.controllers.drugName,
                    label: l10n.prescriptionDrugName,
                    hint: l10n.prescriptionDrugNameHint,
                    enabled: widget.editable,
                    requiredMessage: l10n.prescriptionDrugRequired,
                  ),
                  const SizedBox(height: 10),
                  _field(
                    controller: widget.controllers.dosage,
                    label: l10n.prescriptionDosage,
                    hint: l10n.prescriptionDosageHint,
                    enabled: widget.editable,
                    requiredMessage: widget.requireDosage
                        ? l10n.prescriptionDosageRequiredToFinalize
                        : null,
                  ),
                  const SizedBox(height: 10),
                  _field(
                    controller: widget.controllers.posology,
                    label: l10n.prescriptionPosology,
                    hint: l10n.prescriptionPosologyHint,
                    enabled: widget.editable,
                    maxLines: 2,
                  ),
                  const SizedBox(height: 10),
                  LayoutBuilder(
                    builder: (context, constraints) {
                      final stacked = constraints.maxWidth < 430;
                      final duration = _field(
                        controller: widget.controllers.duration,
                        label: l10n.prescriptionDuration,
                        enabled: widget.editable,
                      );
                      final quantity = _field(
                        controller: widget.controllers.quantity,
                        label: l10n.prescriptionQuantity,
                        enabled: widget.editable,
                      );
                      if (stacked) {
                        return Column(
                          children: [
                            duration,
                            const SizedBox(height: 10),
                            quantity,
                          ],
                        );
                      }
                      return Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(child: duration),
                          const SizedBox(width: 10),
                          Expanded(child: quantity),
                        ],
                      );
                    },
                  ),
                  const SizedBox(height: 4),
                  TextButton.icon(
                    onPressed: () =>
                        setState(() => _detailsExpanded = !_detailsExpanded),
                    style: TextButton.styleFrom(
                      alignment: Alignment.centerLeft,
                      padding: const EdgeInsets.symmetric(vertical: 8),
                    ),
                    icon: Icon(
                      _detailsExpanded
                          ? Icons.keyboard_arrow_up_rounded
                          : Icons.tune_rounded,
                      size: 18,
                    ),
                    label: Text(l10n.prescriptionMoreDetails),
                  ),
                  if (_detailsExpanded) ...[
                    _field(
                      controller: widget.controllers.form,
                      label: l10n.prescriptionForm,
                      enabled: widget.editable,
                    ),
                    const SizedBox(height: 10),
                    _field(
                      controller: widget.controllers.route,
                      label: l10n.prescriptionRoute,
                      enabled: widget.editable,
                    ),
                    const SizedBox(height: 10),
                    _field(
                      controller: widget.controllers.frequency,
                      label: l10n.prescriptionFrequency,
                      enabled: widget.editable,
                    ),
                    const SizedBox(height: 10),
                    _field(
                      controller: widget.controllers.instructions,
                      label: l10n.prescriptionInstructions,
                      enabled: widget.editable,
                      maxLines: 3,
                    ),
                    if (widget.editable) ...[
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              l10n.prescriptionSubstitutionAllowed,
                              style: theme.textTheme.bodyMedium?.copyWith(
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Switch.adaptive(
                            value: widget.controllers.substitutionAllowed,
                            onChanged: (value) {
                              setState(() {
                                widget.controllers.substitutionAllowed = value;
                              });
                              widget.onChanged();
                            },
                          ),
                        ],
                      ),
                    ],
                  ],
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _field({
    required TextEditingController controller,
    required String label,
    required bool enabled,
    String? hint,
    String? requiredMessage,
    int maxLines = 1,
  }) {
    return TextFormField(
      controller: controller,
      enabled: enabled,
      minLines: maxLines,
      maxLines: maxLines,
      decoration: InputDecoration(labelText: label, hintText: hint),
      onChanged: (_) {
        widget.onChanged();
        setState(() {});
      },
      validator: (value) {
        if (requiredMessage != null &&
            (value == null || value.trim().isEmpty)) {
          return requiredMessage;
        }
        return null;
      },
    );
  }
}
