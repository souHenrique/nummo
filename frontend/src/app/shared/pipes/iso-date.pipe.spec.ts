import { IsoDatePipe } from './iso-date.pipe';

describe('IsoDatePipe', () => {
  const pipe = new IsoDatePipe();

  it('formats an ISO date as day/month/year', () => {
    expect(pipe.transform('2026-09-22')).toBe('22/09/2026');
  });

  it('formats the date portion of an ISO timestamp', () => {
    expect(pipe.transform('2026-09-22T14:30:00Z')).toBe('22/09/2026');
  });

  it('uses a fallback for an unavailable date', () => {
    expect(pipe.transform(null)).toBe('—');
  });
});
