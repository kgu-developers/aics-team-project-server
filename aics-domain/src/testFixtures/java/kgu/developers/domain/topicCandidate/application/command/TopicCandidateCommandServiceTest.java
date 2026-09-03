package kgu.developers.domain.topicCandidate.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.OptimisticLockingFailureException;

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
    @DisplayName("팀 이동 수정은 두 팀 행을 팀 id 오름차순으로 잠근다")
    void updateLocksBothTeamsInIdOrder() {
        TopicCandidate mine = candidate(1L, "내 제목");
        given(topicCandidateRepository.findById(1L)).willReturn(Optional.of(mine));
        given(topicCandidateRepository.findByIdForUpdate(1L)).willReturn(Optional.of(mine));

        topicCandidateCommandService.updateTopicCandidate(1L, 40L, null, "새 설명");

        InOrder inOrder = inOrder(topicCandidateRepository);
        inOrder.verify(topicCandidateRepository).lockTeamForUpdate(40L);
        inOrder.verify(topicCandidateRepository).lockTeamForUpdate(100L);
    }

    @Test
    @DisplayName("팀 이동이 없으면 같은 팀 행을 한 번만 잠근다")
    void updateLocksSingleTeamWhenNotMoving() {
        TopicCandidate mine = candidate(1L, "내 제목");
        given(topicCandidateRepository.findById(1L)).willReturn(Optional.of(mine));
        given(topicCandidateRepository.findByIdForUpdate(1L)).willReturn(Optional.of(mine));

        topicCandidateCommandService.updateTopicCandidate(1L, 100L, null, "새 설명");

        verify(topicCandidateRepository).lockTeamForUpdate(100L);
    }

    @Test
    @DisplayName("잠금 전에 팀이 바뀌었으면 수정을 거절한다")
    void updateRejectsTeamChangedBeforeLock() {
        given(topicCandidateRepository.findById(1L)).willReturn(Optional.of(candidate(1L, "내 제목")));
        TopicCandidate moved = TopicCandidate.builder()
                .id(1L)
                .teamId(200L)
                .proposerUserId("20230001")
                .title("내 제목")
                .description("설명")
                .createdAt(LocalDateTime.now())
                .build();
        given(topicCandidateRepository.findByIdForUpdate(1L)).willReturn(Optional.of(moved));

        assertThatThrownBy(() -> topicCandidateCommandService.updateTopicCandidate(1L, null, null, "새 설명"))
                .isInstanceOf(OptimisticLockingFailureException.class);

        verify(topicCandidateRepository, never()).save(any(TopicCandidate.class));
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
    @DisplayName("삭제는 락 대기 중 후보가 다른 팀으로 이동하면 소프트 삭제하지 않는다")
    void deleteAbortsWhenTeamMovedWhileWaitingForLock() {
        // 잠금 조회가 잠근 팀과 새로고침 후 팀의 불일치를 감지해 충돌을 올린다
        willThrow(new OptimisticLockingFailureException("팀 변경"))
                .given(topicCandidateRepository).findByIdForUpdate(1L);

        assertThatThrownBy(() -> topicCandidateCommandService.deleteTopicCandidate(1L))
                .isInstanceOf(OptimisticLockingFailureException.class);

        verify(topicCandidateRepository, never()).save(any(TopicCandidate.class));
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
        given(topicCandidateRepository.findById(2L))
                .willReturn(Optional.of(candidate(2L, "내 제목")));
        given(topicCandidateRepository.findByIdForUpdate(2L))
                .willReturn(Optional.of(candidate(2L, "내 제목")));
        given(topicCandidateRepository.findIncludingDeletedByTeamIdAndTitleForUpdate(100L, "남의 제목"))
                .willReturn(Optional.of(candidate(1L, "남의 제목")));

        assertThatThrownBy(() -> topicCandidateCommandService.updateTopicCandidate(2L, 100L, "남의 제목", null))
                .isInstanceOf(DuplicateTopicCandidateTitleException.class);

        verify(topicCandidateRepository, never()).save(any(TopicCandidate.class));
    }

    @Test
    @DisplayName("수정은 소프트 삭제된 행이 점유한 제목도 중복으로 본다")
    void updateRejectsTitleHeldByDeletedCandidate() {
        TopicCandidate deleted = candidate(1L, "삭제된 제목");
        deleted.delete();
        given(topicCandidateRepository.findById(2L)).willReturn(Optional.of(candidate(2L, "내 제목")));
        given(topicCandidateRepository.findByIdForUpdate(2L)).willReturn(Optional.of(candidate(2L, "내 제목")));
        given(topicCandidateRepository.findIncludingDeletedByTeamIdAndTitleForUpdate(100L, "삭제된 제목"))
                .willReturn(Optional.of(deleted));

        assertThatThrownBy(() -> topicCandidateCommandService.updateTopicCandidate(2L, 100L, "삭제된 제목", null))
                .isInstanceOf(DuplicateTopicCandidateTitleException.class);

        verify(topicCandidateRepository, never()).save(any(TopicCandidate.class));
    }

    @Test
    @DisplayName("수정은 자기 제목을 그대로 둔 경우를 중복으로 보지 않는다")
    void updateAllowsKeepingOwnTitle() {
        TopicCandidate mine = candidate(1L, "내 제목");
        given(topicCandidateRepository.findById(1L)).willReturn(Optional.of(mine));
        given(topicCandidateRepository.findByIdForUpdate(1L)).willReturn(Optional.of(mine));
        given(topicCandidateRepository.findIncludingDeletedByTeamIdAndTitleForUpdate(100L, "내 제목"))
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
