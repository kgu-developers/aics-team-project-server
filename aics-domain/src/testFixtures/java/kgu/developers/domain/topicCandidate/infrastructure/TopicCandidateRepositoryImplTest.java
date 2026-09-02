package kgu.developers.domain.topicCandidate.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.exception.TopicCandidateNotFoundException;

@ExtendWith(MockitoExtension.class)
class TopicCandidateRepositoryImplTest {

    @Mock
    private JpaTopicCandidateRepository jpaTopicCandidateRepository;

    @Mock
    private EntityManager entityManager;

    @Test
    @DisplayName("저장소는 삭제되지 않은 주제 후보만 조회한다")
    void findByIdExcludesDeleted() {
        TopicCandidate active = TopicCandidate.builder()
                .id(1L)
                .teamId(100L)
                .proposerUserId("20230001")
                .title("활성 주제")
                .description("설명")
                .createdAt(LocalDateTime.now())
                .deletedAt(null)
                .build();
        given(jpaTopicCandidateRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(TopicCandidateJpaEntity.toEntity(active)));
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository, entityManager);

        Optional<TopicCandidate> result = repository.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getTitle()).isEqualTo("활성 주제");
    }

    @Test
    @DisplayName("저장소는 삭제된 주제 후보를 조회하지 않는다")
    void findByIdReturnsEmptyForDeleted() {
        given(jpaTopicCandidateRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.empty());
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository, entityManager);

        Optional<TopicCandidate> result = repository.findById(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("저장소는 팀별 삭제되지 않은 주제 후보만 조회한다")
    void findByTeamIdExcludesDeleted() {
        TopicCandidate active1 = TopicCandidate.builder()
                .id(1L)
                .teamId(100L)
                .proposerUserId("20230001")
                .title("활성 주제1")
                .description("설명1")
                .createdAt(LocalDateTime.now())
                .deletedAt(null)
                .build();
        TopicCandidate active2 = TopicCandidate.builder()
                .id(2L)
                .teamId(100L)
                .proposerUserId("20230002")
                .title("활성 주제2")
                .description("설명2")
                .createdAt(LocalDateTime.now())
                .deletedAt(null)
                .build();
        given(jpaTopicCandidateRepository.findByTeamIdAndDeletedAtIsNull(100L))
                .willReturn(List.of(
                        TopicCandidateJpaEntity.toEntity(active1),
                        TopicCandidateJpaEntity.toEntity(active2)
                ));
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository, entityManager);

        List<TopicCandidate> result = repository.findByTeamId(100L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TopicCandidate::getTitle).containsExactly("활성 주제1", "활성 주제2");
    }

    @Test
    @DisplayName("저장소는 제안자별 삭제되지 않은 주제 후보만 조회한다")
    void findByProposerUserIdExcludesDeleted() {
        TopicCandidate active = TopicCandidate.builder()
                .id(1L)
                .teamId(100L)
                .proposerUserId("20230001")
                .title("활성 주제")
                .description("설명")
                .createdAt(LocalDateTime.now())
                .deletedAt(null)
                .build();
        given(jpaTopicCandidateRepository.findByProposerUserIdAndDeletedAtIsNull("20230001"))
                .willReturn(List.of(TopicCandidateJpaEntity.toEntity(active)));
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository, entityManager);

        List<TopicCandidate> result = repository.findByProposerUserId("20230001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProposerUserId()).isEqualTo("20230001");
    }

    @Test
    @DisplayName("주제 후보 삭제는 소프트 삭제를 수행한다")
    void deleteByIdPerformsSoftDelete() {
        TopicCandidate active = TopicCandidate.builder()
                .id(1L)
                .teamId(100L)
                .proposerUserId("20230001")
                .title("활성 주제")
                .description("설명")
                .createdAt(LocalDateTime.now())
                .deletedAt(null)
                .build();
        TopicCandidateJpaEntity entity = TopicCandidateJpaEntity.toEntity(active);
        given(jpaTopicCandidateRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(entity));
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository, entityManager);

        repository.deleteById(1L);

        assertThat(entity.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 주제 후보 삭제 시 예외를 발생한다")
    void deleteByIdThrowsExceptionWhenNotFound() {
        given(jpaTopicCandidateRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.empty());
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository, entityManager);

        assertThatThrownBy(() -> repository.deleteById(1L))
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

    @Test
    @DisplayName("중복 검사 조회는 팀 행에 쓰기 락을 걸고 살아있는 같은 제목만 본다")
    void findActiveByTeamIdAndTitleForUpdateLocksTeam() {
        given(jpaTopicCandidateRepository.findByTeamIdAndTitleAndDeletedAtIsNull(100L, "중복 제목"))
                .willReturn(Optional.of(TopicCandidateJpaEntity.toEntity(candidate(1L, "중복 제목"))));
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository, entityManager);

        Optional<TopicCandidate> result = repository.findActiveByTeamIdAndTitleForUpdate(100L, "중복 제목");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        verify(entityManager).find(TeamJpaEntity.class, 100L, PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("생성 경로의 삭제 포함 조회도 팀 행에 쓰기 락을 건다")
    void findIncludingDeletedByTeamIdAndTitleForUpdateLocksTeam() {
        given(jpaTopicCandidateRepository.findByTeamIdAndTitle(100L, "중복 제목"))
                .willReturn(Optional.of(TopicCandidateJpaEntity.toEntity(candidate(1L, "중복 제목"))));
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository, entityManager);

        Optional<TopicCandidate> result = repository.findIncludingDeletedByTeamIdAndTitleForUpdate(100L, "중복 제목");

        assertThat(result).isPresent();
        verify(entityManager).find(TeamJpaEntity.class, 100L, PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("중복 검사 조회는 소프트 삭제된 제목을 무시한다")
    void findActiveByTeamIdAndTitleForUpdateIgnoresDeleted() {
        given(jpaTopicCandidateRepository.findByTeamIdAndTitleAndDeletedAtIsNull(100L, "삭제된 제목"))
                .willReturn(Optional.empty());
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository, entityManager);

        assertThat(repository.findActiveByTeamIdAndTitleForUpdate(100L, "삭제된 제목")).isEmpty();
    }
}
