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
import org.mockito.ArgumentCaptor;
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
    @DisplayName("등록은 같은 제목이 없으면 새 후보를 저장한다")
    void createSavesNewCandidateWhenNoneExists() {
        given(topicCandidateRepository.findIncludingDeletedByTeamIdAndTitleForUpdate(100L, "새 제목"))
                .willReturn(Optional.empty());
        given(topicCandidateRepository.save(any(TopicCandidate.class)))
                .willReturn(candidate(2L, "새 제목"));

        Long id = topicCandidateCommandService.createTopicCandidate(100L, "20230002", "새 제목", "새 설명");

        assertThat(id).isEqualTo(2L);
        TopicCandidate saved = savedCandidate();
        assertThat(saved.getId()).isNull();
        assertThat(saved.getProposerUserId()).isEqualTo("20230002");
        assertThat(saved.getDescription()).isEqualTo("새 설명");
    }

    @Test
    @DisplayName("등록은 소프트 삭제된 후보를 되살리며 새 제안자와 설명을 반영한다")
    void createReactivatesDeletedCandidate() {
        TopicCandidate deleted = candidate(2L, "삭제된 제목");
        deleted.delete();
        given(topicCandidateRepository.findIncludingDeletedByTeamIdAndTitleForUpdate(100L, "삭제된 제목"))
                .willReturn(Optional.of(deleted));
        given(topicCandidateRepository.save(any(TopicCandidate.class))).willReturn(deleted);

        Long id = topicCandidateCommandService.createTopicCandidate(100L, "20230002", "삭제된 제목", "새 설명");

        assertThat(id).isEqualTo(2L);
        TopicCandidate saved = savedCandidate();
        assertThat(saved.getId()).isEqualTo(2L);
        assertThat(saved.getDeletedAt()).isNull();
        assertThat(saved.getProposerUserId()).isEqualTo("20230002");
        assertThat(saved.getDescription()).isEqualTo("새 설명");
    }

    @Test
    @DisplayName("수정은 후보가 속한 팀을 그대로 유지한다")
    void updateKeepsTeamId() {
        TopicCandidate mine = candidate(1L, "내 제목");
        given(topicCandidateRepository.findByIdForUpdate(1L)).willReturn(Optional.of(mine));

        topicCandidateCommandService.updateTopicCandidate(1L, null, "새 설명");

        assertThat(savedCandidate().getTeamId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("삭제는 수정과 같은 잠금 경로로 후보를 읽고 소프트 삭제한다")
    void deleteUsesSameLockedRead() {
        TopicCandidate mine = candidate(1L, "내 제목");
        given(topicCandidateRepository.findByIdForUpdate(1L)).willReturn(Optional.of(mine));

        topicCandidateCommandService.deleteTopicCandidate(1L);

        assertThat(savedCandidate().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("없는 주제 후보를 삭제하면 예외가 난다")
    void deleteThrowsWhenNotFound() {
        given(topicCandidateRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> topicCandidateCommandService.deleteTopicCandidate(1L))
                .isInstanceOf(TopicCandidateNotFoundException.class);

        verify(topicCandidateRepository, never()).save(any(TopicCandidate.class));
    }

    private TopicCandidate savedCandidate() {
        ArgumentCaptor<TopicCandidate> captor = ArgumentCaptor.forClass(TopicCandidate.class);
        verify(topicCandidateRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("수정도 제목 중복을 검사한다")
    void updateChecksTitleToo() {
        given(topicCandidateRepository.findByIdForUpdate(2L))
                .willReturn(Optional.of(candidate(2L, "내 제목")));
        given(topicCandidateRepository.findIncludingDeletedByTeamIdAndTitleForUpdate(100L, "남의 제목"))
                .willReturn(Optional.of(candidate(1L, "남의 제목")));

        assertThatThrownBy(() -> topicCandidateCommandService.updateTopicCandidate(2L, "남의 제목", null))
                .isInstanceOf(DuplicateTopicCandidateTitleException.class);

        verify(topicCandidateRepository, never()).save(any(TopicCandidate.class));
    }

    @Test
    @DisplayName("수정은 소프트 삭제된 행이 점유한 제목도 중복으로 본다")
    void updateRejectsTitleHeldByDeletedCandidate() {
        TopicCandidate deleted = candidate(1L, "삭제된 제목");
        deleted.delete();
        given(topicCandidateRepository.findByIdForUpdate(2L)).willReturn(Optional.of(candidate(2L, "내 제목")));
        given(topicCandidateRepository.findIncludingDeletedByTeamIdAndTitleForUpdate(100L, "삭제된 제목"))
                .willReturn(Optional.of(deleted));

        assertThatThrownBy(() -> topicCandidateCommandService.updateTopicCandidate(2L, "삭제된 제목", null))
                .isInstanceOf(DuplicateTopicCandidateTitleException.class);

        verify(topicCandidateRepository, never()).save(any(TopicCandidate.class));
    }

    @Test
    @DisplayName("수정은 자기 제목을 그대로 둔 경우를 중복으로 보지 않는다")
    void updateAllowsKeepingOwnTitle() {
        TopicCandidate mine = candidate(1L, "내 제목");
        given(topicCandidateRepository.findByIdForUpdate(1L)).willReturn(Optional.of(mine));
        given(topicCandidateRepository.findIncludingDeletedByTeamIdAndTitleForUpdate(100L, "내 제목"))
                .willReturn(Optional.of(mine));

        topicCandidateCommandService.updateTopicCandidate(1L, "내 제목", "새 설명");

        assertThat(mine.getDescription()).isEqualTo("새 설명");
        verify(topicCandidateRepository).save(mine);
    }

    @Test
    @DisplayName("없는 주제 후보를 수정하면 예외가 난다")
    void updateThrowsWhenNotFound() {
        given(topicCandidateRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> topicCandidateCommandService.updateTopicCandidate(1L, "제목", null))
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
