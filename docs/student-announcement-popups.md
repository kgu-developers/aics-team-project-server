# 학생 공지 팝업 조회·일주일간 숨김

PR #345(교수용 팝업 관리)의 version 및 도메인을 그대로 사용한다. 로그인 응답을 바꾸지 않고, 프론트가 로그인 성공 후 학생 팝업 목록을 별도로 조회한다.

## 프론트 연동

1. `GET /api/v1/announcement-popups`를 로그인 쿠키로 호출한다.
2. 응답 `contents`의 팝업을 표시한다. 없으면 빈 배열이다.
3. “일주일간 보지 않기”를 선택한 팝업만 `PUT /api/v1/announcement-popups/{id}/dismissal`로 저장한다.

요청 예시:

```json
{"version": 0}
```

version은 GET 응답 값을 그대로 사용한다. 사용자 ID는 서버의 인증 정보에서 가져오므로 요청에 보내지 않는다. PUT에는 기존 CSRF 쿠키와 `X-XSRF-TOKEN` 헤더가 필요하다. 응답은 `popupId`, `version`, `hiddenUntil`이다.

## 노출·권한 기준

- 활성 강좌·분반의 ACTIVE STUDENT 수강 정보가 있는 본인만 조회·숨김 가능하다.
- 비활성/삭제 팝업, 삭제 공지·분반·강좌, 탈퇴 수강 정보, 종료 강좌, 미래 게시 공지는 제외한다.
- 접근 불가 팝업은404, 필수 version 누락/음수는400, 수정 전의 오래된 version은409다.
- 서버 시각으로7일간 숨긴다. 정확한 만료 시각부터 다시 노출한다.
- 교수가 팝업을 수정해 version이 바뀌면7일 이내라도 다시 표시한다.
- 학생별 DB 저장이므로 같은 계정의 다른 기기에도 적용된다. 브라우저 저장소나 로그인 응답에는 숨김 상태를 넣지 않는다.
- 같은 학생·팝업의 동시 요청은 원자적 upsert로 처리한다. 늦게 도착한 이전 version이 최신 상태를 되돌리거나 숨김 기한을 단축하지 않는다.

## 배포 순서

1. PR #345의 코드와 `database/announcement_popup.sql`이 선행한다.
2. `database/announcement_popup_dismissal.sql`을 대상 개발 DB에 적용한다.
3. 이 변경을 개발 서버에 배포한 뒤 더미 학생으로 노출→숨김→교수 수정 후 재노출을 확인한다.

이 작업에서는 서버나 DB에 적용하지 않는다. `ddl-auto=validate` 환경에서는 두 DDL을 앱 배포보다 먼저 적용해야 한다. SQL은 신규 테이블만 만들며 기존 공지·회원 데이터를 수정하지 않는다.

## 검증 결과

- 학생 Facade·Controller 테스트13건 통과.
- 학생 저장소 PostgreSQL 통합 테스트14건 통과.
- PR #345 기존 저장소 PostgreSQL 테스트11건 통과.
- API/Auth/Admin Java17 컴파일 통과.
- 로컬 Testcontainers 자동 연결은 Docker 환경 인식 오류가 있어, 전용 임시 postgres:16.15를 `TEST_DB_URL`로 지정해 DB 테스트를 실행했다. 해당 DB는 실제 서비스 DB와 분리되어 있다.
