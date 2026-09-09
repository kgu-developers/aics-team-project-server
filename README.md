# aics-team-project-server

## Architecture

Multi-module Gradle project mirroring the layered/module structure of `aics-server`:

- `aics-infra` — infrastructure config, no internal dependencies
- `aics-common` — cross-cutting concerns (config, exception, response), depends on `aics-infra`
- `aics-global-utils` — shared utilities (JWT, AES, logging), depends on `aics-common` for the `CustomException` / `ExceptionCode` contract
- `aics-domain` — domain layer (entity/repository/command/query), depends on `aics-common`, `aics-infra`, `aics-global-utils`
- `aics-admin` / `aics-api` / `aics-auth` — application entry points (Spring Boot apps), each depends on `aics-domain`, `aics-common`, `aics-infra`, `aics-global-utils`

구현 현황은 모듈마다 다릅니다.

- `aics-admin` + `aics-domain` — 회원(`user`), OOP 강좌(`course`) 기능이 presentation/application/domain/infrastructure 레이어링을 그대로 갖춘 채 구현되어 있습니다.
- `aics-auth` — 로그인·토큰 재발급·로그아웃 등 JWT 쿠키 인증이 구현되어 있습니다.
- `aics-common`, `aics-global-utils`, `aics-infra` — 공통 예외 처리, JWT/AES/로깅 유틸, 인프라 설정이 구현되어 있습니다.
- `aics-api` — 아직 Spring Boot 진입점과 회원 조회 정도만 있습니다.

## 로컬에서 실행하기

### 1. 서버 띄우기

```bash
./gradlew :aics-admin:bootRun --args='--spring.profiles.active=local'   # 관리자 서버 http://localhost:8081
```

**`local` 프로필로 실행하면 환경변수를 하나도 설정하지 않아도 됩니다.**
개발용 기본값이 전부 채워져 있기 때문입니다. 같은 서버를 하나 더 띄워보고 싶다면 뒤에
`--server.port=8093` 처럼 포트만 바꿔주면 됩니다.

### 2. `local` 프로필이 대신 채워주는 값

| 항목 | `local` 값 | 왜 이렇게 두었나 |
|---|---|---|
| DB 계정 | `root` / `root` | PostgreSQL 기본 계정이 아니라 **이 프로젝트의 로컬 규약**입니다. 직접 만들어야 합니다 |
| `ddl-auto` | `update` | 엔티티를 고치면 테이블이 따라 바뀝니다 (운영은 `validate`) |
| JWT 서명 키 | 고정된 개발용 키 | admin·auth가 같은 키를 써야 토큰이 통합니다 |
| 파일 암호화 키 | `local-dev-key-16` | 정확히 16바이트 |
| 쿠키 `Secure` 옵션 (auth) | `false` | 로컬은 https가 아니라서, 켜두면 브라우저가 쿠키를 버립니다 |

`root` 롤과 `aics` 데이터베이스가 없다면 먼저 만들어 주세요.

```bash
createuser -s root
psql -d postgres -c "ALTER USER root WITH PASSWORD 'root'"
createdb -U root aics
```

### 3. 잘 떴는지 확인하기

로그에 `Started AicsAdminApplication ...` 이 찍히면 성공입니다. 실제로 요청까지 확인하려면:

인증은 **auth 서버가 발급한 `accessToken` 쿠키 하나뿐**입니다. admin 서버에는 별도 로그인 수단이 없습니다.

```bash
# 1. auth 서버(8082)에 로그인해 쿠키를 받아둔다
curl -c cookies.txt -H 'Content-Type: application/json' \
  -d '{"studentNumber":"202699999","password":"12345678"}' \
  http://localhost:8082/api/v1/auth/login

# 2. 그 쿠키로 admin 서버(8081)를 부른다
curl -b cookies.txt http://localhost:8081/api/v1/admin/users
```

`{"contents":[...]}` 가 오면 정상입니다. 쿠키 없이 부르면 401, 권한이 `ADMIN`이 아니면 403입니다.

첫 관리자는 아직 아무도 승격시켜 줄 수 없으니 DB에서 직접 올립니다 (그 뒤로는 관리자 API로 승격 가능).

```bash
psql -U root -d aics -c "UPDATE \"user\" SET global_role = 'ADMIN' WHERE student_number = '202699999'"
```

API 문서는 <http://localhost:8081/swagger-ui/index.html> (문서 경로 자체는 인증 없이 열려 있고, 운영에서는 꺼져 있습니다).

## 환경변수

`local` 프로필이 아니면 (기본·dev·prod) 아래 값들은 **반드시** 설정해야 합니다.
하나라도 없으면 서버가 뜨지 않고 `Could not resolve placeholder '<이름>'` 로 즉시 멈춥니다.
**한 번에 하나씩만 알려주기 때문에**, 처음부터 전부 채워두는 편이 빠릅니다.

| 환경변수 | 쓰는 곳 | 설명 |
|---|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | admin, auth | PostgreSQL 계정 |
| `REDIS_PASSWORD` | admin, auth | 비밀번호가 없는 Redis라면 빈 값으로 두되, **변수 자체는 정의**해야 합니다 |
| `JWT_SECRET_KEY` | admin, auth | 토큰 서명 키. **두 서버가 같은 값**이어야 admin이 auth가 발급한 토큰을 검증할 수 있습니다 |
| `FILE_SECRET_KEY` | admin, auth | 파일 암호화 키. **UTF-8 기준 16 / 24 / 32바이트**여야 하며, 아니면 기동 시점에 거부됩니다 |

아래는 기본값이 있어서 **따로 설정하지 않아도 되는** 값들입니다. 환경이 다를 때만 덮어쓰세요.

| 환경변수 | 기본값 |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `localhost`, `5432`, `aics` |
| `REDIS_HOST`, `REDIS_PORT` | `localhost` (dev 프로필은 `redis`), `6379` |
| `JWT_ISSUER` | `kgudevelopers@gmail.com` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` (프론트 주소가 다르면 이 값을 바꿔야 브라우저 요청이 통합니다) |
| `FILE_UPLOAD_PATH`, `MAX_FILE_SIZE`, `MAX_REQUEST_SIZE` | `./cloud`, `10MB`, `10MB` |
| `ADMIN_DOCS_URL`, `API_DOCS_URL`, `AUTH_DOCS_URL` | 로컬 Swagger UI 주소 |
