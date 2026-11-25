#!/usr/bin/env bash
# Docker Hub 공개 저장소에서 "태그가 지금 가리키는 이미지"의 digest 를 로그인 없이 조회해 출력한다.
# docker pull 이 받아 올 대상과 같은 값(레지스트리가 돌려주는 Docker-Content-Digest)이라, 태그가 다른 이미지로 덮어써졌는지
# 판단하는 기준으로 쓴다. 사용: registry-digest.sh <사용자명>/<저장소> <태그>
set -euo pipefail

repo=${1:?사용법: registry-digest.sh <사용자명>/<저장소> <태그>}
tag=${2:?사용법: registry-digest.sh <사용자명>/<저장소> <태그>}

token=$(curl -fsS -m 20 "https://auth.docker.io/token?service=registry.docker.io&scope=repository:${repo}:pull" \
  | python3 -c 'import sys, json; print(json.load(sys.stdin)["token"])')

accept="application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json"
accept="${accept}, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json"

if ! headers=$(curl -fsSI -m 20 -H "Authorization: Bearer ${token}" -H "Accept: ${accept}" \
  "https://registry-1.docker.io/v2/${repo}/manifests/${tag}" 2>/dev/null); then
  echo "digest 를 조회하지 못했습니다(태그가 없거나 저장소에 접근할 수 없음): ${repo}:${tag}" >&2
  exit 1
fi

digest=$(printf '%s' "${headers}" | tr -d '\r' | awk -F': ' 'tolower($1) == "docker-content-digest" {print $2}')
if [ -z "${digest}" ]; then
  echo "응답에 digest 가 없습니다: ${repo}:${tag}" >&2
  exit 1
fi
echo "${digest}"
