package kgu.developers.domain.midreport.exception;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Getter
@AllArgsConstructor
public enum MidReportExceptionCode implements ExceptionCode {
    MID_REPORT_NOT_FOUND(NOT_FOUND, "중간보고서를 찾을 수 없습니다."),
    MID_REPORT_BLOCK_NOT_FOUND(NOT_FOUND, "중간보고서 작성 영역을 찾을 수 없습니다."),
    VERSION_CONFLICT(CONFLICT, "다른 팀원의 저장 내용이 있습니다. 최신 중간보고서를 다시 불러와 주세요."),
    MID_REPORT_SUBMITTED(CONFLICT, "제출한 중간보고서는 수정할 수 없습니다."),
    MID_REPORT_LEADER_ONLY(FORBIDDEN, "팀장만 중간보고서를 최종 제출할 수 있습니다."),
    MID_REPORT_GUI_IMAGE_NOT_OWNED(FORBIDDEN, "현재 팀원이 업로드한 이미지 파일만 중간보고서에 연결할 수 있습니다."),
    INVALID_MID_REPORT_FIELDS(BAD_REQUEST, "작성 영역의 필드 형식이 올바르지 않습니다."),
    BLOCK_INCOMPLETE(UNPROCESSABLE_ENTITY, "모든 필수 항목을 작성한 뒤 완료 처리해 주세요.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
