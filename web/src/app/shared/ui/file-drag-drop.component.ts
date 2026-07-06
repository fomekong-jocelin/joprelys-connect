import { Component, input, model, output, signal, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-file-drag-drop',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-1.5 w-full">
      @if (label()) {
        <label class="ui-label block text-sm font-medium text-slate-700">
          {{ label() }} @if (required()) { <span class="text-red-500">*</span> }
        </label>
      }

      <div
        class="relative border-2 border-dashed rounded-md p-6 flex flex-col items-center justify-center text-center transition-all duration-200 cursor-pointer min-h-[160px]"
        [class.border-indigo-500]="isDragOver()"
        [class.bg-indigo-50/20]="isDragOver()"
        [class.border-slate-300]="!isDragOver() && !previewUrl()"
        [class.border-emerald-500]="!isDragOver() && previewUrl()"
        [class.hover:border-indigo-400]="!previewUrl()"
        [class.bg-slate-50/50]="!isDragOver()"
        (dragover)="onDragOver($event)"
        (dragleave)="onDragLeave()"
        (drop)="onDrop($event)"
        (click)="fileInput.click()"
      >
        <input
          #fileInput
          type="file"
          class="hidden"
          [accept]="accept()"
          (change)="onFileSelected($event)"
        />

        @if (previewUrl()) {
          <!-- Prévisualisation de l'image -->
          <div class="relative flex flex-col items-center space-y-3 w-full" (click)="$event.stopPropagation()">
            <img
              [src]="previewUrl()"
              alt="Preview"
              class="max-h-[140px] max-w-full object-contain rounded border border-slate-200 shadow-sm"
            />
            <div class="flex items-center space-x-2">
              <span class="text-xs text-slate-500 truncate max-w-[200px]" *ngIf="selectedFileName()">
                {{ selectedFileName() }}
              </span>
              <button
                type="button"
                class="text-xs text-red-600 hover:text-red-800 font-medium px-2 py-1 rounded bg-red-50 hover:bg-red-100 transition-colors"
                (click)="removeFile()"
              >
                Supprimer
              </button>
            </div>
          </div>
        } @else {
          <!-- Zone vide pour glisser-déposer -->
          <div class="flex flex-col items-center justify-center space-y-2 pointer-events-none">
            <svg
              class="w-10 h-10 text-slate-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.5"
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
              ></path>
            </svg>
            <p class="text-sm font-medium text-slate-600">
              Glissez-déposez l'image ici, ou <span class="text-indigo-600">parcourez</span>
            </p>
            <p class="text-xs text-slate-400">
              Format {{ accept() }} (Max. {{ maxSizeMb() }} Mo)
            </p>
          </div>
        }
      </div>

      @if (errorMessage()) {
        <p class="text-xs text-red-600 font-medium mt-1">{{ errorMessage() }}</p>
      }
    </div>
  `,
  styles: []
})
export class FileDragDropComponent {
  readonly label = input<string | null>(null);
  readonly required = input<boolean>(false);
  readonly accept = input<string>('image/png, image/jpeg');
  readonly maxSizeMb = input<number>(2);
  
  readonly fileSelected = output<File>();
  readonly fileRemoved = output<void>();

  readonly previewUrl = model<string | null>(null);
  readonly selectedFileName = signal<string | null>(null);
  readonly isDragOver = signal(false);
  readonly errorMessage = signal<string | null>(null);

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver.set(true);
  }

  onDragLeave(): void {
    this.isDragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver.set(false);
    
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.handleFile(event.dataTransfer.files[0]);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0]);
    }
  }

  setPreviewUrl(url: string | null, name: string | null = null): void {
    this.previewUrl.set(url);
    this.selectedFileName.set(name);
  }

  removeFile(): void {
    this.previewUrl.set(null);
    this.selectedFileName.set(null);
    this.errorMessage.set(null);
    this.fileRemoved.emit();
  }

  private handleFile(file: File): void {
    this.errorMessage.set(null);

    // Vérifier la taille
    const maxSizeBytes = this.maxSizeMb() * 1024 * 1024;
    if (file.size > maxSizeBytes) {
      this.errorMessage.set(`Le fichier dépasse la taille maximale autorisée de ${this.maxSizeMb()} Mo.`);
      return;
    }

    // Vérifier le type MIME
    const allowedTypes = this.accept().split(',').map(type => type.trim().toLowerCase());
    const fileType = file.type.toLowerCase();
    
    // Support basique des wildcards (ex: image/*)
    const isAllowed = allowedTypes.some(allowed => {
      if (allowed.endsWith('/*')) {
        const prefix = allowed.split('/')[0];
        return fileType.startsWith(prefix + '/');
      }
      return allowed === fileType;
    });

    if (!isAllowed) {
      this.errorMessage.set(`Type de fichier non autorisé. Types acceptés : ${this.accept()}.`);
      return;
    }

    this.selectedFileName.set(file.name);

    // Générer une preview locale
    const reader = new FileReader();
    reader.onload = () => {
      this.previewUrl.set(reader.result as string);
    };
    reader.readAsDataURL(file);

    this.fileSelected.emit(file);
  }
}
