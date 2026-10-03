package kgu.developers.domain.submission.application.query;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordRepository;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportRepository;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.submission.domain.ArtifactType;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionArtifact;
import kgu.developers.domain.submission.domain.SubmissionArtifactRepository;
import kgu.developers.domain.submission.domain.SubmissionRepository;
import kgu.developers.domain.submission.domain.SubmissionStatus;
import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.submission.domain.SubmissionVersionRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SectionArtifactQueryService {
    private static final Set<MilestoneType> SUBMITTABLE_TYPES = EnumSet.of(
            MilestoneType.PROPOSAL,
            MilestoneType.MID_REPORT,
            MilestoneType.PRESENTATION,
            MilestoneType.FINAL_REPORT);
    private static final FileStats NO_FILES = new FileStats(0, 0, 0L);
    // 팀명을 문자열로만 세우면 "1팀, 10팀, 2팀"이 된다. 앞자리 숫자를 먼저 보고, 없으면 뒤로 민다.
    private static final Pattern LEADING_NUMBER = Pattern.compile("^(\\d{1,9})");
    private static final Comparator<Team> BY_TEAM_NAME =
            Comparator.comparingInt((Team team) -> leadingNumber(team.getName()))
                    .thenComparing(Team::getName, Comparator.nullsLast(Comparator.naturalOrder()));
    // 분반 하나의 회의록을 한 페이지로 받기 위한 상한. 분반당 회의록이 이 수를 넘으면 집계가 잘린다.
    // ponytail: 잘리더라도 호출마다 같은 집합이 나오도록 id로 정렬만 해 둔다. 집계 전용 프로젝션
    // 쿼리(group by team_id로 count·sum(version))를 추가하면 상한 자체가 사라진다.
    private static final int MAX_MEETING_RECORDS = 10_000;

    private final MilestoneRepository milestoneRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionVersionRepository submissionVersionRepository;
    private final SubmissionArtifactRepository submissionArtifactRepository;
    private final FileObjectRepository fileObjectRepository;
    private final MeetingRecordRepository meetingRecordRepository;
    private final MidReportRepository midReportRepository;
    private final UserRepository userRepository;

    public List<SectionArtifactTeamRow> getSectionArtifactRows(Long sectionId, LocalDateTime until) {
        List<Team> teams = teamRepository.findAllBySectionId(sectionId).stream()
                .sorted(BY_TEAM_NAME)
                .toList();
        List<Milestone> milestones = submittableMilestones(sectionId);
        List<Long> teamIds = teams.stream().map(Team::getId).toList();

        Map<Long, List<SectionArtifactMember>> members = membersByTeam(teamIds);
        Map<Long, Map<Long, Submission>> submissions = submissionsByMilestoneAndTeam(milestones);
        Map<Long, List<SubmissionVersion>> versions = versionsBySubmission(submissions, until);
        Map<Long, FileStats> fileStats = fileStatsByVersion(latestVersionIds(versions));
        Map<Long, Map<Long, MidReport>> midReports = midReportsByMilestoneAndTeam(milestones, teamIds);

        List<SectionArtifactTeamRow> rows = new ArrayList<>();
        Map<Long, List<MeetingRecord>> meetingRecords = meetingRecordsByTeam(teamIds, until);
        for (Team team : teams) {
            List<SectionArtifactStageRow> stages = milestones.stream()
                    .map(milestone -> stageRow(team, milestone, submissions, versions, fileStats, midReports, until))
                    .toList();
            List<MeetingRecord> teamRecords = meetingRecords.getOrDefault(team.getId(), List.of());
            rows.add(new SectionArtifactTeamRow(
                    team.getId(),
                    team.getName(),
                    members.getOrDefault(team.getId(), List.of()),
                    teamRecords.size(),
                    // ponytail: 회의록 수정 이력 테이블이 없어서 낙관적 락 version(= 수정 횟수) 합으로
                    // 근사한다. 기준일 이후의 수정도 섞이므로, 수정 이력을 따로 적재하면 그걸로 바꿀 것.
                    teamRecords.stream().mapToLong(MeetingRecord::getVersion).sum(),
                    stages));
        }
        return rows;
    }

    // 팀마다 조회하면 팀 수만큼(구현체는 호출당 3쿼리) 늘어나므로 분반의 팀 전체를 한 번에 받는다.
    private Map<Long, List<MeetingRecord>> meetingRecordsByTeam(List<Long> teamIds, LocalDateTime until) {
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        return meetingRecordRepository
                .findAllByTeamIdIn(teamIds, PageRequest.of(0, MAX_MEETING_RECORDS, Sort.by("id")))
                .getContent().stream()
                .filter(record -> record.getCreatedAt() == null || !record.getCreatedAt().isAfter(until))
                .collect(groupingBy(MeetingRecord::getTeamId));
    }

    private SectionArtifactStageRow stageRow(
            Team team,
            Milestone milestone,
            Map<Long, Map<Long, Submission>> submissions,
            Map<Long, List<SubmissionVersion>> versions,
            Map<Long, FileStats> fileStats,
            Map<Long, Map<Long, MidReport>> midReports,
            LocalDateTime until
    ) {
        Submission submission = submissions.getOrDefault(milestone.getId(), Map.of()).get(team.getId());
        List<SubmissionVersion> submitted = submission == null
                ? List.of()
                : versions.getOrDefault(submission.getId(), List.of());
        LocalDateTime dueAt = milestone.getSchedule() == null ? null : milestone.getSchedule().dueAt();

        if (!submitted.isEmpty()) {
            SubmissionVersion latest = submitted.get(submitted.size() - 1);
            LocalDateTime firstSubmittedAt = submitted.get(0).getSubmittedAt();
            FileStats stats = fileStats.getOrDefault(latest.getId(), NO_FILES);
            return new SectionArtifactStageRow(
                    milestone.getType(),
                    dueAt,
                    submission.getStatus(),
                    firstSubmittedAt,
                    latest.getSubmittedAt(),
                    latest.getVersion(),
                    late(dueAt, firstSubmittedAt),
                    false,
                    stats.fileCount(),
                    stats.imageCount(),
                    stats.totalSize());
        }

        // 중간점검은 파일 제출이 아니라 블록 양식이라 제출 이력이 MidReport에만 남는다
        // (SubmissionAdminFacade가 상태를 덮어쓰는 것과 같은 이유). 버전·제출 시각도 거기서 읽는다.
        MidReport midReport = midReports.getOrDefault(milestone.getId(), Map.of()).get(team.getId());
        if (midReport != null && midReport.getCreatedAt() != null && midReport.getCreatedAt().isAfter(until)) {
            midReport = null;  // 기준일 이후에 만들어진 행은 그때 없었던 것으로 본다
        }
        if (midReport != null && midReport.getSubmittedAt() != null
                && !midReport.getSubmittedAt().isAfter(until)) {
            return new SectionArtifactStageRow(
                    milestone.getType(),
                    dueAt,
                    midReportStatus(midReport),
                    midReport.getSubmittedAt(),
                    midReport.getSubmittedAt(),
                    midReport.getVersion() == null ? null : midReport.getVersion().intValue(),
                    midReportLate(dueAt, midReport),
                    false,
                    0,
                    0,
                    0L);
        }

        return new SectionArtifactStageRow(
                milestone.getType(),
                dueAt,
                SubmissionStatus.NOT_SUBMITTED,
                null,
                null,
                null,
                null,
                dueAt != null && !dueAt.isAfter(until),
                0,
                0,
                0L);
    }

    // 중간점검은 submit()이 submittedAt을 덮어써서 반려 후 재제출하면 최초 제출 시각이 남지 않는다.
    // 반려 이력이 있으면 지각 여부를 알 수 없으므로 판정을 포기한다(반려가 없었다면 그 값이 곧 최초 제출이다).
    private Boolean midReportLate(LocalDateTime dueAt, MidReport midReport) {
        if (midReport.getRevision() != null) {
            return null;
        }
        return late(dueAt, midReport.getSubmittedAt());
    }

    private SubmissionStatus midReportStatus(MidReport midReport) {
        return switch (midReport.getStatus()) {
            case SUBMITTED -> SubmissionStatus.SUBMITTED;
            case REVISION_REQUESTED -> SubmissionStatus.REVISION_REQUESTED;
            case DRAFT -> SubmissionStatus.NOT_SUBMITTED;
        };
    }

    private List<Milestone> submittableMilestones(Long sectionId) {
        return milestoneRepository.findAllBySectionIdOrderByWeekNumber(sectionId).stream()
                .filter(milestone -> milestone.getStatus() != MilestoneStatus.DRAFT)
                .filter(milestone -> SUBMITTABLE_TYPES.contains(milestone.getType()))
                .toList();
    }

    private Map<Long, List<SectionArtifactMember>> membersByTeam(List<Long> teamIds) {
        List<TeamMember> members = teamMemberRepository.findAllByTeamIdIn(teamIds);
        if (members.isEmpty()) {
            return Map.of();
        }
        // 팀원이 그 뒤 탈퇴(소프트 삭제)했더라도 이름이 계속 보여야 한다(SubmissionAdminFacade와 같은 이유).
        Map<String, String> names = userRepository.findAllIncludingDeletedByStudentNumberIn(
                        members.stream().map(TeamMember::getUserId).distinct().toList()).stream()
                .collect(toMap(User::getStudentNumber, User::getName, (first, ignored) -> first));
        return members.stream()
                .sorted(Comparator.comparing((TeamMember member) -> !member.isLeader())
                        .thenComparing(TeamMember::getUserId))
                .collect(groupingBy(TeamMember::getTeamId,
                        mapping(member -> new SectionArtifactMember(
                                member.getUserId(), names.get(member.getUserId())), toList())));
    }

    private Map<Long, Map<Long, Submission>> submissionsByMilestoneAndTeam(List<Milestone> milestones) {
        Map<Long, Map<Long, Submission>> submissions = new HashMap<>();
        for (Milestone milestone : milestones) {
            submissions.put(milestone.getId(), submissionRepository.findAllByMilestoneId(milestone.getId()).stream()
                    .collect(toMap(Submission::getTeamId, identity(), (first, ignored) -> first)));
        }
        return submissions;
    }

    private Map<Long, List<SubmissionVersion>> versionsBySubmission(
            Map<Long, Map<Long, Submission>> submissions,
            LocalDateTime until
    ) {
        List<Long> submissionIds = submissions.values().stream()
                .flatMap(byTeam -> byTeam.values().stream())
                .map(Submission::getId)
                .toList();
        return submissionVersionRepository.findAllBySubmissionIdIn(submissionIds).stream()
                .filter(version -> version.getSubmittedAt() != null && !version.getSubmittedAt().isAfter(until))
                .sorted(Comparator.comparingInt(SubmissionVersion::getVersion))
                .collect(groupingBy(SubmissionVersion::getSubmissionId));
    }

    private List<Long> latestVersionIds(Map<Long, List<SubmissionVersion>> versions) {
        return versions.values().stream()
                .map(list -> list.get(list.size() - 1))
                .map(SubmissionVersion::getId)
                .toList();
    }

    private Map<Long, FileStats> fileStatsByVersion(List<Long> versionIds) {
        List<SubmissionArtifact> artifacts = submissionArtifactRepository.findAllByVersionIdIn(versionIds).stream()
                .filter(artifact -> artifact.getType() == ArtifactType.FILE && artifact.getFileId() != null)
                .toList();
        if (artifacts.isEmpty()) {
            return Map.of();
        }
        Map<Long, FileObject> files = fileObjectRepository.findAllByIdAndDeletedAtIsNull(
                        artifacts.stream().map(SubmissionArtifact::getFileId).distinct().toList()).stream()
                .collect(toMap(FileObject::getId, identity(), (first, ignored) -> first));

        Map<Long, FileStats> stats = new HashMap<>();
        for (SubmissionArtifact artifact : artifacts) {
            FileObject file = files.get(artifact.getFileId());
            if (file == null) {
                continue;  // 삭제된 파일은 개수·용량에서 뺀다
            }
            boolean image = file.getContentType() != null
                    && file.getContentType().toLowerCase().startsWith("image/");
            FileStats previous = stats.getOrDefault(artifact.getVersionId(), NO_FILES);
            stats.put(artifact.getVersionId(), new FileStats(
                    previous.fileCount() + 1,
                    previous.imageCount() + (image ? 1 : 0),
                    previous.totalSize() + file.getSize()));
        }
        return stats;
    }

    private Map<Long, Map<Long, MidReport>> midReportsByMilestoneAndTeam(
            List<Milestone> milestones,
            List<Long> teamIds
    ) {
        Map<Long, Map<Long, MidReport>> midReports = new HashMap<>();
        for (Milestone milestone : milestones) {
            if (milestone.getType() != MilestoneType.MID_REPORT) {
                continue;
            }
            midReports.put(milestone.getId(),
                    midReportRepository.findAllByTeamIdInAndMilestoneId(teamIds, milestone.getId()).stream()
                            .collect(toMap(MidReport::getTeamId, identity(), (first, ignored) -> first)));
        }
        return midReports;
    }

    private static int leadingNumber(String teamName) {
        if (teamName == null) {
            return Integer.MAX_VALUE;
        }
        Matcher matcher = LEADING_NUMBER.matcher(teamName);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : Integer.MAX_VALUE;
    }

    private Boolean late(LocalDateTime dueAt, LocalDateTime firstSubmittedAt) {
        if (dueAt == null || firstSubmittedAt == null) {
            return null;
        }
        return firstSubmittedAt.isAfter(dueAt);
    }

    private record FileStats(int fileCount, int imageCount, long totalSize) {
    }
}
