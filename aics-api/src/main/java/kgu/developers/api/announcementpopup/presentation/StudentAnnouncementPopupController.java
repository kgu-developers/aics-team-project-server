package kgu.developers.api.announcementpopup.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kgu.developers.api.announcementpopup.presentation.request.AnnouncementPopupDismissRequest;
import kgu.developers.api.announcementpopup.presentation.response.AnnouncementPopupDismissalResponse;
import kgu.developers.api.announcementpopup.presentation.response.StudentAnnouncementPopupListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "StudentAnnouncementPopup", description = "학생 로그인 후 팝업 조회·일주일간 숨김 API")
public interface StudentAnnouncementPopupController {
    @Operation(summary = "학생 팝업 목록 조회 API", description = """
        Description : 로그인 후 별도로 호출한다. 활성 강좌·분반에 ACTIVE STUDENT로 소속된 사용자의 게시된 공지 팝업만 최신순으로 반환한다.
            비활성·탈퇴 학생, 조교·교수만 소속된 분반의 팝업은 제외하며, 대상이 없으면 contents는 빈 배열이다.
            팝업별 숨김 만료 전에는 제외하고, 제목·내용·이미지가 실제로 바뀌어 contentVersion이 변경되거나 숨김 기한과 같거나 지난 시각에는 다시 반환한다.
        Assignee : 최태양
        """)
    ResponseEntity<StudentAnnouncementPopupListResponse> getPopups(Authentication authentication);

    @Operation(summary = "학생 팝업 일주일간 보지 않기 API", description = """
        Description : 로그인한 본인과 해당 팝업에만 서버 KST 기준 현재 시각부터 7일간 숨김을 저장한다. 다른 기기에서도 적용된다.
            조회 응답의 contentVersion을 보내야 하며, 내용이 수정된 팝업의 오래된 contentVersion이면 409다. 409 응답 시 목록을 다시 조회한다.
            꺼진·예약 게시·다른 분반·없는 팝업 등 현재 접근할 수 없는 팝업은 404다.
            같은 버전의 중복 요청은 숨김 기한을 단축하지 않고 갱신한다. 로그인·CSRF 쿠키가 필요하다.
        Assignee : 최태양
        """)
    ResponseEntity<AnnouncementPopupDismissalResponse> dismiss(
        Long id, AnnouncementPopupDismissRequest request, Authentication authentication);
}
