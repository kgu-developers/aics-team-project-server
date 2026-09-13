package teammessage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import kgu.developers.domain.teammessage.application.query.TeamMessageQueryService;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageReadReceiptRepository;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.teammessage.domain.TeamMessageRepository;
import kgu.developers.domain.teammessage.domain.TeamMessageUnreadRepository;
import kgu.developers.domain.teammessage.exception.TeamMessageNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TeamMessageQueryServiceTest {

    @Mock
    private TeamMessageRepository teamMessageRepository;

    @Mock
    private TeamMessageReadReceiptRepository teamMessageReadReceiptRepository;

    @Mock
    private TeamMessageUnreadRepository teamMessageUnreadRepository;

    @InjectMocks
    private TeamMessageQueryService teamMessageQueryService;

    @Test
    @DisplayName("ID로 팀 메시지를 조회한다")
    void getMessage_Success() {
        TeamMessage message = TeamMessage.builder().id(1L).threadId(10L).build();
        given(teamMessageRepository.findById(1L)).willReturn(Optional.of(message));

        TeamMessage result = teamMessageQueryService.getMessage(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 팀 메시지 조회 시 TeamMessageNotFoundException이 발생한다")
    void getMessage_NotFound_ThrowsException() {
        given(teamMessageRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamMessageQueryService.getMessage(999L))
            .isInstanceOf(TeamMessageNotFoundException.class);
    }

    @Test
    @DisplayName("스레드 ID와 연관 유형으로 메시지 목록을 조회한다")
    void getMessages_ByThreadIdAndRelatedType() {
        Pageable pageable = PageRequest.of(0, 10);
        TeamMessage message = TeamMessage.builder()
            .id(1L)
            .threadId(10L)
            .relatedType(TeamMessageRelatedType.MID_REPORT)
            .build();
        given(teamMessageRepository.findByThreadIdAndRelatedType(10L, TeamMessageRelatedType.MID_REPORT, pageable))
            .willReturn(new PageImpl<>(List.of(message), pageable, 1));

        Page<TeamMessage> result = teamMessageQueryService.getMessages(10L, TeamMessageRelatedType.MID_REPORT, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(teamMessageRepository).findByThreadIdAndRelatedType(10L, TeamMessageRelatedType.MID_REPORT, pageable);
    }

    @Test
    @DisplayName("스레드 ID, 연관 유형, 연관 ID로 메시지 목록을 조회한다 (피드백 이력 필터링)")
    void getMessages_ByThreadIdAndRelatedTypeAndRelatedId() {
        Pageable pageable = PageRequest.of(0, 10);
        TeamMessage message = TeamMessage.builder()
            .id(1L)
            .threadId(10L)
            .relatedType(TeamMessageRelatedType.MID_REPORT)
            .relatedId(100L)
            .build();
        given(teamMessageRepository.findByThreadIdAndRelatedTypeAndRelatedId(
            10L, TeamMessageRelatedType.MID_REPORT, 100L, pageable))
            .willReturn(new PageImpl<>(List.of(message), pageable, 1));

        Page<TeamMessage> result = teamMessageQueryService.getMessages(
            10L, TeamMessageRelatedType.MID_REPORT, 100L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRelatedId()).isEqualTo(100L);
        verify(teamMessageRepository).findByThreadIdAndRelatedTypeAndRelatedId(
            10L, TeamMessageRelatedType.MID_REPORT, 100L, pageable);
    }

    @Test
    @DisplayName("relatedId가 null이면 연관 유형만으로 조회한다")
    void getMessages_NullRelatedId_FallsBackToRelatedType() {
        Pageable pageable = PageRequest.of(0, 10);
        TeamMessage message = TeamMessage.builder()
            .id(1L)
            .threadId(10L)
            .relatedType(TeamMessageRelatedType.MID_REPORT)
            .build();
        given(teamMessageRepository.findByThreadIdAndRelatedType(10L, TeamMessageRelatedType.MID_REPORT, pageable))
            .willReturn(new PageImpl<>(List.of(message), pageable, 1));

        Page<TeamMessage> result = teamMessageQueryService.getMessages(
            10L, TeamMessageRelatedType.MID_REPORT, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(teamMessageRepository).findByThreadIdAndRelatedType(10L, TeamMessageRelatedType.MID_REPORT, pageable);
    }

    @Test
    @DisplayName("relatedType과 relatedId가 모두 null이면 스레드 ID만으로 조회한다")
    void getMessages_NullTypeAndId_FallsBackToThreadId() {
        Pageable pageable = PageRequest.of(0, 10);
        TeamMessage message = TeamMessage.builder()
            .id(1L)
            .threadId(10L)
            .build();
        given(teamMessageRepository.findByThreadId(10L, pageable))
            .willReturn(new PageImpl<>(List.of(message), pageable, 1));

        Page<TeamMessage> result = teamMessageQueryService.getMessages(
            10L, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(teamMessageRepository).findByThreadId(10L, pageable);
    }

    @Test
    @DisplayName("스레드 ID 목록으로 메시지를 페이징 조회한다")
    void getMessages_ByThreadIds() {
        Pageable pageable = PageRequest.of(0, 10);
        given(teamMessageRepository.findByThreadIdIn(List.of(10L, 20L), pageable))
            .willReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<TeamMessage> result = teamMessageQueryService.getMessages(List.of(10L, 20L), pageable);

        assertThat(result.getContent()).isEmpty();
        verify(teamMessageRepository).findByThreadIdIn(List.of(10L, 20L), pageable);
    }

    @Test
    @DisplayName("스레드 ID 목록과 연관 유형으로 메시지를 페이징 조회한다")
    void getMessages_ByThreadIdsAndRelatedType() {
        Pageable pageable = PageRequest.of(0, 10);
        given(teamMessageRepository.findByThreadIdInAndRelatedType(
            List.of(10L, 20L), TeamMessageRelatedType.MID_REPORT, pageable))
            .willReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<TeamMessage> result = teamMessageQueryService.getMessages(
            List.of(10L, 20L), TeamMessageRelatedType.MID_REPORT, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(teamMessageRepository).findByThreadIdInAndRelatedType(
            List.of(10L, 20L), TeamMessageRelatedType.MID_REPORT, pageable);
    }

    @Test
    @DisplayName("사용자가 읽은 메시지 ID 목록을 조회한다")
    void findReadMessageIds() {
        given(teamMessageReadReceiptRepository.findReadMessageIds("user1", List.of(1L, 2L)))
            .willReturn(Set.of(1L));

        Set<Long> result = teamMessageQueryService.findReadMessageIds("user1", List.of(1L, 2L));

        assertThat(result).containsExactly(1L);
        verify(teamMessageReadReceiptRepository).findReadMessageIds("user1", List.of(1L, 2L));
    }

    @Test
    @DisplayName("단일 스레드의 미읽음 메시지 수를 조회한다")
    void countUnread_SingleThread() {
        given(teamMessageUnreadRepository.countUnreadByThreadIdIn(List.of(10L), "user1"))
            .willReturn(3L);

        long count = teamMessageQueryService.countUnread(10L, "user1");

        assertThat(count).isEqualTo(3L);
        verify(teamMessageUnreadRepository).countUnreadByThreadIdIn(List.of(10L), "user1");
    }

    @Test
    @DisplayName("복수 스레드의 미읽음 메시지 수를 조회한다")
    void countUnread_MultipleThreads() {
        given(teamMessageUnreadRepository.countUnreadByThreadIdIn(List.of(10L, 20L), "user1"))
            .willReturn(5L);

        long count = teamMessageQueryService.countUnread(List.of(10L, 20L), "user1");

        assertThat(count).isEqualTo(5L);
        verify(teamMessageUnreadRepository).countUnreadByThreadIdIn(List.of(10L, 20L), "user1");
    }
}
