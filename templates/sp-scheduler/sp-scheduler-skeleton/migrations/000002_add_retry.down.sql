ALTER TABLE sps_action DROP COLUMN IF EXISTS retry;

DROP INDEX IF EXISTS sps_action_retry_idx;
