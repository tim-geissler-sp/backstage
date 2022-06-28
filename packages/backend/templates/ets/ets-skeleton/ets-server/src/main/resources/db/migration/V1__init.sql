CREATE TABLE event (
    id BIGSERIAL NOT NULL,
    event_json TEXT NOT NULL,
    topic TEXT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE invocation (
    id TEXT NOT NULL,
    created TIMESTAMP NOT NULL,
    context TEXT,
    tenant_id TEXT NOT NULL,
    trigger_id TEXT NOT NULL,
    deadline TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX invocation_deadline_idx ON invocation (deadline);

CREATE TABLE subscription (
    id TEXT NOT NULL,
    created TIMESTAMP,
    config TEXT,
    tenant_id TEXT NOT NULL,
    trigger_id TEXT NOT NULL,
    type TEXT NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX subscription_tenant_id_idx ON subscription (tenant_id);
CREATE INDEX subscription_tenant_id_trigger_id_idx ON subscription (tenant_id, trigger_id);
