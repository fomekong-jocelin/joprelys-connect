import { I18nService } from '../core/i18n/i18n.service';
import { interpolate } from '../core/i18n/interpolate';
import { ConfirmDialogOptions } from '../shared/ui/dialog.service';

export interface ConsultationCloseSummary {
  readonly patientName: string;
  readonly diagnosis: string;
  readonly prescriptionLineCount: number;
  readonly labExamCount: number;
}

/** Récapitulatif affiché avant « Valider & Clôturer » : action qui verrouille la consultation. */
export function consultationCloseConfirmation(i18n: I18nService, summary: ConsultationCloseSummary): ConfirmDialogOptions {
  return {
    title: i18n.t('consultation.closeConfirm.title'),
    message: interpolate(i18n.t('consultation.closeConfirm.message'), {
      patient: summary.patientName || '—',
      diagnosis: summary.diagnosis.trim() || '—',
      prescriptionCount: summary.prescriptionLineCount,
      examCount: summary.labExamCount,
    }),
    confirmLabel: i18n.t('consultation.closeConfirm.action'),
    cancelLabel: i18n.t('common.cancel'),
    tone: 'warning',
    icon: 'shield-check',
  };
}
