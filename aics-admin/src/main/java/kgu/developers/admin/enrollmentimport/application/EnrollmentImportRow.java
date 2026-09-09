package kgu.developers.admin.enrollmentimport.application;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.admin.importcommon.RowStatus;
import kgu.developers.domain.enrollment.domain.Role;

public record EnrollmentImportRow(
    @Schema(description = "엑셀 행 번호", example = "2")
    int rowNumber,

    @Schema(description = "학번", example = "202412345")
    String studentNumber,

    @Schema(description = "이름", example = "홍길동")
    String name,

    @Schema(description = "이메일 (비어 있으면 학번 기준 학교 메일로 채운다)", example = "hong@kyonggi.ac.kr")
    String email,

    @Schema(description = "연락처", example = "010-1234-5678")
    String phone,

    @Schema(description = "전공 (엑셀에 전공 또는 학과 열이 없거나 값이 비어 있으면 null)", example = "컴퓨터공학부")
    String major,

    @Schema(description = "역할", example = "STUDENT")
    Role role,

    @Schema(description = "행 상태", example = "VALID")
    RowStatus status,

    @Schema(description = "상태 사유", example = "이미 등록된 수강생입니다.")
    String message
) {
    public EnrollmentImportRow(int rowNumber, String studentNumber, String name, String email,
        String phone, Role role, RowStatus status, String message) {
        this(rowNumber, studentNumber, name, email, phone, null, role, status, message);
    }

    public EnrollmentImportRow with(RowStatus status, String message) {
        return new EnrollmentImportRow(rowNumber, studentNumber, name, email, phone, major, role, status, message);
    }
}
