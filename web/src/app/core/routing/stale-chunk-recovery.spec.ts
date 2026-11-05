import { NavigationError } from '@angular/router';
import { isStaleChunkError, reloadOnStaleChunk } from './stale-chunk-recovery';

describe('stale chunk recovery', () => {
  function fakeWindow(storage: Pick<Storage, 'getItem' | 'setItem'>) {
    return { sessionStorage: storage, location: { assign: vi.fn() } } as unknown as Window & { location: { assign: ReturnType<typeof vi.fn> } };
  }

  function memoryStorage(): Pick<Storage, 'getItem' | 'setItem'> {
    const values = new Map<string, string>();
    return {
      getItem: (key: string) => values.get(key) ?? null,
      setItem: (key: string, value: string) => { values.set(key, value); },
    };
  }

  const staleError = (url = '/dashboard') =>
    new NavigationError(1, url, new TypeError('Failed to fetch dynamically imported module: https://joprelys.com/chunk-OLD.js'));

  it('recognises missing lazy chunks in the main browsers', () => {
    expect(isStaleChunkError(new TypeError('Failed to fetch dynamically imported module: x.js'))).toBe(true);
    expect(isStaleChunkError(new TypeError('error loading dynamically imported module'))).toBe(true);
    expect(isStaleChunkError(new TypeError('Importing a module script failed.'))).toBe(true);
    expect(isStaleChunkError(new Error("'text/html' is not a valid JavaScript MIME type."))).toBe(true);
    expect(isStaleChunkError(new Error('NG04002: Cannot match any routes'))).toBe(false);
    expect(isStaleChunkError(undefined)).toBe(false);
  });

  it('reloads the target page once, then lets a persistent failure surface', () => {
    const win = fakeWindow(memoryStorage());

    expect(reloadOnStaleChunk(staleError(), win)).toBe(true);
    expect(win.location.assign).toHaveBeenCalledWith('/dashboard');

    expect(reloadOnStaleChunk(staleError(), win)).toBe(false);
    expect(win.location.assign).toHaveBeenCalledTimes(1);
  });

  it('ignores other navigation errors', () => {
    const win = fakeWindow(memoryStorage());

    expect(reloadOnStaleChunk(new NavigationError(1, '/dashboard', new Error('guard failed')), win)).toBe(false);
    expect(win.location.assign).not.toHaveBeenCalled();
  });

  it('never reloads when it cannot remember the previous attempt', () => {
    const win = fakeWindow({
      getItem: () => { throw new Error('storage disabled'); },
      setItem: () => { throw new Error('storage disabled'); },
    });

    expect(reloadOnStaleChunk(staleError(), win)).toBe(false);
    expect(win.location.assign).not.toHaveBeenCalled();
  });
});
