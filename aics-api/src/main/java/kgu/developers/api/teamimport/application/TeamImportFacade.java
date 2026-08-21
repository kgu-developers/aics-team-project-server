package kgu.developers.api.teamimport.application;

import static kgu.developers.api.importcommon.RowStatus.DUPLICATE;
import static kgu.developers.api.importcommon.RowStatus.INVALID;
import static kgu.developers.api.importcommon.RowStatus.VALID;
import static kgu.developers.domain.enrollment.domain.Status.ACTIVE;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;

import kgu.developers.api.importcommon.SectionStaffValidator;
import kgu.developers.api.teamimport.presentation.response.TeamImportApplyResponse;
import kgu.developers.api.teamimport.presentation.response.TeamImportPreviewResponse;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.importBatch.domain.ImportBatch;
import kgu.developers.domain.importBatch.domain.ImportBatchRepository;
import kgu.developers.domain.importBatch.domain.Type;
import kgu.developers.domain.importBatch.exception.ImportBatchNotFoundException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import lombok.RequiredArgsConstructor;

@Component
@Transactional
@RequiredArgsConstructor
public class TeamImportFacade {
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);

    private final ImportBatchRepository importBatchRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SectionStaffValidator sectionStaffValidator;

    public TeamImportPreviewResponse preview(Long sectionId, String uploaderId, MultipartFile file) {
        sectionStaffValidator.validate(sectionId, uploaderId);

        List<TeamImportRow> rows = validate(sectionId, TeamSheetReader.read(file));
        TeamImportSummary summary = TeamImportSummary.of(rows);

        ImportBatch batch = ImportBatch.create(uploaderId, sectionId, Type.TEAM,
            JsonConverter.toTree(rows), JsonConverter.toTree(summary),
            LocalDateTime.now().plus(PREVIEW_TTL));

        return new TeamImportPreviewResponse(importBatchRepository.save(batch).getId(), summary, rows);
    }

    public TeamImportApplyResponse apply(Long importId, String userId) {
        ImportBatch batch = importBatchRepository.findById(importId)
            .orElseThrow(ImportBatchNotFoundException::new);
        if (batch.getType() != Type.TEAM) {
            throw new ImportBatchNotFoundException();
        }
        sectionStaffValidator.validate(batch.getSectionId(), userId);

        batch.apply(LocalDateTime.now());

        Map<String, Long> teamIds = new HashMap<>();
        teamRepository.findAllBySectionId(batch.getSectionId())
            .forEach(team -> teamIds.put(team.getName(), team.getId()));
        Set<String> enrolled = activeEnrollments(batch.getSectionId());

        int createdTeams = 0;
        int appliedMembers = 0;
        int skipped = 0;
        for (JsonNode row : batch.getPayload()) {
            if (!VALID.name().equals(row.path("status").asText())) {
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
        Map<String, String> assignedTeamOf = new HashMap<>();  // 학번 -> 이미 속한 팀명
        Set<String> teamsWithLeader = new HashSet<>();
        for (Team team : teams) {
            teamMemberRepository.findAllByTeamId(team.getId()).forEach(member -> {
                assignedTeamOf.put(member.getUserId(), team.getName());
                if (member.isLeader()) {
                    teamsWithLeader.add(team.getName());
                }
            });
        }

        Set<String> seenNumbers = new HashSet<>();
        Set<String> leaderTeams = new HashSet<>(teamsWithLeader);
        return rows.stream()
            .map(row -> classify(row, enrolled, assignedTeamOf, leaderTeams, seenNumbers))
            .toList();
    }

    private TeamImportRow classify(TeamImportRow row, Set<String> enrolled, Map<String, String> assignedTeamOf,
        Set<String> leaderTeams, Set<String> seenNumbers) {
        if (row.status() == INVALID) {
            return row;
        }
        if (!seenNumbers.add(row.studentNumber())) {
            return row.with(INVALID, "파일 안에 중복된 학번입니다.");
        }
        if (!enrolled.contains(row.studentNumber())) {
            return row.with(INVALID, "해당 분반에 수강 등록되지 않은 학생입니다.");
        }
        String assigned = assignedTeamOf.get(row.studentNumber());
        if (assigned != null) {
            return assigned.equals(row.teamName())
                ? row.with(DUPLICATE, "이미 이 팀에 편성되어 있습니다.")
                : row.with(INVALID, "이미 다른 팀(" + assigned + ")에 편성되어 있습니다.");
        }
        if (row.leader() && !leaderTeams.add(row.teamName())) {
            return row.with(INVALID, "이 팀에는 이미 팀장이 있습니다.");
        }
        return row;
    }
}
