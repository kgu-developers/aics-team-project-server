package kgu.developers.api.topiccandidate.application;

import java.util.List;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.topiccandidate.presentation.request.TopicCandidateCreateRequest;
import kgu.developers.api.topiccandidate.presentation.request.TopicFinalizeRequest;
import kgu.developers.api.topiccandidate.presentation.response.TopicCandidateListResponse;
import kgu.developers.api.topiccandidate.presentation.response.TopicCandidatePersistResponse;
import kgu.developers.api.topiccandidate.presentation.response.TopicFinalizeResponse;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.topicCandidate.application.command.TopicCandidateCommandService;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicCandidate.exception.TopicCandidateNotFoundException;
import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.domain.TopicVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TopicCandidateFacade {

    private final TopicCandidateCommandService topicCandidateCommandService;
    private final TopicCandidateRepository topicCandidateRepository;
    private final TopicVoteRepository topicVoteRepository;
    private final ProjectRepository projectRepository;
    private final TeamAccessValidator teamAccessValidator;

    public TopicCandidateListResponse getTopicCandidates(Long teamId, String userId) {
        teamAccessValidator.validateMembership(teamId, userId);

        List<TopicCandidate> candidates = topicCandidateRepository.findByTeamId(teamId);
        List<TopicVote> votes = topicVoteRepository.findAllByCandidateIdIn(
            candidates.stream().map(TopicCandidate::getId).toList()
        );
        return TopicCandidateListResponse.of(candidates, votes, userId);
    }

    @Transactional
    public TopicCandidatePersistResponse createTopicCandidate(
        Long teamId,
        String proposerUserId,
        TopicCandidateCreateRequest request
    ) {
        teamAccessValidator.validateMembership(teamId, proposerUserId);
        TopicCandidate topicCandidate = topicCandidateCommandService.createTopicCandidate(
            teamId,
            proposerUserId,
            request.title(),
            request.description()
        );
        return TopicCandidatePersistResponse.of(topicCandidate);
    }

    @Transactional
    public TopicFinalizeResponse finalizeTopic(Long teamId, String userId, TopicFinalizeRequest request) {
        teamAccessValidator.validateLeader(teamId, userId);

        TopicCandidate topicCandidate = topicCandidateRepository.findById(request.candidateId())
            .orElseThrow(TopicCandidateNotFoundException::new);
        if (!teamId.equals(topicCandidate.getTeamId())) {
            throw new AccessDeniedException("해당 팀의 주제 후보만 확정할 수 있습니다.");
        }

        Project project = projectRepository.findAllByTeamId(teamId).stream()
            .findFirst()
            .orElseGet(() -> Project.create(
                teamId,
                topicCandidate.getTitle(),
                topicCandidate.getDescription(),
                topicCandidate.getDescription(),
                null,
                null,
                ApprovalStatus.DRAFT,
                null
            ));
        project.updateTitle(topicCandidate.getTitle());
        project.updateTopicCandidateId(topicCandidate.getId());
        Project savedProject = projectRepository.save(project);
        return TopicFinalizeResponse.of(savedProject, topicCandidate);
    }
}
