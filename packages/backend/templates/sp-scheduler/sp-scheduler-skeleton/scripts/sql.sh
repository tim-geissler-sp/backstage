#!/usr/bin/env sh

docker exec -it -e PGPASSWORD=2thecloud scripts_postgres_1 psql -h localhost -p 5432 -U postgres


