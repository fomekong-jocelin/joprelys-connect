import { Component } from '@angular/core';
import { ToastHostComponent } from './shared/ui/toast-host.component';
import { RouterOutlet } from '@angular/router';
import { AccessRequirementBannerComponent } from './core/http/access-requirement-banner.component';
import { ThemeService } from './core/theme/theme.service';
import { AppTitleService } from './core/title/app-title.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AccessRequirementBannerComponent, ToastHostComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  constructor(_themeService: ThemeService, _appTitleService: AppTitleService) {
    void _themeService;
    _appTitleService.init();
  }
}
