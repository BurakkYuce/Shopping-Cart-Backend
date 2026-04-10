import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ChatRequest, ChatResponse, ChatMessage,
  Conversation, ConversationDetail, PagedConversations
} from '../models/chat.models';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/chat`;
  private readonly convUrl = `${environment.apiUrl}/conversations`;

  // Session-scoped chat state
  private readonly _messages = signal<ChatMessage[]>([]);
  readonly messages = this._messages.asReadonly();
  private sessionId: string | null = null;
  private conversationId: string | null = null;

  readonly activeConversationId = signal<string | null>(null);

  // Panel visibility
  readonly panelOpen = signal<boolean>(false);

  // Conversation list
  private readonly _conversations = signal<Conversation[]>([]);
  readonly conversations = this._conversations.asReadonly();
  readonly conversationsLoading = signal<boolean>(false);

  openPanel(): void { this.panelOpen.set(true); }
  closePanel(): void { this.panelOpen.set(false); }
  togglePanel(): void { this.panelOpen.update((v) => !v); }

  send(message: string): Observable<ChatResponse> {
    const body: ChatRequest = {
      message,
      sessionId: this.sessionId ?? undefined,
      conversationId: this.conversationId ?? undefined,
    };

    this.pushMessage({ role: 'user', content: message, timestamp: new Date() });
    const loader: ChatMessage = { role: 'assistant', content: '', timestamp: new Date(), loading: true };
    this.pushMessage(loader);

    return this.http.post<ChatResponse>(`${this.baseUrl}/ask`, body).pipe(
      tap((res) => {
        this.sessionId = res.sessionId;
        this.conversationId = res.conversationId ?? this.conversationId;
        this.activeConversationId.set(this.conversationId);

        const msgs = [...this._messages()];
        const loaderIdx = msgs.findIndex((m) => m.loading);
        if (loaderIdx >= 0) {
          msgs[loaderIdx] = {
            role: 'assistant',
            content: res.message,
            timestamp: new Date(),
            plotlyJson: res.plotlyJson,
            intent: res.intent,
            generatedSql: res.generatedSql,
          };
          this._messages.set(msgs);
        }
      }),
    );
  }

  // --- Conversation CRUD ---

  loadConversations(): void {
    this.conversationsLoading.set(true);
    this.http.get<PagedConversations>(this.convUrl, {
      params: new HttpParams().set('size', '50'),
    }).subscribe({
      next: (res) => {
        this._conversations.set(res.content);
        this.conversationsLoading.set(false);
      },
      error: () => this.conversationsLoading.set(false),
    });
  }

  loadConversation(id: string): Observable<ConversationDetail> {
    return this.http.get<ConversationDetail>(`${this.convUrl}/${id}`).pipe(
      tap((detail) => {
        this.conversationId = detail.id;
        this.activeConversationId.set(detail.id);
        const msgs: ChatMessage[] = detail.messages.map((m) => ({
          id: m.id,
          role: m.role,
          content: m.content,
          timestamp: new Date(m.createdAt),
          plotlyJson: m.plotlyJson ? m.plotlyJson : undefined,
          generatedSql: m.generatedSql ?? undefined,
          intent: m.intent ?? undefined,
        }));
        this._messages.set(msgs);
      }),
    );
  }

  deleteConversation(id: string): Observable<void> {
    return this.http.delete<void>(`${this.convUrl}/${id}`).pipe(
      tap(() => {
        this._conversations.update((list) => list.filter((c) => c.id !== id));
        if (this.conversationId === id) {
          this.reset();
        }
      }),
    );
  }

  renameConversation(id: string, title: string): Observable<Conversation> {
    return this.http.patch<Conversation>(`${this.convUrl}/${id}/title`, { title }).pipe(
      tap((updated) => {
        this._conversations.update((list) =>
          list.map((c) => (c.id === id ? { ...c, title: updated.title } : c))
        );
      }),
    );
  }

  newConversation(): void {
    this.reset();
  }

  reset(): void {
    this._messages.set([]);
    this.sessionId = null;
    this.conversationId = null;
    this.activeConversationId.set(null);
  }

  private pushMessage(msg: ChatMessage): void {
    this._messages.update((arr) => [...arr, msg]);
  }
}
