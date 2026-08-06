#!/bin/sh
set -eu

[ -n "${DB_URL:-}" ] || exit 0

dbname="${DB_URL%%\?*}"
dbname="${dbname##*/}"

[ -n "$dbname" ] || exit 0
[ "$dbname" = "$POSTGRES_DB" ] && exit 0

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-SQL
    SELECT 'CREATE DATABASE "$dbname"'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$dbname')\gexec
SQL
