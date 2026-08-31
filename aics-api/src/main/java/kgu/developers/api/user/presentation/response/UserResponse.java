package kgu.developers.api.user.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.api.section.presentation.response.SectionResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.section.domain.SectionDetail;
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

        @Schema(description = "권한", example = "USER", requiredMode = REQUIRED)
        UserGlobalRole globalRole,

        @Schema(description = "전화번호", example = "010-1234-6789", requiredMode = REQUIRED)
        String phone,

        @Schema(description = "소속 분반 목록")
        List<SectionResponse> sections,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt,

        @Schema(description = "수정 일시")
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .studentNumber(user.getStudentNumber())
                .email(user.getEmail())
                .name(user.getName())
                .globalRole(user.getGlobalRole())
                .phone(user.getPhone())
                .sections(Collections.emptyList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static UserResponse from(User user, List<Enrollment> enrollments, List<SectionDetail> enrollmentSectionDetails, List<SectionDetail> professorSections) {
        Map<Long, SectionDetail> enrollmentSectionMap = enrollmentSectionDetails.stream()
                .collect(Collectors.toMap(sd -> sd.section().getId(), Function.identity()));

        List<SectionResponse> enrollmentSections = enrollments.stream()
                .filter(e -> enrollmentSectionMap.containsKey(e.getSectionId()))
                .map(e -> SectionResponse.from(enrollmentSectionMap.get(e.getSectionId())))
                .toList();

        List<SectionResponse> professorSectionResponses = professorSections.stream()
                .map(SectionResponse::from)
                .toList();

        List<SectionResponse> allSections = new java.util.ArrayList<>(enrollmentSections);
        allSections.addAll(professorSectionResponses);

        List<SectionResponse> deduplicatedSections = allSections.stream()
                .filter(distinctByKey(SectionResponse::id))
                .toList();

        return UserResponse.builder()
                .studentNumber(user.getStudentNumber())
                .email(user.getEmail())
                .name(user.getName())
                .globalRole(user.getGlobalRole())
                .phone(user.getPhone())
                .sections(deduplicatedSections)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}

