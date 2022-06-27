ALTER TABLE subscription
ADD COLUMN response_dead_line TEXT DEFAULT 'PT1H';
