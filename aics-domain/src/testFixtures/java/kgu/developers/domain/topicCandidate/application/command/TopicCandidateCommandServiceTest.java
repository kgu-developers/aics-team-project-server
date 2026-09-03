package kgu.developers.domain.topicCandidate.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicCandidate.exception.DuplicateTopicCandidateTitleException;
import kgu.developers.domain.topicCandidate.exception.TopicCandidateNotFoundException;

@ExtendWith(MockitoExtension.class)
class TopicCandidateCommandServiceTest {

    @Mock
    private TopicCandidateRepository topicCandidateRepository;

    @InjectMocks
    private TopicCandidateCommandService topicCandidateCommandService;

    @Test
    @DisplayName("등록은 같은 팀에 살아있는 같은 제목이 있으면 거절한다")
    void createRejectsDuplicateActiveTitle() {
        given(topicCandidateRepository.findIncludingDeletedByTeamIdAndTitleForUpdate(100L, "중복 제목"))
                .willReturn(Optional.of(candidate(1L, "중복 제목")));

        assertThatThrownBy(() ->
                topicCandidateCommandService.createTopicCandidate(100L, "20230002", "중복 제목", "설명"))
                .isInstanceOf(DuplicateTopicCandidateTitleException.class);

        verify(topicCandidateRepository, never()).save(any(TopicCandidate.class));
    }

    @Test
    @DisplayName("등록은 소프트 삭제된 제목이면 같은 팀에서 다시 쓸 수 있다")
    void createAllowsReusingDeletedTitle() {
        given(topicCandidateRepository.findIncludingDeletedByTeamIdAndTitleForUpdate(100L, "삭제된 제목"))
                .willReturn(Optional.empty());
        given(topicCandidateRepository.save(any(TopicCandidate.class)))
                .willReturn(candidate(2L, "삭제된 제목"));

        Long id = topicCandidateCommandService.createTopicCandidate(100L, "20230002", "삭제된 제목", "새 설명");

        assertThat(id).isEqualTo(2L);
    }

    @Test
    @DisplayName("등록은 삭제된 후보를 되살릴 때 새 제안자와 설명을 반영한다")
    void createReactivatesWithNewProposerAndDescription() {
        TopicCandidate deleted = candidate(2L, "삭제된 제목");
        deleted.delete();
        given(topicCandidateRepository.findIncludingDeletedByTeamIdAndTitleForUpdate(100L, "삭제된 제목"))
                .willReturn(Optional.of(deleted));
        given(topicCandidateRepository.save(deleted)).willReturn(deleted);

        Long id = topicCandidateCommandService.createTopicCandidate(100L, "20230002", "삭제된 제목", "새 설명");

        assertThat(id).isEqualTo(2L);
        assertThat(deleted.getDeletedAt()).isNull();
        assertThat(deleted.getProposerUserId()).isEqualTo("20230002");
        assertThat(deleted.getDescription()).isEqualTo("새 설명");
    }

    @Test
    @DisplayName("수정도 제목 중복을 검사한다")
    void updateChecksTitleToo() {
        given(topicCandidateRepository.findById(2L))
                .willReturn(Optional.of(candidate(2L, "내 제목")));
        given(topicCandidateRepository.findActiveByTeamIdAndTitleForUpdate(100L, "남의 제목"))
                .willReturn(Optional.of(candidate(1L, "남의 제목")));

        assertThatThrownBy(() -> topicCandidateCommandService.updateTopicCandidate(2L, 100L, "남의 제목", null))
                .isInstanceOf(DuplicateTopicCandidateTitleException.class);

        verify(topicCandidateRepository, never()).save(any(TopicCandidate.class));
    }

    @Test
    @DisplayName("수정은 자기 제목을 그대로 둔 경우를 중복으로 보지 않는다")
    void updateAllowsKeepingOwnTitle() {
        TopicCandidate mine = candidate(1L, "내 제목");
        given(topicCandidateRepository.findById(1L)).willReturn(Optional.of(mine));
        given(topicCandidateRepository.findActiveByTeamIdAndTitleForUpdate(100L, "내 제목"))
                .willReturn(Optional.of(mine));

        topicCandidateCommandService.updateTopicCandidate(1L, 100L, "내 제목", "새 설명");

        assertThat(mine.getDescription()).isEqualTo("새 설명");
        verify(topicCandidateRepository).save(mine);
    }

    @Test
    @DisplayName("없는 주제 후보를 수정하면 예외가 난다")
    void updateThrowsWhenNotFound() {
        given(topicCandidateRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> topicCandidateCommandService.updateTopicCandidate(1L, 100L, "제목", null))
                .isInstanceOf(TopicCandidateNotFoundException.class);
    }

    private TopicCandidate candidate(Long id, String title) {
        return TopicCandidate.builder()
                .id(id)
                .teamId(100L)
                .proposerUserId("20230001")
                .title(title)
                .description("설명")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
