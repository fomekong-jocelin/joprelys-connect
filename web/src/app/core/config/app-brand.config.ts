export type AppTheme = 'light' | 'dark';
export type AppLocale = 'fr' | 'en';

export interface AppBrandConfig {
  readonly appName: string;
  readonly appShortName: string;
  readonly appSlogan: string;
  readonly publisherName: string;
  readonly defaultLocale: AppLocale;
  readonly supportedLocales: readonly AppLocale[];
  readonly defaultTheme: AppTheme;
  readonly supportEmail: string;
  readonly publicLinks: {
    readonly terms: string;
    readonly privacy: string;
    readonly help: string;
  };
}

export const APP_BRAND_CONFIG: AppBrandConfig = {
  appName: 'Joprelys Connect',
  appShortName: 'Joprelys',
  appSlogan: 'Coordination clinique securisee',
  publisherName: 'Joprelys HealthTech',
  defaultLocale: 'fr',
  supportedLocales: ['fr', 'en'],
  defaultTheme: 'light',
  supportEmail: 'support@joprelys.local',
  publicLinks: {
    terms: '#',
    privacy: '#',
    help: '#',
  },
};
