package kgu.developers.admin.teamimport.application;

import static kgu.developers.admin.importcommon.RowStatus.DUPLICATE;
import static kgu.developers.admin.importcommon.RowStatus.INVALID;
import static kgu.developers.admin.importcommon.RowStatus.UPDATE;
import static kgu.developers.admin.importcommon.RowStatus.VALID;
import static kgu.developers.domain.enrollment.domain.Status.ACTIVE;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserRepository;
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
    private final UserRepository userRepository;
    private final SectionRepository sectionRepository;
    private final SectionStaffValidator sectionStaffValidator;
    private final TransactionTemplate transactionTemplate;

    public TeamImportPreviewResponse preview(Long sectionId, String uploaderId, MultipartFile file) {
        sectionStaffValidator.validate(sectionId, uploaderId);
        if (sectionRepository.findById(sectionId).isEmpty()) {
            throw new SectionNotFoundException();
        }

        // 임시파일 복사·zip해제·워크북 파싱은 시간이 걸릴 수 있는 순수 I/O라, DB 트랜잭션을
        // 열어둔 채로 하지 않는다(sunzx0428 리뷰 09-03) — 커넥션 풀을 불필요하게 오래
        // 점유하게 됨. 트랜잭션이 필요한 조회·저장 구간만 아래에서 별도로 감싼다.
        List<TeamImportRow> parsedRows = TeamSheetReader.read(file);

        return transactionTemplate.execute(status -> {
            List<TeamImportRow> rows = validate(sectionId, parsedRows);
            TeamImportSummary summary = TeamImportSummary.of(rows);

            ImportBatch batch = ImportBatch.create(uploaderId, sectionId, Type.TEAM,
                JsonConverter.toTree(rows), JsonConverter.toTree(summary),
                LocalDateTime.now().plus(PREVIEW_TTL));

            return new TeamImportPreviewResponse(importBatchRepository.save(batch).getId(), summary, rows);
        });
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

        List<Team> teams = teamRepository.findAllBySectionId(batch.getSectionId());
        Map<String, Long> teamIds = new HashMap<>();
        teams.forEach(team -> teamIds.put(team.getName(), team.getId()));
        // 일반 팀원 변경 경로(TeamMemberCommandService.validateUpdateAllowed)는 확정된 팀의
        // 이동·역할변경을 막는데, 엑셀 임포트는 그 서비스를 안 거쳐서 이 규칙을 우회하고
        // 있었다(sunzx0428 리뷰 09-03) — 여기서도 같은 규칙을 적용한다.
        Map<Long, Status> teamStatusById = new HashMap<>();
        teams.forEach(team -> teamStatusById.put(team.getId(), team.getStatus()));

        // validate() 처럼 분반의 모든 팀원을 한 번에 불러와, plan()/plannedLeaders() 가
        // 행/팀마다 따로 조회하지 않고 이 맵에서 찾도록 한다
        List<TeamMember> existingMembers = teamMemberRepository.findAllByTeamIdIn(
            teams.stream().map(Team::getId).toList());
        Map<String, TeamMember> activeAssignedOf = existingMembers.stream()
            .filter(member -> member.getDeletedAt() == null)
            .collect(Collectors.toMap(TeamMember::getUserId, member -> member, (a, b) -> a));
        Map<Long, String> currentLeaderByTeamId = existingMembers.stream()
            .filter(member -> member.getDeletedAt() == null && member.isLeader())
            .collect(Collectors.toMap(TeamMember::getTeamId, TeamMember::getUserId, (a, b) -> a));

        // 팀장 판정이 행 순서에 좌우되지 않도록, 반영할 행을 먼저 추린 뒤 파일 전체의 팀장 구성을 계산한다
        List<PlannedRow> planned = new ArrayList<>();
        int skipped = plan(batch, teamIds, activeAssignedOf, planned);
        Map<String, String> leaderOf = plannedLeaders(planned, teamIds, currentLeaderByTeamId);

        // 위 teamStatusById는 잠금 없이 읽은 스냅샷이라, 그 사이 다른 요청이
        // TeamCommandService.finalizeTeam()(findByIdForUpdate로 팀을 잠그고 CONFIRMED로
        // 전환)을 실행하면 옛 FORMING 상태를 그대로 믿고 이미 확정된 팀의 팀원을 수정할 수
        // 있다(sunzx0428 리뷰 09-03). 실제로 반영 대상인 팀들만 ID 오름차순으로 잠가(다른
        // 트랜잭션과 반대 순서로 잠가서 나는 데드락 방지, Part 5 참고) 최신 상태로 다시 확인한다.
        Set<Long> touchedTeamIds = new TreeSet<>();
        planned.forEach(row -> {
            Long teamId = row.assigned() != null ? row.assigned().getTeamId() : teamIds.get(row.teamName());
            if (teamId != null) {
                touchedTeamIds.add(teamId);
            }
        });
        touchedTeamIds.forEach(id -> teamStatusById.put(id,
            teamRepository.findByIdForUpdate(id).orElseThrow(TeamNotFoundException::new).getStatus()));

        int createdTeams = 0;
        int appliedMembers = 0;
        // leaderOf 는 파일 전체를 놓고 "최종적으로 누가 팀장이어야 하는가"를 옳게 계산하지만,
        // DB 반영은 한 행씩 순서대로 저장되고 TeamMemberRepositoryImpl.save() 가 저장 시점의
        // 실제 DB 상태로 팀장 중복을 검사한다. 승격 행이 해제 행보다 먼저 저장되면 그 순간
        // 팀에 팀장이 둘이 되어 LeaderAlreadyExistsException 이 나므로, 해제(leader=false)
        // 행을 전부 먼저 반영한 뒤에 승격(leader=true) 행을 반영한다.
        List<PlannedRow> orderedByLeaderLast = planned.stream()
            .sorted(Comparator.comparing(PlannedRow::leader))
            .toList();
        for (PlannedRow row : orderedByLeaderLast) {
            if (row.leader() && !row.studentNumber().equals(leaderOf.get(row.teamName()))) {
                skipped++;
                continue;
            }

            if (row.assigned() != null) {
                if (teamStatusById.get(row.assigned().getTeamId()) == Status.CONFIRMED) {
                    skipped++;
                    continue;
                }
                row.assigned().updateIsLeader(row.leader());
                row.assigned().updateProjectRole(row.projectRole());
                teamMemberRepository.save(row.assigned());
                appliedMembers++;
                continue;
            }

            Long teamId = teamIds.get(row.teamName());
            if (teamId != null && teamStatusById.get(teamId) == Status.CONFIRMED) {
                skipped++;
                continue;
            }
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

    private int plan(ImportBatch batch, Map<String, Long> teamIds, Map<String, TeamMember> activeAssignedOf,
        List<PlannedRow> planned) {
        Set<String> enrolled = activeEnrollments(batch.getSectionId());

        int skipped = 0;
        for (JsonNode row : batch.getPayload()) {
            String status = row.path("status").asText();
            boolean update = UPDATE.name().equals(status);
            if (!update && !VALID.name().equals(status)) {
                if (DUPLICATE.name().equals(status)) {
                    skipped++;
                }
                continue;
            }
            String teamName = row.path("teamName").asText();
            String studentNumber = row.path("studentNumber").asText();

            if (!enrolled.contains(studentNumber)) {
                skipped++;
                continue;
            }

            TeamMember assigned = activeAssignedOf.get(studentNumber);
            if (assigned != null && (!update || !assigned.getTeamId().equals(teamIds.get(teamName)))) {
                skipped++;
                continue;
            }

            planned.add(new PlannedRow(teamName, studentNumber, row.path("leader").asBoolean(),
                row.path("projectRole").asText(), assigned));
        }
        return skipped;
    }

    private Map<String, String> plannedLeaders(List<PlannedRow> planned, Map<String, Long> teamIds,
        Map<Long, String> currentLeaderByTeamId) {
        Map<String, String> leaderOf = new HashMap<>();
        planned.stream().map(PlannedRow::teamName).distinct().forEach(teamName -> {
            Long teamId = teamIds.get(teamName);
            String currentLeader = teamId == null ? null : currentLeaderByTeamId.get(teamId);
            if (currentLeader != null) {
                leaderOf.put(teamName, currentLeader);
            }
        });

        planned.stream().filter(row -> !row.leader())
            .forEach(row -> leaderOf.remove(row.teamName(), row.studentNumber()));
        planned.stream().filter(PlannedRow::leader)
            .forEach(row -> leaderOf.putIfAbsent(row.teamName(), row.studentNumber()));
        return leaderOf;
    }

    // 활성 수강만으로는 부족하다 — preview 이후 탈퇴(소프트삭제)한 계정도 Enrollment는
    // ACTIVE로 남아있을 수 있어서, 활성 사용자 여부와 교집합으로 걸러야 탈퇴 계정의
    // 팀원을 생성·재활성화하는 걸 막을 수 있다(sunzx0428 리뷰 09-03).
    private Set<String> activeEnrollments(Long sectionId) {
        Set<String> enrolled = enrollmentRepository.findAllBySectionId(sectionId).stream()
            .filter(enrollment -> enrollment.getStatus() == ACTIVE)
            .map(Enrollment::getUserId)
            .collect(Collectors.toSet());
        if (enrolled.isEmpty()) {
            return enrolled;
        }
        Set<String> activeUsers = userRepository.findAllByStudentNumberIn(List.copyOf(enrolled)).stream()
            .map(User::getStudentNumber)
            .collect(Collectors.toSet());
        enrolled.retainAll(activeUsers);
        return enrolled;
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
