import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/i18n/app_locale_formatters.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../data/prescription_api.dart';
import '../../domain/prescription.dart';
import '../prescription_localizations.dart';
import 'prescription_item_form.dart';

class PrescriptionEditorSheet extends ConsumerStatefulWidget {
  const PrescriptionEditorSheet({
    required this.consultationId,
    required this.reference,
    required this.canWrite,
    this.onChanged,
    super.key,
  });

  final String consultationId;
  final String reference;
  final bool canWrite;
  final VoidCallback? onChanged;

  static Future<void> show(
    BuildContext context, {
    required String consultationId,
    required String reference,
    required bool canWrite,
    VoidCallback? onChanged,
  }) {
    return showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => FractionallySizedBox(
        heightFactor: 0.94,
        child: PrescriptionEditorSheet(
          consultationId: consultationId,
          reference: reference,
          canWrite: canWrite,
          onChanged: onChanged,
        ),
      ),
    );
  }

  @override
  ConsumerState<PrescriptionEditorSheet> createState() =>
      _PrescriptionEditorSheetState();
}

class _PrescriptionEditorSheetState
    extends ConsumerState<PrescriptionEditorSheet> {
  final _formKey = GlobalKey<FormState>();
  final List<PrescriptionItemControllers> _items = [];

  Prescription? _prescription;
  bool _loading = true;
  bool _busy = false;
  bool _dirty = false;
  bool _requireDosage = false;
  String? _error;

  bool get _editable => widget.canWrite && (_prescription?.isDraft ?? true);

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _disposeItems();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final prescription = await ref
          .read(prescriptionApiProvider)
          .getForConsultation(widget.consultationId);
      if (!mounted) return;
      _replaceItems(prescription?.items ?? const <PrescriptionItem>[]);
      if (prescription == null && widget.canWrite) {
        _items.add(PrescriptionItemControllers.empty());
      }
      setState(() {
        _prescription = prescription;
        _dirty = false;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = _message(error, fallback: _l10n.prescriptionLoadError);
      });
    }
  }

  AppLocalizations get _l10n => AppLocalizations.of(context);

  void _replaceItems(List<PrescriptionItem> items) {
    _disposeItems();
    _items.addAll(items.map(PrescriptionItemControllers.fromItem));
  }

  void _disposeItems() {
    for (final item in _items) {
      item.dispose();
    }
    _items.clear();
  }

  void _addItem() {
    if (!_editable || _busy) return;
    setState(() {
      _items.add(PrescriptionItemControllers.empty());
      _dirty = true;
      _requireDosage = false;
      _error = null;
    });
  }

  void _removeItem(int index) {
    if (!_editable || _busy) return;
    final removed = _items.removeAt(index);
    removed.dispose();
    setState(() {
      _dirty = true;
      _error = null;
    });
  }

  List<PrescriptionItem> _draftItems() {
    return [
      for (var index = 0; index < _items.length; index++)
        _items[index].toItem(sortOrder: index),
    ];
  }

  bool _validateDraft() {
    setState(() => _requireDosage = false);
    if (_items.isEmpty) {
      setState(() => _error = _l10n.prescriptionAtLeastOneMedication);
      return false;
    }
    return _formKey.currentState?.validate() == true;
  }

  bool _validateFinalization() {
    setState(() => _requireDosage = true);
    if (_items.isEmpty) {
      setState(() => _error = _l10n.prescriptionAtLeastOneMedication);
      return false;
    }
    return _formKey.currentState?.validate() == true;
  }

  Future<Prescription?> _saveDraft({bool notify = true}) async {
    if (!_validateDraft()) return null;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final saved = await ref.read(prescriptionApiProvider).saveDraft(
            consultationId: widget.consultationId,
            items: _draftItems(),
          );
      if (!mounted) return null;
      _replaceItems(saved.items);
      setState(() {
        _prescription = saved;
        _dirty = false;
        _busy = false;
      });
      widget.onChanged?.call();
      if (notify) _notice(_l10n.prescriptionDraftSaved);
      return saved;
    } catch (error) {
      if (!mounted) return null;
      setState(() {
        _busy = false;
        _error = _message(error, fallback: _l10n.prescriptionSaveError);
      });
      return null;
    }
  }

  Future<void> _finalize() async {
    if (!_validateFinalization()) return;
    final confirmed = await _confirm(
      title: _l10n.prescriptionFinalizeTitle,
      body: _l10n.prescriptionFinalizeBody,
      destructive: false,
    );
    if (!confirmed || !mounted) return;

    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      var prescription = _prescription;
      if (prescription == null || _dirty) {
        setState(() => _busy = false);
        prescription = await _saveDraft(notify: false);
        if (prescription == null || !mounted) return;
        setState(() => _busy = true);
      }
      final finalized = await ref
          .read(prescriptionApiProvider)
          .finalize(prescription.id);
      if (!mounted) return;
      _replaceItems(finalized.items);
      setState(() {
        _prescription = finalized;
        _dirty = false;
        _busy = false;
        _requireDosage = false;
      });
      widget.onChanged?.call();
      _notice(_l10n.prescriptionFinalized);
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _busy = false;
        _error = _message(error, fallback: _l10n.prescriptionSaveError);
      });
    }
  }

  Future<void> _cancel() async {
    final prescription = _prescription;
    if (prescription == null || !widget.canWrite) return;
    final confirmed = await _confirm(
      title: _l10n.prescriptionCancelTitle,
      body: _l10n.prescriptionCancelBody,
      destructive: true,
    );
    if (!confirmed || !mounted) return;

    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final cancelled = await ref
          .read(prescriptionApiProvider)
          .cancel(prescription.id);
      if (!mounted) return;
      setState(() {
        _prescription = cancelled;
        _busy = false;
        _dirty = false;
      });
      widget.onChanged?.call();
      _notice(_l10n.prescriptionCancelled);
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _busy = false;
        _error = _message(error, fallback: _l10n.prescriptionSaveError);
      });
    }
  }

  Future<void> _transmit() async {
    final prescription = _prescription;
    if (prescription == null || !prescription.isActive || !widget.canWrite) {
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final transmitted = await ref
          .read(prescriptionApiProvider)
          .transmit(prescription.id);
      if (!mounted) return;
      setState(() {
        _prescription = transmitted;
        _busy = false;
      });
      widget.onChanged?.call();
      _notice(_l10n.prescriptionTransmitted);
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _busy = false;
        _error = _message(error, fallback: _l10n.prescriptionSaveError);
      });
    }
  }

  Future<bool> _confirm({
    required String title,
    required String body,
    required bool destructive,
  }) async {
    final result = await showDialog<bool>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text(title),
          content: Text(body),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: Text(_l10n.prescriptionBack),
            ),
            TextButton(
              onPressed: () => Navigator.of(context).pop(true),
              style: destructive
                  ? TextButton.styleFrom(
                      foregroundColor: Theme.of(context).colorScheme.error,
                    )
                  : null,
              child: Text(_l10n.prescriptionConfirm),
            ),
          ],
        );
      },
    );
    return result == true;
  }

  void _notice(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), behavior: SnackBarBehavior.floating),
    );
  }

  String _message(Object error, {required String fallback}) {
    if (error is ApiException && error.message.trim().isNotEmpty) {
      return error.message;
    }
    return fallback;
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(AppDesignTokens.radiusLg),
        ),
      ),
      child: Column(
        children: [
          _header(),
          Divider(height: 1, color: colors.outlineVariant),
          Expanded(child: _content()),
          if (!_loading && _error == null) _actionBar(),
        ],
      ),
    );
  }

  Widget _header() {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Column(
      children: [
        const SizedBox(height: 8),
        Container(
          width: 42,
          height: 4,
          decoration: BoxDecoration(
            color: colors.outlineVariant,
            borderRadius: BorderRadius.circular(AppDesignTokens.radiusXs),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 8, 10),
          child: Row(
            children: [
              Icon(Icons.medication_outlined, color: colors.primary, size: 22),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _l10n.prescriptionTitle,
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    Text(
                      widget.reference,
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
                tooltip: _l10n.prescriptionClose,
                onPressed: _busy ? null : () => Navigator.of(context).pop(),
                icon: const Icon(Icons.close_rounded),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _content() {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null && _prescription == null && _items.isEmpty) {
      return _RetryState(message: _error!, onRetry: _load);
    }

    final prescription = _prescription;
    final editable = _editable;
    return Form(
      key: _formKey,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 14, 16, 24),
        children: [
          if (prescription != null) _metadata(prescription),
          if (prescription != null && !prescription.isDraft) ...[
            const SizedBox(height: 10),
            _ReadOnlyNotice(text: _l10n.prescriptionReadOnly),
          ],
          if (_error != null) ...[
            const SizedBox(height: 10),
            _InlineError(message: _error!),
          ],
          const SizedBox(height: 12),
          if (_items.isEmpty && !editable)
            _EmptyPrescription(message: _l10n.prescriptionEmptyBody)
          else ...[
            for (var index = 0; index < _items.length; index++) ...[
              PrescriptionItemForm(
                key: ValueKey('prescription-item-$index'),
                index: index,
                controllers: _items[index],
                editable: editable,
                requireDosage: _requireDosage,
                initiallyExpanded: editable && index == _items.length - 1,
                onChanged: () {
                  if (!_dirty) setState(() => _dirty = true);
                },
                onRemove: () => _removeItem(index),
              ),
              const SizedBox(height: 10),
            ],
          ],
          if (editable)
            Align(
              alignment: Alignment.centerLeft,
              child: OutlinedButton.icon(
                onPressed: _busy ? null : _addItem,
                icon: const Icon(Icons.add_rounded, size: 18),
                label: Text(_l10n.prescriptionAddMedication),
              ),
            ),
          if (widget.canWrite &&
              prescription != null &&
              !prescription.isCancelled &&
              !prescription.isExpired) ...[
            const SizedBox(height: 12),
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton.icon(
                onPressed: _busy ? null : _cancel,
                style: TextButton.styleFrom(
                  foregroundColor: Theme.of(context).colorScheme.error,
                ),
                icon: const Icon(Icons.block_rounded, size: 18),
                label: Text(_l10n.prescriptionCancel),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _metadata(Prescription prescription) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final locale = Localizations.localeOf(context);
    final status = _statusLabel(prescription.status);
    final transmission = _transmissionLabel(prescription.transmissionStatus);

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
        border: Border.all(color: colors.outlineVariant),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  prescription.prescriptionNumber ?? _l10n.prescriptionTitle,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              _StatusTag(label: status, status: prescription.status),
            ],
          ),
          if (prescription.expiresAt != null || transmission != null) ...[
            const SizedBox(height: 8),
            Wrap(
              spacing: 12,
              runSpacing: 6,
              children: [
                if (prescription.expiresAt != null)
                  _MetaText(
                    icon: Icons.event_outlined,
                    text: AppLocaleFormatters.formatDate(
                      prescription.expiresAt!,
                      locale,
                    ),
                  ),
                if (transmission != null)
                  _MetaText(icon: Icons.sync_alt_rounded, text: transmission),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _actionBar() {
    final prescription = _prescription;
    if (!widget.canWrite) return const SizedBox.shrink();

    if (prescription?.isActive == true && !prescription!.isTransmitted) {
      return _BottomActions(
        children: [
          Expanded(
            child: AppButton(
              label: _l10n.prescriptionTransmit,
              icon: Icons.send_outlined,
              loading: _busy,
              onPressed: _busy ? null : _transmit,
              expand: true,
            ),
          ),
        ],
      );
    }
    if (!_editable) return const SizedBox.shrink();

    return _BottomActions(
      children: [
        Expanded(
          child: AppButton(
            label: _l10n.prescriptionSaveDraft,
            variant: AppButtonVariant.secondary,
            icon: Icons.save_outlined,
            loading: _busy,
            onPressed: _busy ? null : _saveDraft,
            expand: true,
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: AppButton(
            label: _l10n.prescriptionFinalize,
            icon: Icons.verified_outlined,
            loading: _busy,
            onPressed: _busy ? null : _finalize,
            expand: true,
          ),
        ),
      ],
    );
  }

  String _statusLabel(String status) {
    return switch (status.toUpperCase()) {
      'DRAFT' => _l10n.prescriptionStatusDraft,
      'ACTIVE' => _l10n.prescriptionStatusActive,
      'CANCELLED' => _l10n.prescriptionStatusCancelled,
      'EXPIRED' => _l10n.prescriptionStatusExpired,
      _ => _l10n.prescriptionStatusUnknown,
    };
  }

  String? _transmissionLabel(String? status) {
    return switch (status?.toUpperCase()) {
      'PENDING' => _l10n.prescriptionTransmissionPending,
      'TRANSMITTED' => _l10n.prescriptionTransmissionSent,
      'FAILED' => _l10n.prescriptionTransmissionFailed,
      _ => null,
    };
  }
}

class _BottomActions extends StatelessWidget {
  const _BottomActions({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
        decoration: BoxDecoration(
          color: colors.surface,
          border: Border(top: BorderSide(color: colors.outlineVariant)),
        ),
        child: Row(children: children),
      ),
    );
  }
}

class _StatusTag extends StatelessWidget {
  const _StatusTag({required this.label, required this.status});

  final String label;
  final String status;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final normalized = status.toUpperCase();
    final foreground = switch (normalized) {
      'ACTIVE' => AppDesignTokens.success,
      'DRAFT' => AppDesignTokens.warning,
      'CANCELLED' => colors.error,
      _ => colors.onSurfaceVariant,
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: foreground.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(color: foreground.withValues(alpha: 0.35)),
      ),
      child: Text(
        label,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: foreground,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _MetaText extends StatelessWidget {
  const _MetaText({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 15, color: colors.onSurfaceVariant),
        const SizedBox(width: 5),
        Text(
          text,
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
            color: colors.onSurfaceVariant,
          ),
        ),
      ],
    );
  }
}

class _InlineError extends StatelessWidget {
  const _InlineError({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: colors.errorContainer,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
      ),
      child: Text(
        message,
        style: TextStyle(color: colors.onErrorContainer),
      ),
    );
  }
}

class _ReadOnlyNotice extends StatelessWidget {
  const _ReadOnlyNotice({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(Icons.lock_outline_rounded, size: 17, color: colors.onSurfaceVariant),
        const SizedBox(width: 7),
        Expanded(
          child: Text(
            text,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: colors.onSurfaceVariant,
            ),
          ),
        ),
      ],
    );
  }
}

class _EmptyPrescription extends StatelessWidget {
  const _EmptyPrescription({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 24),
      child: Column(
        children: [
          Icon(Icons.medication_outlined, color: colors.onSurfaceVariant),
          const SizedBox(height: 8),
          Text(
            message,
            textAlign: TextAlign.center,
            style: TextStyle(color: colors.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

class _RetryState extends StatelessWidget {
  const _RetryState({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh_rounded),
              label: Text(AppLocalizations.of(context).dashboardQueueRetry),
            ),
          ],
        ),
      ),
    );
  }
}
