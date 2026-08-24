package kgu.developers.domain.sectionannouncement.exception;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SectionAnnouncementExceptionCode implements ExceptionCode {

    SECTION_ANNOUNCEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공지사항을 찾을 수 없습니다."),
    SECTION_ANNOUNCEMENT_INVALID_CONTENT(HttpStatus.BAD_REQUEST, "제목과 내용은 공백일 수 없습니다."),
    SECTION_ANNOUNCEMENT_EMPTY_UPDATE(HttpStatus.BAD_REQUEST, "수정할 내용이 없습니다."),
    SECTION_ANNOUNCEMENT_CONCURRENTLY_MODIFIED(HttpStatus.CONFLICT, "다른 요청이 먼저 공지사항을 변경했습니다. 다시 시도해주세요.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
