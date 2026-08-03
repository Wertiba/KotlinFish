#!/usr/bin/env bash
# One-shot template bootstrap: renames the com.picoding.fish package (and everything
# derived from it - rootProject.name, group, Dockerfile jar name, docker-compose
# container/volume names, the logs/*.log path) to your own project's identity.
#
# Usage:
#   ./actions/rename-project.sh <new.package.name> [project-name]
#
# Examples:
#   ./actions/rename-project.sh com.acme.orders
#   ./actions/rename-project.sh com.acme.orderservice orders
#
# Safe to run from anywhere; it cd's to the repo root on its own. Run it once, right
# after cloning the template, before you've made changes you care about - review the
# diff with `git status`/`git diff` afterwards. Delete this script (and its .ps1
# sibling) once you're happy with the result.
set -euo pipefail

OLD_PACKAGE="com.picoding.fish"
OLD_GROUP="com.picoding"
OLD_NAME="fish"

if [[ $# -lt 1 || $# -gt 2 ]]; then
    echo "Usage: $0 <new.package.name> [project-name]" >&2
    echo "Example: $0 com.acme.orders orders" >&2
    exit 1
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

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

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

echo "Done."
echo
echo "Next steps:"
echo "  - skim README.md - the prose (title, description) still talks about the old project"
echo "  - update .env / .env.example: admin credentials, JWT secret, DB name"
echo "  - git status / git diff to review everything that changed"
echo "  - delete actions/rename-project.sh and actions/rename-project.ps1 once you're happy"
