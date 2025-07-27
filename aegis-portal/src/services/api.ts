import axios from 'axios';
import { 
  type RegistrationKey, 
  type RegistrationKeyRequest,
  type Policy,
  type PolicyViolation,
  type Device,
  type DeviceSearchParams,
  type FraudReport 
} from '../types';

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

export const policyService = {
  // Create a new policy
  createPolicy: async (policy: Policy): Promise<Policy> => {
    const response = await api.post<Policy>('/policies', policy);
    return response.data;
  },

  // Update an existing policy
  updatePolicy: async (policyId: number, policy: Policy): Promise<Policy> => {
    const response = await api.put<Policy>(`/policies/${policyId}`, policy);
    return response.data;
  },

  // Get policies by client ID
  getPoliciesByClientId: async (clientId: string): Promise<Policy[]> => {
    // URL encode the clientId to handle spaces and special characters
    const response = await api.get<Policy[]>(`/policies/client/${encodeURIComponent(clientId)}`);
    return response.data;
  },

  // Delete a policy
  deletePolicy: async (policyId: number): Promise<void> => {
    await api.delete(`/policies/${policyId}`);
  },

  // Get policy violations for a device
  getViolationHistory: async (
    deviceId: string, 
    from: string, 
    to: string
  ): Promise<PolicyViolation[]> => {
    const response = await api.get<PolicyViolation[]>(`/policies/violations/${encodeURIComponent(deviceId)}`, {
      params: { from, to }
    });
    return response.data;
  },
};

export const deviceService = {
  // Search devices
  searchDevices: async (params: DeviceSearchParams): Promise<{devices: Device[], pagination: any}> => {
    const response = await api.get('/devices/search', { params });
    return response.data;
  },

  // Get device details
  getDeviceDetails: async (deviceId: string): Promise<any> => {
    const response = await api.get(`/devices/${encodeURIComponent(deviceId)}/fraud-status`);
    return response.data;
  },

  // Block a device
  blockDevice: async (deviceId: string, reason: string, blockType: string = 'TEMPORARILY_BLOCKED'): Promise<any> => {
    const response = await api.post(`/devices/${encodeURIComponent(deviceId)}/block`, {
      reason,
      blockType
    });
    return response.data;
  },

  // Unblock a device (Admin only)
  unblockDevice: async (deviceId: string, reason: string): Promise<any> => {
    const response = await api.post(`/devices/${encodeURIComponent(deviceId)}/unblock`, {
      reason
    });
    return response.data;
  },

  // Mark device as fraudulent
  markDeviceAsFraudulent: async (deviceId: string, reason: string): Promise<any> => {
    const response = await api.post(`/devices/${encodeURIComponent(deviceId)}/mark-fraudulent`, {
      reason
    });
    return response.data;
  },

  // Get device transaction history
  getDeviceHistory: async (deviceId: string, page: number = 0, size: number = 20): Promise<any> => {
    const response = await api.get(`/devices/${encodeURIComponent(deviceId)}/history`, {
      params: { page, size }
    });
    return response.data;
  },

  // Submit fraud report
  submitFraudReport: async (fraudReport: FraudReport): Promise<any> => {
    const response = await api.post('/fraud-report', fraudReport);
    return response.data;
  },
};

export const fraudService = {
  // Get fraud statistics
  getFraudStatistics: async (period: string = '30d'): Promise<any> => {
    const response = await api.get('/fraud/statistics', {
      params: { period }
    });
    return response.data;
  },

  // Get recent fraud reports
  getRecentFraudReports: async (limit: number = 10): Promise<any[]> => {
    const response = await api.get('/fraud/reports/recent', {
      params: { limit }
    });
    return response.data;
  },
};

export default api;