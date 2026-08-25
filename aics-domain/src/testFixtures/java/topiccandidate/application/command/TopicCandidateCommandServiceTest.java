package topiccandidate.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import kgu.developers.domain.topicCandidate.application.command.TopicCandidateCommandService;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TopicCandidateCommandServiceTest {

    private static final Long TEAM_ID = 1L;
    private static final String PROPOSER_USER_ID = "202412345";
    private static final String TITLE = "AI 기반 학습 도우미";
    private static final String DESCRIPTION = "학생별 맞춤형 학습 계획을 지원합니다.";

    private TopicCandidateRepository topicCandidateRepository;
    private TopicCandidateCommandService topicCandidateCommandService;

    @BeforeEach
    void setUp() {
        topicCandidateRepository = mock(TopicCandidateRepository.class);
        topicCandidateCommandService = new TopicCandidateCommandService(topicCandidateRepository);
    }

    @Test
    @DisplayName("createTopicCandidate는 전달받은 정보로 후보를 저장하고 저장 결과를 반환한다")
    void createTopicCandidate_SavesAndReturnsCandidate() {
        // given
        TopicCandidate savedCandidate = TopicCandidate.builder()
            .id(1L)
            .teamId(TEAM_ID)
            .proposerUserId(PROPOSER_USER_ID)
            .title(TITLE)
            .description(DESCRIPTION)
            .build();
        given(topicCandidateRepository.save(any(TopicCandidate.class))).willReturn(savedCandidate);

        // when
        TopicCandidate result = topicCandidateCommandService.createTopicCandidate(
            TEAM_ID, PROPOSER_USER_ID, TITLE, DESCRIPTION
        );

        // then
        ArgumentCaptor<TopicCandidate> candidateCaptor = ArgumentCaptor.forClass(TopicCandidate.class);
        verify(topicCandidateRepository).save(candidateCaptor.capture());
        assertThat(candidateCaptor.getValue())
            .extracting(
                TopicCandidate::getTeamId,
                TopicCandidate::getProposerUserId,
                TopicCandidate::getTitle,
                TopicCandidate::getDescription
            )
            .containsExactly(TEAM_ID, PROPOSER_USER_ID, TITLE, DESCRIPTION);
        assertThat(result).isSameAs(savedCandidate);
    }
}
