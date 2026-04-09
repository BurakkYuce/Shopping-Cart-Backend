import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ChatRequest, ChatResponse, ChatMessage } from '../models/chat.models';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/chat`;

  // Session-scoped chat state
  private readonly _messages = signal<ChatMessage[]>([]);
  readonly messages = this._messages.asReadonly();
  private sessionId: string | null = null;

  // Panel visibility — consumed by chat-bubble
  readonly panelOpen = signal<boolean>(false);

  openPanel(): void {
    this.panelOpen.set(true);
  }
  closePanel(): void {
    this.panelOpen.set(false);
  }
  togglePanel(): void {
    this.panelOpen.update((v) => !v);
  }

  send(message: string): Observable<ChatResponse> {
    const body: ChatRequest = { message, sessionId: this.sessionId ?? undefined };

    // Push user message locally
    this.pushMessage({ role: 'user', content: message, timestamp: new Date() });
    // Placeholder loader
    const loader: ChatMessage = { role: 'assistant', content: '', timestamp: new Date(), loading: true };
    this.pushMessage(loader);

    return this.http.post<ChatResponse>(`${this.baseUrl}/ask`, body).pipe(
      tap((res) => {
        this.sessionId = res.sessionId;
        const msgs = [...this._messages()];
        const loaderIdx = msgs.findIndex((m) => m.loading);
        if (loaderIdx >= 0) {
          msgs[loaderIdx] = {
            role: 'assistant',
            content: res.message,
            timestamp: new Date(),
            plotlyJson: res.plotlyJson,
          };
          this._messages.set(msgs);
        }
      }),
    );
  }

  reset(): void {
    this._messages.set([]);
    this.sessionId = null;
  }

  private pushMessage(msg: ChatMessage): void {
    this._messages.update((arr) => [...arr, msg]);
  }
}
