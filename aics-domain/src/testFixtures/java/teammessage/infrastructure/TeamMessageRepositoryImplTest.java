package teammessage.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import kgu.developers.domain.teammessage.infrastructure.JpaTeamMessageRepository;
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
