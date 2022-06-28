CREATE TABLE identity_state (
    tenant_id TEXT NOT NULL,
    id TEXT NOT NULL,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    attributes TEXT,
    accounts TEXT,
    last_event_time TIMESTAMP,
    deleted BOOLEAN,
    expiration TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX identity_state_tenant_id_idx ON identity_state (tenant_id);
