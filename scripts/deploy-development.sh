#!/usr/bin/env bash
set -Eeuo pipefail

readonly EXPECTED_INSTANCE_ID="i-0eb11976c412eb88b"
readonly EXPECTED_USER="ubuntu"
readonly APP_DIR="/home/ubuntu/aics-pr-test-app"
readonly INSTANCE_ID_FILE="/var/lib/cloud/data/instance-id"
readonly ECR_REGISTRY="792854191046.dkr.ecr.ap-northeast-2.amazonaws.com"
readonly EXPECTED_NAMESPACE="$ECR_REGISTRY/aics-pr-test"
readonly AWS_BIN="/usr/local/bin/aws"
readonly PROJECT_NAME="aics-pr-test"
readonly HEALTH_TIMEOUT=180
readonly SLEEP_SECONDS=2

usage() {
    echo "Usage: $0 [--check] FULL_HEAD_SHA $EXPECTED_NAMESPACE" >&2
}

check_only=false
if [[ ${1:-} == --check ]]; then
    check_only=true
    shift
fi
if [[ $# != 2 || ! $1 =~ ^[a-f0-9]{40}$ || $2 != "$EXPECTED_NAMESPACE" ]]; then
    usage
    exit 2
fi
readonly head_sha=$1
readonly namespace=$2

if [[ ! -r $INSTANCE_ID_FILE || $(<"$INSTANCE_ID_FILE") != "$EXPECTED_INSTANCE_ID" ]]; then
    echo "This command requires the dedicated AICS development EC2 instance." >&2
    exit 2
fi
if [[ $(id -un) != "$EXPECTED_USER" ]]; then
    echo "This command must run as $EXPECTED_USER." >&2
    exit 2
fi
if [[ ! -d $APP_DIR ]]; then
    echo "Development application directory is missing." >&2
    exit 2
fi
cd "$APP_DIR"

readonly override="preview-images.compose.yml"
readonly release_marker=".development-release"
readonly base_url="http://127.0.0.1:8088"
readonly -a apps=(aics-auth aics-admin aics-api)
readonly -a routes=(/auth-api-docs /admin-api-docs /v3/api-docs)
readonly -a compose_files=(
    docker-compose.yml
    first-boot.compose.yml
    preview-images.compose.yml
    ../aics-pr-test-db/redis.compose.yml
    ../aics-pr-test-db/database.compose.yml
    https.compose.yml
)

for required_file in app.env "${compose_files[@]}"; do
    if [[ ! -f $required_file ]]; then
        echo "Required development configuration is missing: $required_file" >&2
        exit 2
    fi
done

docker() { sudo -n docker "$@"; }
compose() {
    local -a args=(compose -p "$PROJECT_NAME" --env-file app.env)
    local file
    for file in "${compose_files[@]}"; do
        args+=(-f "$file")
    done
    docker "${args[@]}" "$@"
}

check_route() {
    local route=$1 expected=$2
    shift 2
    local status
    status=$(curl --silent --show-error --max-time 5 --output /dev/null \
        --write-out '%{http_code}' "$@" "$base_url$route") || return 1
    [[ $status == "$expected" ]]
}

wait_for_route() {
    local route=$1 expected=$2
    shift 2
    local deadline=$((SECONDS + HEALTH_TIMEOUT))
    until check_route "$route" "$expected" "$@"; do
        if (( SECONDS >= deadline )); then
            echo "Readiness timed out for $route (expected HTTP $expected)." >&2
            return 1
        fi
        sleep "$SLEEP_SECONDS"
    done
}

smoke_test() {
    local route
    for route in "${routes[@]}" /swagger-ui/index.html; do
        wait_for_route "$route" 200 || return 1
    done
    wait_for_route /pgadmin/ 404 || return 1
    wait_for_route /api/v1/auth/login 400 \
        -H 'Content-Type: application/json' --data '{}' || return 1
}

valid_rollback_image() {
    local image=$1 app=$2
    local prefix tag
    for prefix in hotan35 aics-pr-test "$EXPECTED_NAMESPACE"; do
        if [[ $image == "$prefix/$app:"* ]]; then
            tag=${image#"$prefix/$app:"}
            [[ $tag =~ ^(dev-)?[a-f0-9]{40}$ ]]
            return
        fi
    done
    return 1
}

write_override() {
    local temporary
    temporary=$(mktemp .development-images.XXXXXX)
    {
        echo "services:"
        local index
        for index in "${!apps[@]}"; do
            printf '  %s:\n    image: %s\n' "${apps[index]}" "${1}"
            shift
        done
    } >"$temporary"
    chmod 600 "$temporary"
    mv "$temporary" "$override"
}

start_apps() {
    local index
    for index in "${!apps[@]}"; do
        compose up -d --no-deps --pull never "${apps[index]}" || return 1
        wait_for_route "${routes[index]}" 200 || return 1
    done
}

if ! $check_only; then
    umask 077
    exec 9>.preview-deploy.lock
    flock -n 9 || { echo "A development deployment is already running." >&2; exit 1; }
fi

compose config --quiet
for route in "${routes[@]}"; do
    check_route "$route" 200 || { echo "Current development deployment is not healthy: $route" >&2; exit 1; }
done

candidate_images=()
previous_images=()
for app in "${apps[@]}"; do
    candidate_images+=("$namespace/$app:dev-$head_sha")
    current=$(docker inspect --format '{{.Config.Image}}' "$PROJECT_NAME-$app-1")
    if ! valid_rollback_image "$current" "$app"; then
        echo "Cannot identify a safe rollback image for $app." >&2
        exit 1
    fi
    previous_images+=("$current")
done

if $check_only; then
    echo "Development deployment preflight passed for $head_sha."
    exit 0
fi

registry_config_dir=""
cleanup_registry_credentials() {
    local directory=${registry_config_dir:-}
    [[ -n $directory ]] || return 0
    sudo -n rm -f "$directory/config.json"
    rmdir "$directory"
    registry_config_dir=""
}

# Keep ECR credentials in a one-use Docker config and remove them before any
# application is restarted. The normal Docker config is never read or changed.
registry_config_dir=$(mktemp -d .ecr-docker-config.XXXXXX)
chmod 700 "$registry_config_dir"
trap cleanup_registry_credentials EXIT
trap 'exit 1' INT TERM
"$AWS_BIN" ecr get-login-password --region ap-northeast-2 |
    sudo -n docker --config "$registry_config_dir" login \
        --username AWS --password-stdin "$ECR_REGISTRY" >/dev/null

# Pull every candidate before changing the compose override or restarting an app.
for image in "${candidate_images[@]}"; do
    timeout 180 sudo -n docker --config "$registry_config_dir" pull "$image"
done
cleanup_registry_credentials
trap - EXIT INT TERM

mutation_started=false

rollback() {
    local failed=false
    if ! write_override "${previous_images[@]}"; then
        echo "Could not restore the previous development image override." >&2
        return 1
    fi
    for index in "${!apps[@]}"; do
        if ! compose up -d --no-deps --pull never "${apps[index]}"; then
            failed=true
            continue
        fi
        wait_for_route "${routes[index]}" 200 || failed=true
    done
    smoke_test || failed=true
    if $failed; then
        echo "Development rollback verification failed; inspect the development host." >&2
        return 1
    fi
    echo "Previous development app images restored and checked." >&2
}

finish() {
    local result=$?
    trap - EXIT INT TERM
    if (( result != 0 )) && $mutation_started; then
        rollback || true
    fi
    exit "$result"
}
trap finish EXIT
trap 'exit 1' INT TERM

mutation_started=true
write_override "${candidate_images[@]}"
compose config --quiet
start_apps
smoke_test

marker=$(mktemp .development-release.XXXXXX)
printf 'head=%s\nverified_at=%s\n' "$head_sha" "$(date -u +%FT%TZ)" >"$marker"
mv "$marker" "$release_marker"
mutation_started=false
trap - EXIT INT TERM
printf 'Verified development release %s.\n' "$head_sha"
