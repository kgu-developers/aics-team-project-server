package kgu.developers.admin.meetingrecord.application;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kgu.developers.admin.meetingrecord.presentation.response.MeetingRecordAdminPageResponse;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
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
public class MeetingRecordAdminFacade {

    private static final Sort LATEST_FIRST = Sort.by(
        Sort.Order.desc("meetingAt"),
        Sort.Order.desc("id")
    );

    private final SectionRepository sectionRepository;
    private final TeamRepository teamRepository;
    private final MeetingRecordQueryService meetingRecordQueryService;

    public MeetingRecordAdminPageResponse getMeetingRecords(
        Long sectionId,
        Pageable pageable,
        String professorId
    ) {
        List<Section> sections = resolveSections(sectionId, professorId);
        List<Team> teams = sections.stream()
            .flatMap(section -> teamRepository.findAllBySectionId(section.getId()).stream())
            .toList();

        Pageable latestFirstPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            LATEST_FIRST
        );
        Page<MeetingRecord> meetingRecords = meetingRecordQueryService.getMeetingRecords(
            teams.stream().map(Team::getId).toList(), latestFirstPageable);
        Map<Long, Team> teamsById = teams.stream()
            .collect(Collectors.toMap(Team::getId, Function.identity()));
        Map<Long, Section> sectionsById = sections.stream()
            .collect(Collectors.toMap(Section::getId, Function.identity()));

        return MeetingRecordAdminPageResponse.from(meetingRecords, teamsById, sectionsById);
    }

    private List<Section> resolveSections(Long sectionId, String professorId) {
        if (sectionId == null) {
            return sectionRepository.findAllByProfessorId(professorId).stream()
                .map(SectionDetail::section)
                .toList();
        }

        Section section = sectionRepository.findById(sectionId)
            .orElseThrow(SectionNotFoundException::new)
            .section();
        if (!professorId.equals(section.getProfessorId())) {
            throw new AccessDeniedException("담당 분반의 회의록만 조회할 수 있습니다.");
        }
        return List.of(section);
    }
}
