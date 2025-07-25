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
    role?: string;
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

// Policy types
export type PolicyType = 
  | 'DEVICE_SECURITY'
  | 'TRANSACTION_LIMIT'
  | 'GEOGRAPHIC_RESTRICTION'
  | 'TIME_RESTRICTION'
  | 'DEVICE_BINDING'
  | 'RISK_ASSESSMENT'
  | 'API_RATE_LIMIT'
  | 'AUTHENTICATION_REQUIREMENT';

export type EnforcementLevel = 
  | 'BLOCK'
  | 'WARN'
  | 'NOTIFY'
  | 'REQUIRE_MFA'
  | 'MONITOR';

export type RuleOperator = 
  | 'EQUALS'
  | 'NOT_EQUALS'
  | 'GREATER_THAN'
  | 'LESS_THAN'
  | 'GREATER_THAN_OR_EQUALS'
  | 'LESS_THAN_OR_EQUALS'
  | 'CONTAINS'
  | 'NOT_CONTAINS'
  | 'STARTS_WITH'
  | 'ENDS_WITH'
  | 'IN'
  | 'NOT_IN'
  | 'REGEX_MATCH'
  | 'BETWEEN'
  | 'IS_NULL'
  | 'IS_NOT_NULL';

export interface PolicyRule {
  id?: number;
  ruleName: string;
  conditionField: string;
  operator: RuleOperator;
  conditionValue: string;
  errorMessage?: string;
  priority?: number;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface Policy {
  id?: number;
  clientId: string;
  policyName: string;
  policyType: PolicyType;
  enforcementLevel: EnforcementLevel;
  description?: string;
  isActive?: boolean;
  rules?: PolicyRule[];
  createdAt?: string;
  updatedAt?: string;
}

export interface PolicyViolation {
  id: number;
  deviceId: string;
  policy: Policy;
  violatedRule?: PolicyRule;
  actionTaken: string;
  requestDetails?: string;
  violationDetails?: string;
  ipAddress?: string;
  userAgent?: string;
  createdAt: string;
}