package kgu.developers.admin.enrollment.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentDetail;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.user.domain.User;

public record EnrollmentAdminResponse(
    @Schema(description = "수강 ID", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "학번", example = "202699999", requiredMode = REQUIRED)
    String studentNumber,

    @Schema(description = "이름", example = "김철수", requiredMode = REQUIRED)
    String name,

    @Schema(description = "이메일", example = "kgu@kyonggi.ac.kr", requiredMode = REQUIRED)
    String email,

    @Schema(description = "전화번호", example = "010-1234-6789")
    String phone,

    @Schema(description = "역할", example = "STUDENT", requiredMode = REQUIRED)
    Role role,

    @Schema(description = "상태", example = "ACTIVE", requiredMode = REQUIRED)
    Status status,

    @Schema(description = "등록 일시")
    LocalDateTime createdAt
) {

    public static EnrollmentAdminResponse from(EnrollmentDetail detail) {
        Enrollment enrollment = detail.enrollment();
        User user = detail.user();
        return new EnrollmentAdminResponse(
            enrollment.getId(),
            user.getStudentNumber(),
            user.getName(),
            user.getEmail(),
            user.getPhone(),
            enrollment.getRole(),
            enrollment.getStatus(),
            enrollment.getCreatedAt()
        );
    }
}
