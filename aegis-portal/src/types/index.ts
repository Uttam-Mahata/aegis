export interface RegistrationKey {
  id: number;
  clientId: string;
  registrationKey: string;
  description?: string;
  isActive: boolean;
  expiresAt?: string;
  createdAt: string;
}

export interface RegistrationKeyRequest {
  clientId: string;
  description?: string;
  expiresAt?: string;
}

export interface ApiResponse<T> {
  status: 'success' | 'error';
  message?: string;
  data?: T;
}

export interface AuthState {
  isAuthenticated: boolean;
  user?: {
    id: string;
    name: string;
    email: string;
    organization: string;
  };
  token?: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface Organization {
  id: string;
  name: string;
  email: string;
  contactPerson: string;
  phone?: string;
  address?: string;
  createdAt: string;
}