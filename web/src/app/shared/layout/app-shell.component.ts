import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthApiService } from '../../auth/auth-api.service';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';
import { APP_BRAND_CONFIG, AppLocale } from '../../core/config/app-brand.config';
import { I18nService } from '../../core/i18n/i18n.service';
import { ThemeService } from '../../core/theme/theme.service';
import { AppLogoComponent } from '../ui/app-logo.component';
import { BreadcrumbComponent } from '../ui/breadcrumb.component';
import { PatientPortalService } from '../../patient/portal/services/patient-portal.service';
import { AppShellNavComponent } from './app-shell-nav.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [AppLogoComponent, BreadcrumbComponent, AppShellNavComponent, RouterLink],
  template: `
    <main class="app-page flex min-h-screen flex-col">
      <header class="app-topbar">
        <div class="app-container flex items-center justify-between py-4">
          <div class="flex items-center gap-2">
            <button
              type="button"
              (click)="toggleMobileMenu()"
              class="inline-flex md:hidden items-center justify-center w-9 h-9 rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] text-slate-500 hover:text-brand-cyan hover:bg-[var(--app-surface-muted)] transition-all duration-150 cursor-pointer"
              [attr.aria-label]="mobileMenuOpen() ? 'Close menu' : 'Open menu'"
            >
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
              </svg>
            </button>

            <a routerLink="/dashboard" class="inline-flex w-fit">
              <app-logo />
            </a>
          </div>

          <div class="flex items-center gap-4">
            <div class="hidden md:flex items-center gap-1.5 text-xs font-bold border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-2.5 py-1.5 rounded-sm select-none">
              <button
                type="button"
                (click)="setLang('fr')"
                [class]="locale() === 'fr' ? 'text-brand-cyan font-extrabold pointer-events-none' : 'text-slate-400 dark:text-slate-500 hover:text-slate-600 dark:hover:text-slate-300 cursor-pointer'"
              >
                FR
              </button>
              <span class="text-slate-300 dark:text-slate-700">|</span>
              <button
                type="button"
                (click)="setLang('en')"
                [class]="locale() === 'en' ? 'text-brand-cyan font-extrabold pointer-events-none' : 'text-slate-400 dark:text-slate-500 hover:text-slate-600 dark:hover:text-slate-300 cursor-pointer'"
              >
                EN
              </button>
            </div>

            <button
              type="button"
              (click)="toggleTheme()"
              [title]="themeTooltip()"
              [attr.aria-label]="themeTooltip()"
              class="hidden md:inline-flex items-center justify-center w-9 h-9 rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] text-slate-500 dark:text-slate-400 hover:text-brand-cyan hover:bg-[var(--app-surface-muted)] transition-all duration-150 cursor-pointer"
            >
              @if (theme() === 'dark') {
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4.5 h-4.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 3v2.25m0 13.5V21M4.978 4.978l1.59 1.59m10.862 10.862l1.59 1.59M3 12h2.25m13.5 0H21M4.978 19.022l1.59-1.59m10.862-10.862l1.59-1.59M12 7.5a4.5 4.5 0 100 9 4.5 4.5 0 000-9z" />
                </svg>
              } @else {
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4.5 h-4.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M21.752 15.002A9.718 9.718 0 0118 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 003 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 009.002-5.998z" />
                </svg>
              }
            </button>

            <div class="hidden md:block h-6 w-px bg-slate-200 dark:bg-slate-800"></div>

            @if (session(); as currentSession) {
              <div class="flex items-center gap-3">
                <div class="hidden text-right sm:flex sm:flex-col justify-center">
                  <span class="font-display text-sm font-extrabold leading-none text-slate-800 dark:text-slate-200">
                    {{ currentSession.name }}
                  </span>
                  <span class="text-[0.68rem] font-extrabold uppercase tracking-wider mt-1 text-slate-400 dark:text-slate-500">
                    {{ currentSession.role }}
                  </span>
                </div>
                <div class="ui-avatar shadow-xs select-none">{{ currentSession.name.charAt(0) }}</div>
              </div>

              @if (currentSession.role !== 'PATIENT') {
                <a
                  routerLink="/profile"
                  [title]="profileLabel()"
                  class="hidden md:inline-flex items-center justify-center w-9 h-9 rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] text-slate-500 hover:text-brand-cyan hover:bg-[var(--app-surface-muted)] transition-all duration-150 cursor-pointer ml-1"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4.5 h-4.5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
                  </svg>
                </a>
              }

              <button
                type="button"
                (click)="logout()"
                [title]="logoutLabel()"
                class="hidden md:inline-flex items-center justify-center w-9 h-9 rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] text-slate-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/20 hover:border-red-200 dark:hover:border-red-900 transition-all duration-150 cursor-pointer ml-1"
              >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" class="w-4.5 h-4.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75" />
                </svg>
              </button>
            }
          </div>
        </div>
      </header>

      <div class="flex flex-1">
        @if (session(); as currentSession) {
          <aside
            [class.w-64]="!sidebarCollapsed()"
            [class.w-16]="sidebarCollapsed()"
            class="hidden md:flex flex-col bg-[var(--app-surface)] border-r border-[var(--app-border)] transition-all duration-200"
          >
            <div class="flex justify-end p-2 border-b border-[var(--app-border)]">
              <button
                type="button"
                (click)="toggleSidebar()"
                [title]="sidebarCollapsed() ? i18n.t('shell.sidebar.expand') : i18n.t('shell.sidebar.collapse')"
                [attr.aria-label]="sidebarCollapsed() ? i18n.t('shell.sidebar.expand') : i18n.t('shell.sidebar.collapse')"
                class="inline-flex items-center justify-center w-8 h-8 rounded-sm hover:bg-[var(--app-surface-muted)] text-slate-500 hover:text-slate-900 dark:hover:text-slate-100 transition-colors cursor-pointer"
              >
                @if (sidebarCollapsed()) {
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4.5 h-4.5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
                  </svg>
                } @else {
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4.5 h-4.5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5 8.25 12l7.5-7.5" />
                  </svg>
                }
              </button>
            </div>

            <nav class="flex-1 p-2 space-y-1 overflow-y-auto">
              <app-shell-nav
                [session]="currentSession"
                [sidebarCollapsed]="sidebarCollapsed()"
                [isMobile]="false"
                [unreadCount]="unreadNotificationCount()"
              />
            </nav>
          </aside>
        }

        @if (session() && mobileMenuOpen()) {
          <div class="fixed inset-0 z-[100] flex md:hidden">
            <div
              class="fixed inset-0 bg-slate-900/60 backdrop-blur-xs transition-opacity"
              (click)="closeMobileMenu()"
            ></div>

            <div class="relative flex w-full max-w-xs flex-1 flex-col bg-[var(--app-surface)] border-r border-[var(--app-border)] pt-5 pb-4 transition-transform duration-300 ease-in-out">
              <div class="absolute top-4 right-4">
                <button
                  type="button"
                  (click)="closeMobileMenu()"
                  class="inline-flex items-center justify-center w-8 h-8 rounded-sm hover:bg-[var(--app-surface-muted)] text-slate-500 hover:text-slate-900 dark:hover:text-slate-100 transition-colors cursor-pointer"
                  aria-label="Close menu"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>

              <div class="flex items-center px-4 mb-5">
                <app-logo />
              </div>

              @if (session(); as currentSession) {
                <div class="px-4 py-3 mb-4 mx-3 bg-[var(--app-surface-muted)] border border-[var(--app-border)] rounded-sm">
                  <div class="flex items-center gap-3">
                    <div class="ui-avatar shadow-xs select-none">{{ currentSession.name.charAt(0) }}</div>
                    <div class="flex flex-col">
                      <span class="font-display text-sm font-extrabold leading-none text-slate-800 dark:text-slate-200">
                        {{ currentSession.name }}
                      </span>
                      <span class="text-[0.65rem] font-extrabold uppercase tracking-wider mt-1 text-slate-400 dark:text-slate-500">
                        {{ currentSession.role }}
                      </span>
                    </div>
                  </div>
                </div>
              }

              <nav class="flex-1 px-3 space-y-1 overflow-y-auto">
                @if (session(); as currentSession) {
                  <app-shell-nav
                    [session]="currentSession"
                    [sidebarCollapsed]="false"
                    [isMobile]="true"
                    [unreadCount]="unreadNotificationCount()"
                    (linkClicked)="closeMobileMenu()"
                  />
                }
              </nav>

              <div class="border-t border-[var(--app-border)] p-4 space-y-4">
                <div class="flex items-center justify-between">
                  <span class="text-xs font-bold text-slate-500 dark:text-slate-400">Thème</span>
                  <button
                    type="button"
                    (click)="toggleTheme()"
                    [title]="themeTooltip()"
                    class="inline-flex items-center justify-center w-9 h-9 rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] text-slate-500 dark:text-slate-400 hover:text-brand-cyan transition-all duration-150 cursor-pointer"
                  >
                    @if (theme() === 'dark') {
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4.5 h-4.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 3v2.25m0 13.5V21M4.978 4.978l1.59 1.59m10.862 10.862l1.59 1.59M3 12h2.25m13.5 0H21M4.978 19.022l1.59-1.59m10.862-10.862l1.59-1.59M12 7.5a4.5 4.5 0 100 9 4.5 4.5 0 000-9z" />
                      </svg>
                    } @else {
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4.5 h-4.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M21.752 15.002A9.718 9.718 0 0118 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 003 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 009.002-5.998z" />
                      </svg>
                    }
                  </button>
                </div>

                <div class="flex items-center justify-between">
                  <span class="text-xs font-bold text-slate-500 dark:text-slate-400">Langue</span>
                  <div class="flex items-center gap-1.5 text-xs font-bold border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-2.5 py-1.5 rounded-sm select-none">
                    <button
                      type="button"
                      (click)="setLang('fr')"
                      [class]="locale() === 'fr' ? 'text-brand-cyan font-extrabold pointer-events-none' : 'text-slate-400 dark:text-slate-500 hover:text-slate-600 dark:hover:text-slate-300 cursor-pointer'"
                    >
                      FR
                    </button>
                    <span class="text-slate-300 dark:text-slate-700">|</span>
                    <button
                      type="button"
                      (click)="setLang('en')"
                      [class]="locale() === 'en' ? 'text-brand-cyan font-extrabold pointer-events-none' : 'text-slate-400 dark:text-slate-500 hover:text-slate-600 dark:hover:text-slate-300 cursor-pointer'"
                    >
                      EN
                    </button>
                  </div>
                </div>

                @if (session(); as currentSession) {
                  <button
                    type="button"
                    (click)="logout(); closeMobileMenu()"
                    class="flex w-full items-center justify-center gap-2 py-2.5 rounded-sm border border-red-200 dark:border-red-900 bg-red-50 dark:bg-red-950/20 text-red-600 hover:bg-red-100 transition-all duration-150 cursor-pointer text-sm font-bold"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4.5 h-4.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75" />
                    </svg>
                    <span>{{ logoutLabel() }}</span>
                  </button>
                }
              </div>
            </div>
          </div>
        }

        <div class="flex-1 flex flex-col min-w-0">
          <app-breadcrumb />

          <section class="flex-1 p-0 md:p-6">
            <ng-content></ng-content>
          </section>
        </div>
      </div>

      <footer class="app-footer">
        <div class="app-container flex flex-col gap-2 py-5 text-xs font-semibold sm:flex-row sm:items-center sm:justify-between">
          <span>{{ copyrightLabel() }}</span>
          <div class="flex flex-wrap gap-3">
            <a class="ui-link" [href]="links.terms">{{ termsLabel() }}</a>
            <a class="ui-link" [href]="links.privacy">{{ privacyLabel() }}</a>
            <a class="ui-link" [href]="links.help">{{ helpLabel() }}</a>
          </div>
        </div>
      </footer>
    </main>
  `,
})
export class AppShellComponent {
  private readonly authApi = inject(AuthApiService);
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly router = inject(Router);
  readonly i18n = inject(I18nService);
  private readonly themeService = inject(ThemeService);
  private readonly patientPortalService = inject(PatientPortalService);

  readonly unreadNotificationCount = signal<number>(0);

  readonly session = this.tokenStorage.session;
  readonly locale = this.i18n.locale;
  readonly theme = this.themeService.theme;
  readonly links = APP_BRAND_CONFIG.publicLinks;
  readonly profileLabel = computed(() => this.i18n.t('shell.profile') || 'Mon Profil');
  readonly logoutLabel = computed(() => this.i18n.t('shell.logout'));
  readonly termsLabel = computed(() => this.i18n.t('shell.terms'));
  readonly privacyLabel = computed(() => this.i18n.t('shell.privacy'));
  readonly helpLabel = computed(() => this.i18n.t('shell.help'));
  readonly copyrightLabel = computed(() =>
    this.i18n.t('shell.copyright').replace('{publisher}', APP_BRAND_CONFIG.publisherName)
  );
  readonly themeTooltip = computed(() =>
    this.theme() === 'dark' ? this.i18n.t('shell.theme.light') : this.i18n.t('shell.theme.dark')
  );

  readonly sidebarCollapsed = signal<boolean>(this.resolveInitialSidebarState());
  readonly mobileMenuOpen = signal<boolean>(false);

  constructor() {
    const currentSession = this.session();
    if (currentSession?.role === 'PATIENT') {
      this.loadUnreadNotificationCount();
    }
  }

  private loadUnreadNotificationCount(): void {
    this.patientPortalService.getUnreadNotificationCount().subscribe({
      next: (count) => this.unreadNotificationCount.set(count),
      error: () => this.unreadNotificationCount.set(0)
    });
  }

  private resolveInitialSidebarState(): boolean {
    try {
      const stored = localStorage.getItem('joprelys.sidebar.collapsed');
      return stored === 'true';
    } catch {
      return false;
    }
  }

  toggleSidebar(): void {
    const next = !this.sidebarCollapsed();
    this.sidebarCollapsed.set(next);
    try {
      localStorage.setItem('joprelys.sidebar.collapsed', String(next));
    } catch {}
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(v => !v);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  setLang(lang: AppLocale): void {
    this.i18n.setLocale(lang);
  }

  toggleTheme(): void {
    const nextTheme = this.theme() === 'dark' ? 'light' : 'dark';
    this.themeService.setTheme(nextTheme);
  }

  logout(): void {
    this.authApi.logout().subscribe({
      next: () => this.router.navigate(['/']),
      error: () => this.router.navigate(['/']),
    });
  }
}
