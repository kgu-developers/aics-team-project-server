# aics-team-project-server

## Architecture

Multi-module Gradle project mirroring the layered/module structure of `aics-server`:

- `aics-infra` — infrastructure config, no internal dependencies
- `aics-common` — cross-cutting concerns (config, exception, response), depends on `aics-infra`
- `aics-global-utils` — shared utilities (JWT, AES, logging), depends on `aics-common` for the `CustomException` / `ExceptionCode` contract
- `aics-domain` — domain layer (entity/repository/command/query), depends on `aics-common`, `aics-infra`, `aics-global-utils`
- `aics-admin` / `aics-api` / `aics-auth` — application entry points (Spring Boot apps), each depends on `aics-domain`, `aics-common`, `aics-infra`, `aics-global-utils`

Each module's `src` tree here is a skeleton: folder structure and `build.gradle` only, with a representative feature slice (e.g. `about`) showing the presentation/application/domain/infrastructure layering. Business logic source files are intentionally omitted.

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
| DB 계정 | `root` / `root` | 로컬 PostgreSQL 기본 계정 |
| `ddl-auto` | `update` | 엔티티를 고치면 테이블이 따라 바뀝니다 (운영은 `validate`) |
| JWT 서명 키 | 고정된 개발용 키 | admin·auth가 같은 키를 써야 토큰이 통합니다 |
| 파일 암호화 키 | `local-dev-key-16` | 정확히 16바이트 |
| 쿠키 `Secure` 옵션 (auth) | `false` | 로컬은 https가 아니라서, 켜두면 브라우저가 쿠키를 버립니다 |
| HTTP Basic 계정 (admin) | `admin` / `admin` (권한 `PROFESSOR`) | Postman으로 관리자 API를 찔러볼 때 사용 |

### 3. 잘 떴는지 확인하기

로그에 `Started AicsAdminApplication ...` 이 찍히면 성공입니다. 실제로 요청까지 확인하려면:

```bash
# 1) 관리자 API — 위에서 받은 쿠키로 (PROFESSOR 계정이어야 200)
curl -b 'accessToken=<복사한 값>' http://localhost:8081/api/v1/admin/oop/courses

# 2) 쿠키 대신 Basic 계정으로도 됩니다
curl -u admin:admin http://localhost:8081/api/v1/admin/oop/courses
```

API 문서는 <http://localhost:8081/swagger-ui/index.html> (인증 필요, 위 Basic 계정으로 로그인).

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
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | admin | HTTP Basic 관리자 계정(권한 `PROFESSOR`). JWT 없이도 통과하는 계정이라 값 관리에 주의하세요 |

아래는 기본값이 있어서 **따로 설정하지 않아도 되는** 값들입니다. 환경이 다를 때만 덮어쓰세요.

| 환경변수 | 기본값 |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `localhost`, `5432`, `aics` |
| `REDIS_HOST`, `REDIS_PORT` | `localhost` (dev 프로필은 `redis`), `6379` |
| `JWT_ISSUER` | `kgudevelopers@gmail.com` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` (프론트 주소가 다르면 이 값을 바꿔야 브라우저 요청이 통합니다) |
| `FILE_UPLOAD_PATH`, `MAX_FILE_SIZE`, `MAX_REQUEST_SIZE` | `./cloud`, `10MB`, `10MB` |
| `ADMIN_DOCS_URL`, `API_DOCS_URL`, `AUTH_DOCS_URL` | 로컬 Swagger UI 주소 |
