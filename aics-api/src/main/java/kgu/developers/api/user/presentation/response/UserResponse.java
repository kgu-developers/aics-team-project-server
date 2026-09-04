package kgu.developers.api.user.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.api.section.presentation.response.SectionResponse;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import lombok.Builder;

@Builder
public record UserResponse(
        @Schema(description = "학번", example = "202699999", requiredMode = REQUIRED)
        String studentNumber,

        @Schema(description = "이메일", example = "kgu@kyonggi.ac.kr", requiredMode = REQUIRED)
        String email,

        @Schema(description = "이름", example = "김철수", requiredMode = REQUIRED)
        String name,

        @Schema(
                description = "전역 권한",
                example = "USER",
                allowableValues = {"ADMIN", "USER"},
                requiredMode = REQUIRED
        )
        UserGlobalRole globalRole,

        @Schema(description = "전화번호", example = "010-1234-6789", requiredMode = REQUIRED)
        String phone,

        @Schema(description = "소속 분반 목록")
        List<SectionResponse> sections,

        @Schema(description = "내 팀 ID", example = "1")
        Long teamId,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt,

        @Schema(description = "수정 일시")
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user, List<SectionResponse> sections, Long teamId) {
        return UserResponse.builder()
                .studentNumber(user.getStudentNumber())
                .email(user.getEmail())
                .name(user.getName())
                .globalRole(user.getGlobalRole())
                .phone(user.getPhone())
                .sections(sections)
                .teamId(teamId)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
