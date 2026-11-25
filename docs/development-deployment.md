# 개발 서버 자동 배포

## 배포 흐름

이 변경이 develop에 머지되면 develop push는 개발 전용 EC2로만 배포한다.
운영 워크플로와 cloud-prod 환경은 변경하지 않는다. 운영 승격은 별도로 검토한다.

1. GitHub pr-test 환경에서 OIDC로 개발 배포 역할을 받는다.
2. 고정된 개발 인스턴스의 현재 상태를 SSM check 명령으로 확인한다.
3. Java 17로 API/Auth/Admin 이미지를 빌드하고 개발 전용 private ECR에 dev-전체SHA 태그로 올린다.
4. SSM 문서 AicsPrTestDeploy 버전 3이 개발 호스트의 배포 스크립트를 실행한다.
5. 후보 이미지를 모두 받은 뒤 앱만 순서대로 교체한다. DB/Redis/Caddy와 볼륨은 유지한다.
6. API 문서, Swagger, 로그인 입력 검증, pgAdmin 외부 차단을 확인한다. 실패하면 이전 앱 이미지로 복구한다.
7. 외부 HTTPS 응답을 추가 확인한다. 외부 확인 실패는 배포 후 조사 대상이며, 이 단계에서 임의 롤백하지 않는다.

## 고정된 개발 리소스

- EC2: i-0eb11976c412eb88b (52.79.214.148)
- 주소: https://api.dev.kgudevelopers.monster
- ECR: 792854191046.dkr.ecr.ap-northeast-2.amazonaws.com/aics-pr-test/aics-{api,auth,admin}
- GitHub 역할: aics-pr-test-github-deploy
- SSM 문서: AicsPrTestDeploy, 버전 3
- 호스트 스크립트: /home/ubuntu/aics-pr-test-app/deploy-development.sh
- 릴리스 기록: /home/ubuntu/aics-pr-test-app/.development-release

IAM은 해당 개발 인스턴스, 고정 SSM 문서, 개발 ECR 세 저장소로 범위를 제한한다.
운영 SSH 키와 cloud-dev/cloud-prod 비밀값을 개발 Actions에 전달하지 않는다.
ECR 인증은 일회용 Docker 설정에만 저장하고 이미지 pull 직후 삭제한다.

## 활성화 순서

이 브랜치는 PR #341의 재사용 워크플로 변경을 포함한다. #341만 먼저 머지하면 기존 대상 서버로 배포될 수 있으므로 이 격리 변경을 함께 검토하고 먼저 활성화해야 한다.

- pr-test 환경의 배포 브랜치 제한을 develop만 허용하도록 설정한다.
- 이 PR을 리뷰/승인 후 머지한다. 그 전까지 기본 develop의 기존 배포 동작은 바뀌지 않는다.
- 승인된 기능 PR은 그 뒤 하나씩 develop에 머지하고 개발 테스트를 진행한다.
- DB는 ddl-auto=validate를 유지한다. 기능 PR의 스키마 변경은 개발 DB에 먼저 적용한 뒤 배포한다.

## 점검과 복구

수동 workflow_dispatch는 기본 verify_only=true다. false이면 해당 ref를 실제 개발 서버에 배포하므로 브랜치 제한을 유지한다.
배포 스크립트 변경은 Git push만으로 호스트에 자동 설치되지 않는다. 고정 개발 호스트에 검토된 스크립트를 설치하고 check가 통과한 뒤 사용한다.

GitHub observer가 시간 초과되어도 원격 명령을 취소하지 않는다. SSM 명령 상태와 호스트 락을 확인한 뒤 다음 배포를 시작한다.
자동 복구도 실패한 경우 개발 서버를 점검한다. 운영 서버를 복구 대상으로 사용하지 않는다.

## 로컬 검증

```sh
bash -n scripts/deploy-development.sh scripts/run-development-command.sh
bash scripts/test-deploy-development.sh
actionlint .github/workflows/cloud-dev-build-deploy.yml
```

테스트는 잘못된 대상/인자 차단, check 무변경, 인증/pull 실패 시 무변경, 앱 교체 실패 시 복구,
이미지 전체 pull 후 교체, 일회용 인증 정리, 초기 응답 지연 재시도를 확인한다.

## 2026-11-26 검증 근거

- GitHub Actions https://github.com/kgu-developers/aics-team-project-server/actions/runs/45652031103 전체 성공.
- 검증 배포 커밋: 428d87b5fee8c6e58483f187e0cb275ccb0122e6.
- 개발 더미 학생 계정으로 로그인200 → CSRF 헤더 포함 갱신200 → 로그아웃200 → 재갱신401 확인.
- localhost:5173 CORS preflight200, Origin 일치, credentials=true.
- DB/Redis/Caddy의 ID/기동시각 유지, 앱 세 개 재시작횟수0/OOMfalse.
- 시험용 브랜치 push 트리거는 검증 후 제거했다. 최종 verify job은 develop ref만 실행한다.
- Environment API의 브랜치 제한 저장 오류는 남아 있다. 관리 화면에서 develop-only 제한을 확인하기 전 이 PR은 활성화하지 않는다.
