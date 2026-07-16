# aics-team-project-server

## Architecture

Multi-module Gradle project mirroring the layered/module structure of `aics-server`:

- `aics-global-utils` — shared utilities, no internal dependencies
- `aics-infra` — infrastructure config, depends on `aics-global-utils`
- `aics-common` — cross-cutting concerns (auth, config, exception, response, interceptor), depends on `aics-infra`, `aics-global-utils`
- `aics-domain` — domain layer (entity/repository/command/query), depends on `aics-common`, `aics-infra`, `aics-global-utils`
- `aics-admin` / `aics-api` / `aics-auth` — application entry points (Spring Boot apps), each depends on `aics-domain`, `aics-common`, `aics-infra`, `aics-global-utils`

Each module's `src` tree here is a skeleton: folder structure and `build.gradle` only, with a representative feature slice (e.g. `about`) showing the presentation/application/domain/infrastructure layering. Business logic source files are intentionally omitted.