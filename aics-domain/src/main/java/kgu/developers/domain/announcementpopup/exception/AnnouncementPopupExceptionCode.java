package kgu.developers.domain.announcementpopup.exception;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AnnouncementPopupExceptionCode implements ExceptionCode {

    ANNOUNCEMENT_POPUP_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 팝업을 찾을 수 없습니다."),
    ANNOUNCEMENT_POPUP_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 팝업이 등록된 공지사항입니다."),
    ANNOUNCEMENT_POPUP_INVALID_CONTENT(HttpStatus.BAD_REQUEST, "팝업 내용은 공백일 수 없습니다."),
    ANNOUNCEMENT_POPUP_TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "팝업 제목은 200자 이하여야 합니다."),
    ANNOUNCEMENT_POPUP_EMPTY_UPDATE(HttpStatus.BAD_REQUEST, "수정할 내용이 없습니다."),
    ANNOUNCEMENT_POPUP_IMAGE_CONFLICT(HttpStatus.BAD_REQUEST, "이미지를 교체하면서 동시에 삭제할 수 없습니다."),
    ANNOUNCEMENT_POPUP_CONCURRENTLY_MODIFIED(HttpStatus.CONFLICT, "다른 요청이 먼저 팝업을 변경했습니다. 다시 시도해주세요.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
