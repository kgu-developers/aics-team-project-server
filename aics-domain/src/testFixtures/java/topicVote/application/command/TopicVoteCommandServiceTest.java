package topicvote.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicCandidate.exception.TopicCandidateNotFoundException;
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
    private TopicCandidateRepository topicCandidateRepository;
    private TopicVoteCommandService topicVoteCommandService;

    @BeforeEach
    void setUp() {
        topicVoteRepository = mock(TopicVoteRepository.class);
        topicCandidateRepository = mock(TopicCandidateRepository.class);
        topicVoteCommandService = new TopicVoteCommandService(topicVoteRepository, topicCandidateRepository);
        givenActiveCandidate(CANDIDATE_ID, TEAM_ID);
        givenActiveCandidate(CHANGED_CANDIDATE_ID, TEAM_ID);
    }

    private void givenActiveCandidate(Long candidateId, Long teamId) {
        given(topicCandidateRepository.findByIdForUpdate(candidateId)).willReturn(Optional.of(
            TopicCandidate.builder().id(candidateId).teamId(teamId).proposerUserId(VOTER_USER_ID)
                .title("주제 " + candidateId).description("설명").build()
        ));
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
        // given
        TopicVote existingVote = TopicVote.builder()
            .id(1L).teamId(TEAM_ID).candidateId(CANDIDATE_ID).voterUserId(VOTER_USER_ID).build();
        given(topicVoteRepository.findByCandidateIdAndVoterUserIdWithLock(CANDIDATE_ID, VOTER_USER_ID))
            .willReturn(Optional.of(existingVote));
        given(topicVoteRepository.save(any(TopicVote.class))).willReturn(existingVote);

        // when
        topicVoteCommandService.cancelVote(TEAM_ID, CANDIDATE_ID, VOTER_USER_ID);

        // then
        assertThat(existingVote.getDeletedAt()).isNotNull();
        verify(topicVoteRepository).save(existingVote);
    }

    @Test
    @DisplayName("cancelVote는 후보 ID가 다르면 해당 후보의 투표만 취소한다")
    void cancelVote_DeletesOnlySpecificCandidateVote() {
        // given
        Long differentCandidateId = 2L;
        TopicVote existingVote = TopicVote.builder()
            .id(1L).teamId(TEAM_ID).candidateId(differentCandidateId).voterUserId(VOTER_USER_ID).build();
        given(topicVoteRepository.findByCandidateIdAndVoterUserIdWithLock(differentCandidateId, VOTER_USER_ID))
            .willReturn(Optional.of(existingVote));
        given(topicVoteRepository.save(any(TopicVote.class))).willReturn(existingVote);

        // when
        topicVoteCommandService.cancelVote(TEAM_ID, differentCandidateId, VOTER_USER_ID);

        // then
        assertThat(existingVote.getDeletedAt()).isNotNull();
        verify(topicVoteRepository).save(existingVote);
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
    @DisplayName("vote는 저장 직전 후보가 삭제되어 있으면 투표를 저장하지 않는다")
    void vote_Throws_WhenCandidateDeletedBeforeSave() {
        // given
        given(topicCandidateRepository.findByIdForUpdate(CANDIDATE_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> topicVoteCommandService.vote(TEAM_ID, CANDIDATE_ID, VOTER_USER_ID))
            .isInstanceOf(TopicCandidateNotFoundException.class);
        verify(topicVoteRepository, never()).save(any(TopicVote.class));
    }

    @Test
    @DisplayName("vote는 후보가 다른 팀 소속이면 투표를 저장하지 않는다")
    void vote_Throws_WhenCandidateBelongsToAnotherTeam() {
        // given
        givenActiveCandidate(CANDIDATE_ID, 99L);

        // when & then
        assertThatThrownBy(() -> topicVoteCommandService.vote(TEAM_ID, CANDIDATE_ID, VOTER_USER_ID))
            .isInstanceOf(TopicCandidateNotFoundException.class);
        verify(topicVoteRepository, never()).save(any(TopicVote.class));
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
