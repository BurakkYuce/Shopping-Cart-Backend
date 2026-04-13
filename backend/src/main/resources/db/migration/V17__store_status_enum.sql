UPDATE stores SET status = UPPER(status) WHERE status IS NOT NULL AND status = LOWER(status);
UPDATE stores SET status = 'ACTIVE' WHERE status IS NULL OR status = '';

ALTER TABLE stores
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN status SET DEFAULT 'PENDING_APPROVAL';

ALTER TABLE stores
    ALTER COLUMN status TYPE varchar(20);
