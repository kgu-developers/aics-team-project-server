package kgu.developers.admin.teammessage.application;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kgu.developers.admin.teammessage.presentation.response.TeamMessageAdminPageResponse;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teammessage.application.query.TeamMessageQueryService;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teamthread.application.query.TeamThreadQueryService;
import kgu.developers.domain.teamthread.domain.TeamThread;
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
public class TeamMessageAdminFacade {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Order.desc("id"));

    private final SectionRepository sectionRepository;
    private final TeamRepository teamRepository;
    private final TeamThreadQueryService teamThreadQueryService;
    private final TeamMessageQueryService teamMessageQueryService;

    public TeamMessageAdminPageResponse getMessages(Long sectionId, Pageable pageable, String professorId) {
        List<Section> sections = resolveSections(sectionId, professorId);
        List<Team> teams = teamRepository.findAllBySectionIdIn(
            sections.stream().map(Section::getId).toList());
        List<TeamThread> threads = teamThreadQueryService.getThreads(teams.stream().map(Team::getId).toList());
        List<Long> threadIds = threads.stream().map(TeamThread::getId).toList();

        Pageable latestFirstPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            LATEST_FIRST
        );
        Page<TeamMessage> messages = teamMessageQueryService.getMessages(threadIds, latestFirstPageable);
        List<Long> pageMessageIds = messages.getContent().stream().map(TeamMessage::getId).toList();
        Set<Long> readMessageIds = teamMessageQueryService.findReadMessageIds(professorId, pageMessageIds);
        long unreadCount = teamMessageQueryService.countUnread(threadIds, professorId);

        Map<Long, TeamThread> threadsById = threads.stream()
            .collect(Collectors.toMap(TeamThread::getId, Function.identity()));
        Map<Long, Team> teamsById = teams.stream()
            .collect(Collectors.toMap(Team::getId, Function.identity()));
        Map<Long, Section> sectionsById = sections.stream()
            .collect(Collectors.toMap(Section::getId, Function.identity()));

        return TeamMessageAdminPageResponse.from(
            messages, unreadCount, readMessageIds, threadsById, teamsById, sectionsById);
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
            throw new AccessDeniedException("담당 분반의 메시지만 조회할 수 있습니다.");
        }
        return List.of(section);
    }
}
