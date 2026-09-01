package kgu.developers.api.sectionannouncement.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kgu.developers.api.sectionannouncement.presentation.request.SectionAnnouncementCreateRequest;
import kgu.developers.api.sectionannouncement.presentation.request.SectionAnnouncementUpdateRequest;
import kgu.developers.api.sectionannouncement.presentation.response.SectionAnnouncementListResponse;
import kgu.developers.api.sectionannouncement.presentation.response.SectionAnnouncementResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "SectionAnnouncement", description = "분반 공지사항 API")
public interface SectionAnnouncementController {

    @Operation(
        summary = "공지사항 목록 조회 API",
        description = """
            Description : 분반의 공지사항 목록을 조회한다. 분반 소속(학생/조교/담당교수)만 조회할 수 있다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SectionAnnouncementListResponse.class)))
    ResponseEntity<SectionAnnouncementListResponse> getAnnouncements(
        @PathVariable Long sectionId,
        Authentication authentication
    );

    @Operation(
        summary = "공지사항 등록 API",
        description = """
            Description : 분반에 공지사항을 등록한다. 담당 교수만 등록할 수 있으며, 등록 성공 시 분반 소속(ACTIVE) 전원에게 알림이 발송된다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = SectionAnnouncementResponse.class)))
    ResponseEntity<SectionAnnouncementResponse> createAnnouncement(
        @PathVariable Long sectionId,
        @Valid @RequestBody SectionAnnouncementCreateRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "공지사항 수정 API",
        description = """
            Description : 공지사항의 제목/내용/게시일시 중 요청에 포함된 필드만 수정한다. 담당 교수만 수정할 수 있다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SectionAnnouncementResponse.class)))
    ResponseEntity<SectionAnnouncementResponse> updateAnnouncement(
        @PathVariable Long id,
        @Valid @RequestBody SectionAnnouncementUpdateRequest request,
        Authentication authentication
    );
}
