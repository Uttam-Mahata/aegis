import axios from 'axios';
import { type RegistrationKey, type RegistrationKeyRequest } from '../types';

const API_BASE_URL = 'http://localhost:8080/api/admin';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add auth token to requests
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('authToken');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const registrationKeyService = {
  // Create a new registration key
  createRegistrationKey: async (data: RegistrationKeyRequest): Promise<RegistrationKey> => {
    const response = await api.post<RegistrationKey>('/registration-keys', data);
    return response.data;
  },

  // Get all registration keys
  getAllRegistrationKeys: async (): Promise<RegistrationKey[]> => {
    const response = await api.get<RegistrationKey[]>('/registration-keys');
    return response.data;
  },

  // Get registration key by client ID
  getRegistrationKey: async (clientId: string): Promise<RegistrationKey> => {
    const response = await api.get<RegistrationKey>(`/registration-keys/${clientId}`);
    return response.data;
  },

  // Revoke a registration key
  revokeRegistrationKey: async (clientId: string): Promise<RegistrationKey> => {
    const response = await api.put<RegistrationKey>(`/registration-keys/${clientId}/revoke`);
    return response.data;
  },

  // Regenerate a registration key
  regenerateRegistrationKey: async (clientId: string): Promise<RegistrationKey> => {
    const response = await api.put<RegistrationKey>(`/registration-keys/${clientId}/regenerate`);
    return response.data;
  },

  // Check health
  checkHealth: async (): Promise<string> => {
    const response = await api.get<string>('/health');
    return response.data;
  },
};

export const adminService = {
  // Get all organizations
  getAllOrganizations: async (): Promise<any[]> => {
    const response = await api.get('/organizations');
    return response.data;
  },

  // Get pending organizations
  getPendingOrganizations: async (): Promise<any[]> => {
    const response = await api.get('/organizations/pending');
    return response.data;
  },

  // Approve an organization
  approveOrganization: async (userId: number, approvedBy: string): Promise<any> => {
    const response = await api.post(`/organizations/${userId}/approve`, { approvedBy });
    return response.data;
  },

  // Reject an organization
  rejectOrganization: async (userId: number, rejectedBy: string, reason?: string): Promise<any> => {
    const response = await api.post(`/organizations/${userId}/reject`, { 
      approvedBy: rejectedBy,
      reason 
    });
    return response.data;
  },
};

export default api;