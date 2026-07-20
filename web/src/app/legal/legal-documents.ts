export type LegalDocumentId =
  | 'privacy'
  | 'terms'
  | 'legal-notice'
  | 'cookies'
  | 'health-data'
  | 'retention'
  | 'rights';

export type LegalBlock =
  | { type: 'paragraph'; key: string }
  | { type: 'list'; keys: string[] }
  | { type: 'callout'; key: string; tone?: 'info' | 'warning' }
  | { type: 'contacts'; contactIds: LegalContactId[] };

export interface LegalSection {
  id: string;
  titleKey: string;
  blocks: LegalBlock[];
}

export interface LegalDocumentDefinition {
  id: LegalDocumentId;
  path: string;
  navKey: string;
  titleKey: string;
  summaryKey: string;
  sections: LegalSection[];
}

export type LegalContactId =
  | 'support'
  | 'privacy'
  | 'dpo'
  | 'security'
  | 'legal'
  | 'complaints'
  | 'contact';

export interface LegalContactDefinition {
  id: LegalContactId;
  labelKey: string;
  email: string;
}

export const LEGAL_CONTACTS: Record<LegalContactId, LegalContactDefinition> = {
  support: { id: 'support', labelKey: 'legal.contact.support', email: 'support@joprelys.com' },
  privacy: { id: 'privacy', labelKey: 'legal.contact.privacy', email: 'confidentialite@joprelys.com' },
  dpo: { id: 'dpo', labelKey: 'legal.contact.dpo', email: 'dpo@joprelys.com' },
  security: { id: 'security', labelKey: 'legal.contact.security', email: 'securite@joprelys.com' },
  legal: { id: 'legal', labelKey: 'legal.contact.legal', email: 'juridique@joprelys.com' },
  complaints: { id: 'complaints', labelKey: 'legal.contact.complaints', email: 'reclamations@joprelys.com' },
  contact: { id: 'contact', labelKey: 'legal.contact.general', email: 'contact@joprelys.com' },
};

const privacy: LegalDocumentDefinition = {
  id: 'privacy',
  path: '/legal/privacy',
  navKey: 'legal.nav.privacy',
  titleKey: 'legal.privacy.title',
  summaryKey: 'legal.privacy.summary',
  sections: [
    {
      id: 'scope',
      titleKey: 'legal.privacy.scope.title',
      blocks: [
        { type: 'paragraph', key: 'legal.privacy.scope.p1' },
        { type: 'paragraph', key: 'legal.privacy.scope.p2' },
        { type: 'callout', key: 'legal.common.cameroonOnly', tone: 'info' },
      ],
    },
    {
      id: 'roles',
      titleKey: 'legal.privacy.roles.title',
      blocks: [
        { type: 'paragraph', key: 'legal.privacy.roles.p1' },
        { type: 'list', keys: ['legal.privacy.roles.i1', 'legal.privacy.roles.i2', 'legal.privacy.roles.i3'] },
      ],
    },
    {
      id: 'data',
      titleKey: 'legal.privacy.data.title',
      blocks: [
        { type: 'list', keys: [
          'legal.privacy.data.i1', 'legal.privacy.data.i2', 'legal.privacy.data.i3',
          'legal.privacy.data.i4', 'legal.privacy.data.i5', 'legal.privacy.data.i6',
          'legal.privacy.data.i7', 'legal.privacy.data.i8',
        ] },
      ],
    },
    {
      id: 'purposes',
      titleKey: 'legal.privacy.purposes.title',
      blocks: [
        { type: 'list', keys: [
          'legal.privacy.purposes.i1', 'legal.privacy.purposes.i2', 'legal.privacy.purposes.i3',
          'legal.privacy.purposes.i4', 'legal.privacy.purposes.i5', 'legal.privacy.purposes.i6',
        ] },
        { type: 'paragraph', key: 'legal.privacy.purposes.p1' },
      ],
    },
    {
      id: 'sharing',
      titleKey: 'legal.privacy.sharing.title',
      blocks: [
        { type: 'paragraph', key: 'legal.privacy.sharing.p1' },
        { type: 'list', keys: ['legal.privacy.sharing.i1', 'legal.privacy.sharing.i2', 'legal.privacy.sharing.i3', 'legal.privacy.sharing.i4'] },
      ],
    },
    {
      id: 'hosting',
      titleKey: 'legal.privacy.hosting.title',
      blocks: [
        { type: 'paragraph', key: 'legal.privacy.hosting.p1' },
        { type: 'paragraph', key: 'legal.privacy.hosting.p2' },
      ],
    },
    {
      id: 'security',
      titleKey: 'legal.privacy.security.title',
      blocks: [
        { type: 'paragraph', key: 'legal.privacy.security.p1' },
        { type: 'list', keys: ['legal.privacy.security.i1', 'legal.privacy.security.i2', 'legal.privacy.security.i3', 'legal.privacy.security.i4'] },
      ],
    },
    {
      id: 'rights',
      titleKey: 'legal.privacy.rights.title',
      blocks: [
        { type: 'paragraph', key: 'legal.privacy.rights.p1' },
        { type: 'list', keys: ['legal.privacy.rights.i1', 'legal.privacy.rights.i2', 'legal.privacy.rights.i3', 'legal.privacy.rights.i4', 'legal.privacy.rights.i5'] },
        { type: 'contacts', contactIds: ['privacy', 'dpo', 'complaints'] },
      ],
    },
  ],
};

const terms: LegalDocumentDefinition = {
  id: 'terms',
  path: '/legal/terms',
  navKey: 'legal.nav.terms',
  titleKey: 'legal.terms.title',
  summaryKey: 'legal.terms.summary',
  sections: [
    {
      id: 'purpose',
      titleKey: 'legal.terms.purpose.title',
      blocks: [
        { type: 'paragraph', key: 'legal.terms.purpose.p1' },
        { type: 'paragraph', key: 'legal.terms.purpose.p2' },
      ],
    },
    {
      id: 'access',
      titleKey: 'legal.terms.access.title',
      blocks: [
        { type: 'list', keys: ['legal.terms.access.i1', 'legal.terms.access.i2', 'legal.terms.access.i3', 'legal.terms.access.i4'] },
      ],
    },
    {
      id: 'professional',
      titleKey: 'legal.terms.professional.title',
      blocks: [
        { type: 'list', keys: ['legal.terms.professional.i1', 'legal.terms.professional.i2', 'legal.terms.professional.i3', 'legal.terms.professional.i4', 'legal.terms.professional.i5'] },
      ],
    },
    {
      id: 'patient',
      titleKey: 'legal.terms.patient.title',
      blocks: [
        { type: 'list', keys: ['legal.terms.patient.i1', 'legal.terms.patient.i2', 'legal.terms.patient.i3', 'legal.terms.patient.i4'] },
      ],
    },
    {
      id: 'medical',
      titleKey: 'legal.terms.medical.title',
      blocks: [
        { type: 'callout', key: 'legal.terms.medical.p1', tone: 'warning' },
        { type: 'paragraph', key: 'legal.terms.medical.p2' },
      ],
    },
    {
      id: 'prohibited',
      titleKey: 'legal.terms.prohibited.title',
      blocks: [
        { type: 'list', keys: ['legal.terms.prohibited.i1', 'legal.terms.prohibited.i2', 'legal.terms.prohibited.i3', 'legal.terms.prohibited.i4', 'legal.terms.prohibited.i5'] },
      ],
    },
    {
      id: 'availability',
      titleKey: 'legal.terms.availability.title',
      blocks: [
        { type: 'paragraph', key: 'legal.terms.availability.p1' },
        { type: 'paragraph', key: 'legal.terms.availability.p2' },
      ],
    },
    {
      id: 'responsibility',
      titleKey: 'legal.terms.responsibility.title',
      blocks: [
        { type: 'paragraph', key: 'legal.terms.responsibility.p1' },
        { type: 'paragraph', key: 'legal.terms.responsibility.p2' },
      ],
    },
    {
      id: 'law',
      titleKey: 'legal.terms.law.title',
      blocks: [
        { type: 'paragraph', key: 'legal.terms.law.p1' },
        { type: 'paragraph', key: 'legal.terms.law.p2' },
        { type: 'contacts', contactIds: ['legal', 'complaints'] },
      ],
    },
  ],
};

const legalNotice: LegalDocumentDefinition = {
  id: 'legal-notice',
  path: '/legal/legal-notice',
  navKey: 'legal.nav.legalNotice',
  titleKey: 'legal.notice.title',
  summaryKey: 'legal.notice.summary',
  sections: [
    {
      id: 'publisher',
      titleKey: 'legal.notice.publisher.title',
      blocks: [
        { type: 'list', keys: ['legal.notice.publisher.i1', 'legal.notice.publisher.i2', 'legal.notice.publisher.i3', 'legal.notice.publisher.i4', 'legal.notice.publisher.i5', 'legal.notice.publisher.i6'] },
        { type: 'callout', key: 'legal.common.missingCompanyDetails', tone: 'warning' },
      ],
    },
    {
      id: 'hosting',
      titleKey: 'legal.notice.hosting.title',
      blocks: [
        { type: 'paragraph', key: 'legal.notice.hosting.p1' },
        { type: 'paragraph', key: 'legal.notice.hosting.p2' },
      ],
    },
    {
      id: 'contacts',
      titleKey: 'legal.notice.contacts.title',
      blocks: [
        { type: 'contacts', contactIds: ['contact', 'support', 'privacy', 'security', 'legal'] },
      ],
    },
    {
      id: 'ip',
      titleKey: 'legal.notice.ip.title',
      blocks: [
        { type: 'paragraph', key: 'legal.notice.ip.p1' },
        { type: 'paragraph', key: 'legal.notice.ip.p2' },
      ],
    },
    {
      id: 'liability',
      titleKey: 'legal.notice.liability.title',
      blocks: [
        { type: 'paragraph', key: 'legal.notice.liability.p1' },
      ],
    },
  ],
};

const cookies: LegalDocumentDefinition = {
  id: 'cookies',
  path: '/legal/cookies',
  navKey: 'legal.nav.cookies',
  titleKey: 'legal.cookies.title',
  summaryKey: 'legal.cookies.summary',
  sections: [
    {
      id: 'principle',
      titleKey: 'legal.cookies.principle.title',
      blocks: [
        { type: 'paragraph', key: 'legal.cookies.principle.p1' },
        { type: 'callout', key: 'legal.cookies.principle.p2', tone: 'info' },
      ],
    },
    {
      id: 'necessary',
      titleKey: 'legal.cookies.necessary.title',
      blocks: [
        { type: 'list', keys: ['legal.cookies.necessary.i1', 'legal.cookies.necessary.i2', 'legal.cookies.necessary.i3', 'legal.cookies.necessary.i4'] },
      ],
    },
    {
      id: 'storage',
      titleKey: 'legal.cookies.storage.title',
      blocks: [
        { type: 'paragraph', key: 'legal.cookies.storage.p1' },
        { type: 'list', keys: ['legal.cookies.storage.i1', 'legal.cookies.storage.i2', 'legal.cookies.storage.i3'] },
      ],
    },
    {
      id: 'choices',
      titleKey: 'legal.cookies.choices.title',
      blocks: [
        { type: 'paragraph', key: 'legal.cookies.choices.p1' },
        { type: 'paragraph', key: 'legal.cookies.choices.p2' },
        { type: 'contacts', contactIds: ['privacy', 'support'] },
      ],
    },
  ],
};

const healthData: LegalDocumentDefinition = {
  id: 'health-data',
  path: '/legal/health-data',
  navKey: 'legal.nav.healthData',
  titleKey: 'legal.health.title',
  summaryKey: 'legal.health.summary',
  sections: [
    {
      id: 'sensitive',
      titleKey: 'legal.health.sensitive.title',
      blocks: [
        { type: 'paragraph', key: 'legal.health.sensitive.p1' },
        { type: 'paragraph', key: 'legal.health.sensitive.p2' },
      ],
    },
    {
      id: 'responsibilities',
      titleKey: 'legal.health.responsibilities.title',
      blocks: [
        { type: 'list', keys: ['legal.health.responsibilities.i1', 'legal.health.responsibilities.i2', 'legal.health.responsibilities.i3'] },
      ],
    },
    {
      id: 'access',
      titleKey: 'legal.health.access.title',
      blocks: [
        { type: 'list', keys: ['legal.health.access.i1', 'legal.health.access.i2', 'legal.health.access.i3', 'legal.health.access.i4'] },
      ],
    },
    {
      id: 'sharing',
      titleKey: 'legal.health.sharing.title',
      blocks: [
        { type: 'paragraph', key: 'legal.health.sharing.p1' },
        { type: 'paragraph', key: 'legal.health.sharing.p2' },
      ],
    },
    {
      id: 'emergency',
      titleKey: 'legal.health.emergency.title',
      blocks: [
        { type: 'paragraph', key: 'legal.health.emergency.p1' },
      ],
    },
    {
      id: 'ai',
      titleKey: 'legal.health.ai.title',
      blocks: [
        { type: 'callout', key: 'legal.health.ai.p1', tone: 'warning' },
        { type: 'paragraph', key: 'legal.health.ai.p2' },
        { type: 'paragraph', key: 'legal.health.ai.p3' },
      ],
    },
    {
      id: 'incidents',
      titleKey: 'legal.health.incidents.title',
      blocks: [
        { type: 'paragraph', key: 'legal.health.incidents.p1' },
        { type: 'contacts', contactIds: ['security', 'privacy'] },
      ],
    },
  ],
};

const retention: LegalDocumentDefinition = {
  id: 'retention',
  path: '/legal/retention',
  navKey: 'legal.nav.retention',
  titleKey: 'legal.retention.title',
  summaryKey: 'legal.retention.summary',
  sections: [
    {
      id: 'principle',
      titleKey: 'legal.retention.principle.title',
      blocks: [
        { type: 'paragraph', key: 'legal.retention.principle.p1' },
        { type: 'callout', key: 'legal.retention.principle.p2', tone: 'warning' },
      ],
    },
    {
      id: 'schedule',
      titleKey: 'legal.retention.schedule.title',
      blocks: [
        { type: 'list', keys: [
          'legal.retention.schedule.i1', 'legal.retention.schedule.i2', 'legal.retention.schedule.i3',
          'legal.retention.schedule.i4', 'legal.retention.schedule.i5', 'legal.retention.schedule.i6',
          'legal.retention.schedule.i7',
        ] },
      ],
    },
    {
      id: 'deletion',
      titleKey: 'legal.retention.deletion.title',
      blocks: [
        { type: 'paragraph', key: 'legal.retention.deletion.p1' },
        { type: 'paragraph', key: 'legal.retention.deletion.p2' },
      ],
    },
    {
      id: 'backup',
      titleKey: 'legal.retention.backup.title',
      blocks: [
        { type: 'paragraph', key: 'legal.retention.backup.p1' },
        { type: 'contacts', contactIds: ['privacy', 'dpo'] },
      ],
    },
  ],
};

const rights: LegalDocumentDefinition = {
  id: 'rights',
  path: '/legal/rights',
  navKey: 'legal.nav.rights',
  titleKey: 'legal.rights.title',
  summaryKey: 'legal.rights.summary',
  sections: [
    {
      id: 'rights-list',
      titleKey: 'legal.rights.list.title',
      blocks: [
        { type: 'list', keys: ['legal.rights.list.i1', 'legal.rights.list.i2', 'legal.rights.list.i3', 'legal.rights.list.i4', 'legal.rights.list.i5', 'legal.rights.list.i6'] },
      ],
    },
    {
      id: 'who',
      titleKey: 'legal.rights.who.title',
      blocks: [
        { type: 'paragraph', key: 'legal.rights.who.p1' },
        { type: 'paragraph', key: 'legal.rights.who.p2' },
      ],
    },
    {
      id: 'request',
      titleKey: 'legal.rights.request.title',
      blocks: [
        { type: 'paragraph', key: 'legal.rights.request.p1' },
        { type: 'list', keys: ['legal.rights.request.i1', 'legal.rights.request.i2', 'legal.rights.request.i3', 'legal.rights.request.i4'] },
        { type: 'contacts', contactIds: ['privacy', 'dpo', 'complaints'] },
      ],
    },
    {
      id: 'verification',
      titleKey: 'legal.rights.verification.title',
      blocks: [
        { type: 'paragraph', key: 'legal.rights.verification.p1' },
        { type: 'paragraph', key: 'legal.rights.verification.p2' },
      ],
    },
    {
      id: 'complaint',
      titleKey: 'legal.rights.complaint.title',
      blocks: [
        { type: 'paragraph', key: 'legal.rights.complaint.p1' },
      ],
    },
  ],
};

export const LEGAL_DOCUMENTS: Record<LegalDocumentId, LegalDocumentDefinition> = {
  privacy,
  terms,
  'legal-notice': legalNotice,
  cookies,
  'health-data': healthData,
  retention,
  rights,
};

export const LEGAL_DOCUMENT_NAVIGATION = Object.values(LEGAL_DOCUMENTS);
