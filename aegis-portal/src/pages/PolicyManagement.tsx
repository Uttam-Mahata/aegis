import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Alert,
  Accordion,
  AccordionSummary,
  AccordionDetails,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Security as SecurityIcon,
  ExpandMore as ExpandMoreIcon,
  RemoveCircleOutline as RemoveIcon,
} from '@mui/icons-material';
import { policyService } from '../services/api';
import type{ Policy, PolicyRule, PolicyType, EnforcementLevel, RuleOperator } from '../types';

const PolicyManagement: React.FC = () => {
  const [policies, setPolicies] = useState<Policy[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingPolicy, setEditingPolicy] = useState<Policy | null>(null);
  const [formData, setFormData] = useState<Policy>({
    clientId: '',
    policyName: '',
    policyType: 'TRANSACTION_LIMIT',
    enforcementLevel: 'BLOCK',
    description: '',
    isActive: true,
    rules: [],
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  
  // Get user's organization from localStorage
  const userOrganization = localStorage.getItem('userOrganization') || 'uco-bank';

  useEffect(() => {
    fetchPolicies();
  }, []);

  const fetchPolicies = async () => {
    setLoading(true);
    try {
      const data = await policyService.getPoliciesByClientId(userOrganization);
      setPolicies(data);
    } catch (error) {
      setError('Failed to fetch policies');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateOrUpdate = async () => {
    try {
      const policyData = {
        ...formData,
        clientId: userOrganization,
      };

      if (editingPolicy?.id) {
        await policyService.updatePolicy(editingPolicy.id, policyData);
        setSuccess('Policy updated successfully');
      } else {
        await policyService.createPolicy(policyData);
        setSuccess('Policy created successfully');
      }
      
      setModalOpen(false);
      resetForm();
      fetchPolicies();
    } catch (error) {
      setError('Failed to save policy');
    }
  };

  const handleDelete = async (policyId: number) => {
    if (window.confirm('Are you sure you want to delete this policy?')) {
      try {
        await policyService.deletePolicy(policyId);
        setSuccess('Policy deleted successfully');
        fetchPolicies();
      } catch (error) {
        setError('Failed to delete policy');
      }
    }
  };

  const handleEdit = (policy: Policy) => {
    setEditingPolicy(policy);
    setFormData(policy);
    setModalOpen(true);
  };

  const resetForm = () => {
    setEditingPolicy(null);
    setFormData({
      clientId: '',
      policyName: '',
      policyType: 'TRANSACTION_LIMIT',
      enforcementLevel: 'BLOCK',
      description: '',
      isActive: true,
      rules: [],
    });
  };

  const addRule = () => {
    setFormData({
      ...formData,
      rules: [
        ...(formData.rules || []),
        {
          ruleName: '',
          conditionField: '',
          operator: 'EQUALS',
          conditionValue: '',
          errorMessage: '',
          priority: 100,
          isActive: true,
        },
      ],
    });
  };

  const updateRule = (index: number, field: keyof PolicyRule, value: any) => {
    const updatedRules = [...(formData.rules || [])];
    updatedRules[index] = { ...updatedRules[index], [field]: value };
    setFormData({ ...formData, rules: updatedRules });
  };

  const removeRule = (index: number) => {
    const updatedRules = formData.rules?.filter((_, i) => i !== index) || [];
    setFormData({ ...formData, rules: updatedRules });
  };

  const policyTypeOptions: { value: PolicyType; label: string; color: string }[] = [
    { value: 'DEVICE_SECURITY', label: 'Device Security', color: 'error' },
    { value: 'TRANSACTION_LIMIT', label: 'Transaction Limit', color: 'warning' },
    { value: 'GEOGRAPHIC_RESTRICTION', label: 'Geographic Restriction', color: 'info' },
    { value: 'TIME_RESTRICTION', label: 'Time Restriction', color: 'success' },
    { value: 'DEVICE_BINDING', label: 'Device Binding', color: 'secondary' },
    { value: 'RISK_ASSESSMENT', label: 'Risk Assessment', color: 'warning' },
    { value: 'API_RATE_LIMIT', label: 'API Rate Limit', color: 'info' },
    { value: 'AUTHENTICATION_REQUIREMENT', label: 'Authentication', color: 'primary' },
  ];

  const enforcementOptions: { value: EnforcementLevel; label: string; color: string }[] = [
    { value: 'BLOCK', label: 'Block', color: 'error' },
    { value: 'REQUIRE_MFA', label: 'Require MFA', color: 'warning' },
    { value: 'WARN', label: 'Warn', color: 'warning' },
    { value: 'NOTIFY', label: 'Notify', color: 'info' },
    { value: 'MONITOR', label: 'Monitor', color: 'success' },
  ];

  const operatorOptions: { value: RuleOperator; label: string }[] = [
    { value: 'EQUALS', label: 'Equals (=)' },
    { value: 'NOT_EQUALS', label: 'Not Equals (≠)' },
    { value: 'GREATER_THAN', label: 'Greater Than (>)' },
    { value: 'LESS_THAN', label: 'Less Than (<)' },
    { value: 'GREATER_THAN_OR_EQUALS', label: 'Greater or Equal (≥)' },
    { value: 'LESS_THAN_OR_EQUALS', label: 'Less or Equal (≤)' },
    { value: 'CONTAINS', label: 'Contains' },
    { value: 'NOT_CONTAINS', label: 'Not Contains' },
    { value: 'STARTS_WITH', label: 'Starts With' },
    { value: 'ENDS_WITH', label: 'Ends With' },
    { value: 'IN', label: 'In List' },
    { value: 'NOT_IN', label: 'Not In List' },
    { value: 'REGEX_MATCH', label: 'Regex Match' },
    { value: 'BETWEEN', label: 'Between' },
    { value: 'IS_NULL', label: 'Is Null' },
    { value: 'IS_NOT_NULL', label: 'Is Not Null' },
  ];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            Policy Management
          </Typography>
          <Typography variant="body1" color="textSecondary">
            Configure security policies for your organization
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => {
            resetForm();
            setModalOpen(true);
          }}
        >
          Create New Policy
        </Button>
      </Box>

      {error && (
        <Alert severity="error" onClose={() => setError('')} sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" onClose={() => setSuccess('')} sx={{ mb: 2 }}>
          {success}
        </Alert>
      )}

      <Alert severity="info" sx={{ mb: 3 }}>
        Policies help enforce security rules and compliance requirements for your mobile banking applications. 
        Each policy can have multiple rules that are evaluated during transaction processing.
      </Alert>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Policy Name</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Enforcement</TableCell>
              <TableCell>Rules</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {policies.map((policy) => {
              const typeOption = policyTypeOptions.find(opt => opt.value === policy.policyType);
              const enforcementOption = enforcementOptions.find(opt => opt.value === policy.enforcementLevel);
              
              return (
                <TableRow key={policy.id}>
                  <TableCell>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <SecurityIcon />
                      <Typography fontWeight="bold">{policy.policyName}</Typography>
                    </Box>
                  </TableCell>
                  <TableCell>
                    <Chip 
                      label={typeOption?.label} 
                      color={typeOption?.color as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Chip 
                      label={enforcementOption?.label} 
                      color={enforcementOption?.color as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{policy.rules?.length || 0} rules</TableCell>
                  <TableCell>
                    <Chip 
                      label={policy.isActive ? 'Active' : 'Inactive'} 
                      color={policy.isActive ? 'success' : 'default'}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <IconButton
                      color="primary"
                      onClick={() => handleEdit(policy)}
                      size="small"
                    >
                      <EditIcon />
                    </IconButton>
                    <IconButton
                      color="error"
                      onClick={() => policy.id && handleDelete(policy.id)}
                      size="small"
                    >
                      <DeleteIcon />
                    </IconButton>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={modalOpen} onClose={() => setModalOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          {editingPolicy ? 'Edit Policy' : 'Create New Policy'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Policy Name"
                value={formData.policyName}
                onChange={(e) => setFormData({ ...formData, policyName: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Policy Type</InputLabel>
                <Select
                  value={formData.policyType}
                  onChange={(e) => setFormData({ ...formData, policyType: e.target.value as PolicyType })}
                  label="Policy Type"
                >
                  {policyTypeOptions.map(option => (
                    <MenuItem key={option.value} value={option.value}>
                      {option.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Enforcement Level</InputLabel>
                <Select
                  value={formData.enforcementLevel}
                  onChange={(e) => setFormData({ ...formData, enforcementLevel: e.target.value as EnforcementLevel })}
                  label="Enforcement Level"
                >
                  {enforcementOptions.map(option => (
                    <MenuItem key={option.value} value={option.value}>
                      {option.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.isActive}
                    onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                  />
                }
                label="Active"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={3}
                label="Description"
                value={formData.description || ''}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </Grid>
          </Grid>

          <Typography variant="h6" sx={{ mt: 3, mb: 2 }}>
            Policy Rules
          </Typography>

          {formData.rules?.map((rule, index) => (
            <Accordion key={index}>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Typography>{rule.ruleName || `Rule ${index + 1}`}</Typography>
              </AccordionSummary>
              <AccordionDetails>
                <Grid container spacing={2}>
                  <Grid item xs={12} md={4}>
                    <TextField
                      fullWidth
                      label="Rule Name"
                      value={rule.ruleName}
                      onChange={(e) => updateRule(index, 'ruleName', e.target.value)}
                      required
                    />
                  </Grid>
                  <Grid item xs={12} md={4}>
                    <TextField
                      fullWidth
                      label="Field"
                      value={rule.conditionField}
                      onChange={(e) => updateRule(index, 'conditionField', e.target.value)}
                      required
                    />
                  </Grid>
                  <Grid item xs={12} md={4}>
                    <FormControl fullWidth>
                      <InputLabel>Operator</InputLabel>
                      <Select
                        value={rule.operator}
                        onChange={(e) => updateRule(index, 'operator', e.target.value)}
                        label="Operator"
                      >
                        {operatorOptions.map(option => (
                          <MenuItem key={option.value} value={option.value}>
                            {option.label}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  </Grid>
                  <Grid item xs={12} md={4}>
                    <TextField
                      fullWidth
                      label="Value"
                      value={rule.conditionValue}
                      onChange={(e) => updateRule(index, 'conditionValue', e.target.value)}
                      required
                    />
                  </Grid>
                  <Grid item xs={12} md={4}>
                    <TextField
                      fullWidth
                      type="number"
                      label="Priority"
                      value={rule.priority || 100}
                      onChange={(e) => updateRule(index, 'priority', parseInt(e.target.value))}
                    />
                  </Grid>
                  <Grid item xs={12} md={4}>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={rule.isActive !== false}
                          onChange={(e) => updateRule(index, 'isActive', e.target.checked)}
                        />
                      }
                      label="Active"
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <TextField
                      fullWidth
                      label="Error Message"
                      value={rule.errorMessage || ''}
                      onChange={(e) => updateRule(index, 'errorMessage', e.target.value)}
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <Button
                      color="error"
                      startIcon={<RemoveIcon />}
                      onClick={() => removeRule(index)}
                    >
                      Remove Rule
                    </Button>
                  </Grid>
                </Grid>
              </AccordionDetails>
            </Accordion>
          ))}

          <Button
            variant="outlined"
            startIcon={<AddIcon />}
            onClick={addRule}
            sx={{ mt: 2 }}
          >
            Add Rule
          </Button>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setModalOpen(false)}>Cancel</Button>
          <Button onClick={handleCreateOrUpdate} variant="contained">
            {editingPolicy ? 'Update' : 'Create'} Policy
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default PolicyManagement;