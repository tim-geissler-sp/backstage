ALTER TABLE sps_action ADD COLUMN IF NOT EXISTS timezone_location text;
ALTER TABLE sps_action ADD COLUMN IF NOT EXISTS timezone_offset text;

