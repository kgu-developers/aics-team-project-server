package kgu.developers.admin.enrollment.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.enrollment.domain.EnrollmentDetail;
import lombok.Builder;

@Builder
public record EnrollmentAdminListResponse(
    @Schema(description = "수강생 리스트", requiredMode = REQUIRED)
    List<EnrollmentAdminResponse> contents
) {
    public static EnrollmentAdminListResponse from(List<EnrollmentDetail> details) {
        return EnrollmentAdminListResponse.builder()
                .contents(details.stream().map(EnrollmentAdminResponse::from).toList())
                .build();
    }
}
