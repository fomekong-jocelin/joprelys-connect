import { I18nService } from '../core/i18n/i18n.service';
import { interpolate } from '../core/i18n/interpolate';
import { ConfirmDialogOptions } from '../shared/ui/dialog.service';
import { PharmacyVerifyResponse } from './pharmacy.models';

export interface DispensationConfirmationLine {
  readonly drugName: string;
  readonly quantity: number;
  readonly substitutedWith?: string;
}

/** Récapitulatif (ordonnance, patient, lignes délivrées, substitutions) avant une délivrance définitive. */
export function dispensationConfirmation(
  i18n: I18nService,
  prescription: Pick<PharmacyVerifyResponse, 'prescriptionNumber' | 'patientName'>,
  pharmacyName: string,
  lines: readonly DispensationConfirmationLine[],
): ConfirmDialogOptions {
  const lineSummary = lines.map((line) => {
    const base = interpolate(i18n.t('pharmacy.dispenseConfirm.line'), { drug: line.drugName, quantity: line.quantity });
    return line.substitutedWith
      ? `${base} ${interpolate(i18n.t('pharmacy.dispenseConfirm.substitution'), { substitute: line.substitutedWith })}`
      : base;
  }).join('\n');

  return {
    title: i18n.t('pharmacy.dispenseConfirm.title'),
    message: interpolate(i18n.t('pharmacy.dispenseConfirm.message'), {
      number: prescription.prescriptionNumber,
      patient: prescription.patientName,
      pharmacy: pharmacyName,
      lines: lineSummary,
    }),
    confirmLabel: i18n.t('pharmacy.dispenseConfirm.action'),
    cancelLabel: i18n.t('common.cancel'),
    tone: 'warning',
    icon: 'clipboard-document-list',
  };
}
