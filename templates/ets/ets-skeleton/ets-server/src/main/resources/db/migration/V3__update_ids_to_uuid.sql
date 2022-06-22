ALTER TABLE invocation ALTER COLUMN id TYPE UUID using id::uuid;
ALTER TABLE subscription ALTER COLUMN id TYPE UUID using id::uuid;
