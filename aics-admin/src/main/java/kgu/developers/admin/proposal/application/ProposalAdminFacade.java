package kgu.developers.admin.proposal.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kgu.developers.admin.proposal.presentation.request.ProposalFeedbackAdminRequest;
import kgu.developers.admin.proposal.presentation.response.ProposalFeedbackAdminPageResponse;
import kgu.developers.admin.proposal.presentation.response.ProposalFeedbackAdminResponse;
import kgu.developers.common.response.PageableResponse;
import kgu.developers.domain.project.application.command.ProjectCommandService;
import kgu.developers.domain.project.application.query.ProjectQueryService;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.teammessage.application.command.TeamMessageCommandService;
import kgu.developers.domain.teammessage.application.query.TeamMessageQueryService;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.teamthread.application.command.TeamThreadCommandService;
import kgu.developers.domain.teamthread.application.query.TeamThreadQueryService;
import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.teamthread.exception.TeamThreadNotFoundException;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당 교수가 팀 제안서에 피드백을 남기는 경로. 중간점검(MidReportAdminFacade)과 같은 구조로,
 * 피드백은 팀 메시지함에 relatedType=PROPOSAL 로 쌓이고 제안서는 수정 요청 상태로 리오픈된다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProposalAdminFacade {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Order.desc("id"));

    private final SectionQueryService sectionQueryService;
    private final TeamRepository teamRepository;
    private final ProjectQueryService projectQueryService;
    private final ProjectCommandService projectCommandService;
    private final TeamThreadCommandService teamThreadCommandService;
    private final TeamThreadQueryService teamThreadQueryService;
    private final TeamMessageCommandService teamMessageCommandService;
    private final TeamMessageQueryService teamMessageQueryService;
    private final UserQueryService userQueryService;

    @Transactional
    public ProposalFeedbackAdminResponse postFeedback(
        Long sectionId,
        Long teamId,
        ProposalFeedbackAdminRequest request,
        String professorId
    ) {
        validateProfessorOwnsSectionAndTeam(sectionId, teamId, professorId);
        Project project = projectQueryService.getProjectByTeamId(teamId);

        // 제안 완료 전(팀원 동의 수집 중)이면 reopenProposal이 아무것도 하지 않는다.
        // 중간점검처럼 제출 상태를 강제하지 않는 건, 학생용 피드백 경로(TeamMessageFacade)도
        // 같은 규칙이라 두 경로의 동작이 갈리지 않게 하기 위해서다.
        projectCommandService.reopenProposal(project.getId());

        TeamThread thread = teamThreadCommandService.getOrCreateThread(teamId);
        TeamMessage message = teamMessageCommandService.postMessage(
            thread.getId(),
            professorId,
            TeamMessageRelatedType.PROPOSAL,
            project.getId(),
            request.message()
        );

        return ProposalFeedbackAdminResponse.of(message, teamId, project.getId(), resolveUserName(professorId));
    }

    public ProposalFeedbackAdminPageResponse getFeedbacks(
        Long sectionId,
        Long teamId,
        Pageable pageable,
        String professorId
    ) {
        validateProfessorOwnsSectionAndTeam(sectionId, teamId, professorId);

        Project project;
        TeamThread thread;
        try {
            project = projectQueryService.getProjectByTeamId(teamId);
            thread = teamThreadQueryService.getThread(teamId);
        } catch (ProjectNotFoundException | TeamThreadNotFoundException exception) {
            return emptyFeedbackPage(pageable);
        }

        Page<TeamMessage> messages = teamMessageQueryService.getMessages(
            thread.getId(),
            TeamMessageRelatedType.PROPOSAL,
            project.getId(),
            PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), LATEST_FIRST)
        );

        List<String> senderIds = messages.getContent().stream()
            .map(TeamMessage::getSenderId)
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .toList();
        Map<String, String> senderNames = userQueryService.getUsersByStudentNumbersIncludingDeleted(senderIds).stream()
            .collect(Collectors.toMap(User::getStudentNumber, User::getName, (first, second) -> first));

        return ProposalFeedbackAdminPageResponse.from(messages.map(message ->
            ProposalFeedbackAdminResponse.of(message, teamId, project.getId(), senderNames.get(message.getSenderId()))
        ));
    }

    private ProposalFeedbackAdminPageResponse emptyFeedbackPage(Pageable pageable) {
        return ProposalFeedbackAdminPageResponse.builder()
            .contents(List.of())
            .pageable(PageableResponse.<ProposalFeedbackAdminResponse>builder()
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalPages(0)
                .totalElements(0L)
                .isEnd(true)
                .build())
            .build();
    }

    private void validateProfessorOwnsSectionAndTeam(Long sectionId, Long teamId, String professorId) {
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(sectionId, professorId)) {
            throw new AccessDeniedException("담당 분반의 팀만 접근할 수 있습니다.");
        }
        Team team = teamRepository.findById(teamId)
            .orElseThrow(TeamNotFoundException::new);
        if (!team.getSectionId().equals(sectionId)) {
            throw new AccessDeniedException("해당 분반에 속한 팀이 아닙니다.");
        }
    }

    private String resolveUserName(String studentNumber) {
        if (studentNumber == null || studentNumber.isBlank()) {
            return null;
        }
        try {
            return userQueryService.getUserByStudentNumber(studentNumber).getName();
        } catch (UserNotFoundException e) {
            return null;
        }
    }
}
