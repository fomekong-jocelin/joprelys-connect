import { Component, input } from '@angular/core';

export type UiIconName =
  | 'building'
  | 'bed'
  | 'users'
  | 'check'
  | 'wrench'
  | 'credit-card'
  | 'stethoscope'
  | 'handshake'
  | 'table-cells'
  | 'magnifying-glass'
  | 'document-text'
  | 'arrow-path'
  | 'plus'
  | 'folder-open'
  | 'printer'
  | 'banknotes'
  | 'trash'
  | 'x-mark'
  | 'phone'
  | 'map-pin'
  | 'calendar'
  | 'receipt-percent'
  | 'calculator'
  | 'chart-bar'
  | 'chevron-down'
  | 'information-circle'
  | 'shield-check'
  | 'clipboard-document-list'
  | 'syringe'
  | 'bolt';

@Component({
  selector: 'app-ui-icon',
  standalone: true,
  template: `
    <span class="inline-flex items-center justify-center" [class]="class()">
      @switch (name()) {
        @case ('building') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 21h19.5m-18-18v18m10.5-18v18m6-13.5V21M6.75 6.75h.75m-.75 3h.75m-.75 3h.75m3-6h.75m-.75 3h.75m-.75 3h.75M6.75 21v-3.375c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21M3 3h12m-.75 4.5h.75m-.75 3h.75m-.75 3h.75" />
          </svg>
        }
        @case ('bed') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 12V7.5A2.25 2.25 0 014.5 5.25h9a2.25 2.25 0 012.25 2.25v3.75m0 0H21m-5.25 0V15m0-3.75v4.5a2.25 2.25 0 01-2.25 2.25H4.5a2.25 2.25 0 01-2.25-2.25V12m0 0h2.25" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M5.25 12v4.5m4.5-4.5v4.5" />
          </svg>
        }
        @case ('users') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" />
          </svg>
        }
        @case ('check') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
          </svg>
        }
        @case ('wrench') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75a4.5 4.5 0 01-4.884 4.484c-1.076-.091-2.264.071-3.026.474a2.25 2.25 0 01-2.773 0c-.753-.397-1.94-.565-3.026-.474A4.5 4.5 0 013.75 6.75V5.25m18 1.5V5.25m-18 1.5v1.5m2.25-4.5a3.375 3.375 0 116.75 0 3.375 3.375 0 01-6.75 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M9.75 12.75l-1.5 7.5m7.5-7.5l1.5 7.5" />
          </svg>
        }
        @case ('credit-card') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 8.25h19.5M2.25 9h19.5m-1.5-5.25H3.75a2.25 2.25 0 00-2.25 2.25v13.5a2.25 2.25 0 002.25 2.25h16.5a2.25 2.25 0 002.25-2.25V6a2.25 2.25 0 00-2.25-2.25z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M5.25 15.75h3m-3 3h1.5m3-3h1.5m3 0h1.5m-7.5 3h9" />
          </svg>
        }
        @case ('stethoscope') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 6.75a3 3 0 11-6 0 3 3 0 016 0zM10.5 6.75v3.75c0 2.9-2.35 5.25-5.25 5.25S0 13.4 0 10.5V6.75" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 3.75a3 3 0 11-6 0 3 3 0 016 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 6.75v3.75c0 4.97 4.03 9 9 9s9-4.03 9-9V6.75" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M22.5 6.75v3.75" />
          </svg>
        }
        @case ('handshake') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 12.75L12 20.25l-7.5-7.5M3.75 9.75l4.5-4.5a2.25 2.25 0 013.182 0l.818.818 3.182-3.182a2.25 2.25 0 013.182 0l4.5 4.5" />
          </svg>
        }
        @case ('table-cells') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3.375 3h17.25c.621 0 1.125.504 1.125 1.125v17.25c0 .621-.504 1.125-1.125 1.125H3.375A1.125 1.125 0 012.25 21.375V4.125C2.25 3.504 2.754 3 3.375 3zm0 5.25h19.5M2.25 13.5h19.5M2.25 18.75h19.5M9 3v19.5M15 3v19.5" />
          </svg>
        }
        @case ('magnifying-glass') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
          </svg>
        }
        @case ('document-text') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M8.25 6.75h.008v.008H8.25V6.75zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 9.75h.008v.008H10.5V9.75zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" />
          </svg>
        }
        @case ('arrow-path') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
          </svg>
        }
        @case ('plus') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
        }
        @case ('folder-open') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 9.776c.112-.017.227-.026.344-.026h15.812c.117 0 .232.009.344.026m-16.5 0a2.25 2.25 0 00-1.883 2.542l.857 6a2.25 2.25 0 002.227 1.932H19.05a2.25 2.25 0 002.227-1.932l.857-6a2.25 2.25 0 00-1.883-2.542m-16.5 0V6A2.25 2.25 0 013.75 3.75h3.75a2.25 2.25 0 012.25 2.25v.75m-6 0h15m-11.25 4.5h7.5" />
          </svg>
        }
        @case ('printer') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6.72 13.829c-.24.03-.48.062-.72.096m.72-.096a42.415 42.415 0 0110.56 0m-10.56 0L6.34 18m10.94-4.171c.24.03.48.062.72.096m-.72-.096L17.66 18m0 0l.229 2.523a1.125 1.125 0 01-1.12 1.227H7.231c-.662 0-1.18-.568-1.12-1.227L6.34 18m11.318 0h1.091A2.25 2.25 0 0021 15.75V9.456c0-1.081-.768-2.015-1.837-2.175a48.055 48.055 0 00-1.913-.247M6.34 18H5.25A2.25 2.25 0 013 15.75V9.456c0-1.081.768-2.015 1.837-2.175a48.041 48.041 0 011.913-.247m10.5 0a48.536 48.536 0 00-10.5 0m10.5 0V3.375c0-.621-.504-1.125-1.125-1.125h-8.25c-.621 0-1.125.504-1.125 1.125v3.659" />
          </svg>
        }
        @case ('banknotes') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 18.75a2.25 2.25 0 002.25 2.25h15a2.25 2.25 0 002.25-2.25V8.25a2.25 2.25 0 00-2.25-2.25H4.5a2.25 2.25 0 00-2.25 2.25v10.5z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 12a3 3 0 100-6 3 3 0 000 6z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 10.5c0 .621-.504 1.125-1.125 1.125h-1.5a1.125 1.125 0 01-1.125-1.125v-1.5A1.125 1.125 0 0116.875 7.875h1.5c.621 0 1.125.504 1.125 1.125v1.5zM7.125 16.125c0 .621-.504 1.125-1.125 1.125h-1.5a1.125 1.125 0 01-1.125-1.125v-1.5c0-.621.504-1.125 1.125-1.125h1.5c.621 0 1.125.504 1.125 1.125v1.5z" />
          </svg>
        }
        @case ('trash') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
          </svg>
        }
        @case ('x-mark') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        }
        @case ('phone') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 6.75c0 8.284 6.716 15 15 15h2.25a2.25 2.25 0 002.25-2.25v-1.372c0-.516-.351-.966-.852-1.091l-4.423-1.106c-.44-.11-.902.055-1.173.417l-.97 1.293c-.282.376-.769.542-1.21.38a12.035 12.035 0 01-7.143-7.143c-.162-.441.004-.928.38-1.21l1.293-.97c.363-.271.527-.734.417-1.173L6.963 3.102a1.125 1.125 0 00-1.091-.852H4.5A2.25 2.25 0 002.25 4.5v2.25z" />
          </svg>
        }
        @case ('map-pin') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 10.5a3 3 0 11-6 0 3 3 0 016 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1115 0z" />
          </svg>
        }
        @case ('calendar') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5m-9-6h.008v.008H12v-.008zM12 15h.008v.008H12V15zm0-2.25h.008v.008H12v-.008zM9.75 15h.008v.008H9.75V15zm0-2.25h.008v.008H9.75v-.008zM7.5 15h.008v.008H7.5V15zm0-2.25h.008v.008H7.5v-.008zM14.25 15h.008v.008H14.25V15zm0-2.25h.008v.008H14.25v-.008zM16.5 15h.008v.008H16.5V15zm0-2.25h.008v.008H16.5v-.008z" />
          </svg>
        }
        @case ('receipt-percent') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 14.25l6-6m4.5-3.493V16.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V4.5" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 9.75h.008v.008H8.25V9.75zm0 4.5h.008v.008H8.25v-.008zm7.5-4.5h.008v.008H15.75V9.75zm0 4.5h.008v.008H15.75v-.008z" />
          </svg>
        }
        @case ('calculator') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 15.75V18a2.25 2.25 0 01-2.25 2.25H6a2.25 2.25 0 01-2.25-2.25V6A2.25 2.25 0 016 3.75h7.5A2.25 2.25 0 0115.75 6v2.25m0 0H21m-5.25 0V21m0-12.75h3.75m-3.75 4.5h3.75m-3.75 4.5h3.75M6.75 8.25h3m-3 3.75h3m-3 3.75h3" />
          </svg>
        }
        @case ('chart-bar') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z" />
          </svg>
        }
        @case ('chevron-down') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
          </svg>
        }
        @case ('information-circle') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zM8.25 9.75h.008v.008H8.25V9.75z" />
          </svg>
        }
        @case ('shield-check') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 3.75l7.5 3V12c0 4.5-3 7.5-7.5 8.25C7.5 19.5 4.5 16.5 4.5 12V6.75l7.5-3z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M8.75 12.25l2.25 2.25 4.5-5" />
          </svg>
        }
        @case ('clipboard-document-list') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 5.25h6m-6 0A2.25 2.25 0 0111.25 3h1.5A2.25 2.25 0 0115 5.25m-6 0H6.75A2.25 2.25 0 004.5 7.5v11.25A2.25 2.25 0 006.75 21h10.5a2.25 2.25 0 002.25-2.25V7.5a2.25 2.25 0 00-2.25-2.25H15" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 10.5h.008v.008H8.25V10.5zm2.25 0h5.25m-7.5 4.5h.008v.008H8.25V15zm2.25 0h5.25" />
          </svg>
        }
        @case ('syringe') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 6l4.5 4.5-9.75 9.75-4.5.75.75-4.5L13.5 6zM14.25 4.5l5.25 5.25m-3-7.5l5.25 5.25M6.75 14.25l3 3M3 21l2.25-2.25" />
          </svg>
        }
        @case ('bolt') {
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-[1em] h-[1em]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 13.5l9-11.25-1.5 8.25h9l-9 11.25 1.5-8.25h-9z" />
          </svg>
        }
      }
    </span>
  `,
})
export class IconComponent {
  readonly name = input.required<UiIconName>();
  readonly class = input<string>('');
}
