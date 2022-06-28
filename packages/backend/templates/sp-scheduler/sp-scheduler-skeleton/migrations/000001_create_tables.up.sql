CREATE TABLE IF NOT EXISTS sps_action(
    id UUID NOT NULL,
    created TIMESTAMP NOT NULL,
    deadline TIMESTAMP NOT NULL,
    tenant_id TEXT NOT NULL,
    event_topic TEXT NOT NULL,
    event_type TEXT NOT NULL,
    event_header_json TEXT NOT NULL,
    event_content_json TEXT NOT NULL,
    meta JSONB,  
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS sps_action_deadline_idx ON sps_action(deadline);
CREATE INDEX IF NOT EXISTS sps_action_tenant_id_idx ON sps_action(tenant_id);
CREATE INDEX IF NOT EXISTS sps_action_meta_idx ON sps_action USING gin(meta);