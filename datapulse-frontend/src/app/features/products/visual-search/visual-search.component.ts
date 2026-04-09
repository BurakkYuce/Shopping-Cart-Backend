import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs';

import { NotificationService } from '../../../core/services/notification.service';
import { environment } from '../../../../environments/environment';

interface VisualResult {
  id: string;
  name: string;
  brand?: string;
  price?: number;
  retail_price?: number;
  image_url?: string;
  rating?: number;
  similarity: number;
  matchPct: number;
}

@Component({
  selector: 'app-visual-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './visual-search.component.html',
})
export class VisualSearchComponent {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(NotificationService);
  private readonly router = inject(Router);

  private readonly chatbotUrl = environment.chatbotUrl;

  readonly hint = signal<string>('');
  readonly previewUrl = signal<string | null>(null);
  readonly loading = signal<boolean>(false);
  readonly results = signal<VisualResult[]>([]);
  readonly dragOver = signal<boolean>(false);

  private uploadedFile: File | null = null;

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.handleFile(file);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) this.handleFile(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  onDragLeave(): void {
    this.dragOver.set(false);
  }

  handleFile(file: File): void {
    if (!file.type.startsWith('image/')) {
      this.toast.error('Please drop an image file.');
      return;
    }
    this.uploadedFile = file;
    const reader = new FileReader();
    reader.onload = () => this.previewUrl.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  findSimilar(): void {
    if (!this.uploadedFile && !this.hint().trim()) {
      this.toast.info('Drop an image or describe what you\'re looking for.');
      return;
    }
    this.loading.set(true);
    this.results.set([]);

    const formData = new FormData();
    if (this.uploadedFile) formData.append('image', this.uploadedFile);
    if (this.hint().trim()) formData.append('hint', this.hint().trim());
    formData.append('top_k', '9');

    this.http.post<{ results: any[] }>(`${this.chatbotUrl}/visual-search`, formData).pipe(
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: (res) => {
        const mapped: VisualResult[] = (res.results ?? []).map((r) => ({
          ...r,
          matchPct: Math.round((r.similarity ?? 0) * 100),
        }));
        this.results.set(mapped);
        if (mapped.length === 0) this.toast.info('No visual matches found. Try a different image.');
      },
      error: () => {
        this.results.set([]);
        this.toast.error('Visual search failed. Is the chatbot running?');
      },
    });
  }

  clearImage(): void {
    this.previewUrl.set(null);
    this.uploadedFile = null;
  }

  viewProduct(id: string): void {
    this.router.navigate(['/products', id]);
  }
}
