import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './core/theme/theme.service';
import { AppTitleService } from './core/title/app-title.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  constructor(_themeService: ThemeService, _appTitleService: AppTitleService) {
    void _themeService;
    _appTitleService.init();
  }
}
