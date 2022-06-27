ALTER TABLE sps_action ADD COLUMN IF NOT EXISTS cron_string TEXT;

CREATE INDEX IF NOT EXISTS sps_action_retry_idx ON sps_action(cron_string);
