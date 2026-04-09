/**
 * Backend PageResponse shape (see com.datapulse.dto.response.PageResponse).
 * Fields: content, page, size, totalElements, totalPages, last.
 */
export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiError {
  timestamp?: string;
  status: number;
  error?: string;
  message: string;
  path?: string;
}

export interface PageParams {
  page?: number;
  size?: number;
  sort?: string;
}
