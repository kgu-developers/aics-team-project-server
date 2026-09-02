package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SubmissionExceptionCode implements ExceptionCode {

    SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 제출을 찾을 수 없습니다."),
    SUBMISSION_VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 버전을 찾을 수 없습니다."),
    SUBMISSION_NOT_ALLOWED_NOW(HttpStatus.FORBIDDEN, "지금은 제출할 수 있는 기간이 아닙니다."),
    SUBMISSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "그 팀 소속만 접근할 수 있습니다."),
    SUBMISSION_LEADER_ONLY(HttpStatus.FORBIDDEN, "팀장만 완료 처리할 수 있습니다."),
    SUBMISSION_MEMBER_CONFIRMATION_INCOMPLETE(HttpStatus.PRECONDITION_REQUIRED, "팀원 전원이 아직 확인하지 않았습니다."),
    SUBMISSION_NOT_YET_SUBMITTED(HttpStatus.CONFLICT, "아직 제출되지 않아 완료 처리할 수 없습니다."),
    SUBMISSION_NOT_COMPLETED(HttpStatus.CONFLICT, "완료된 제출만 재오픈할 수 있습니다."),
    SUBMISSION_REQUIRED_ARTIFACT_MISMATCH(HttpStatus.BAD_REQUEST, "이 마일스톤의 필수 산출물 구성과 맞지 않습니다."),
    SUBMISSION_ARTIFACT_COUNT_MISMATCH(HttpStatus.BAD_REQUEST, "파일 개수와 산출물 식별자 개수가 다릅니다."),
    SUBMISSION_INVALID_ARTIFACT_TYPE(HttpStatus.BAD_REQUEST, "파일 타입 산출물은 artifacts가 아니라 files로 보내야 합니다."),
    SUBMISSION_INVALID_PRESENTATION_ORDER(HttpStatus.BAD_REQUEST, "발표순서는 분반의 모든 팀을 중복 없이 양수로 지정해야 합니다."),
    SUBMISSION_MILESTONE_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "발표(PRESENTATION) 마일스톤에서만 사용할 수 있습니다."),
    SUBMISSION_PRESENTATION_IMAGE_OWNERSHIP_INVALID(HttpStatus.BAD_REQUEST, "화면 이미지는 우리 팀이 업로드한 파일만 지정할 수 있습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
