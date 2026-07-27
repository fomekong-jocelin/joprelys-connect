import { AiConsultationDraft } from './ai-consultation-api.service';

export interface AiDraftApplyRequest {
  baseDraft: AiConsultationDraft;
  draft: AiConsultationDraft;
}
