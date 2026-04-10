export type ChatRole = 'user' | 'assistant' | 'system';

export interface ChatMessage {
  id?: string;
  role: ChatRole;
  content: string;
  timestamp: Date;
  plotlyJson?: any;
  generatedSql?: string;
  intent?: string;
  loading?: boolean;
}

export interface ChatRequest {
  message: string;
  sessionId?: string;
  conversationId?: string;
}

export interface ChatResponse {
  message: string;
  sessionId: string;
  status: string;
  plotlyJson?: any;
  conversationId?: string;
  intent?: string;
  generatedSql?: string;
}

export interface Conversation {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface ConversationDetail {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messages: ConversationMessage[];
}

export interface ConversationMessage {
  id: string;
  role: ChatRole;
  content: string;
  plotlyJson?: string;
  generatedSql?: string;
  intent?: string;
  createdAt: string;
}

export interface PagedConversations {
  content: Conversation[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
