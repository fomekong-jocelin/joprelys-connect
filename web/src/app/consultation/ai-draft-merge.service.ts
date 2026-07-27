import { Injectable } from '@angular/core';
import {
  AiConsultationDraft,
  AiField,
  AiPrescriptionLine,
  AiVitalsDraft,
} from './ai-consultation-api.service';
import { AiDraftApplyRequest } from './ai-draft-apply.model';

export interface AiDraftMergePlan {
  textPatch: Record<string, string>;
  prescriptionAdds: AiPrescriptionLine[];
  labAdds: string[];
  vitalsProposal: AiVitalsDraft | null;
  conflicts: AiField[];
}

const TEXT_FIELDS: Array<keyof AiConsultationDraft> = [
  'symptoms',
  'clinicalExam',
  'suspectedDiagnosis',
  'conclusion',
  'advice',
  'followUp',
];

@Injectable({ providedIn: 'root' })
export class AiDraftMergeService {
  plan(request: AiDraftApplyRequest, current: Record<string, unknown>): AiDraftMergePlan {
    const conflicts = new Set<AiField>();
    const textPatch: Record<string, string> = {};

    for (const field of TEXT_FIELDS) {
      const proposed = this.text(request.draft[field]);
      if (!proposed) continue;
      const currentValue = this.text(current[field]);
      const baseValue = this.text(request.baseDraft[field]);
      if (currentValue === baseValue || currentValue === proposed) {
        textPatch[field] = proposed;
      } else {
        conflicts.add(field as AiField);
      }
    }

    const proposedDiagnosis = this.firstText(request.draft.finalDiagnosis, request.draft.diagnosis);
    if (proposedDiagnosis) {
      const baseDiagnosis = this.firstText(request.baseDraft.finalDiagnosis, request.baseDraft.diagnosis);
      const currentDiagnosis = this.text(current['diagnosis']);
      if (currentDiagnosis === baseDiagnosis || currentDiagnosis === proposedDiagnosis) {
        textPatch['diagnosis'] = proposedDiagnosis;
        textPatch['finalDiagnosis'] = proposedDiagnosis;
      } else {
        conflicts.add('diagnosis');
      }
    }

    const prescriptionAdds = this.prescriptionAdds(
      request.draft.prescription,
      current['prescription'],
      conflicts,
    );
    const labAdds = this.labAdds(request.draft.labOrders, current['exams'], conflicts);
    const vitalsProposal = this.vitalsProposal(request.draft.vitals, conflicts);

    return {
      textPatch,
      prescriptionAdds,
      labAdds,
      vitalsProposal,
      conflicts: [...conflicts],
    };
  }

  private prescriptionAdds(
    rawProposal: string | undefined,
    currentValue: unknown,
    conflicts: Set<AiField>,
  ): AiPrescriptionLine[] {
    if (!rawProposal?.trim()) return [];
    const proposed = this.parseArray<AiPrescriptionLine>(rawProposal);
    if (!proposed) {
      conflicts.add('prescription');
      return [];
    }
    const current = Array.isArray(currentValue)
      ? currentValue.filter((item): item is AiPrescriptionLine => this.isPrescriptionLine(item))
      : [];
    const additions: AiPrescriptionLine[] = [];

    for (const line of proposed) {
      if (!this.isPrescriptionLine(line)) {
        conflicts.add('prescription');
        continue;
      }
      const normalized = this.normalizePrescription(line);
      const sameDrug = current.find(item => this.drugKey(item) === this.drugKey(line))
        ?? additions.find(item => this.drugKey(item) === this.drugKey(line));
      if (!sameDrug) {
        additions.push(normalized);
        continue;
      }
      if (this.prescriptionSignature(sameDrug) !== this.prescriptionSignature(normalized)) {
        conflicts.add('prescription');
      }
    }
    return additions;
  }

  private labAdds(
    rawProposal: string | undefined,
    currentValue: unknown,
    conflicts: Set<AiField>,
  ): string[] {
    if (!rawProposal?.trim()) return [];
    const proposed = this.parseArray<unknown>(rawProposal);
    if (!proposed) {
      conflicts.add('labOrders');
      return [];
    }
    const currentKeys = new Set(
      (Array.isArray(currentValue) ? currentValue : [])
        .filter((value): value is string => typeof value === 'string')
        .map(value => this.key(value)),
    );
    const additions: string[] = [];
    for (const item of proposed) {
      if (typeof item !== 'string' || !item.trim()) {
        conflicts.add('labOrders');
        continue;
      }
      const value = item.trim();
      const key = this.key(value);
      if (!currentKeys.has(key)) {
        currentKeys.add(key);
        additions.push(value);
      }
    }
    return additions;
  }

  private vitalsProposal(rawProposal: string | undefined, conflicts: Set<AiField>): AiVitalsDraft | null {
    if (!rawProposal?.trim()) return null;
    try {
      const parsed = JSON.parse(rawProposal) as unknown;
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) throw new Error('invalid');
      const allowed = new Set([
        'temperature', 'weight', 'height', 'pulse', 'systolic', 'diastolic',
        'spo2', 'glycemia', 'respiratoryRate', 'painScale',
      ]);
      const result: AiVitalsDraft = {};
      for (const [key, value] of Object.entries(parsed as Record<string, unknown>)) {
        if (!allowed.has(key) || typeof value !== 'number' || !Number.isFinite(value)) {
          throw new Error('invalid');
        }
        (result as Record<string, number>)[key] = value;
      }
      return Object.keys(result).length > 0 ? result : null;
    } catch {
      conflicts.add('vitals');
      return null;
    }
  }

  private parseArray<T>(raw: string): T[] | null {
    try {
      const parsed = JSON.parse(raw) as unknown;
      return Array.isArray(parsed) ? parsed as T[] : null;
    } catch {
      return null;
    }
  }

  private isPrescriptionLine(value: unknown): value is AiPrescriptionLine {
    if (!value || typeof value !== 'object') return false;
    const line = value as Record<string, unknown>;
    return typeof line['drugName'] === 'string' && !!line['drugName'].trim();
  }

  private normalizePrescription(line: AiPrescriptionLine): AiPrescriptionLine {
    return {
      drugName: line.drugName.trim(),
      dosage: line.dosage?.trim() ?? '',
      posology: line.posology?.trim() ?? '',
      duration: line.duration?.trim() ?? '',
      quantity: line.quantity?.trim() ?? '',
      instructions: line.instructions?.trim() ?? '',
      form: line.form?.trim() ?? '',
      route: line.route?.trim() ?? '',
      frequency: line.frequency?.trim() ?? '',
      substitutionAllowed: line.substitutionAllowed !== false,
    };
  }

  private drugKey(line: AiPrescriptionLine): string {
    return this.key(line.drugName);
  }

  private prescriptionSignature(line: AiPrescriptionLine): string {
    const normalized = this.normalizePrescription(line);
    return JSON.stringify([
      this.key(normalized.drugName),
      this.key(normalized.dosage ?? ''),
      this.key(normalized.posology ?? ''),
      this.key(normalized.duration ?? ''),
      this.key(normalized.quantity ?? ''),
      this.key(normalized.instructions ?? ''),
      this.key(normalized.form ?? ''),
      this.key(normalized.route ?? ''),
      this.key(normalized.frequency ?? ''),
      normalized.substitutionAllowed !== false,
    ]);
  }

  private firstText(...values: unknown[]): string {
    for (const value of values) {
      const text = this.text(value);
      if (text) return text;
    }
    return '';
  }

  private text(value: unknown): string {
    return typeof value === 'string' ? value.trim() : '';
  }

  private key(value: string): string {
    return value.trim().toLocaleLowerCase().replace(/\s+/g, ' ');
  }
}
