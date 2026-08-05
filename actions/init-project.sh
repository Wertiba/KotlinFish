#!/usr/bin/env bash
# One-shot template bootstrap: renames the com.picoding.fish package (and everything
# derived from it - rootProject.name, group, Dockerfile jar name, docker-compose
# container/volume names, the logs/*.log path, and the project directory itself) to
# your own project's identity, and optionally creates the PostgreSQL database
# referenced by DB_URL.
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
    # DB_URL/DB_USER/DB_PASSWORD come from the environment, falling back to .env.
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

    if ! command -v psql &>/dev/null; then
        echo "psql not found on PATH - install the PostgreSQL client to use --create-db." >&2
        exit 1
    fi

    # DB_URL is a JDBC url: jdbc:postgresql://host[:port]/dbname[?params]
    local url="${DB_URL#jdbc:}"
    if [[ ! "$url" =~ ^postgresql://([^/:]+)(:([0-9]+))?/([A-Za-z0-9_]+) ]]; then
        echo "Could not parse DB_URL '$DB_URL' as jdbc:postgresql://host[:port]/dbname" >&2
        exit 1
    fi
    local host="${BASH_REMATCH[1]}"
    local port="${BASH_REMATCH[3]:-5432}"
    local dbname="${BASH_REMATCH[4]}"
    local user="${DB_USER:-postgres}"

    echo "Checking database '$dbname' on $host:$port..."
    export PGPASSWORD="${DB_PASSWORD:-}"
    local exists
    exists=$(psql -h "$host" -p "$port" -U "$user" -d postgres -tAc \
        "SELECT 1 FROM pg_database WHERE datname = '$dbname'")

    if [[ "$exists" == "1" ]]; then
        echo "Database '$dbname' already exists - nothing to do."
    else
        echo "Creating database '$dbname'..."
        psql -h "$host" -p "$port" -U "$user" -d postgres -c "CREATE DATABASE \"$dbname\""
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

# Clean up now-empty package directories left behind by the move (e.g. com/picoding).
find src/main/kotlin src/test/kotlin -depth -type d -empty -delete 2>/dev/null || true

# Rewrite every source/config file that references the old package.
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

# Rename the project directory itself to match. Best-effort: on Windows this can
# transiently fail right after heavy file churn (antivirus/indexer briefly holding a
# file open), or fail outright if a shell/IDE has the directory open as its current
# directory - so it's retried a few times, and a lasting failure is reported without
# aborting the script.
PARENT_DIR="$(dirname "$REPO_ROOT")"
CURRENT_DIR_NAME="$(basename "$REPO_ROOT")"
if [[ "$CURRENT_DIR_NAME" != "$NEW_NAME" ]]; then
    NEW_REPO_ROOT="$PARENT_DIR/$NEW_NAME"
    if [[ -e "$NEW_REPO_ROOT" ]]; then
        echo "Warning: '$NEW_REPO_ROOT' already exists - leaving the project directory name as-is." >&2
    else
        cd "$PARENT_DIR"
        MOVED=false
        for attempt in 1 2 3; do
            if mv "$CURRENT_DIR_NAME" "$NEW_NAME" 2>/dev/null; then
                MOVED=true
                break
            fi
            sleep 1
        done
        if $MOVED; then
            REPO_ROOT="$NEW_REPO_ROOT"
            cd "$REPO_ROOT"
            echo "Renamed project directory: $CURRENT_DIR_NAME -> $NEW_NAME"
        else
            cd "$CURRENT_DIR_NAME"
            echo "Warning: couldn't rename the project directory to '$NEW_NAME' - it's likely still open in this shell/IDE (common on Windows). Close whatever has it open, then run:" >&2
            echo "    cd .. && mv \"$CURRENT_DIR_NAME\" \"$NEW_NAME\"" >&2
        fi
    fi
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
