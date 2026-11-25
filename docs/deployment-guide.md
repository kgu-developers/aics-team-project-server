# 배포 가이드

개발 서버와 운영 서버에 배포하는 방법과, 새 서버를 연결하는 방법을 정리한 문서다.
배포는 모두 GitHub Actions가 하고, 서버에는 SSH로 접속해서 Docker Compose로 올린다.

## 1. 한눈에 보기

```mermaid
flowchart LR
  A[PR을 develop에 머지] --> B[빌드: 이미지 3개를 Docker Hub에 커밋 sha 태그로 업로드]
  B --> C[개발 서버에 자동 배포]
  C --> D{개발 서버에서 확인}
  D -->|문제 없음| E[Actions: Promote to Production 실행]
  E --> F[검증 후 승인자 승인]
  F --> G[운영 서버에 같은 이미지 배포]
  G --> H[main 이동 + 태그]
```

- **개발 서버**: `develop`에 머지하면 **자동**으로 배포된다.
- **운영 서버**: 사람이 **직접** 승격한다. 개발 서버에서 검증한 것과 **같은 이미지**(같은 커밋 sha)를 올리고, 다시 빌드하지 않는다.
- `main` 브랜치는 "운영에 승격한 **가장 앞선** 커밋"을 가리킨다. **현재 운영 상태를 뜻하지는 않는다**(과거 커밋으로 롤백해도 `main`은 뒤로 가지 않는다). 무엇이 언제 배포됐는지는 `prod-날짜-sha` 태그가 기록하고, 현재 운영에 올라가 있는 커밋은 가장 최근의 태그로 확인한다(8장).

이 문서가 다루는 것은 **API 서버**(`team-project-api.kgudevelopers.monster`, 개발은 `api.dev.kgudevelopers.monster`)다. 프론트엔드(`team-project.kgudevelopers.monster`)는 Cloudflare Pages로 서버와 따로 배포되며 이 문서와 무관하다. 프론트 주소는 서버 설정에서 CORS 허용 주소로만 등장한다(4장).

## 2. 환경과 워크플로

| 구분 | 이름 | 역할 |
|---|---|---|
| GitHub Environment | `cloud-dev` | 개발 서버 접속 정보. 이미지 빌드 job도 이 환경을 쓴다 |
| GitHub Environment | `cloud-prod` | 운영 서버 접속 정보. 승인자가 있다 |
| 워크플로 | `cloud-dev-build-deploy.yml` | `develop` push마다 빌드하고 개발 서버에 배포 |
| 워크플로 | `deploy-reusable.yml` | 서버 배포 스크립트 본체. 개발·운영이 함께 쓴다 |
| 워크플로 | `promote-to-production.yml` | 검증된 커밋을 운영 서버에 승격(수동 실행) |
| 워크플로 | `server-preflight.yml` | 서버가 배포할 준비가 됐는지 점검(수동 실행, 읽기 전용) |

환경마다 **같은 시크릿 이름**에 서버별 값을 넣는다. 워크플로가 `environment`로 어느 환경을 쓸지 정하면 그 환경의 값이 쓰인다.

## 3. GitHub Environment 시크릿

`cloud-dev`와 `cloud-prod` 모두에 아래 6개가 있어야 한다.

| 이름 | 값 | 예 |
|---|---|---|
| `SERVER_HOST` | 서버 IP 또는 도메인 | `43.203.37.52` |
| `SERVER_PORT` | SSH 포트 | `22` |
| `SERVER_USERNAME` | SSH 접속 사용자 | `ubuntu` |
| `SSH_PRIVATE_KEY` | 접속용 개인키(pem) **전체 내용**. `-----BEGIN`줄부터 `-----END`줄까지 | |
| `DOCKER_COMPOSE_YAML_PATH` | 서버 안의 compose 폴더(절대 경로) | `/home/ubuntu/aics-team-project-server` |
| `ENVIRONMENT_PATH` | 서버 안의 `.env` 파일(절대 경로) | `/home/ubuntu/aics-team-project-server/.env` |

`cloud-dev`에는 위 6개에 더해 **이미지 빌드용 2개**가 필요하다. 빌드 job이 `cloud-dev` 환경을 쓰기 때문이다. 서버를 바꿔도 이 두 값은 그대로 둔다.

| 이름 | 값 |
|---|---|
| `DOCKERHUB_USERNAME` | Docker Hub 사용자명 |
| `DOCKERHUB_PASSWORD` | Docker Hub **액세스 토큰**(계정 비밀번호가 아님) |

시크릿 값은 GitHub에서 한 번 저장하면 다시 볼 수 없다. 서버 주소·사용자·경로와 Docker Hub 계정 정보는 **본인이 따로 보관**해 두자(개인키 파일 포함).

**환경 변수(Variables)**: 배포 스크립트의 스모크 테스트가 호출할 공개 도메인은 환경의 변수 `PUBLIC_API_HOST`로 정한다. 없으면 운영 API 도메인(`team-project-api.kgudevelopers.monster`)을 쓰므로, **개발 서버 환경(`cloud-dev`)에는 개발 API 도메인을 반드시 지정**해야 한다. 지정하지 않으면 개발 배포가 운영 API 도메인을 검사한다.

## 4. 서버 준비 체크리스트

새 서버(또는 처음 연결하는 서버)에서 아래를 갖춘다.

- [ ] Docker와 Docker Compose 플러그인 설치
- [ ] 접속 사용자가 `sudo` 없이 `docker`를 쓸 수 있음(`sudo usermod -aG docker $USER` 후 재접속)
- [ ] **SSH 포트가 GitHub Actions에서 접속 가능**: 보안그룹(방화벽)이 해당 포트를 모든 IP에 허용해야 한다. GitHub 러너의 IP는 계속 바뀌어서 특정 IP만 허용하면 접속이 안 된다
- [ ] 서버에서 인터넷(Docker Hub)에 접근 가능
- [ ] compose 폴더가 **미리 만들어져** 있음(배포 첫 단계가 이 폴더로 이동한다): `mkdir -p <DOCKER_COMPOSE_YAML_PATH>`
- [ ] `.env` 파일이 준비됨(아래 키 참고), 권한은 `chmod 600`
- [ ] 80, 443 포트가 **다른 스택에 점유되지 않았고** 외부에서 열려 있음(Caddy가 HTTPS를 처리한다). 같은 서버에 다른 compose 스택이 이 포트를 쓰고 있으면 배포가 포트를 잡지 못한다
- [ ] 서비스 API 도메인의 DNS가 이 서버를 가리킴

`docker-compose.yml`, `Caddyfile`, `pgadmin-servers.json`은 **배포 때 저장소에서 자동으로 복사**되므로 서버에 직접 만들지 않는다. 서버에서 고쳐도 다음 배포에 덮어써진다.

### `.env`에 필요한 키

이 저장소의 `docker/development/docker-compose.yml`에서 읽은 목록이다. 점검 워크플로(5장)가 같은 기준으로 확인해 준다.

**필수(없으면 배포가 실패한다)**

| 키 | 설명 |
|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | 데이터베이스 |
| `PGADMIN_DEFAULT_EMAIL`, `PGADMIN_DEFAULT_PASSWORD` | pgAdmin 로그인 |
| `JWT_SECRET_KEY`, `JWT_ISSUER` | 인증 토큰 |
| `FILE_SECRET_KEY` | 파일 처리 |
| `AWS_S3_BUCKET` | 파일 저장소(S3) 버킷 이름 |
| `DOCKERHUB_USERNAME` | 이미지 이름 앞부분(`사용자명/aics-api:태그`). 이미지 저장소는 공개라 서버에서 `docker login`은 필요 없다 |

**S3 자격 증명(둘 중 하나)**: `AWS_S3_ACCESS_KEY`와 `AWS_S3_SECRET_KEY`를 **둘 다** 넣거나, **둘 다 비워 두고 서버(EC2)에 IAM 역할을 연결**한다. 앱은 키가 비어 있으면 서버의 IAM 역할로 S3에 접근한다. 한쪽만 있으면 안 된다. 점검 워크플로는 이 중 하나가 갖춰졌는지 확인하지만, IAM 역할이 S3 버킷에 접근할 권한이 있는지까지는 확인하지 않는다.

**선택(기본값이 있다)**

| 키 | 기본값 | 개발 서버에서는 |
|---|---|---|
| `SITE_ADDRESS` | 운영 API 도메인 | **개발 API 도메인으로 지정**(Caddy가 쓸 사이트 주소). 빈 값으로 두지 않는다(compose가 빈 값을 기본값으로 바꿔 넘긴다) |
| `CORS_ALLOWED_ORIGINS` | 로컬·운영 프론트 주소 | 개발 프론트 주소로 지정 |
| `CSRF_COOKIE_DOMAIN` | 운영 상위 도메인 | 개발 도메인에 맞게 지정 |
| `AWS_S3_REGION`, `REDIS_PASSWORD`, `MAX_FILE_SIZE`, `MAX_REQUEST_SIZE`, `IMAGE_TAG` | 코드의 기본값 | 보통 생략 |

## 5. 새 서버 연결 절차

1. 위 4장의 체크리스트를 따라 서버를 준비한다.
2. GitHub **Settings → Environments**에서 환경(`cloud-dev` 또는 `cloud-prod`)에 시크릿 6개를 입력한다.
3. **Actions → Server Preflight → Run workflow**에서 점검할 환경을 골라 실행한다.
4. 결과 로그에서 `[FAIL]`을 모두 고치고, `[WARN]`은 읽고 판단한다. `[FAIL]`이 없으면 점검한 항목을 통과한 것이고, 앱이 실제로 뜨는지는 첫 배포와 스모크 테스트가 확인한다.

점검은 서버에 아무것도 쓰거나 바꾸지 않고 읽기만 하며(점검용 설정도 파일로 만들지 않고 표준입력으로만 전달한다), 비밀 값은 출력하지 않고 키 **이름**만 출력한다. `.env` 값은 서버의 Docker Compose가 읽은 결과로 판단하므로 따옴표·주석 같은 해석 차이가 생기지 않는다. 터미널에서는 아래로도 실행할 수 있다.

```bash
gh workflow run server-preflight.yml -f environment=cloud-dev
```

| 점검 결과 | 흔한 원인 |
|---|---|
| 러너에서 SSH 포트에 연결할 수 없음 | 보안그룹이 특정 IP만 허용 / `SERVER_PORT`가 실제 포트와 다름 |
| compose 폴더가 없음 | 폴더를 미리 만들지 않음 |
| `.env` 필수 키 없음 / 값이 비어 있음 | 4장의 필수 키 누락 |
| `.env` 값이 `#`로 시작한다는 경고 | `KEY= # 주석`처럼 주석을 쓰려던 것이 값으로 읽힘. 값과 주석을 정리 |
| `.env` 권한 경고 | 소유자 외에도 읽을 수 있음. `chmod 600` |
| S3 자격 증명 실패 | 정적 키가 한쪽만 있거나, 키도 서버 IAM 역할도 없음(4장) |
| 80/443을 **다른 compose 프로젝트**가 쓰고 있음(FAIL) | 같은 서버의 기존 스택이 포트를 점유. 그 스택을 중지하거나 정리해 인계해야 이 폴더의 배포가 포트를 잡는다 |
| 80/443을 docker 컨테이너가 아닌 프로세스가 점유(WARN) | 서버에서 직접 실행 중인 웹 서버 등. `sudo ss -ltnp`로 소유자 확인 |
| docker 데몬에 접근할 수 없음 | 접속 사용자가 `docker` 그룹에 없음 |
| 디스크 사용률 경고 | 오래된 이미지·로그 정리 필요 |
| `DOCKERHUB_USERNAME`이 `DOCKERHUB_NAMESPACE`와 다름 | 서버가 받아 오는 이미지 저장소와 승격이 검증하는 저장소가 달라짐. 9장 참고 |

### 이미 다른 스택이 돌고 있는 서버를 이어받을 때

같은 서버에 이전 방식으로 만든 다른 compose 스택이 이미 있다면, 배포하기 전에 아래를 정한다.

1. **그 스택을 중지·정리할지**: 80/443 포트를 쓰고 있으면 점검이 FAIL로 알려 준다.
2. **기존 DB 데이터를 어떻게 할지**: 이 저장소의 compose는 DB를 이름 있는 볼륨(`pgdata`)에 **새로** 만든다. 기존 DB 데이터를 이어서 쓰려면 이전 방법(덤프·복원 등)을 먼저 정한다. 기존 스택이 쓰는 볼륨은 함부로 지우지 않는다.
3. 컨테이너·네트워크·볼륨 이름이 겹쳐 충돌하지 않는지 확인한다.

## 6. 개발 서버 배포 (자동)

`develop`에 머지하면 `cloud-dev-build-deploy.yml`이 자동으로 실행된다.

1. 이미지 3개(`aics-api`, `aics-admin`, `aics-auth`)를 빌드해 Docker Hub에 **커밋 sha 태그**로 올리고, 올린 이미지의 **digest를 기록**한다(아티팩트 `image-digests-<sha>`). 같은 커밋으로 빌드를 다시 돌리면 sha 태그가 다른 이미지로 덮어써질 수 있어서, 운영 승격은 태그가 아니라 이 digest로 "같은 이미지"인지 확인한다(7장).
2. `deploy-reusable.yml`이 서버의 **`docker-compose.yml`·`Caddyfile`·`pgadmin-servers.json`을 백업**(`.*before-deploy*` 파일)한 뒤 새 설정을 복사하고, 이미지를 받아 앱을 **하나씩 순서대로** 교체한다. 설정을 복사한 뒤 **이미지 pull이나 기존 컨테이너 중지에 실패하거나 설정 전송이 중간에 끊기면** 설정 파일을 직전 릴리스 것으로 되돌리고 종료한다(실패한 설정이 서버에 남아 다음 배포의 "이전 정상 설정"으로 백업되는 일을 막는다). 운영 승격은 서버가 받은 이미지가 검증한 digest와 같은지도 **기존 컨테이너를 멈추기 전에** 확인한다(승인 대기 중 같은 태그가 다른 이미지로 덮어써져도 다른 이미지가 운영에 나가지 않는다).
3. 서비스별 헬스체크(최대 3분), Caddy 설정 검증과 반영, 공개 로그인 경로와 구 Swagger 경로 스모크 테스트를 거친다.
4. 실패하면 직전에 성공한 이미지(서버의 `.last-successful-image-tag`)로 **자동 롤백**한다. 이때 백업해 둔 compose·pgadmin 설정과 Caddyfile도 **함께 복원한 뒤** 이전 이미지를 올린다(설정이 바뀐 커밋이 실패해도 이전 이미지가 새 설정으로 뜨지 않게 하기 위해서). 첫 배포라 이전 기록이 없으면 롤백하지 못한다.

배포 결과는 **Actions** 탭의 해당 실행에서 확인한다. 앱을 순서대로 재시작하므로 배포 중에는 잠깐 `502`가 보일 수 있다.

## 7. 운영 서버 배포 (승격)

개발 서버에서 확인을 마친 커밋을 운영에 올린다. 직접 실행해야 하고, 승인자가 승인해야 배포된다.

### 승격할 커밋(sha) 확인

개발 서버에 성공적으로 배포된 `develop`의 커밋이어야 한다. 전체 40자 sha를 쓴다. 개발 배포가 이미지 digest를 기록하기 **전에** 배포된 커밋은 기본적으로 승격할 수 없다(8장의 롤백 참고).

```bash
git fetch origin
git log origin/develop --format='%H %ad %s' --date=short -10
```

### 실행

1. **Actions → Promote to Production → Run workflow**
2. **Use workflow from**은 `develop`으로 둔다(다른 브랜치는 거부된다).
3. `image_tag`에 커밋 sha를 입력한다. 먼저 `dry_run`을 켜고 실행하면 **검증만** 하고 배포하지 않는다. `allow_unrecorded_digest`는 기본 꺼짐이다(8장).
4. 검증을 통과하면 `cloud-prod` 승인자에게 승인 요청이 간다. 승인하면 배포가 시작된다.
5. 배포가 성공하면 먼저 `prod-날짜-sha` 태그가 붙고, 이어서 `main`이 그 커밋으로 이동한다(`main` 이동이 실패해도 태그는 남는다).

```bash
gh workflow run promote-to-production.yml -f image_tag=<커밋 sha 40자> -f dry_run=true
```

### 자동 검증 항목 (승인 요청 전에 실행)

| 검사 | 막는 경우 |
|---|---|
| `develop` 브랜치에서 실행했는가 | 검토되지 않은 브랜치 코드가 운영 시크릿을 쓰는 것 |
| 소문자 16진수 40자 sha인가 | 짧은 sha, 대문자 |
| `develop` 이력에 있는 커밋인가 | 머지되지 않은 커밋 |
| Docker Hub에 이미지 3개가 있는가 | 빌드되지 않은 커밋 |
| 그 커밋의 **가장 최근** 개발 배포 실행이 성공했는가 | 같은 커밋을 다시 빌드하다 실패해 이미지가 일부만 덮어써진 상태에서, 과거 성공 기록 때문에 통과하는 것 |
| 이미지 digest가 개발 배포 때 기록한 값과 같은가 | 개발에서 검증한 뒤 같은 태그가 다른 이미지로 덮어써진 경우 |
| 기록된 이미지 저장소가 `DOCKERHUB_NAMESPACE`와 같은가 | 빌드와 승격 검증이 서로 다른 Docker Hub 사용자명을 쓰는 것 |

Docker Hub 사용자명은 세 곳에서 같아야 한다: 개발 빌드(시크릿 `DOCKERHUB_USERNAME`), 승격 검증(변수 `DOCKERHUB_NAMESPACE`, 기본 `hotan35`), 서버 `.env`의 `DOCKERHUB_USERNAME`. 서버 쪽은 점검 워크플로(5장)가 확인한다.

**운영 서버의 Spring 프로필**: 승격도 개발과 같은 compose(`docker/development`)를 쓰므로 세 앱이 `dev` 프로필(`ddl-auto: update`)로 뜬다. 지금 운영이 이미 이렇게 동작하고 승격이 바꾸지 않는다. `prod`(`validate`)로 바꾸려면 배포 설정과 DB 스키마 정리가 먼저 필요한 별도 결정이다(10장).

승격은 한 번에 하나만 실행된다. 같은 실행의 마지막 단계에서 `main` 이동이 실패해도 **운영 배포는 이미 성공했고 배포 태그도 이미 기록된 상태**이므로, 로그의 안내대로 `main` 보호 설정 등을 확인한다.

## 8. 롤백

- **배포 중 실패**: 6장의 자동 롤백이 직전 성공 이미지로 **설정 파일과 함께** 되돌린다.
- **배포 후 문제 발견**: 이전의 정상 커밋 sha로 **다시 승격**한다(7장과 같은 절차). 과거 커밋을 승격하면 `main`은 뒤로 옮기지 않고 **태그만** 남는다(`main` 이력을 되돌리지 않기 위해서).
- 그 커밋이 digest 기록 도입 **이전**에 배포된 것이라 "digest 기록을 찾을 수 없다"로 막히면 `allow_unrecorded_digest`를 켜서 실행할 수 있다. 이 경우 개발 배포 이후 태그가 바뀌지 않았는지는 **확인하지 못한 채** 진행하므로, 꼭 필요한 롤백일 때만 쓴다.
- **현재 운영에 올라간 커밋**은 `main`이 아니라 가장 최근의 배포 태그로 확인한다(롤백해도 새 태그가 생기고, `main` 이동이 실패해도 태그는 남는다): `git tag -l 'prod-*' --sort=-creatordate | head -1`. 과거 배포 이력은 `head` 없이 본다. 승격으로 배포한 것만 태그가 남는다.

DB 스키마가 바뀐 커밋으로 올렸다면 코드만 되돌려서는 부족할 수 있다. 10장을 참고한다.

## 9. GitHub 설정 (최초 한 번)

저장소 관리자가 한다. 코드로는 설정할 수 없다.

| 설정 | 위치 | 이유 |
|---|---|---|
| `cloud-prod`에 **승인자(Required reviewers)** 지정 | Settings → Environments → cloud-prod | 없으면 승인 없이 바로 운영에 배포된다 |
| `cloud-prod`의 **배포 브랜치를 `develop`만** 허용 | 같은 화면 | 다른 브랜치가 운영 시크릿을 쓰지 못하게 한다 |
| `main` 보호에서 **GitHub Actions의 푸시를 허용** | Settings → Branches | 승격 마지막 단계가 `main`을 옮긴다. 막혀 있으면 그 단계만 실패한다 |
| `cloud-dev`의 변수 `PUBLIC_API_HOST` | Settings → Environments → cloud-dev → Variables | 3장 참고 |
| (권장) PR의 테스트를 **필수 체크**로 지정 | Settings → Branches | 테스트를 통과한 코드만 `develop`에 들어가게 한다 |

승격 검증과 서버 점검이 쓰는 Docker Hub 사용자명은 저장소 변수 `DOCKERHUB_NAMESPACE`이고, 없으면 `hotan35`를 쓴다. 개발 빌드의 시크릿 `DOCKERHUB_USERNAME`, 서버 `.env`의 `DOCKERHUB_USERNAME`과 **모두 같아야** 한다(7장).

## 10. DB 스키마가 바뀌는 변경

- 스키마(테이블·칼럼·제약)를 바꾸는 PR은 `database/*.sql`에 변경 SQL을 **함께 올린다**.
- 적용 순서: **개발 DB에서 먼저** 적용·확인 → 운영 DB를 `pg_dump`로 백업 → 운영 DB에 적용.
- **한 번에 바꾸지 않고 단계로 나눈다.** 롤백하거나 옛 버전과 새 버전이 잠시 함께 떠 있어도 동작해야 하기 때문이다.
  - 칼럼·제약을 **추가**하는 변경은 앱 배포 **전에** 적용한다. 옛 코드가 그대로 동작해야 한다.
  - **삭제·이름 변경**은 ① 새 칼럼을 추가하고 ② 데이터를 새 칼럼으로 옮기고(복사) ③ 새 칼럼을 읽고 쓰는 코드를 배포하고 ④ 옛 코드가 모두 사라지고 롤백할 일이 없는 **호환 기간이 지난 뒤에** 옛 칼럼을 제거한다. 보통 여러 번의 배포에 나눠서 한다. 배포 직후 옛 칼럼을 바로 지우면, 롤백한 옛 이미지가 지워진 칼럼을 찾다가 실패한다.
- 프로필별 동작이 다르다. `ddl-auto`가 `dev` 프로필은 `update`, `prod` 프로필은 `validate`다. **운영도 개발과 같은 compose의 `dev` 프로필이라 지금은 `update`다**(7장). `update`는 칼럼은 추가해도 **이미 있는 CHECK 제약은 갱신하지 않으므로**(enum 값을 추가하면 기존 CHECK가 새 값을 막을 수 있다) 이런 변경은 개발·운영 모두 SQL을 직접 적용해야 한다.
- 운영 DB를 직접 바꾸기 전에는 반드시 백업하고 다른 사람에게 알린다.

## 11. 문제 해결

| 증상 | 확인할 것 |
|---|---|
| 개발 배포가 SSH 연결 단계에서 실패 | 5장의 서버 점검을 실행한다. 보안그룹, `SERVER_PORT`, 키를 확인 |
| 배포가 헬스체크 단계에서 실패하고 롤백됨 | Actions 로그에서 어느 서비스가 실패했는지 확인. 서버에서 `docker compose logs <서비스>` |
| 스모크 테스트가 엉뚱한 도메인을 검사 | `PUBLIC_API_HOST` 변수가 환경에 지정됐는지 확인(3장) |
| 승격 검증이 "개발 서버 배포가 실행된 기록이 없음" | 그 커밋이 `develop`에 머지돼 개발 배포가 실행됐는지 Actions에서 확인 |
| 승격 검증이 "가장 최근 개발 배포가 성공하지 않았다 / 아직 진행 중" | 그 커밋의 개발 배포를 다시 돌려 **성공**시킨 뒤 승격. 재빌드가 실패했다면 이미지가 일부만 덮어써졌을 수 있다 |
| 승격 검증이 "태그의 digest가 개발 배포 이후 바뀌었다" | 같은 커밋을 다시 빌드해 이미지가 덮어써진 것. 개발 서버에 다시 배포해 검증한 뒤 승격 |
| 승격 검증이 "digest 기록을 찾을 수 없다" | digest 기록 도입 전의 과거 커밋. 꼭 필요한 롤백일 때만 `allow_unrecorded_digest`로 진행(8장) |
| 승격 검증이 "Docker Hub에서 이미지를 확인하지 못함" | 개발 배포의 빌드가 성공했는지, 이미지 저장소가 공개인지 확인 |
| `main`을 옮기지 못했다는 오류 | `main` 보호 설정에서 GitHub Actions 푸시 허용 여부 확인(운영 배포는 이미 성공) |
| 서버 디스크가 가득 참 | 서버에서 `docker system df`로 확인하고 안 쓰는 이미지를 정리 |
