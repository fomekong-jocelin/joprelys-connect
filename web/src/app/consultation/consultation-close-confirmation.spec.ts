import { I18nService } from '../core/i18n/i18n.service';
import { consultationCloseConfirmation } from './consultation-close-confirmation';

describe('consultationCloseConfirmation', () => {
  const i18n = {
    t: (key: string) => ({
      'consultation.closeConfirm.message': 'Patient : {patient}\nDiagnostic : {diagnosis}\nOrdonnance : {prescriptionCount}\nExamens : {examCount}',
    } as Record<string, string>)[key] ?? key,
  } as unknown as I18nService;

  it('récapitule patient, diagnostic, ordonnance et examens avant verrouillage', () => {
    const options = consultationCloseConfirmation(i18n, {
      patientName: 'Awa Ngono',
      diagnosis: '  Paludisme simple ',
      prescriptionLineCount: 2,
      labExamCount: 1,
    });

    expect(options.message).toBe('Patient : Awa Ngono\nDiagnostic : Paludisme simple\nOrdonnance : 2\nExamens : 1');
    expect(options.tone).toBe('warning');
  });

  it('affiche un tiret pour un champ vide', () => {
    const options = consultationCloseConfirmation(i18n, {
      patientName: '',
      diagnosis: ' ',
      prescriptionLineCount: 0,
      labExamCount: 0,
    });

    expect(options.message).toContain('Patient : —');
    expect(options.message).toContain('Diagnostic : —');
  });
});
