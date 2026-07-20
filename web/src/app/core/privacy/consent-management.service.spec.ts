import { TestBed } from '@angular/core/testing';
import {
  CONSENT_POLICY_VERSION,
  CONSENT_STORAGE_KEY,
  ConsentManagementService,
} from './consent-management.service';

describe('ConsentManagementService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should deny all optional categories by default', () => {
    const service = TestBed.inject(ConsentManagementService);

    expect(service.isAllowed('necessary')).toBe(true);
    expect(service.isAllowed('preferences')).toBe(false);
    expect(service.isAllowed('analytics')).toBe(false);
    expect(service.isAllowed('marketing')).toBe(false);
    expect(service.hasStoredDecision()).toBe(false);
  });

  it('should persist a versioned preference decision', () => {
    const service = TestBed.inject(ConsentManagementService);

    const state = service.acceptAvailableOptions();
    const stored = JSON.parse(localStorage.getItem(CONSENT_STORAGE_KEY) ?? '{}');

    expect(state.version).toBe(CONSENT_POLICY_VERSION);
    expect(state.preferences).toBe(true);
    expect(state.analytics).toBe(false);
    expect(state.marketing).toBe(false);
    expect(service.preferencesAllowed()).toBe(true);
    expect(service.hasStoredDecision()).toBe(true);
    expect(stored.version).toBe(CONSENT_POLICY_VERSION);
    expect(stored.preferences).toBe(true);
  });

  it('should clear known preference storage when optional preferences are revoked', () => {
    localStorage.setItem('joprelys.theme', 'dark');
    localStorage.setItem('joprelys.locale', 'en');
    localStorage.setItem('joprelys.sidebar.collapsed', 'true');

    const service = TestBed.inject(ConsentManagementService);
    service.rejectOptionalOptions();

    expect(localStorage.getItem('joprelys.theme')).toBeNull();
    expect(localStorage.getItem('joprelys.locale')).toBeNull();
    expect(localStorage.getItem('joprelys.sidebar.collapsed')).toBeNull();
    expect(localStorage.getItem(CONSENT_STORAGE_KEY)).not.toBeNull();
  });

  it('should ignore a stale policy version and force unavailable categories off', () => {
    localStorage.setItem(CONSENT_STORAGE_KEY, JSON.stringify({
      version: '0.9',
      necessary: false,
      preferences: true,
      analytics: true,
      marketing: true,
      updatedAt: new Date().toISOString(),
    }));

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    const service = TestBed.inject(ConsentManagementService);

    expect(service.hasStoredDecision()).toBe(false);
    expect(service.preferencesAllowed()).toBe(false);
    expect(service.analyticsAllowed()).toBe(false);
    expect(service.marketingAllowed()).toBe(false);
  });
});
