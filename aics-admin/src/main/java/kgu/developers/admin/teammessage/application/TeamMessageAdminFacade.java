package kgu.developers.admin.teammessage.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kgu.developers.admin.teammessage.presentation.response.TeamMessageAdminPageResponse;
import kgu.developers.admin.teammessage.presentation.response.TeamMessageRecipientResponse;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.teammessage.application.command.TeamMessageCommandService;
import kgu.developers.domain.teammessage.application.query.TeamMessageQueryService;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.exception.TeamMessageNotGroupedException;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamthread.application.query.TeamThreadQueryService;
import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    private final SectionRepository sectionRepository;
    private final SectionQueryService sectionQueryService;
    private final TeamRepository teamRepository;
    private final TeamThreadQueryService teamThreadQueryService;
    private final TeamMessageCommandService teamMessageCommandService;
    private final TeamMessageQueryService teamMessageQueryService;
    private final UserQueryService userQueryService;
    private final TeamMemberQueryService teamMemberQueryService;

    public TeamMessageAdminPageResponse getMessages(Long sectionId, Pageable pageable, String professorId) {
        return getMessages(sectionId, null, null, null, Sort.Direction.DESC, pageable, professorId);
    }

    public TeamMessageAdminPageResponse getMessages(
        Long sectionId,
        Long teamId,
        kgu.developers.domain.teammessage.domain.TeamMessageRelatedType relatedType,
        String senderKeyword,
        Sort.Direction sortDirection,
        Pageable pageable,
        String professorId
    ) {
        List<Section> sections = resolveSections(sectionId, professorId);
        List<Team> teams = teamRepository.findAllBySectionIdIn(
            sections.stream().map(Section::getId).toList());
        if (teamId != null) {
            teams = teams.stream()
                .filter(team -> team.getId().equals(teamId))
                .toList();
            if (teams.isEmpty()) {
                throw new AccessDeniedException("담당 분반의 팀 메시지만 조회할 수 있습니다.");
            }
        }
        List<TeamThread> threads = teamThreadQueryService.getThreads(teams.stream().map(Team::getId).toList());
        List<Long> threadIds = threads.stream().map(TeamThread::getId).toList();

        // 일괄 발송(broadcastId)으로 생성된 메시지는 관리자 쪽지함에서 한 줄로 묶어서 보여줘야 해서,
        // DB 페이징이 아니라 조건에 맞는 메시지를 전부 가져온 뒤 애플리케이션에서 그룹 단위로 직접 페이징한다.
        // 이 프로젝트 규모(학기 단위, 분반 몇 개)에서는 분반당 메시지 총량이 크지 않아 이 방식이 더 단순하다.
        Pageable fetchAllPageable = PageRequest.of(
            0,
            Integer.MAX_VALUE,
            Sort.by(sortDirection == null ? Sort.Direction.DESC : sortDirection, "id")
        );

        List<String> senderIds = resolveSenderIds(senderKeyword, threadIds);
        if (senderIds != null && senderIds.isEmpty()) {
            long unreadCountForEmptySearch = teamMessageQueryService.countUnread(threadIds, professorId);
            return TeamMessageAdminPageResponse.from(
                Page.empty(pageable), unreadCountForEmptySearch, Set.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of());
        }

        Page<TeamMessage> allMatching = senderIds == null
            ? teamMessageQueryService.getMessages(threadIds, relatedType, fetchAllPageable)
            : teamMessageQueryService.getMessages(threadIds, relatedType, senderIds, fetchAllPageable);

        LinkedHashMap<String, List<TeamMessage>> groupsByKey = new LinkedHashMap<>();
        for (TeamMessage m : allMatching.getContent()) {
            String key = m.getBroadcastId() != null ? m.getBroadcastId() : "msg-" + m.getId();
            groupsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
        }
        List<List<TeamMessage>> groups = new ArrayList<>(groupsByKey.values());
        int totalGroups = groups.size();
        // pageNumber * pageSize를 int로 직접 곱하면 큰 page 값에서 오버플로로 음수가 될 수 있어
        // Pageable.getOffset()(long 연산)으로 구하고, totalGroups와 비교해 범위를 넘으면 빈 페이지로 자른다.
        int from = (int) Math.min(pageable.getOffset(), totalGroups);
        int to = (int) Math.min(pageable.getOffset() + pageable.getPageSize(), totalGroups);
        List<List<TeamMessage>> pageGroups = groups.subList(from, to);

        List<TeamMessage> messages = pageGroups.stream().map(group -> group.get(0)).toList();
        // 목록에 필터(teamId 등)가 걸려 있으면 group.size()는 필터링된 팀 수일 뿐이라,
        // 일괄 발송 당시의 실제 대상 팀 수를 보려면 필터와 무관하게 broadcastId 전체를 다시 조회해야 한다.
        Map<Long, Integer> targetTeamCountByMessageId = messages.stream()
            .collect(Collectors.toMap(TeamMessage::getId, m -> m.getBroadcastId() == null
                ? 1
                : teamMessageQueryService.getMessagesByBroadcastId(m.getBroadcastId()).size()));
        Page<TeamMessage> page = new PageImpl<>(messages, pageable, totalGroups);

        List<Long> pageMessageIds = messages.stream().map(TeamMessage::getId).toList();
        Set<Long> readMessageIds = teamMessageQueryService.findReadMessageIds(professorId, pageMessageIds);
        long unreadCount = teamMessageQueryService.countUnread(threadIds, professorId);

        Map<Long, TeamThread> threadsById = threads.stream()
            .collect(Collectors.toMap(TeamThread::getId, Function.identity()));
        Map<Long, Team> teamsById = teams.stream()
            .collect(Collectors.toMap(Team::getId, Function.identity()));
        Map<Long, Section> sectionsById = sections.stream()
            .collect(Collectors.toMap(Section::getId, Function.identity()));

        List<String> pageSenderIds = messages.stream()
            .map(TeamMessage::getSenderId)
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .toList();
        Map<String, String> senderNamesBySenderId = userQueryService
            .getUsersByStudentNumbersIncludingDeleted(pageSenderIds).stream()
            .collect(Collectors.toMap(User::getStudentNumber, User::getName, (first, second) -> first));
        Map<Long, String> leaderNamesByTeamId = teamMemberQueryService
            .getLeaderNamesByTeamIds(teams.stream().map(Team::getId).toList());

        return TeamMessageAdminPageResponse.from(
            page, unreadCount, readMessageIds, threadsById, teamsById, sectionsById, senderNamesBySenderId,
            targetTeamCountByMessageId, leaderNamesByTeamId);
    }

    public List<TeamMessageRecipientResponse> getRecipientTeams(Long messageId, String professorId) {
        TeamMessage message = teamMessageQueryService.getMessage(messageId);
        TeamThread thread = teamThreadQueryService.getThreadById(message.getThreadId());
        Team representativeTeam = teamRepository.findById(thread.getTeamId())
            .orElseThrow(TeamNotFoundException::new);

        if (!sectionQueryService.isActiveSectionOwnedByProfessor(representativeTeam.getSectionId(), professorId)) {
            throw new AccessDeniedException("담당 분반의 메시지만 조회할 수 있습니다.");
        }

        // 일괄발송은 분반 하나 안에서만 전송되므로, 대표 메시지의 분반 소유권 검증만으로
        // 같은 broadcastId로 묶인 나머지 수신 팀 전체에 대한 권한도 보장된다.
        List<TeamMessage> group = message.getBroadcastId() == null
            ? List.of(message)
            : teamMessageQueryService.getMessagesByBroadcastId(message.getBroadcastId());
        if (group.size() <= 1) {
            throw new TeamMessageNotGroupedException();
        }

        List<Long> teamIds = group.stream()
            .map(m -> teamThreadQueryService.getThreadById(m.getThreadId()).getTeamId())
            .distinct()
            .toList();
        Map<Long, Team> teamsById = teamRepository.findAllById(teamIds).stream()
            .collect(Collectors.toMap(Team::getId, Function.identity()));
        Map<Long, String> leaderNamesByTeamId = teamMemberQueryService.getLeaderNamesByTeamIds(teamIds);

        return teamIds.stream()
            .map(teamId -> {
                Team team = Optional.ofNullable(teamsById.get(teamId)).orElseThrow(TeamNotFoundException::new);
                return TeamMessageRecipientResponse.from(team, leaderNamesByTeamId.get(teamId));
            })
            .toList();
    }

    @Transactional
    public void markAsRead(Long messageId, String professorId) {
        TeamMessage message = teamMessageQueryService.getMessage(messageId);
        TeamThread thread = teamThreadQueryService.getThreadById(message.getThreadId());
        Team team = teamRepository.findById(thread.getTeamId())
            .orElseThrow(TeamNotFoundException::new);

        if (!sectionQueryService.isActiveSectionOwnedByProfessor(team.getSectionId(), professorId)) {
            throw new AccessDeniedException("담당 분반의 메시지만 읽음 처리할 수 있습니다.");
        }

        List<Long> targetMessageIds = teamMessageQueryService.resolveMarkAsReadTargetIds(message, true);
        targetMessageIds.forEach(id -> teamMessageCommandService.markAsRead(id, professorId));
    }

    private List<Section> resolveSections(Long sectionId, String professorId) {
        if (sectionId == null) {
            return sectionRepository.findAllByProfessorId(professorId).stream()
                .map(SectionDetail::section)
                .toList();
        }

        if (!sectionQueryService.isActiveSectionOwnedByProfessor(sectionId, professorId)) {
            throw new AccessDeniedException("담당 분반의 메시지만 조회할 수 있습니다.");
        }
        return List.of(sectionQueryService.getSectionById(sectionId).section());
    }

    private List<String> resolveSenderIds(String senderKeyword, List<Long> threadIds) {
        if (senderKeyword == null || senderKeyword.isBlank()) {
            return null;
        }
        String keyword = senderKeyword.trim();

        // 접근 가능한 스레드에 실제로 메시지를 남긴 적 있는 모든 발신자를 후보로 삼는다.
        // 팀에서 탈퇴했거나 교수가 바뀐 경우에도 과거 발신자를 검색할 수 있어야 한다.
        List<String> candidateIds = teamMessageQueryService.findDistinctSenderIds(threadIds);

        return userQueryService.getUsersByStudentNumbersIncludingDeleted(candidateIds)
            .stream()
            .filter(user -> user.getStudentNumber().contains(keyword) || user.getName().contains(keyword))
            .map(User::getStudentNumber)
            .distinct()
            .toList();
    }
}
