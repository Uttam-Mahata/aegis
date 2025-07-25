-- Sample policies for UCO Bank
INSERT IGNORE INTO policies (client_id, policy_name, policy_type, enforcement_level, description, is_active, created_at, updated_at)
VALUES 
('UCO Bank', 'Transaction Limit Policy', 'TRANSACTION_LIMIT', 'BLOCK', 'Enforce transaction limits based on amount', true, NOW(), NOW()),
('UCO Bank', 'Device Security Policy', 'DEVICE_SECURITY', 'BLOCK', 'Ensure devices meet security requirements', true, NOW(), NOW()),
('UCO Bank', 'Geographic Restriction Policy', 'GEOGRAPHIC_RESTRICTION', 'WARN', 'Monitor transactions from restricted locations', true, NOW(), NOW());

-- Sample rules for Transaction Limit Policy
INSERT IGNORE INTO policy_rules (policy_id, rule_name, condition_field, operator, condition_value, error_message, priority, is_active, created_at, updated_at)
SELECT 
    p.id,
    'Daily Transfer Limit',
    'transaction.amount',
    'GREATER_THAN',
    '50000',
    'Transaction amount exceeds daily limit of Rs. 50,000',
    100,
    true,
    NOW(),
    NOW()
FROM policies p
WHERE p.client_id = 'UCO Bank' AND p.policy_name = 'Transaction Limit Policy';

INSERT IGNORE INTO policy_rules (policy_id, rule_name, condition_field, operator, condition_value, error_message, priority, is_active, created_at, updated_at)
SELECT 
    p.id,
    'Single Transaction Limit',
    'transaction.amount',
    'GREATER_THAN',
    '25000',
    'Single transaction exceeds limit of Rs. 25,000',
    90,
    true,
    NOW(),
    NOW()
FROM policies p
WHERE p.client_id = 'UCO Bank' AND p.policy_name = 'Transaction Limit Policy';

-- Sample rules for Device Security Policy
INSERT IGNORE INTO policy_rules (policy_id, rule_name, condition_field, operator, condition_value, error_message, priority, is_active, created_at, updated_at)
SELECT 
    p.id,
    'Minimum OS Version',
    'device.osVersion',
    'LESS_THAN',
    '12.0',
    'Device OS version is below minimum required version',
    100,
    true,
    NOW(),
    NOW()
FROM policies p
WHERE p.client_id = 'UCO Bank' AND p.policy_name = 'Device Security Policy';

INSERT IGNORE INTO policy_rules (policy_id, rule_name, condition_field, operator, condition_value, error_message, priority, is_active, created_at, updated_at)
SELECT 
    p.id,
    'App Version Check',
    'device.appVersion',
    'LESS_THAN',
    '2.0.0',
    'App version is outdated. Please update to latest version',
    90,
    true,
    NOW(),
    NOW()
FROM policies p
WHERE p.client_id = 'UCO Bank' AND p.policy_name = 'Device Security Policy';

-- Sample rules for Geographic Restriction Policy
INSERT IGNORE INTO policy_rules (policy_id, rule_name, condition_field, operator, condition_value, error_message, priority, is_active, created_at, updated_at)
SELECT 
    p.id,
    'Restricted Countries',
    'location.country',
    'IN',
    'KP,IR,SY',
    'Transactions from this country are restricted',
    100,
    true,
    NOW(),
    NOW()
FROM policies p
WHERE p.client_id = 'UCO Bank' AND p.policy_name = 'Geographic Restriction Policy';