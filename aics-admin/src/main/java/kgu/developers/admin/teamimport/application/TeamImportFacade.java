package kgu.developers.admin.teamimport.application;

import static kgu.developers.admin.importcommon.RowStatus.DUPLICATE;
import static kgu.developers.admin.importcommon.RowStatus.INVALID;
import static kgu.developers.admin.importcommon.RowStatus.UPDATE;
import static kgu.developers.admin.importcommon.RowStatus.VALID;
import static kgu.developers.domain.enrollment.domain.Status.ACTIVE;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TeamImportFacade {
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);
    private static final String LEADER_TAKEN = "이 팀에는 이미 팀장이 있습니다.";

    private final ImportBatchRepository importBatchRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SectionRepository sectionRepository;
    private final SectionStaffValidator sectionStaffValidator;

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

        sectionRepository.findActiveByIdForUpdate(batch.getSectionId())
            .orElseThrow(SectionNotFoundException::new);

        batch.apply(LocalDateTime.now());

        Map<String, Long> teamIds = new HashMap<>();
        teamRepository.findAllBySectionId(batch.getSectionId())
            .forEach(team -> teamIds.put(team.getName(), team.getId()));

        // 팀장 판정이 행 순서에 좌우되지 않도록, 반영할 행을 먼저 추린 뒤 파일 전체의 팀장 구성을 계산한다
        List<PlannedRow> planned = new ArrayList<>();
        int skipped = plan(batch, teamIds, planned);
        Map<String, String> leaderOf = plannedLeaders(planned, teamIds);

        int createdTeams = 0;
        int appliedMembers = 0;
        for (PlannedRow row : planned) {
            if (row.leader() && !row.studentNumber().equals(leaderOf.get(row.teamName()))) {
                skipped++;
                continue;
            }

            if (row.assigned() != null) {
                row.assigned().updateIsLeader(row.leader());
                row.assigned().updateProjectRole(row.projectRole());
                teamMemberRepository.save(row.assigned());
                appliedMembers++;
                continue;
            }

            Long teamId = teamIds.get(row.teamName());
            if (teamId == null) {
                teamId = teamRepository.save(
                    Team.create(batch.getSectionId(), row.teamName(), "", "", Status.FORMING)).getId();
                teamIds.put(row.teamName(), teamId);
                createdTeams++;
            }
            TeamMember existing = teamMemberRepository
                .findIncludingDeleted(teamId, row.studentNumber()).orElse(null);
            if (existing != null && existing.getDeletedAt() == null) {
                skipped++;
                continue;
            }
            if (existing != null) {
                existing.reactivate(row.leader(), row.projectRole());
                teamMemberRepository.save(existing);
            } else {
                teamMemberRepository.save(
                    TeamMember.create(teamId, row.studentNumber(), row.leader(), row.projectRole()));
            }
            appliedMembers++;
        }
        importBatchRepository.save(batch);

        return new TeamImportApplyResponse(batch.getId(), createdTeams, appliedMembers, skipped);
    }

    // 반영할 행. assigned는 이미 이 분반의 팀에 속해 있어 갱신할 팀원이고, null이면 새로 편성한다
    private record PlannedRow(String teamName, String studentNumber, boolean leader, String projectRole,
        TeamMember assigned) {
    }

    private int plan(ImportBatch batch, Map<String, Long> teamIds, List<PlannedRow> planned) {
        Set<String> enrolled = activeEnrollments(batch.getSectionId());

        int skipped = 0;
        for (JsonNode row : batch.getPayload()) {
            String status = row.path("status").asText();
            boolean update = UPDATE.name().equals(status);
            if (!update && !VALID.name().equals(status)) {
                continue;
            }
            String teamName = row.path("teamName").asText();
            String studentNumber = row.path("studentNumber").asText();

            if (!enrolled.contains(studentNumber)) {
                skipped++;
                continue;
            }

            TeamMember assigned = teamMemberRepository
                .findActiveBySectionIdAndUserId(batch.getSectionId(), studentNumber)
                .orElse(null);
            if (assigned != null && (!update || !assigned.getTeamId().equals(teamIds.get(teamName)))) {
                skipped++;
                continue;
            }

            planned.add(new PlannedRow(teamName, studentNumber, row.path("leader").asBoolean(),
                row.path("projectRole").asText(), assigned));
        }
        return skipped;
    }

    private Map<String, String> plannedLeaders(List<PlannedRow> planned, Map<String, Long> teamIds) {
        Map<String, String> leaderOf = new HashMap<>();
        planned.stream().map(PlannedRow::teamName).distinct().forEach(teamName -> {
            Long teamId = teamIds.get(teamName);
            if (teamId == null) {
                return;
            }
            teamMemberRepository.findAllByTeamId(teamId).stream()
                .filter(TeamMember::isLeader)
                .findFirst()
                .ifPresent(leader -> leaderOf.put(teamName, leader.getUserId()));
        });

        planned.stream().filter(row -> !row.leader())
            .forEach(row -> leaderOf.remove(row.teamName(), row.studentNumber()));
        planned.stream().filter(PlannedRow::leader)
            .forEach(row -> leaderOf.putIfAbsent(row.teamName(), row.studentNumber()));
        return leaderOf;
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
        Map<String, String> leaderOf = new HashMap<>();        // 팀명 -> 현재 팀장 학번
        teamMemberRepository.findAllByTeamIdIn(teams.stream().map(Team::getId).toList()).forEach(member -> {
            assignedOf.put(member.getUserId(), member);
            if (member.isLeader()) {
                leaderOf.putIfAbsent(teamNames.get(member.getTeamId()), member.getUserId());
            }
        });

        // 팀장 충돌은 행 하나만 봐서는 알 수 없으므로, 나머지 사유로 먼저 분류한 뒤 파일 전체를 놓고 판정한다
        Set<String> seenNumbers = new HashSet<>();
        List<TeamImportRow> classified = rows.stream()
            .map(row -> classify(row, enrolled, assignedOf, teamNames, seenNumbers))
            .toList();
        return resolveLeaders(classified, leaderOf);
    }

    private TeamImportRow classify(TeamImportRow row, Set<String> enrolled, Map<String, TeamMember> assignedOf,
        Map<Long, String> teamNames, Set<String> seenNumbers) {
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
            return row.with(UPDATE, "팀장·역할이 바뀌어 갱신 예정입니다.");
        }
        return row;
    }

    /**
     * 파일을 모두 반영한 뒤의 팀장 구성을 계산해, 자리를 얻지 못하는 승격 행만 오류로 표시한다.
     * 팀장 해제를 먼저 반영하므로 해제 행과 승격 행의 순서는 결과에 영향을 주지 않는다.
     */
    private List<TeamImportRow> resolveLeaders(List<TeamImportRow> rows, Map<String, String> currentLeaders) {
        Map<String, String> leaderOf = new HashMap<>(currentLeaders);
        rows.stream()
            .filter(row -> row.status() != INVALID && !row.leader())
            .forEach(row -> leaderOf.remove(row.teamName(), row.studentNumber()));

        return rows.stream().map(row -> {
            if (row.status() == INVALID || !row.leader()) {
                return row;
            }
            String leader = leaderOf.putIfAbsent(row.teamName(), row.studentNumber());
            return leader == null || leader.equals(row.studentNumber())
                ? row
                : row.with(INVALID, LEADER_TAKEN);
        }).toList();
    }
}
