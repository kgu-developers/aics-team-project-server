# 학생 공지 팝업 조회·일주일간 숨김

PR #345(교수용 팝업 관리)의 contentVersion 및 도메인을 그대로 사용한다. 로그인 응답을 바꾸지 않고, 프론트가 로그인 성공 후 학생 팝업 목록을 별도로 조회한다.

## 프론트 연동

1. `GET /api/v1/announcement-popups`를 로그인 쿠키로 호출한다.
2. 응답 `contents`의 팝업을 표시한다. 없으면 빈 배열이다.
3. “일주일간 보지 않기”를 선택한 팝업만 `PUT /api/v1/announcement-popups/{id}/dismissal`로 저장한다.

요청 예시:

```json
{"contentVersion": 0}
```

contentVersion은 GET 응답 값을 그대로 사용한다. 사용자 ID는 서버의 인증 정보에서 가져오므로 요청에 보내지 않는다. PUT에는 기존 CSRF 쿠키와 `X-XSRF-TOKEN` 헤더가 필요하다. 응답은 `popupId`, `contentVersion`, `hiddenUntil`이다.

## 노출·권한 기준

- 활성 강좌·분반의 ACTIVE STUDENT 수강 정보가 있는 본인만 조회·숨김 가능하다.
- 비활성/삭제 팝업, 삭제 공지·분반·강좌, 탈퇴 수강 정보, 종료 강좌, 미래 게시 공지는 제외한다.
- 접근 불가 팝업은 404, 필수 contentVersion 누락/음수는 400, 수정 전의 오래된 contentVersion은 409다. 409 응답 시 목록을 다시 조회한다.
- 서버 시각으로7일간 숨긴다. 정확한 만료 시각부터 다시 노출한다.
- 교수가 제목·내용·이미지를 실제로 수정해 contentVersion이 바뀌면 7일 이내라도 다시 표시한다. 노출 여부만 바꾸거나 같은 값으로 저장하면 숨김은 유지된다.
- 학생별 DB 저장이므로 같은 계정의 다른 기기에도 적용된다. 브라우저 저장소나 로그인 응답에는 숨김 상태를 넣지 않는다.
- 같은 학생·팝업의 동시 요청은 원자적 upsert로 처리한다. 늦게 도착한 이전 contentVersion이 최신 상태를 되돌리거나 숨김 기한을 단축하지 않는다.

## 배포 순서

1. PR #345의 코드와 `database/announcement_popup.sql`이 선행한다.
2. `database/announcement_popup_dismissal.sql`을 대상 개발 DB에 적용한다.
3. 이 변경을 개발 서버에 배포한 뒤 더미 학생으로 노출→숨김→교수 수정 후 재노출을 확인한다.

이 작업에서는 서버나 DB에 적용하지 않는다. `ddl-auto=validate` 환경에서는 두 DDL을 앱 배포보다 먼저 적용해야 한다. SQL은 신규 테이블만 만들며 기존 공지·회원 데이터를 수정하지 않는다.

숨김 테이블의 `popup_version`에는 `contentVersion`을 저장한다. 기존 컬럼명을 유지하고 DDL 주석으로 의미를 명시한다. 관리자 수정 충돌을 검증하는 `version`과는 별개다.
