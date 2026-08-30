package kgu.developers.admin.enrollmentimport.application;

import static kgu.developers.admin.importcommon.RowStatus.DUPLICATE;
import static kgu.developers.admin.importcommon.RowStatus.INVALID;
import static kgu.developers.admin.importcommon.RowStatus.NEW_USER;
import static kgu.developers.admin.importcommon.RowStatus.VALID;

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

import kgu.developers.admin.enrollmentimport.presentation.response.EnrollmentImportApplyResponse;
import kgu.developers.admin.enrollmentimport.presentation.response.EnrollmentImportPreviewResponse;
import kgu.developers.admin.importcommon.SectionStaffValidator;
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
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.user.application.command.UserCommandService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnrollmentImportFacade {
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);

    private final ImportBatchRepository importBatchRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentCommandService enrollmentCommandService;
    private final UserCommandService userCommandService;
    private final UserRepository userRepository;
    private final SectionRepository sectionRepository;
    private final SectionStaffValidator sectionStaffValidator;

    @Transactional
    public EnrollmentImportPreviewResponse preview(Long sectionId, String uploaderId, MultipartFile file) {
        sectionStaffValidator.validate(sectionId, uploaderId);
        if (sectionRepository.findById(sectionId).isEmpty()) {
            throw new SectionNotFoundException();
        }

        List<EnrollmentImportRow> rows = validate(sectionId, EnrollmentSheetReader.read(file), uploaderId);
        EnrollmentImportSummary summary = EnrollmentImportSummary.of(rows);

        ImportBatch batch = ImportBatch.create(uploaderId, sectionId, Type.ENROLLMENT,
            JsonConverter.toTree(rows), JsonConverter.toTree(summary),
            LocalDateTime.now().plus(PREVIEW_TTL));

        return new EnrollmentImportPreviewResponse(importBatchRepository.save(batch).getId(), summary, rows);
    }

    @Transactional
    public EnrollmentImportApplyResponse apply(Long importId, String userId) {
        ImportBatch batch = importBatchRepository.findById(importId)
            .orElseThrow(ImportBatchNotFoundException::new);
        if (batch.getType() != Type.ENROLLMENT) {
            throw new ImportBatchNotFoundException();
        }
        sectionStaffValidator.validate(batch.getSectionId(), userId);

        boolean isAssistantOnly = sectionStaffValidator.isAssistantOnly(batch.getSectionId(), userId);

        batch.apply(LocalDateTime.now());

        int applied = 0;
        int createdUsers = 0;
        int skipped = 0;
        for (JsonNode row : batch.getPayload()) {
            String status = row.path("status").asText();
            boolean newUser = NEW_USER.name().equals(status);
            if (!newUser && !VALID.name().equals(status)) {
                continue;
            }
            String studentNumber = row.path("studentNumber").asText();
            Role role = Role.valueOf(row.path("role").asText());

            if (isAssistantOnly && role == Role.ASSISTANT) {
                skipped++;
                continue;
            }

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
            if (newUser && userRepository.findByStudentNumber(studentNumber).isEmpty()) {
                String phone = row.path("phone").asText();
                userCommandService.createUser(studentNumber, row.path("email").asText(),
                    row.path("name").asText(), phone, UserGlobalRole.USER,
                    phone, false);
                createdUsers++;
            }
            enrollmentCommandService.createEnrollment(batch.getSectionId(), studentNumber, role);
            applied++;
        }
        importBatchRepository.save(batch);

        return new EnrollmentImportApplyResponse(batch.getId(), applied, createdUsers, skipped);
    }

    private List<EnrollmentImportRow> validate(Long sectionId, List<EnrollmentImportRow> rows, String uploaderId) {
        List<String> studentNumbers = rows.stream().map(EnrollmentImportRow::studentNumber).toList();
        Set<String> members = userRepository.findAllByStudentNumberIn(studentNumbers).stream()
            .map(User::getStudentNumber)
            .collect(Collectors.toSet());
        Set<String> everRegistered = userRepository.findAllIncludingDeletedByStudentNumberIn(studentNumbers).stream()
            .map(User::getStudentNumber)
            .collect(Collectors.toSet());
        Map<String, String> emailOwners = userRepository
            .findAllByEmailIn(rows.stream().map(EnrollmentImportRow::email).toList()).stream()
            .collect(Collectors.toMap(User::getEmail, User::getStudentNumber));
        Map<String, String> everUsedEmails = userRepository
            .findAllIncludingDeletedByEmailIn(rows.stream().map(EnrollmentImportRow::email).toList()).stream()
            .collect(Collectors.toMap(User::getEmail, User::getStudentNumber));
        Map<String, Status> enrolled = enrollmentRepository.findAllBySectionId(sectionId).stream()
            .collect(Collectors.toMap(Enrollment::getUserId, Enrollment::getStatus));

        boolean isAssistantOnly = sectionStaffValidator.isAssistantOnly(sectionId, uploaderId);

        Set<String> seenNumbers = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();
        return rows.stream()
            .map(row -> classify(row, members, everRegistered, emailOwners, everUsedEmails, enrolled, seenNumbers, seenEmails, isAssistantOnly))
            .toList();
    }

    private EnrollmentImportRow classify(EnrollmentImportRow row, Set<String> members,
        Set<String> everRegistered, Map<String, String> emailOwners, Map<String, String> everUsedEmails,
        Map<String, Status> enrolled, Set<String> seenNumbers, Set<String> seenEmails, boolean isAssistantOnly) {
        if (row.status() == INVALID) {
            return row;
        }
        
        if (isAssistantOnly && row.role() == Role.ASSISTANT) {
            return row.with(INVALID, "조교는 학생 역할만 지정할 수 있습니다.");
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
        if (everRegistered.contains(row.studentNumber())) {
            return row.with(INVALID, "탈퇴 이력이 있는 학번입니다. 관리자에게 문의하세요.");
        }
        if (!seenEmails.add(row.email())) {
            return row.with(INVALID, "파일 안에 중복된 이메일입니다.");
        }
        String emailOwner = emailOwners.get(row.email());
        if (emailOwner != null && !emailOwner.equals(row.studentNumber())) {
            return row.with(INVALID, "이미 사용 중인 이메일입니다.");
        }
        String everUsedEmailOwner = everUsedEmails.get(row.email());
        if (everUsedEmailOwner != null && !everUsedEmailOwner.equals(row.studentNumber())) {
            return row.with(INVALID, "탈퇴한 사용자가 사용했던 이메일입니다. 관리자에게 문의하세요.");
        }
        if (row.phone().isEmpty()) {
            return row.with(INVALID, "신규 가입 대상은 연락처가 필요합니다.");
        }
        return row.with(NEW_USER, "가입되지 않은 학생입니다. 반영 시 계정을 만듭니다.");
    }
}
