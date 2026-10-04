import { TestBed } from '@angular/core/testing';
import { readFileSync } from 'node:fs';
import { I18nService } from '../../core/i18n/i18n.service';
import { FileDragDropComponent } from './file-drag-drop.component';

describe('FileDragDropComponent translated instructions and errors', () => {
  for (const locale of ['fr', 'en']) {
    it(`uses the ${locale} dictionary for instructions, rejection and removal`, () => {
      const dictionary = JSON.parse(readFileSync(`src/assets/i18n/${locale}.json`, 'utf8')) as Record<string, string>;
      TestBed.configureTestingModule({ imports: [FileDragDropComponent], providers: [
        { provide: I18nService, useValue: { t: (key: string) => dictionary[key] ?? key } },
      ] });
      const fixture = TestBed.createComponent(FileDragDropComponent);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain(dictionary['fileUpload.drop']);
      expect(fixture.nativeElement.textContent).toContain(dictionary['fileUpload.browse']);
      expect(fixture.nativeElement.textContent).not.toContain('{size}');
      const file = new File(['test'], 'document.txt', { type: 'text/plain' });
      fixture.componentInstance.onFileSelected({ target: { files: [file] } } as unknown as Event);
      expect(fixture.componentInstance.errorMessage()).toBe(dictionary['fileUpload.invalidType'].replace('{types}', 'image/png, image/jpeg, image/webp'));
      fixture.componentRef.setInput('maxSizeMb', 0);
      fixture.componentInstance.onFileSelected({ target: { files: [file] } } as unknown as Event);
      expect(fixture.componentInstance.errorMessage()).toBe(dictionary['fileUpload.tooLarge'].replace('{size}', '0'));
      fixture.componentInstance.setPreviewUrl('/synthetic.png', 'test.png');
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('button').textContent).toContain(dictionary['fileUpload.remove']);
      expect(fixture.nativeElement.querySelector('img').alt).toBe(dictionary['fileUpload.preview']);
    });
  }
});
