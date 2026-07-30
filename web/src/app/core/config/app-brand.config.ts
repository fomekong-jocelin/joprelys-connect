export type AppTheme = 'light' | 'dark';
export type AppLocale = 'fr' | 'en';

export interface AppBrandConfig {
  readonly appName: string;
  readonly appShortName: string;
  readonly productName: string;
  readonly appSlogan: string;
  readonly logoPath: string;
  readonly logoOnDarkPath: string;
  readonly publisherName: string;
  readonly publicSiteUrl: string;
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
  productName: 'Connect',
  appSlogan: 'Coordination clinique securisee',
  logoPath: 'assets/branding/logo_principal.png',
  logoOnDarkPath: 'assets/branding/logo_white_blue_bg.png',
  publisherName: 'JOPRELYS SARL',
  publicSiteUrl: 'https://joprelys.com',
  defaultLocale: 'fr',
  supportedLocales: ['fr', 'en'],
  defaultTheme: 'light',
  supportEmail: 'support@joprelys.com',
  publicLinks: {
    terms: '/legal/terms',
    privacy: '/legal/privacy',
    help: 'mailto:support@joprelys.com',
  },
};
