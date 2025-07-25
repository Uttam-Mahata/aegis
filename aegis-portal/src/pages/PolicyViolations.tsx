import React, { useState } from 'react';
import { 
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Grid,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Alert,
  CircularProgress,
  Collapse,
  IconButton,
} from '@mui/material';
import { 
  Search as SearchIcon, 
  Warning as WarningIcon, 
  Security as SecurityIcon,
  KeyboardArrowDown as KeyboardArrowDownIcon,
  KeyboardArrowUp as KeyboardArrowUpIcon,
} from '@mui/icons-material';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import { policyService } from '../services/api';
import { type PolicyViolation } from '../types';
import { format } from 'date-fns';

const PolicyViolations: React.FC = () => {
  const [violations, setViolations] = useState<PolicyViolation[]>([]);
  const [loading, setLoading] = useState(false);
  const [deviceId, setDeviceId] = useState('');
  const [startDate, setStartDate] = useState<Date | null>(new Date(Date.now() - 7 * 24 * 60 * 60 * 1000));
  const [endDate, setEndDate] = useState<Date | null>(new Date());
  const [error, setError] = useState('');
  const [expandedRows, setExpandedRows] = useState<Set<number>>(new Set());

  const fetchViolations = async () => {
    if (!deviceId) {
      setError('Please enter a device ID');
      return;
    }

    if (!startDate || !endDate) {
      setError('Please select date range');
      return;
    }

    setLoading(true);
    setError('');
    try {
      const data = await policyService.getViolationHistory(
        deviceId,
        startDate.toISOString(),
        endDate.toISOString()
      );
      setViolations(data);
    } catch (error) {
      setError('Failed to fetch policy violations');
    } finally {
      setLoading(false);
    }
  };

  const getActionColor = (action: string): any => {
    switch (action) {
      case 'BLOCKED':
        return 'error';
      case 'MFA_REQUIRED':
        return 'warning';
      case 'WARNED':
        return 'warning';
      case 'NOTIFIED':
        return 'info';
      case 'MONITORED':
        return 'success';
      default:
        return 'default';
    }
  };

  const toggleRowExpanded = (id: number) => {
    const newExpanded = new Set(expandedRows);
    if (newExpanded.has(id)) {
      newExpanded.delete(id);
    } else {
      newExpanded.add(id);
    }
    setExpandedRows(newExpanded);
  };

  const violationStats = {
    total: violations.length,
    blocked: violations.filter(v => v.actionTaken === 'BLOCKED').length,
    mfaRequired: violations.filter(v => v.actionTaken === 'MFA_REQUIRED').length,
    warned: violations.filter(v => v.actionTaken === 'WARNED').length,
  };

  const StatCard: React.FC<{ title: string; value: number; color?: string }> = 
    ({ title, value, color }) => (
      <Card>
        <CardContent>
          <Typography color="textSecondary" gutterBottom variant="body2">
            {title}
          </Typography>
          <Typography 
            variant="h4" 
            component="div" 
            fontWeight="bold"
            sx={{ color: color || 'text.primary' }}
          >
            {value}
          </Typography>
        </CardContent>
      </Card>
    );

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight="bold" gutterBottom>
          Policy Violations Monitor
        </Typography>
        <Typography variant="body1" color="textSecondary">
          Track and analyze policy violations across devices
        </Typography>
      </Box>

      <Alert 
        severity="info" 
        icon={<WarningIcon />}
        sx={{ mb: 3 }}
      >
        This dashboard shows all policy violations detected by the Aegis Security Engine. 
        Use it to identify security threats and compliance issues.
      </Alert>

      {error && (
        <Alert severity="error" onClose={() => setError('')} sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard 
            title="Total Violations" 
            value={violationStats.total}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard 
            title="Blocked Requests" 
            value={violationStats.blocked}
            color="#f44336"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard 
            title="MFA Required" 
            value={violationStats.mfaRequired}
            color="#ff9800"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard 
            title="Warnings Issued" 
            value={violationStats.warned}
            color="#ffc107"
          />
        </Grid>
      </Grid>

      <Paper sx={{ p: 3, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} md={3}>
            <TextField
              fullWidth
              placeholder="Enter Device ID"
              value={deviceId}
              onChange={(e) => setDeviceId(e.target.value)}
              InputProps={{
                startAdornment: <SearchIcon sx={{ mr: 1, color: 'action.active' }} />,
              }}
            />
          </Grid>
          <Grid item xs={12} md={3}>
            <LocalizationProvider dateAdapter={AdapterDateFns}>
              <DateTimePicker
                label="Start Date"
                value={startDate}
                onChange={(newValue) => setStartDate(newValue)}
                slotProps={{ textField: { fullWidth: true } }}
              />
            </LocalizationProvider>
          </Grid>
          <Grid item xs={12} md={3}>
            <LocalizationProvider dateAdapter={AdapterDateFns}>
              <DateTimePicker
                label="End Date"
                value={endDate}
                onChange={(newValue) => setEndDate(newValue)}
                slotProps={{ textField: { fullWidth: true } }}
              />
            </LocalizationProvider>
          </Grid>
          <Grid item xs={12} md={3}>
            <Button 
              fullWidth
              variant="contained"
              onClick={fetchViolations}
              disabled={loading}
              startIcon={loading ? <CircularProgress size={20} /> : <SearchIcon />}
            >
              Search Violations
            </Button>
          </Grid>
        </Grid>
      </Paper>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell width={40} />
              <TableCell>Timestamp</TableCell>
              <TableCell>Policy</TableCell>
              <TableCell>Rule Violated</TableCell>
              <TableCell>Action Taken</TableCell>
              <TableCell>IP Address</TableCell>
              <TableCell>Details</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {violations.map((violation) => (
              <React.Fragment key={violation.id}>
                <TableRow>
                  <TableCell>
                    <IconButton
                      size="small"
                      onClick={() => toggleRowExpanded(violation.id)}
                    >
                      {expandedRows.has(violation.id) ? 
                        <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />
                      }
                    </IconButton>
                  </TableCell>
                  <TableCell>
                    {format(new Date(violation.createdAt), 'yyyy-MM-dd HH:mm:ss')}
                  </TableCell>
                  <TableCell>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <SecurityIcon fontSize="small" />
                      <Typography fontWeight="bold">
                        {violation.policy.policyName}
                      </Typography>
                    </Box>
                  </TableCell>
                  <TableCell>
                    {violation.violatedRule?.ruleName || 'N/A'}
                  </TableCell>
                  <TableCell>
                    <Chip 
                      label={violation.actionTaken.replace('_', ' ')}
                      color={getActionColor(violation.actionTaken)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{violation.ipAddress || 'N/A'}</TableCell>
                  <TableCell>
                    <Typography 
                      variant="body2" 
                      sx={{ 
                        maxWidth: 300, 
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap'
                      }}
                    >
                      {violation.violationDetails}
                    </Typography>
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={7}>
                    <Collapse in={expandedRows.has(violation.id)} timeout="auto" unmountOnExit>
                      <Box sx={{ margin: 2 }}>
                        <Grid container spacing={2}>
                          <Grid item xs={12} md={6}>
                            <Typography variant="h6" gutterBottom>
                              Request Details
                            </Typography>
                            <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
                              <Typography 
                                variant="body2" 
                                component="pre"
                                sx={{ 
                                  overflow: 'auto',
                                  fontSize: '0.875rem'
                                }}
                              >
                                {violation.requestDetails || 'N/A'}
                              </Typography>
                            </Paper>
                          </Grid>
                          <Grid item xs={12} md={6}>
                            <Typography variant="h6" gutterBottom>
                              Additional Information
                            </Typography>
                            <Typography variant="body2" gutterBottom>
                              <strong>User Agent:</strong> {violation.userAgent || 'N/A'}
                            </Typography>
                            <Typography variant="body2" gutterBottom>
                              <strong>Policy Type:</strong>{' '}
                              <Chip 
                                label={violation.policy.policyType} 
                                color="primary" 
                                size="small"
                              />
                            </Typography>
                            <Typography variant="body2">
                              <strong>Enforcement Level:</strong>{' '}
                              <Chip 
                                label={violation.policy.enforcementLevel} 
                                color="warning" 
                                size="small"
                              />
                            </Typography>
                          </Grid>
                        </Grid>
                      </Box>
                    </Collapse>
                  </TableCell>
                </TableRow>
              </React.Fragment>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {violations.length === 0 && !loading && (
        <Box sx={{ textAlign: 'center', py: 4 }}>
          <Typography variant="body1" color="textSecondary">
            No violations found for the selected criteria
          </Typography>
        </Box>
      )}
    </Box>
  );
};

export default PolicyViolations;