import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../data/lab_order_api.dart';
import '../lab_localizations.dart';

class LabOrderCreateSheet extends ConsumerStatefulWidget {
  const LabOrderCreateSheet({
    required this.patientId,
    required this.patientName,
    this.visitId,
    this.onCreated,
    super.key,
  });

  final String patientId;
  final String patientName;
  final String? visitId;
  final VoidCallback? onCreated;

  static Future<void> show(
    BuildContext context, {
    required String patientId,
    required String patientName,
    String? visitId,
    VoidCallback? onCreated,
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
          heightFactor: 0.88,
          child: LabOrderCreateSheet(
            patientId: patientId,
            patientName: patientName,
            visitId: visitId,
            onCreated: onCreated,
          ),
        ),
      ),
    );
  }

  @override
  ConsumerState<LabOrderCreateSheet> createState() =>
      _LabOrderCreateSheetState();
}

class _LabOrderCreateSheetState extends ConsumerState<LabOrderCreateSheet> {
  static const _examTypes = <String>[
    'LABORATOIRE',
    'IMAGERIE',
    'CARDIOLOGIE',
    'ORL',
    'OPHTALMOLOGIE',
    'AUTRE',
  ];

  final _formKey = GlobalKey<FormState>();
  final _examsController = TextEditingController();
  final _reasonController = TextEditingController();
  String _examType = 'LABORATOIRE';
  String _priority = 'NORMALE';
  bool _saving = false;
  String? _error;

  @override
  void dispose() {
    _examsController.dispose();
    _reasonController.dispose();
    super.dispose();
  }

  List<String> get _exams {
    final seen = <String>{};
    final exams = <String>[];
    for (final value in _examsController.text.split(RegExp(r'[\n,;]+'))) {
      final exam = value.trim();
      if (exam.isEmpty) continue;
      final key = exam.toLowerCase();
      if (seen.add(key)) exams.add(exam);
    }
    return exams;
  }

  Future<void> _submit() async {
    if (_saving || _formKey.currentState?.validate() != true) return;
    final l10n = AppLocalizations.of(context);
    setState(() {
      _saving = true;
      _error = null;
    });

    try {
      await ref
          .read(labOrderApiProvider)
          .createOrder(
            patientId: widget.patientId,
            visitId: widget.visitId,
            examType: _examType,
            exams: _exams,
            reason: _reasonController.text,
            priority: _priority,
          );
      if (!mounted) return;
      widget.onCreated?.call();
      Navigator.of(context).pop();
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(l10n.labCreateSuccess)));
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _saving = false;
        _error = _message(error, l10n.labCreateError);
      });
    }
  }

  String _message(Object error, String fallback) {
    if (error is ApiException &&
        error.message.isNotEmpty &&
        !error.message.startsWith('ApiException')) {
      return error.message;
    }
    return fallback;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);

    return Material(
      color: colors.surface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(AppDesignTokens.radiusLg),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 8, 10),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        l10n.labCreateTitle,
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        widget.patientName,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: colors.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  tooltip: MaterialLocalizations.of(context).closeButtonTooltip,
                  onPressed: _saving ? null : () => Navigator.of(context).pop(),
                  icon: const Icon(Icons.close_rounded),
                ),
              ],
            ),
          ),
          Divider(height: 1, color: colors.outlineVariant),
          Expanded(
            child: Form(
              key: _formKey,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 28),
                children: [
                  DropdownButtonFormField<String>(
                    initialValue: _examType,
                    decoration: InputDecoration(labelText: l10n.labExamType),
                    items: [
                      for (final type in _examTypes)
                        DropdownMenuItem(
                          value: type,
                          child: Text(l10n.labExamTypeLabel(type)),
                        ),
                    ],
                    onChanged: _saving
                        ? null
                        : (value) {
                            if (value != null)
                              setState(() => _examType = value);
                          },
                    validator: (value) => value == null || value.isEmpty
                        ? l10n.labExamTypeRequired
                        : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _examsController,
                    enabled: !_saving,
                    minLines: 3,
                    maxLines: 5,
                    textCapitalization: TextCapitalization.sentences,
                    decoration: InputDecoration(
                      labelText: l10n.labRequestedExams,
                      hintText: l10n.labRequestedExamsHint,
                      alignLabelWithHint: true,
                    ),
                    validator: (_) =>
                        _exams.isEmpty ? l10n.labRequestedExamsRequired : null,
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    initialValue: _priority,
                    decoration: InputDecoration(labelText: l10n.labPriority),
                    items: [
                      DropdownMenuItem(
                        value: 'NORMALE',
                        child: Text(l10n.labPriorityNormal),
                      ),
                      DropdownMenuItem(
                        value: 'URGENTE',
                        child: Text(l10n.labPriorityUrgent),
                      ),
                    ],
                    onChanged: _saving
                        ? null
                        : (value) {
                            if (value != null)
                              setState(() => _priority = value);
                          },
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _reasonController,
                    enabled: !_saving,
                    minLines: 2,
                    maxLines: 4,
                    textCapitalization: TextCapitalization.sentences,
                    decoration: InputDecoration(
                      labelText: l10n.labReason,
                      hintText: l10n.labReasonHint,
                      alignLabelWithHint: true,
                    ),
                  ),
                  if (_error != null) ...[
                    const SizedBox(height: 12),
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: colors.errorContainer,
                        borderRadius: BorderRadius.circular(
                          AppDesignTokens.radiusSm,
                        ),
                      ),
                      child: Text(
                        _error!,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: colors.onErrorContainer,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ],
                  const SizedBox(height: 20),
                  AppButton(
                    label: l10n.labCreateSubmit,
                    icon: Icons.add_task_rounded,
                    loading: _saving,
                    expand: true,
                    onPressed: _saving ? null : _submit,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
