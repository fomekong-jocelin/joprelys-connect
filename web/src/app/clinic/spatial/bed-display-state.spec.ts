import { bedActions, bedDisplayState } from './bed-display-state';
import { bedFixture } from '../../../testing/bed-fixtures';

const ALL = { capacity: true, cleaning: true, maintenance: true };
const NONE = { capacity: false, cleaning: false, maintenance: false };

describe('bedDisplayState', () => {
  it('gives priority to the patient over every other state', () => {
    expect(bedDisplayState(bedFixture({ usageStatus: 'OCCUPIED', capacityStatus: 'CLOSED', readinessStatus: 'CLEANING' })))
      .toBe('OCCUPIED');
  });

  it('reports a closed bed before its readiness', () => {
    expect(bedDisplayState(bedFixture({ capacityStatus: 'CLOSED', readinessStatus: 'MAINTENANCE' }))).toBe('CLOSED');
  });

  it('maps cleaning and maintenance', () => {
    expect(bedDisplayState(bedFixture({ readinessStatus: 'CLEANING', available: false }))).toBe('CLEANING');
    expect(bedDisplayState(bedFixture({ readinessStatus: 'MAINTENANCE', available: false }))).toBe('MAINTENANCE');
  });

  it('trusts the server availability for an open and ready bed', () => {
    expect(bedDisplayState(bedFixture())).toBe('FREE');
    expect(bedDisplayState(bedFixture({ available: false }))).toBe('UNAVAILABLE');
  });
});

describe('bedActions', () => {
  it('offers cleaning, maintenance and closing on a free bed', () => {
    expect(bedActions(bedFixture(), ALL)).toEqual(['START_CLEANING', 'START_MAINTENANCE', 'CLOSE']);
  });

  it('offers to finish the current task first', () => {
    expect(bedActions(bedFixture({ readinessStatus: 'CLEANING', available: false }), ALL)).toEqual(['END_CLEANING', 'CLOSE']);
    expect(bedActions(bedFixture({ readinessStatus: 'MAINTENANCE', available: false }), ALL)).toEqual(['END_MAINTENANCE', 'CLOSE']);
  });

  it('only offers to reopen a closed bed', () => {
    expect(bedActions(bedFixture({ capacityStatus: 'CLOSED', available: false }), ALL)).toEqual(['REOPEN']);
  });

  it('never acts on an occupied bed', () => {
    expect(bedActions(bedFixture({ usageStatus: 'OCCUPIED', available: false }), ALL)).toEqual([]);
  });

  it('keeps only the actions the user is allowed to do', () => {
    expect(bedActions(bedFixture(), NONE)).toEqual([]);
    expect(bedActions(bedFixture(), { ...NONE, cleaning: true })).toEqual(['START_CLEANING']);
    expect(bedActions(bedFixture({ capacityStatus: 'CLOSED', available: false }), { ...ALL, capacity: false })).toEqual([]);
  });
});
