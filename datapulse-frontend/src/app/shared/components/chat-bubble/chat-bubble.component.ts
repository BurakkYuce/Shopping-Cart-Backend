import { Component, ElementRef, ViewChild, effect, inject, signal, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ChatService } from '../../../core/services/chat.service';
import { ChatMessage, Conversation } from '../../../core/models/chat.models';

@Component({
  selector: 'app-chat-bubble',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-bubble.component.html',
  styleUrl: './chat-bubble.component.scss',
})
export class ChatBubbleComponent implements AfterViewInit, OnDestroy {
  readonly chat = inject(ChatService);

  readonly input = signal<string>('');
  readonly sending = signal<boolean>(false);
  readonly errorMsg = signal<string | null>(null);
  readonly showHistory = signal<boolean>(false);

  readonly suggestions = [
    'Which categories are trending this week?',
    'Compare my best-selling stores',
    'Show me orders that need action',
  ];

  @ViewChild('threadRef') threadRef?: ElementRef<HTMLDivElement>;
  private chartObservers: ResizeObserver[] = [];

  constructor() {
    effect(() => {
      const msgs = this.chat.messages();
      if (msgs.length > 0 && this.threadRef) {
        queueMicrotask(() => {
          const el = this.threadRef!.nativeElement;
          el.scrollTop = el.scrollHeight;
          this.renderPlots(msgs);
        });
      }
    });
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.chartObservers.forEach((o) => o.disconnect());
  }

  send(): void {
    const text = this.input().trim();
    if (!text || this.sending()) return;
    this.sending.set(true);
    this.errorMsg.set(null);
    this.input.set('');
    this.chat.send(text).subscribe({
      next: () => this.sending.set(false),
      error: () => {
        this.sending.set(false);
        this.errorMsg.set('Could not reach the assistant. Try again in a moment.');
      },
    });
  }

  useSuggestion(s: string): void {
    this.input.set(s);
    this.send();
  }

  onKeydown(ev: KeyboardEvent): void {
    if (ev.key === 'Enter' && !ev.shiftKey) {
      ev.preventDefault();
      this.send();
    }
  }

  trackMsg(index: number, msg: ChatMessage): string {
    return `${index}-${msg.timestamp.getTime()}`;
  }

  toggleHistory(): void {
    this.showHistory.update((v) => !v);
    if (this.showHistory()) {
      this.chat.loadConversations();
    }
  }

  loadConversation(conv: Conversation): void {
    this.chat.loadConversation(conv.id).subscribe({
      next: () => this.showHistory.set(false),
      error: () => this.errorMsg.set('Failed to load conversation.'),
    });
  }

  startNewConversation(): void {
    this.chat.newConversation();
    this.showHistory.set(false);
  }

  deleteConversation(conv: Conversation, event: Event): void {
    event.stopPropagation();
    this.chat.deleteConversation(conv.id).subscribe();
  }

  trackConv(index: number, conv: Conversation): string {
    return conv.id;
  }

  private async renderPlots(msgs: ChatMessage[]): Promise<void> {
    const plotMsgs = msgs.filter((m) => m.plotlyJson);
    if (plotMsgs.length === 0) return;
    try {
      const Plotly = await import('plotly.js-dist-min');
      plotMsgs.forEach((msg, idx) => {
        const id = `chat-plot-${msgs.indexOf(msg)}`;
        const el = document.getElementById(id);
        if (!el || el.hasChildNodes()) return;
        try {
          const data = typeof msg.plotlyJson === 'string' ? JSON.parse(msg.plotlyJson) : msg.plotlyJson;
          Plotly.newPlot(
            el,
            data.data ?? data,
            {
              ...(data.layout ?? {}),
              autosize: true,
              margin: { t: 30, l: 40, r: 20, b: 40 },
              font: { family: 'Inter, sans-serif', color: '#2C2118', size: 12 },
              paper_bgcolor: 'transparent',
              plot_bgcolor: 'transparent',
            },
            { responsive: true, displayModeBar: false },
          );
        } catch (e) {
          console.warn('Plotly render failed', e);
        }
      });
    } catch (e) {
      console.warn('Plotly import failed', e);
    }
  }
}
