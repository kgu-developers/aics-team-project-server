package kgu.developers.api.announcementpopup.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kgu.developers.api.announcementpopup.presentation.request.AnnouncementPopupCreateRequest;
import kgu.developers.api.announcementpopup.presentation.request.AnnouncementPopupUpdateRequest;
import kgu.developers.api.announcementpopup.presentation.response.AnnouncementPopupListResponse;
import kgu.developers.api.announcementpopup.presentation.response.AnnouncementPopupResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "AnnouncementPopup", description = "공지사항 팝업 API")
public interface AnnouncementPopupController {

    @Operation(
        summary = "팝업 등록 API",
        description = """
            Description : 공지사항을 학생 화면에 팝업으로 띄운다. 담당 교수만 등록할 수 있고, 공지사항 하나에 팝업은 하나만 둘 수 있다.
                팝업은 공지와 별개의 내용(필수), 제목(선택), 이미지(선택)를 가지며, 등록하면 바로 노출 상태가 된다.
                이미지는 multipart/form-data의 image 파트로 보낸다(jpeg, png, gif, webp).
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = AnnouncementPopupResponse.class)))
    ResponseEntity<AnnouncementPopupResponse> createPopup(
        @PathVariable Long announcementId,
        @Valid @RequestPart("request") AnnouncementPopupCreateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image,
        Authentication authentication
    );

    @Operation(
        summary = "팝업 수정 API",
        description = """
            Description : 팝업의 제목/내용/노출 여부/이미지 중 요청에 포함된 항목만 수정한다. 담당 교수만 수정할 수 있다.
                제목은 빈 문자열을 보내면 지워진다(제목은 선택 항목).
                노출을 끄려면 request.enabled를 false로 보낸다(삭제 API는 없다). 이미지는 image 파트로 교체하고,
                request.removeImage를 true로 보내면 제거한다(둘을 함께 보낼 수 없다).
                수정하면 응답의 version이 오른다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = AnnouncementPopupResponse.class)))
    ResponseEntity<AnnouncementPopupResponse> updatePopup(
        @PathVariable Long id,
        @Valid @RequestPart("request") AnnouncementPopupUpdateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image,
        Authentication authentication
    );

    @Operation(
        summary = "분반 팝업 목록 조회 API",
        description = """
            Description : 분반의 팝업을 최신순으로 조회한다. 꺼진 팝업도 포함한다. 담당 교수만 조회할 수 있다(관리 화면용).
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = AnnouncementPopupListResponse.class)))
    ResponseEntity<AnnouncementPopupListResponse> getPopups(
        @PathVariable Long sectionId,
        Authentication authentication
    );
}
