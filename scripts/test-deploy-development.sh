#!/usr/bin/env bash
set -Eeuo pipefail

readonly source_script="$(cd "$(dirname "$0")" && pwd)/deploy-development.sh"
readonly valid_sha="0123456789abcdef0123456789abcdef01234567"
readonly previous_sha="abcdef0123456789abcdef0123456789abcdef01"
readonly ecr_registry="792854191046.dkr.ecr.ap-northeast-2.amazonaws.com"
readonly namespace="$ecr_registry/aics-pr-test"
test_root=$(mktemp -d)
trap 'rm -rf "$test_root"' EXIT

fail() { echo "FAIL: $*" >&2; exit 1; }
assert_contains() { grep -Fq -- "$2" "$1" || fail "$1 does not contain: $2"; }
assert_not_contains() { ! grep -Fq -- "$2" "$1" || fail "$1 unexpectedly contains: $2"; }

prepare_fixture() {
    local name=$1
    fixture="$test_root/$name"
    app_dir="$fixture/app"
    bin_dir="$fixture/bin"
    mkdir -p "$app_dir" "$bin_dir" "$fixture/db"
    : >"$fixture/docker.log"
    : >"$fixture/timeout.log"
    printf 'test-instance\n' >"$fixture/instance-id"
    for file in app.env docker-compose.yml first-boot.compose.yml https.compose.yml; do
        : >"$app_dir/$file"
    done
    : >"$fixture/db/redis.compose.yml"
    : >"$fixture/db/database.compose.yml"
    cat >"$app_dir/preview-images.compose.yml" <<EOF
services:
  aics-auth:
    image: aics-pr-test/aics-auth:$previous_sha
  aics-admin:
    image: aics-pr-test/aics-admin:$previous_sha
  aics-api:
    image: aics-pr-test/aics-api:$previous_sha
EOF

    sed \
        -e 's|EXPECTED_INSTANCE_ID="i-0eb11976c412eb88b"|EXPECTED_INSTANCE_ID="test-instance"|' \
        -e "s|EXPECTED_USER=\"ubuntu\"|EXPECTED_USER=\"$(id -un)\"|" \
        -e "s|APP_DIR=\"/home/ubuntu/aics-pr-test-app\"|APP_DIR=\"$app_dir\"|" \
        -e "s|INSTANCE_ID_FILE=\"/var/lib/cloud/data/instance-id\"|INSTANCE_ID_FILE=\"$fixture/instance-id\"|" \
        -e 's|HEALTH_TIMEOUT=180|HEALTH_TIMEOUT=1|' \
        -e 's|SLEEP_SECONDS=2|SLEEP_SECONDS=0|' \
        -e "s|AWS_BIN=\"/usr/local/bin/aws\"|AWS_BIN=\"$bin_dir/aws\"|" \
        -e "s|../aics-pr-test-db/|$fixture/db/|g" \
        "$source_script" >"$fixture/deploy.sh"
    chmod +x "$fixture/deploy.sh"

    cat >"$bin_dir/sudo" <<'EOF'
#!/usr/bin/env bash
[[ ${1:-} == -n ]] && shift
exec "$@"
EOF
    cat >"$bin_dir/docker" <<'EOF'
#!/usr/bin/env bash
set -eu
printf '%s\n' "$*" >>"$MOCK_DOCKER_LOG"
docker_config=""
if [[ ${1:-} == --config ]]; then
    docker_config=$2
    shift 2
fi
command=${1:-}
if [[ $command == login ]]; then
    cat >/dev/null
    printf '{"auths":{}}\n' >"$docker_config/config.json"
    [[ ${MOCK_AUTH_FAIL:-0} != 1 ]] || exit 1
    exit 0
fi
if [[ $command == inspect ]]; then
    container=${*: -1}
    app=${container#aics-pr-test-}
    app=${app%-1}
    printf '%s/%s:%s\n' "${MOCK_PREVIOUS_PREFIX:-aics-pr-test}" "$app" "$MOCK_PREVIOUS_SHA"
    exit 0
fi
if [[ $command == pull && ${MOCK_PULL_FAIL:-0} == 1 ]]; then
    exit 1
fi
if [[ $command == compose && " $* " == *" up "* && ${MOCK_CANDIDATE_UP_FAIL:-0} == 1 ]]; then
    if grep -q "dev-$MOCK_VALID_SHA" "$MOCK_APP_DIR/preview-images.compose.yml"; then
        exit 1
    fi
fi
exit 0
EOF
    cat >"$bin_dir/aws" <<'EOF'
#!/usr/bin/env bash
[[ $* == 'ecr get-login-password --region ap-northeast-2' ]] || exit 2
printf 'test-token\n'
EOF
    cat >"$bin_dir/curl" <<'EOF'
#!/usr/bin/env bash
if [[ ${MOCK_COLD_ROUTE:-0} == 1 && $* == *'/swagger-ui/index.html'* && ! -e "$MOCK_APP_DIR/.cold-route-seen" ]]; then
    touch "$MOCK_APP_DIR/.cold-route-seen"
    exit 28
fi
case "$*" in
  *'/pgadmin/'*) printf 404 ;;
  *'/api/v1/auth/login'*) printf 400 ;;
  *) printf 200 ;;
esac
EOF
    cat >"$bin_dir/flock" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF
    cat >"$bin_dir/timeout" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$*" >>"$MOCK_TIMEOUT_LOG"
shift
exec "$@"
EOF
    chmod +x "$bin_dir"/*
    test_env=(
        "PATH=$bin_dir:$PATH"
        "MOCK_DOCKER_LOG=$fixture/docker.log"
        "MOCK_TIMEOUT_LOG=$fixture/timeout.log"
        "MOCK_PREVIOUS_SHA=$previous_sha"
        "MOCK_VALID_SHA=$valid_sha"
        "MOCK_APP_DIR=$app_dir"
    )
}

prepare_fixture invalid
if env "${test_env[@]}" "$fixture/deploy.sh" bad "$namespace" >/dev/null 2>&1; then
    fail "invalid SHA was accepted"
fi
[[ ! -s $fixture/docker.log ]] || fail "invalid args invoked docker"

prepare_fixture invalid-namespace
if env "${test_env[@]}" "$fixture/deploy.sh" "$valid_sha" other >/dev/null 2>&1; then
    fail "invalid namespace was accepted"
fi
[[ ! -s $fixture/docker.log ]] || fail "invalid namespace invoked docker"

prepare_fixture wrong-instance
printf 'wrong-instance\n' >"$fixture/instance-id"
if env "${test_env[@]}" "$fixture/deploy.sh" "$valid_sha" "$namespace" >/dev/null 2>&1; then
    fail "wrong instance was accepted"
fi
[[ ! -s $fixture/docker.log ]] || fail "wrong instance invoked docker"

prepare_fixture check
before=$(shasum "$app_dir/preview-images.compose.yml")
env "${test_env[@]}" "$fixture/deploy.sh" --check "$valid_sha" "$namespace" >/dev/null
after=$(shasum "$app_dir/preview-images.compose.yml")
[[ $before == "$after" ]] || fail "check mode modified override"
assert_not_contains "$fixture/docker.log" "pull "
assert_not_contains "$fixture/docker.log" " up "
assert_not_contains "$fixture/docker.log" " login "
[[ ! -e $app_dir/.preview-deploy.lock ]] || fail "check mode wrote a lock file"
[[ ! -e $app_dir/.development-release ]] || fail "check mode wrote a release marker"

prepare_fixture check-ecr-rollback
env "${test_env[@]}" MOCK_PREVIOUS_PREFIX="$namespace" \
    "$fixture/deploy.sh" --check "$valid_sha" "$namespace" >/dev/null
assert_not_contains "$fixture/docker.log" " login "

prepare_fixture pull-failure
before=$(shasum "$app_dir/preview-images.compose.yml")
if env "${test_env[@]}" MOCK_PULL_FAIL=1 "$fixture/deploy.sh" "$valid_sha" "$namespace" >/dev/null 2>&1; then
    fail "pull failure returned success"
fi
after=$(shasum "$app_dir/preview-images.compose.yml")
[[ $before == "$after" ]] || fail "pull failure modified override"
assert_not_contains "$fixture/docker.log" " up "
[[ -z $(find "$app_dir" -maxdepth 1 -name '.ecr-docker-config.*' -print -quit) ]] ||
    fail "pull failure left registry credentials"

prepare_fixture auth-failure
before=$(shasum "$app_dir/preview-images.compose.yml")
if env "${test_env[@]}" MOCK_AUTH_FAIL=1 "$fixture/deploy.sh" "$valid_sha" "$namespace" >/dev/null 2>&1; then
    fail "authentication failure returned success"
fi
after=$(shasum "$app_dir/preview-images.compose.yml")
[[ $before == "$after" ]] || fail "authentication failure modified override"
assert_not_contains "$fixture/docker.log" " up "
[[ -z $(find "$app_dir" -maxdepth 1 -name '.ecr-docker-config.*' -print -quit) ]] ||
    fail "authentication failure left registry credentials"

prepare_fixture rollback
before=$(shasum "$app_dir/preview-images.compose.yml")
if env "${test_env[@]}" MOCK_CANDIDATE_UP_FAIL=1 "$fixture/deploy.sh" "$valid_sha" "$namespace" >/dev/null 2>&1; then
    fail "candidate start failure returned success"
fi
after=$(shasum "$app_dir/preview-images.compose.yml")
[[ $before == "$after" ]] || fail "rollback did not restore override"
assert_contains "$fixture/docker.log" "up -d --no-deps --pull never aics-auth"
[[ ! -e $app_dir/.development-release ]] || fail "failed deployment wrote a release marker"

prepare_fixture success
env "${test_env[@]}" "$fixture/deploy.sh" "$valid_sha" "$namespace" >/dev/null
assert_contains "$app_dir/preview-images.compose.yml" "$namespace/aics-auth:dev-$valid_sha"
assert_contains "$app_dir/preview-images.compose.yml" "$namespace/aics-admin:dev-$valid_sha"
assert_contains "$app_dir/preview-images.compose.yml" "$namespace/aics-api:dev-$valid_sha"
assert_contains "$app_dir/.development-release" "head=$valid_sha"
assert_contains "$fixture/docker.log" \
    "login --username AWS --password-stdin $ecr_registry"
(( $(wc -l <"$fixture/timeout.log") == 3 )) || fail "each candidate pull was not timeout-bounded"
assert_contains "$fixture/timeout.log" "180 sudo -n docker --config"
assert_contains "$fixture/timeout.log" "pull $namespace/aics-auth:dev-$valid_sha"
assert_contains "$fixture/timeout.log" "pull $namespace/aics-admin:dev-$valid_sha"
assert_contains "$fixture/timeout.log" "pull $namespace/aics-api:dev-$valid_sha"
last_pull=$(grep -n ' pull ' "$fixture/docker.log" | tail -1 | cut -d: -f1)
first_up=$(grep -n ' up -d --no-deps --pull never ' "$fixture/docker.log" | head -1 | cut -d: -f1)
(( last_pull < first_up )) || fail "an app was restarted before every candidate image was pulled"
assert_not_contains "$fixture/docker.log" "up -d --no-deps --pull never postgres"
assert_not_contains "$fixture/docker.log" "up -d --no-deps --pull never redis"
assert_not_contains "$fixture/docker.log" "up -d --no-deps --pull never caddy"
[[ -z $(find "$app_dir" -maxdepth 1 -name '.ecr-docker-config.*' -print -quit) ]] ||
    fail "successful deployment left registry credentials"

echo "All development deployment script tests passed."

prepare_fixture cold-route
env "${test_env[@]}" MOCK_COLD_ROUTE=1 "$fixture/deploy.sh" "$valid_sha" "$namespace" >/dev/null
[[ -f $app_dir/.development-release ]] || fail "transient cold route caused rollback"
echo "Cold-start smoke retry passed."
