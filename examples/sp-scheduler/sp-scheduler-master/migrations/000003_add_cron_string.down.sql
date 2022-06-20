ALTER TABLE sps_action DROP COLUMN IF EXISTS cron_string;

DROP INDEX IF EXISTS sps_action_retry_idx;
