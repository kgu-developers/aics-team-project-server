package kgu.developers.api.sectionannouncement.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementAttachment;
import lombok.Builder;

@Builder
public record SectionAnnouncementAttachmentResponse(

    @Schema(description = "첨부파일 식별자(수정 시 deleteAttachmentIds에 사용)", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "파일 이름", example = "중간고사_일정.pdf", requiredMode = REQUIRED)
    String fileName,

    @Schema(description = "파일 크기(바이트)", example = "1048576", requiredMode = REQUIRED)
    Long size,

    @Schema(description = "파일 MIME 타입", example = "application/pdf")
    String mimeType,

    @Schema(description = "다운로드용 임시 URL(15분 후 만료)", requiredMode = REQUIRED)
    String downloadUrl
) {

    public static SectionAnnouncementAttachmentResponse of(
        SectionAnnouncementAttachment attachment, FileObject fileObject, String downloadUrl
    ) {
        return SectionAnnouncementAttachmentResponse.builder()
            .id(attachment.getId())
            .fileName(fileObject.getFileName())
            .size(fileObject.getSize())
            .mimeType(fileObject.getContentType())
            .downloadUrl(downloadUrl)
            .build();
    }
}
