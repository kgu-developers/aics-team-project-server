package topicvote.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;
import kgu.developers.domain.topicVote.application.command.TopicVoteCommandService;
import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.domain.TopicVoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class TopicVoteCommandServiceTest {

    private static final Long TEAM_ID = 1L;
    private static final Long CANDIDATE_ID = 1L;
    private static final Long CHANGED_CANDIDATE_ID = 2L;
    private static final String VOTER_USER_ID = "202412345";

    private TopicVoteRepository topicVoteRepository;
    private TopicVoteCommandService topicVoteCommandService;

    @BeforeEach
    void setUp() {
        topicVoteRepository = mock(TopicVoteRepository.class);
        topicVoteCommandService = new TopicVoteCommandService(topicVoteRepository);
    }

    @Test
    @DisplayName("vote는 해당 후보에 첫 투표면 새 투표를 저장한다")
    void vote_SavesNewVote_WhenVoteDoesNotExist() {
        // given
        TopicVote savedVote = TopicVote.builder()
            .id(1L).teamId(TEAM_ID).candidateId(CANDIDATE_ID).voterUserId(VOTER_USER_ID).build();
        given(topicVoteRepository.findByTeamIdAndVoterUserIdWithLock(TEAM_ID, VOTER_USER_ID)).willReturn(Optional.empty());
        given(topicVoteRepository.save(any(TopicVote.class))).willReturn(savedVote);

        // when
        TopicVote result = topicVoteCommandService.vote(TEAM_ID, CANDIDATE_ID, VOTER_USER_ID);

        // then
        ArgumentCaptor<TopicVote> voteCaptor = ArgumentCaptor.forClass(TopicVote.class);
        verify(topicVoteRepository).save(voteCaptor.capture());
        assertThat(voteCaptor.getValue())
            .extracting(TopicVote::getTeamId, TopicVote::getCandidateId, TopicVote::getVoterUserId)
            .containsExactly(TEAM_ID, CANDIDATE_ID, VOTER_USER_ID);
        assertThat(result).isSameAs(savedVote);
    }

    @Test
    @DisplayName("vote는 같은 팀의 다른 후보에 요청하면 기존 투표 후보를 변경한다")
    void vote_ChangesCandidate_WhenVoteAlreadyExistsInTeam() {
        // given
        TopicVote existingVote = TopicVote.builder()
            .id(1L).teamId(TEAM_ID).candidateId(CANDIDATE_ID).voterUserId(VOTER_USER_ID).build();
        given(topicVoteRepository.findByTeamIdAndVoterUserIdWithLock(TEAM_ID, VOTER_USER_ID))
            .willReturn(Optional.of(existingVote));
        given(topicVoteRepository.save(existingVote)).willReturn(existingVote);

        // when
        TopicVote result = topicVoteCommandService.vote(TEAM_ID, CHANGED_CANDIDATE_ID, VOTER_USER_ID);

        // then
        assertThat(result).isSameAs(existingVote);
        assertThat(result.getCandidateId()).isEqualTo(CHANGED_CANDIDATE_ID);
        verify(topicVoteRepository).save(existingVote);
    }

    @Test
    @DisplayName("cancelVote는 팀과 투표자 기준으로 투표를 취소한다")
    void cancelVote_DeletesVote() {
        // when
        topicVoteCommandService.cancelVote(TEAM_ID, VOTER_USER_ID);

        // then
        verify(topicVoteRepository).deleteByTeamIdAndVoterUserId(TEAM_ID, VOTER_USER_ID);
    }

    @Test
    @DisplayName("vote는 취소된 투표 후 재투표를 허용한다")
    void vote_AllowsRevoteAfterCancellation() {
        // given
        TopicVote deletedVote = TopicVote.builder()
            .id(1L).teamId(TEAM_ID).candidateId(CANDIDATE_ID).voterUserId(VOTER_USER_ID)
            .deletedAt(java.time.LocalDateTime.now()).build();
        given(topicVoteRepository.findByTeamIdAndVoterUserIdWithLock(TEAM_ID, VOTER_USER_ID))
            .willReturn(Optional.of(deletedVote));
        given(topicVoteRepository.save(any(TopicVote.class))).willReturn(deletedVote);

        // when
        TopicVote result = topicVoteCommandService.vote(TEAM_ID, CANDIDATE_ID, VOTER_USER_ID);

        // then
        assertThat(result).isSameAs(deletedVote);
        assertThat(result.getDeletedAt()).isNull();
        verify(topicVoteRepository).save(any(TopicVote.class));
    }

    @Test
    @DisplayName("vote는 동시 투표 시 중복 생성을 방지하고 기존 투표를 반환한다")
    void vote_HandlesConcurrentVoteCreation() {
        // given
        TopicVote existingVote = TopicVote.builder()
            .id(1L).teamId(TEAM_ID).candidateId(CANDIDATE_ID).voterUserId(VOTER_USER_ID).build();
        
        given(topicVoteRepository.findByTeamIdAndVoterUserIdWithLock(TEAM_ID, VOTER_USER_ID))
            .willReturn(Optional.empty(), Optional.of(existingVote));

        given(topicVoteRepository.save(any(TopicVote.class)))
            .willThrow(new DataIntegrityViolationException("duplicate key"))
            .willReturn(existingVote);

        // when
        TopicVote result = topicVoteCommandService.vote(TEAM_ID, CANDIDATE_ID, VOTER_USER_ID);

        // then
        assertThat(result).isSameAs(existingVote);
        assertThat(result.getCandidateId()).isEqualTo(CANDIDATE_ID);
    }

    @Test
    @DisplayName("vote는 동시 투표 시 중복 생성을 방지하고 후보를 변경한다")
    void vote_HandlesConcurrentVoteCreationWithDifferentCandidate() {
        // given
        TopicVote existingVote = TopicVote.builder()
            .id(1L).teamId(TEAM_ID).candidateId(CANDIDATE_ID).voterUserId(VOTER_USER_ID).build();
        
        given(topicVoteRepository.findByTeamIdAndVoterUserIdWithLock(TEAM_ID, VOTER_USER_ID))
            .willReturn(Optional.empty(), Optional.of(existingVote));
        
        given(topicVoteRepository.save(any(TopicVote.class)))
            .willThrow(new DataIntegrityViolationException("duplicate key"))
            .willReturn(existingVote);

        // when
        TopicVote result = topicVoteCommandService.vote(TEAM_ID, CHANGED_CANDIDATE_ID, VOTER_USER_ID);

        // then
        assertThat(result).isSameAs(existingVote);
        assertThat(result.getCandidateId()).isEqualTo(CHANGED_CANDIDATE_ID);
    }
}
