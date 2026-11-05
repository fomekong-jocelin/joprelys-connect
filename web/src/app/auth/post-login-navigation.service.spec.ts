import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { PostLoginNavigationService } from './post-login-navigation.service';

describe('PostLoginNavigationService', () => {
  let router: { url: string; navigateByUrl: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    router = { url: '/login', navigateByUrl: vi.fn().mockResolvedValue(true) };
    TestBed.configureTestingModule({ providers: [{ provide: Router, useValue: router }] });
  });

  it('shares one navigation between simultaneous callers', async () => {
    const service = TestBed.inject(PostLoginNavigationService);

    const results = await Promise.all([service.open('/dashboard'), service.open('/dashboard')]);

    expect(results).toEqual([true, true]);
    expect(router.navigateByUrl).toHaveBeenCalledTimes(1);
  });

  it('fails when the navigation is cancelled on the login page', async () => {
    router.navigateByUrl.mockResolvedValue(false);

    await expect(TestBed.inject(PostLoginNavigationService).open('/dashboard')).resolves.toBe(false);
  });

  it('succeeds when a guard redirected the user away from the login page', async () => {
    router.navigateByUrl.mockImplementation(async () => {
      router.url = '/unauthorized';
      return false;
    });

    await expect(TestBed.inject(PostLoginNavigationService).open('/dashboard')).resolves.toBe(true);
  });

  it('fails on a navigation error and allows a new attempt', async () => {
    router.navigateByUrl.mockRejectedValueOnce(new TypeError('Failed to fetch dynamically imported module'));
    const service = TestBed.inject(PostLoginNavigationService);

    await expect(service.open('/dashboard')).resolves.toBe(false);
    await expect(service.open('/dashboard')).resolves.toBe(true);
    expect(router.navigateByUrl).toHaveBeenCalledTimes(2);
  });
});
