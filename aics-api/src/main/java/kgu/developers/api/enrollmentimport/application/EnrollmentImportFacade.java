package kgu.developers.api.enrollmentimport.application;

import static kgu.developers.api.importcommon.RowStatus.DUPLICATE;
import static kgu.developers.api.importcommon.RowStatus.INVALID;
import static kgu.developers.api.importcommon.RowStatus.NEW_USER;
import static kgu.developers.api.importcommon.RowStatus.VALID;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;

import kgu.developers.api.enrollmentimport.presentation.response.EnrollmentImportApplyResponse;
import kgu.developers.api.enrollmentimport.presentation.response.EnrollmentImportPreviewResponse;
import kgu.developers.api.importcommon.RowStatus;
import kgu.developers.api.importcommon.SectionStaffValidator;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.enrollment.application.command.EnrollmentCommandService;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.importBatch.domain.ImportBatch;
import kgu.developers.domain.importBatch.domain.ImportBatchRepository;
import kgu.developers.domain.importBatch.domain.Type;
import kgu.developers.domain.importBatch.exception.ImportBatchNotFoundException;
import kgu.developers.domain.user.application.command.UserCommandService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;

@Component
@Transactional
@RequiredArgsConstructor
public class EnrollmentImportFacade {
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);

    private final ImportBatchRepository importBatchRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentCommandService enrollmentCommandService;
    private final UserCommandService userCommandService;
    private final UserRepository userRepository;
    private final SectionStaffValidator sectionStaffValidator;

    public EnrollmentImportPreviewResponse preview(Long sectionId, String uploaderId, MultipartFile file) {
        sectionStaffValidator.validate(sectionId, uploaderId);

        List<EnrollmentImportRow> rows = validate(sectionId, EnrollmentSheetReader.read(file));
        EnrollmentImportSummary summary = EnrollmentImportSummary.of(rows);

        ImportBatch batch = ImportBatch.create(uploaderId, sectionId, Type.ENROLLMENT,
            JsonConverter.toTree(rows), JsonConverter.toTree(summary),
            LocalDateTime.now().plus(PREVIEW_TTL));

        return new EnrollmentImportPreviewResponse(importBatchRepository.save(batch).getId(), summary, rows);
    }

    public EnrollmentImportApplyResponse apply(Long importId, String userId) {
        ImportBatch batch = importBatchRepository.findById(importId)
            .orElseThrow(ImportBatchNotFoundException::new);
        if (batch.getType() != Type.ENROLLMENT) {
            throw new ImportBatchNotFoundException();
        }
        sectionStaffValidator.validate(batch.getSectionId(), userId);

        batch.apply(LocalDateTime.now());

        int applied = 0;
        int createdUsers = 0;
        int skipped = 0;
        for (JsonNode row : batch.getPayload()) {
            RowStatus status = RowStatus.valueOf(row.path("status").asText());
            if (status != VALID && status != NEW_USER) {
                continue;
            }
            String studentNumber = row.path("studentNumber").asText();
            Role role = Role.valueOf(row.path("role").asText());

            // 미리보기 이후 등록됐을 수 있으므로 다시 확인한다. 수강 취소 상태면 다시 활성화한다.
            Enrollment existing = enrollmentRepository
                .findBySectionIdAndUserId(batch.getSectionId(), studentNumber).orElse(null);
            if (existing != null) {
                if (existing.getStatus() == Status.ACTIVE) {
                    skipped++;
                    continue;
                }
                enrollmentCommandService.updateEnrollment(batch.getSectionId(), studentNumber, role, Status.ACTIVE);
                applied++;
                continue;
            }
            if (status == NEW_USER && userRepository.findByStudentNumber(studentNumber).isEmpty()) {
                userCommandService.createUser(studentNumber, row.path("email").asText(),
                    row.path("name").asText(), studentNumber, UserGlobalRole.USER,
                    row.path("phone").asText(), false);
                createdUsers++;
            }
            enrollmentCommandService.createEnrollment(batch.getSectionId(), studentNumber, role);
            applied++;
        }
        importBatchRepository.save(batch);

        return new EnrollmentImportApplyResponse(batch.getId(), applied, createdUsers, skipped);
    }

    private List<EnrollmentImportRow> validate(Long sectionId, List<EnrollmentImportRow> rows) {
        Set<String> members = userRepository
            .findAllByStudentNumberIn(rows.stream().map(EnrollmentImportRow::studentNumber).toList())
            .stream()
            .map(User::getStudentNumber)
            .collect(Collectors.toSet());
        Map<String, Status> enrolled = enrollmentRepository.findAllBySectionId(sectionId).stream()
            .collect(Collectors.toMap(Enrollment::getUserId, Enrollment::getStatus));

        Set<String> seenNumbers = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();
        return rows.stream()
            .map(row -> classify(row, members, enrolled, seenNumbers, seenEmails))
            .toList();
    }

    private EnrollmentImportRow classify(EnrollmentImportRow row, Set<String> members,
        Map<String, Status> enrolled, Set<String> seenNumbers, Set<String> seenEmails) {
        if (row.status() == INVALID) {
            return row;
        }
        if (!seenNumbers.add(row.studentNumber())) {
            return row.with(INVALID, "파일 안에 중복된 학번입니다.");
        }
        Status enrollmentStatus = enrolled.get(row.studentNumber());
        if (enrollmentStatus == Status.ACTIVE) {
            return row.with(DUPLICATE, "이미 등록된 수강생입니다.");
        }
        if (enrollmentStatus != null) {
            return row.with(VALID, "수강 취소 상태입니다. 반영 시 다시 활성화합니다.");
        }
        if (members.contains(row.studentNumber())) {
            return row;
        }
        if (userRepository.findIncludingDeleted(row.studentNumber()).isPresent()) {
            return row.with(INVALID, "탈퇴 이력이 있는 학번입니다. 관리자에게 문의하세요.");
        }
        if (!seenEmails.add(row.email())) {
            return row.with(INVALID, "파일 안에 중복된 이메일입니다.");
        }
        if (userRepository.existsByEmailAndStudentNumberNotAndDeletedAtIsNull(row.email(), row.studentNumber())) {
            return row.with(INVALID, "이미 사용 중인 이메일입니다.");
        }
        return row.with(NEW_USER, "가입되지 않은 학생입니다. 반영 시 계정을 만듭니다.");
    }
}
