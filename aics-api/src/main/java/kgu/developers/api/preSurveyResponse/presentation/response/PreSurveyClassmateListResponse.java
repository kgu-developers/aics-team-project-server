package kgu.developers.api.preSurveyResponse.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.enrollment.domain.EnrollmentDetail;
import lombok.Builder;

@Builder
public record PreSurveyClassmateListResponse(

    @Schema(description = "지목할 수 있는 같은 분반 학생 목록(본인 제외)", requiredMode = REQUIRED)
    List<Classmate> contents
) {

    public record Classmate(

        @Schema(description = "학번", example = "202054321", requiredMode = REQUIRED)
        String userId,

        @Schema(description = "이름", example = "이영희", requiredMode = REQUIRED)
        String name
    ) {
    }

    public static PreSurveyClassmateListResponse from(List<EnrollmentDetail> details) {
        return PreSurveyClassmateListResponse.builder()
            .contents(details.stream()
                .map(detail -> new Classmate(detail.user().getStudentNumber(), detail.user().getName()))
                .toList())
            .build();
    }
}
