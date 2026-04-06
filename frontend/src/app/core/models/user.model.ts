import { UserRole } from './auth.model';

export interface User {
  id: string;
  email: string;
  roleType: UserRole;
  gender?: string | null;
}
