export type ChatRole = 'user' | 'assistant' | 'system';

export interface ChatMessage {
  id?: string;
  role: ChatRole;
  content: string;
  timestamp: Date;
  plotlyJson?: any;
  loading?: boolean;
}

export interface ChatRequest {
  message: string;
  sessionId?: string;
}

export interface ChatResponse {
  message: string;
  sessionId: string;
  status: string;
  plotlyJson?: any;
}
