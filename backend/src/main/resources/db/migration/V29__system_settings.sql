CREATE TABLE IF NOT EXISTS system_settings (
    setting_key   varchar(100) PRIMARY KEY,
    setting_value varchar(500) NOT NULL,
    description   varchar(500),
    updated_at    timestamp NOT NULL DEFAULT now()
);

INSERT INTO system_settings (setting_key, setting_value, description) VALUES
    ('commissionRate', '0.10', 'Platform commission rate on store sales'),
    ('returnPolicyDays', '14', 'Number of days allowed for product returns'),
    ('defaultCurrency', 'TRY', 'Default currency for orders'),
    ('taxRate', '0.20', 'KDV tax rate applied to orders'),
    ('freeShippingThreshold', '500', 'Minimum order amount for free shipping')
ON CONFLICT (setting_key) DO NOTHING;
