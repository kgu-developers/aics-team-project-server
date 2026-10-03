package kgu.developers.admin.meetingrecord.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import kgu.developers.admin.meetingrecord.presentation.response.MeetingRecordEditLogAdminPageResponse;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordEditLogQueryService;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MeetingRecordEditLogAdminFacade {

    private static final Sort LATEST_FIRST = Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.desc("id")
    );

    private final SectionRepository sectionRepository;
    private final TeamRepository teamRepository;
    private final MeetingRecordQueryService meetingRecordQueryService;
    private final MeetingRecordEditLogQueryService meetingRecordEditLogQueryService;
    private final UserQueryService userQueryService;

    public MeetingRecordEditLogAdminPageResponse getSectionMeetingRecordLogs(
        Long sectionId,
        Long teamId,
        Long meetingRecordId,
        Pageable pageable,
        String professorId
    ) {
        Section section = resolveOwnedSection(sectionId, professorId);
        List<Team> teams = teamRepository.findAllBySectionId(section.getId());
        List<Long> teamIds = resolveTeamIds(teams, teamId);
        validateMeetingRecordFilter(meetingRecordId, teamIds);

        Page<MeetingRecordEditLog> logs = meetingRecordEditLogQueryService.getSectionLogs(
            teamIds,
            meetingRecordId,
            PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), LATEST_FIRST)
        );

        return MeetingRecordEditLogAdminPageResponse.from(
            logs,
            meetingRecordsById(logs.getContent()),
            teams.stream().collect(Collectors.toMap(Team::getId, Function.identity())),
            editorsByStudentNumber(logs.getContent())
        );
    }

    // 존재하지 않는 분반과 다른 교수의 분반을 같은 403으로 돌려 존재 여부가 드러나지 않게 한다.
    private Section resolveOwnedSection(Long sectionId, String professorId) {
        return sectionRepository.findById(sectionId)
            .map(SectionDetail::section)
            .filter(section -> professorId.equals(section.getProfessorId()))
            .orElseThrow(() -> new AccessDeniedException("담당 분반의 회의록 수정 이력만 조회할 수 있습니다."));
    }

    private List<Long> resolveTeamIds(List<Team> teams, Long teamId) {
        if (teamId == null) {
            return teams.stream().map(Team::getId).toList();
        }

        boolean belongsToSection = teams.stream()
            .anyMatch(team -> team.getId().equals(teamId));
        if (!belongsToSection) {
            throw new AccessDeniedException("담당 분반의 회의록 수정 이력만 조회할 수 있습니다.");
        }
        return List.of(teamId);
    }

    private void validateMeetingRecordFilter(Long meetingRecordId, List<Long> teamIds) {
        if (meetingRecordId == null) {
            return;
        }

        boolean belongsToSection = meetingRecordQueryService.getMeetingRecords(List.of(meetingRecordId)).stream()
            .anyMatch(meetingRecord -> teamIds.contains(meetingRecord.getTeamId()));
        if (!belongsToSection) {
            throw new AccessDeniedException("담당 분반의 회의록 수정 이력만 조회할 수 있습니다.");
        }
    }

    private Map<Long, MeetingRecord> meetingRecordsById(List<MeetingRecordEditLog> logs) {
        List<Long> meetingRecordIds = logs.stream()
            .map(MeetingRecordEditLog::getMeetingRecordId)
            .distinct()
            .toList();
        if (meetingRecordIds.isEmpty()) {
            return Map.of();
        }
        return meetingRecordQueryService.getMeetingRecords(meetingRecordIds).stream()
            .collect(Collectors.toMap(MeetingRecord::getId, Function.identity()));
    }

    private Map<String, User> editorsByStudentNumber(List<MeetingRecordEditLog> logs) {
        List<String> editorIds = logs.stream()
            .map(MeetingRecordEditLog::getEditorId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (editorIds.isEmpty()) {
            return Map.of();
        }
        return userQueryService.getUsersByStudentNumbers(editorIds).stream()
            .collect(Collectors.toMap(User::getStudentNumber, Function.identity()));
    }
}
