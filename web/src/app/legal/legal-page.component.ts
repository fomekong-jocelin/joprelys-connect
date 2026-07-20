import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AppLocale } from '../core/config/app-brand.config';
import { I18nService } from '../core/i18n/i18n.service';
import { ThemeService } from '../core/theme/theme.service';
import { AppLogoComponent } from '../shared/ui/app-logo.component';
import {
  LEGAL_CONTACTS,
  LEGAL_DOCUMENT_NAVIGATION,
  LEGAL_DOCUMENTS,
  LegalContactId,
  LegalDocumentId,
} from './legal-documents';

@Component({
  selector: 'app-legal-page',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './legal-page.component.html',
  styleUrl: './legal-page.component.css',
})
export class LegalPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly i18n = inject(I18nService);
  private readonly themeService = inject(ThemeService);

  readonly locale = this.i18n.locale;
  readonly theme = this.themeService.theme;
  readonly navigation = LEGAL_DOCUMENT_NAVIGATION;
  readonly contacts = LEGAL_CONTACTS;

  readonly documentId = this.resolveDocumentId();
  readonly document = LEGAL_DOCUMENTS[this.documentId];

  t(key: string): string {
    return this.i18n.t(key);
  }

  setLang(lang: AppLocale): void {
    void this.i18n.setLocale(lang);
  }

  toggleTheme(): void {
    this.themeService.setTheme(this.theme() === 'light' ? 'dark' : 'light');
  }

  themeTooltip(): string {
    return this.theme() === 'dark'
      ? this.t('shell.theme.light')
      : this.t('shell.theme.dark');
  }

  contact(contactId: LegalContactId) {
    return this.contacts[contactId];
  }

  private resolveDocumentId(): LegalDocumentId {
    const configured = this.route.snapshot.data['legalDocument'] as LegalDocumentId | undefined;
    return configured && configured in LEGAL_DOCUMENTS ? configured : 'privacy';
  }
}
