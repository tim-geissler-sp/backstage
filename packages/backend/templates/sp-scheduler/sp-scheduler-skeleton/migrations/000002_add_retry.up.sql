ALTER TABLE sps_action ADD COLUMN IF NOT EXISTS retry INTEGER DEFAULT 0 NOT NULL;

CREATE INDEX IF NOT EXISTS sps_action_retry_idx ON sps_action(retry);
