package teammessage.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import kgu.developers.domain.teammessage.infrastructure.JpaTeamMessageRepository;
import kgu.developers.domain.teammessage.infrastructure.TeamMessageJpaEntity;
import kgu.developers.domain.teammessage.infrastructure.TeamMessageRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamMessageRepositoryImplTest {

    @Mock
    private JpaTeamMessageRepository jpaTeamMessageRepository;

    @InjectMocks
    private TeamMessageRepositoryImpl teamMessageRepository;

    @Test
    @DisplayName("통합 미읽음 개수는 데이터베이스 집계 결과를 반환한다")
    void countUnreadByThreadIdIn() {
        given(jpaTeamMessageRepository.countUnreadByThreadIdIn(
            List.of(10L, 20L),
            "202699999"
        )).willReturn(3L);

        long result = teamMessageRepository.countUnreadByThreadIdIn(
            List.of(10L, 20L),
            "202699999"
        );

        assertThat(result).isEqualTo(3L);
        verify(jpaTeamMessageRepository).countUnreadByThreadIdIn(
            List.of(10L, 20L),
            "202699999"
        );
    }

    @Test
    @DisplayName("스레드 ID와 연관 유형, 연관 ID로 메시지 페이징을 조회한다")
    void findByThreadIdAndRelatedTypeAndRelatedId() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        TeamMessageJpaEntity entity = TeamMessageJpaEntity.builder()
            .id(1L)
            .threadId(10L)
            .senderId("202699999")
            .message("테스트 피드백")
            .relatedType(kgu.developers.domain.teammessage.domain.TeamMessageRelatedType.MID_REPORT)
            .relatedId(100L)
            .important(false)
            .build();
        given(jpaTeamMessageRepository.findByThreadIdAndRelatedTypeAndRelatedId(
            10L,
            kgu.developers.domain.teammessage.domain.TeamMessageRelatedType.MID_REPORT,
            100L,
            pageable
        )).willReturn(new org.springframework.data.domain.PageImpl<>(List.of(entity), pageable, 1));

        org.springframework.data.domain.Page<kgu.developers.domain.teammessage.domain.TeamMessage> result =
            teamMessageRepository.findByThreadIdAndRelatedTypeAndRelatedId(
                10L,
                kgu.developers.domain.teammessage.domain.TeamMessageRelatedType.MID_REPORT,
                100L,
                pageable
            );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getRelatedId()).isEqualTo(100L);
        verify(jpaTeamMessageRepository).findByThreadIdAndRelatedTypeAndRelatedId(
            10L,
            kgu.developers.domain.teammessage.domain.TeamMessageRelatedType.MID_REPORT,
            100L,
            pageable
        );
    }

    @Test
    @DisplayName("조회할 스레드가 없으면 데이터베이스에 접근하지 않는다")
    void countUnreadByEmptyThreadIds() {
        long result = teamMessageRepository.countUnreadByThreadIdIn(
            List.of(),
            "202699999"
        );

        assertThat(result).isZero();
        verifyNoInteractions(jpaTeamMessageRepository);
    }
}
