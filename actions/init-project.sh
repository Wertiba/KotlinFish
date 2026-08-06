#!/usr/bin/env bash
# One-shot template bootstrap: renames the com.picoding.fish package (and everything
# derived from it - rootProject.name, group, Dockerfile jar name, docker-compose
# container/volume names, the logs/*.log path) to your own project's identity, and
# optionally creates the PostgreSQL database referenced by DB_URL.
#
# Usage:
#   ./actions/init-project.sh [--create-db] [<new.package.name> [project-name]]
#
# Examples:
#   ./actions/init-project.sh com.acme.orders
#   ./actions/init-project.sh com.acme.orderservice orders
#   ./actions/init-project.sh --create-db com.acme.orders
#   ./actions/init-project.sh --create-db   # only create the database, skip renaming
#
# Safe to run from anywhere; it cd's to the repo root on its own. Run the rename part
# once, right after cloning the template, before you've made changes you care about -
# review the diff with `git status`/`git diff` afterwards. Delete this script (and its
# .ps1 sibling) once you're happy with the result.
set -euo pipefail

OLD_PACKAGE="com.picoding.fish"
OLD_GROUP="com.picoding"
OLD_NAME="fish"

CREATE_DB=false
ARGS=()
for arg in "$@"; do
    case "$arg" in
        --create-db)
            CREATE_DB=true
            ;;
        -h|--help)
            echo "Usage: $0 [--create-db] [<new.package.name> [project-name]]" >&2
            exit 0
            ;;
        *)
            ARGS+=("$arg")
            ;;
    esac
done
set -- "${ARGS[@]+"${ARGS[@]}"}"

if [[ $# -gt 2 ]]; then
    echo "Usage: $0 [--create-db] [<new.package.name> [project-name]]" >&2
    echo "Example: $0 com.acme.orders orders" >&2
    exit 1
fi

if [[ $# -eq 0 && "$CREATE_DB" == false ]]; then
    echo "Usage: $0 [--create-db] [<new.package.name> [project-name]]" >&2
    echo "Example: $0 com.acme.orders orders" >&2
    exit 1
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

create_database() {
    if [[ -z "${DB_URL:-}" && -f "$REPO_ROOT/.env" ]]; then
        set -a
        # shellcheck disable=SC1091
        source "$REPO_ROOT/.env"
        set +a
    fi

    if [[ -z "${DB_URL:-}" ]]; then
        echo "DB_URL is not set (checked the environment and .env) - cannot create database." >&2
        exit 1
    fi

    local url="${DB_URL#jdbc:}"
    if [[ ! "$url" =~ ^postgresql://([^/:]+)(:([0-9]+))?/([A-Za-z0-9_]+) ]]; then
        echo "Could not parse DB_URL '$DB_URL' as jdbc:postgresql://host[:port]/dbname" >&2
        exit 1
    fi
    local host="${BASH_REMATCH[1]}"
    local port="${BASH_REMATCH[3]:-5432}"
    local dbname="${BASH_REMATCH[4]}"
    local user="${DB_USER:-postgres}"
    export PGPASSWORD="${DB_PASSWORD:-}"

    local -a psql_cmd
    if command -v psql &>/dev/null; then
        psql_cmd=(psql -h "$host" -p "$port" -U "$user")
    elif command -v docker &>/dev/null; then
        local docker_host="$host"
        local -a docker_args=(run --rm -e PGPASSWORD)
        if [[ "$host" == "localhost" || "$host" == "127.0.0.1" ]]; then
            docker_host="host.docker.internal"
            docker_args+=(--add-host "host.docker.internal:host-gateway")
        fi
        echo "psql not found on PATH - running it via a throwaway postgres:16-alpine container instead."
        psql_cmd=(docker "${docker_args[@]}" postgres:16-alpine psql -h "$docker_host" -p "$port" -U "$user")
    else
        echo "Neither psql nor docker found on PATH - install the PostgreSQL client (or Docker) to use --create-db." >&2
        exit 1
    fi

    echo "Checking database '$dbname' on $host:$port..."
    local exists
    exists=$("${psql_cmd[@]}" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$dbname'")

    if [[ "$exists" == "1" ]]; then
        echo "Database '$dbname' already exists - nothing to do."
    else
        echo "Creating database '$dbname'..."
        "${psql_cmd[@]}" -d postgres -c "CREATE DATABASE \"$dbname\""
        echo "Database '$dbname' created."
    fi
}

if [[ $# -eq 0 ]]; then
    create_database
    exit 0
fi

NEW_PACKAGE="$1"
NEW_NAME="${2:-${NEW_PACKAGE##*.}}"
NEW_GROUP="${NEW_PACKAGE%.*}"

if [[ "$NEW_PACKAGE" == "$NEW_GROUP" ]]; then
    echo "New package must have at least two segments, e.g. com.acme.orders" >&2
    exit 1
fi

if [[ ! "$NEW_PACKAGE" =~ ^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)+$ ]]; then
    echo "Package must be lowercase, dot-separated identifiers, e.g. com.acme.orders" >&2
    exit 1
fi

OLD_DOTS_ESCAPED="${OLD_PACKAGE//./\\.}"
OLD_PATH="${OLD_PACKAGE//./\/}"
NEW_PATH="${NEW_PACKAGE//./\/}"

MAIN_OLD="src/main/kotlin/$OLD_PATH"
TEST_OLD="src/test/kotlin/$OLD_PATH"

if [[ ! -d "$MAIN_OLD" ]]; then
    echo "Expected package directory '$MAIN_OLD' not found - has this already been renamed?" >&2
    exit 1
fi

IN_GIT=false
if git rev-parse --is-inside-work-tree &>/dev/null; then
    IN_GIT=true
fi

echo "Renaming package: $OLD_PACKAGE -> $NEW_PACKAGE"
echo "Renaming project: name '$OLD_NAME' -> '$NEW_NAME', group '$OLD_GROUP' -> '$NEW_GROUP'"
echo

move_tree() {
    local old_dir=$1 new_dir=$2
    [[ -d "$old_dir" ]] || return 0
    mkdir -p "$(dirname "$new_dir")"
    if $IN_GIT && git ls-files --error-unmatch "$old_dir" &>/dev/null; then
        git mv "$old_dir" "$new_dir"
    else
        mv "$old_dir" "$new_dir"
    fi
}

move_tree "$MAIN_OLD" "src/main/kotlin/$NEW_PATH"
move_tree "$TEST_OLD" "src/test/kotlin/$NEW_PATH"

find src/main/kotlin src/test/kotlin -depth -type d -empty -delete 2>/dev/null || true

mapfile -t FILES < <(grep -rlF "$OLD_PACKAGE" \
    --include="*.kt" --include="*.kts" \
    src build.gradle.kts settings.gradle.kts Dockerfile README.md 2>/dev/null || true)

for f in "${FILES[@]}"; do
    [[ -f "$f" ]] || continue
    sed -i.bak "s|$OLD_DOTS_ESCAPED|$NEW_PACKAGE|g" "$f"
    rm -f "$f.bak"
done

sed -i.bak "s|rootProject\\.name = \"$OLD_NAME\"|rootProject.name = \"$NEW_NAME\"|" settings.gradle.kts
rm -f settings.gradle.kts.bak

sed -i.bak "s|group = \"$OLD_GROUP\"|group = \"$NEW_GROUP\"|" build.gradle.kts
rm -f build.gradle.kts.bak

VERSION=$(grep -oE 'version = "[^"]+"' build.gradle.kts | head -1 | sed -E 's/version = "(.*)"/\1/')
if [[ -n "$VERSION" ]]; then
    sed -i.bak "s|$OLD_NAME-$VERSION\\.jar|$NEW_NAME-$VERSION.jar|" Dockerfile
    rm -f Dockerfile.bak
fi

sed -i.bak "s|logs/$OLD_NAME\\.log|logs/$NEW_NAME.log|g" src/main/resources/application.yaml
rm -f src/main/resources/application.yaml.bak
if [[ -f README.md ]]; then
    sed -i.bak "s|logs/$OLD_NAME\\.log|logs/$NEW_NAME.log|g" README.md
    rm -f README.md.bak
fi

if [[ -f docker-compose.yml ]]; then
    sed -i.bak \
        -e "s|container_name: kotlinapp|container_name: $NEW_NAME|" \
        -e "s|container_name: db|container_name: $NEW_NAME-db|" \
        -e "s|pgdata:|${NEW_NAME}_pgdata:|g" \
        -e "s|applogs:|${NEW_NAME}_applogs:|g" \
        docker-compose.yml
    rm -f docker-compose.yml.bak
fi

echo "Done."
echo

if [[ "$CREATE_DB" == true ]]; then
    create_database
    echo
fi

echo "Next steps:"
echo "  - skim README.md - the prose (title, description) still talks about the old project"
echo "  - update .env / .env.example: admin credentials, JWT secret, DB name"
echo "  - git status / git diff to review everything that changed"
echo "  - delete actions/init-project.sh and actions/init-project.ps1 once you're happy"
