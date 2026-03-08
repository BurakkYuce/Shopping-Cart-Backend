import { Component } from '@angular/core';
import {FormsModule} from '@angular/forms';
import { CommonModule } from '@angular/common';

interface ChatMessage {
  text: string;
  sender: 'user' | "ai"
}

@Component({
  selector: 'app-chat-widget',
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-widget.html',
  styleUrl: './chat-widget.css',
  standalone: true
})
export class ChatWidget {
  isOpen:boolean = false;
  userInput: string = '';
  isLoading: boolean = false;

  messages: ChatMessage[] = [
    { text: 'Hello! How can I assist you today?', sender: 'ai' }
  ];

  toggleChat():void{
    this.isOpen = !this.isOpen;
  }

  sendMessage(): void {
    const trimmedMessage = this.userInput.trim();
    if (!trimmedMessage) {
      return;
    }
    this.messages.push({
      text: trimmedMessage,
      sender: 'user'
    });

    this.userInput = '';
    this.isLoading = true;

    
      this.messages.push({
        sender: 'ai',
        text: `AI response: I can help analyze products in this application based on your question: "${trimmedMessage}".`
      });

      this.isLoading = false;

  }
}
