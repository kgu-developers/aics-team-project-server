package kgu.developers.admin.teamimport.application;

import static kgu.developers.admin.importcommon.RowStatus.DUPLICATE;
import static kgu.developers.admin.importcommon.RowStatus.INVALID;
import static kgu.developers.admin.importcommon.RowStatus.UPDATE;
import static kgu.developers.admin.importcommon.RowStatus.VALID;
import static kgu.developers.domain.enrollment.domain.Status.ACTIVE;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import com.fasterxml.jackson.databind.JsonNode;

import kgu.developers.admin.teamimport.presentation.response.TeamImportApplyResponse;
import kgu.developers.admin.teamimport.presentation.response.TeamImportPreviewResponse;
import kgu.developers.admin.importcommon.SectionStaffValidator;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.importBatch.domain.ImportBatch;
import kgu.developers.domain.importBatch.domain.ImportBatchRepository;
import kgu.developers.domain.importBatch.domain.Type;
import kgu.developers.domain.importBatch.exception.ImportBatchNotFoundException;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TeamImportFacade {
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);

    private final ImportBatchRepository importBatchRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SectionRepository sectionRepository;
    private final SectionStaffValidator sectionStaffValidator;
    private final EntityManager entityManager;

    @Transactional
    public TeamImportPreviewResponse preview(Long sectionId, String uploaderId, MultipartFile file) {
        sectionStaffValidator.validate(sectionId, uploaderId);
        if (sectionRepository.findById(sectionId).isEmpty()) {
            throw new SectionNotFoundException();
        }

        List<TeamImportRow> rows = validate(sectionId, TeamSheetReader.read(file));
        TeamImportSummary summary = TeamImportSummary.of(rows);

        ImportBatch batch = ImportBatch.create(uploaderId, sectionId, Type.TEAM,
            JsonConverter.toTree(rows), JsonConverter.toTree(summary),
            LocalDateTime.now().plus(PREVIEW_TTL));

        return new TeamImportPreviewResponse(importBatchRepository.save(batch).getId(), summary, rows);
    }

    @Transactional
    public TeamImportApplyResponse apply(Long importId, String userId) {
        ImportBatch batch = importBatchRepository.findById(importId)
            .orElseThrow(ImportBatchNotFoundException::new);
        if (batch.getType() != Type.TEAM) {
            throw new ImportBatchNotFoundException();
        }
        sectionStaffValidator.validate(batch.getSectionId(), userId);

        SectionJpaEntity section = entityManager.find(SectionJpaEntity.class, batch.getSectionId(),
            LockModeType.PESSIMISTIC_WRITE);

        if (section == null || section.getDeletedAt() != null) {
            throw new SectionNotFoundException();
        }

        batch.apply(LocalDateTime.now());

        Map<String, Long> teamIds = new HashMap<>();
        teamRepository.findAllBySectionId(batch.getSectionId())
            .forEach(team -> teamIds.put(team.getName(), team.getId()));
        Set<String> enrolled = activeEnrollments(batch.getSectionId());

        int createdTeams = 0;
        int appliedMembers = 0;
        int skipped = 0;
        for (JsonNode row : batch.getPayload()) {
            String status = row.path("status").asText();
            boolean update = UPDATE.name().equals(status);
            if (!update && !VALID.name().equals(status)) {
                continue;
            }
            String teamName = row.path("teamName").asText();
            String studentNumber = row.path("studentNumber").asText();

            boolean leader = row.path("leader").asBoolean();
            String projectRole = row.path("projectRole").asText();

            if (!enrolled.contains(studentNumber)) {
                skipped++;
                continue;
            }

            TeamMember existingInSection = teamMemberRepository
                .findActiveBySectionIdAndUserId(batch.getSectionId(), studentNumber)
                .orElse(null);
            
            if (existingInSection != null) {
                if (!update || !existingInSection.getTeamId().equals(teamIds.get(teamName))) {
                    skipped++;
                    continue;
                }
                existingInSection.updateIsLeader(leader);
                existingInSection.updateProjectRole(projectRole);
                teamMemberRepository.save(existingInSection);
                appliedMembers++;
                continue;
            }

            Long teamId = teamIds.get(teamName);
            if (teamId == null) {
                teamId = teamRepository.save(
                    Team.create(batch.getSectionId(), teamName, "", "", Status.FORMING)).getId();
                teamIds.put(teamName, teamId);
                createdTeams++;
            }
            TeamMember existing = teamMemberRepository.findIncludingDeleted(teamId, studentNumber).orElse(null);
            if (existing != null && existing.getDeletedAt() == null) {
                skipped++;
                continue;
            }
            if (existing != null) {
                existing.reactivate(leader, projectRole);
                teamMemberRepository.save(existing);
            } else {
                teamMemberRepository.save(TeamMember.create(teamId, studentNumber, leader, projectRole));
            }
            appliedMembers++;
        }
        importBatchRepository.save(batch);

        return new TeamImportApplyResponse(batch.getId(), createdTeams, appliedMembers, skipped);
    }

    private Set<String> activeEnrollments(Long sectionId) {
        return enrollmentRepository.findAllBySectionId(sectionId).stream()
            .filter(enrollment -> enrollment.getStatus() == ACTIVE)
            .map(Enrollment::getUserId)
            .collect(Collectors.toSet());
    }

    private List<TeamImportRow> validate(Long sectionId, List<TeamImportRow> rows) {
        Set<String> enrolled = activeEnrollments(sectionId);

        List<Team> teams = teamRepository.findAllBySectionId(sectionId);
        Map<Long, String> teamNames = teams.stream()
            .collect(Collectors.toMap(Team::getId, Team::getName));
        Map<String, TeamMember> assignedOf = new HashMap<>();  // 학번 -> 이미 속한 팀원 정보
        Set<String> teamsWithLeader = new HashSet<>();
        teamMemberRepository.findAllByTeamIdIn(teams.stream().map(Team::getId).toList()).forEach(member -> {
            assignedOf.put(member.getUserId(), member);
            if (member.isLeader()) {
                teamsWithLeader.add(teamNames.get(member.getTeamId()));
            }
        });

        Set<String> seenNumbers = new HashSet<>();
        Set<String> leaderTeams = new HashSet<>(teamsWithLeader);
        return rows.stream()
            .map(row -> classify(row, enrolled, assignedOf, teamNames, leaderTeams, seenNumbers))
            .toList();
    }

    private TeamImportRow classify(TeamImportRow row, Set<String> enrolled, Map<String, TeamMember> assignedOf,
        Map<Long, String> teamNames, Set<String> leaderTeams, Set<String> seenNumbers) {
        if (row.status() == INVALID) {
            return row;
        }
        if (!seenNumbers.add(row.studentNumber())) {
            return row.with(INVALID, "파일 안에 중복된 학번입니다.");
        }
        if (!enrolled.contains(row.studentNumber())) {
            return row.with(INVALID, "해당 분반에 수강 등록되지 않은 학생입니다.");
        }
        TeamMember assigned = assignedOf.get(row.studentNumber());
        if (assigned != null) {
            String assignedTeam = teamNames.get(assigned.getTeamId());
            if (!row.teamName().equals(assignedTeam)) {
                return row.with(INVALID, "이미 다른 팀(" + assignedTeam + ")에 편성되어 있습니다.");
            }
            // 빈 셀은 ""로 읽히고 DB에는 null로 들어갈 수 있어서, 둘을 같은 값으로 본다
            if (assigned.isLeader() == row.leader()
                && Objects.toString(assigned.getProjectRole(), "")
                    .equals(Objects.toString(row.projectRole(), ""))) {
                return row.with(DUPLICATE, "이미 이 팀에 편성되어 있습니다.");
            }
            if (row.leader() && !leaderTeams.add(row.teamName())) {
                return row.with(INVALID, "이 팀에는 이미 팀장이 있습니다.");
            }
            if (!row.leader() && assigned.isLeader()) {
                // ponytail: 팀장 해제 행이 승격 행보다 뒤에 오면 승격 쪽이 먼저 거부된다.
                // 팀장 판정을 파일 전체 단위로 옮기면 풀리는데, 그때 가서 옮긴다.
                leaderTeams.remove(row.teamName());
            }
            return row.with(UPDATE, "팀장·역할이 바뀌어 갱신 예정입니다.");
        }
        if (row.leader() && !leaderTeams.add(row.teamName())) {
            return row.with(INVALID, "이 팀에는 이미 팀장이 있습니다.");
        }
        return row;
    }
}
