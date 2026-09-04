package kgu.developers.domain.topicCandidate.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;

@ExtendWith(MockitoExtension.class)
class TopicCandidateRepositoryImplTest {

    @Mock
    private JpaTopicCandidateRepository jpaTopicCandidateRepository;

    @Mock
    private EntityManager entityManager;

    @Test
    @DisplayName("save는 후보를 저장하고 flush한다")
    void saveSavesAndFlushes() {
        TopicCandidate candidate = candidate(1L, "새 주제");
        TopicCandidateJpaEntity entity = TopicCandidateJpaEntity.toEntity(candidate);
        given(jpaTopicCandidateRepository.save(any(TopicCandidateJpaEntity.class)))
                .willReturn(entity);
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository, entityManager);

        TopicCandidate result = repository.save(candidate);

        assertThat(result.getTitle()).isEqualTo("새 주제");
        InOrder inOrder = inOrder(jpaTopicCandidateRepository);
        inOrder.verify(jpaTopicCandidateRepository).save(any(TopicCandidateJpaEntity.class));
        inOrder.verify(jpaTopicCandidateRepository).flush();
    }

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
    @DisplayName("잠금 조회는 팀 행에 쓰기 락을 건 뒤 후보를 다시 읽는다")
    void findByIdForUpdateLocksTeamThenRefreshes() {
        TopicCandidateJpaEntity entity = TopicCandidateJpaEntity.toEntity(candidate(1L, "활성 주제"));
        given(jpaTopicCandidateRepository.findById(1L)).willReturn(Optional.of(entity));
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository, entityManager);

        Optional<TopicCandidate> result = repository.findByIdForUpdate(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        InOrder inOrder = inOrder(entityManager);
        inOrder.verify(entityManager).find(TeamJpaEntity.class, 100L, PESSIMISTIC_WRITE);
        inOrder.verify(entityManager).refresh(entity);
    }

    @Test
    @DisplayName("잠금 조회는 락을 잡은 뒤 삭제된 것으로 확인되면 비어 있다")
    void findByIdForUpdateReturnsEmptyForDeleted() {
        TopicCandidateJpaEntity entity = TopicCandidateJpaEntity.toEntity(candidate(1L, "활성 주제"));
        given(jpaTopicCandidateRepository.findById(1L)).willReturn(Optional.of(entity));
        // 락 대기 중 다른 트랜잭션이 삭제를 커밋한 상황
        willAnswer(invocation -> {
            entity.delete();
            return null;
        }).given(entityManager).refresh(entity);
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository, entityManager);

        assertThat(repository.findByIdForUpdate(1L)).isEmpty();
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
    @DisplayName("중복 검사 조회는 소프트 삭제된 제목도 점유로 본다")
    void findIncludingDeletedByTeamIdAndTitleForUpdateSeesDeleted() {
        TopicCandidate deleted = candidate(1L, "삭제된 제목");
        deleted.delete();
        given(jpaTopicCandidateRepository.findByTeamIdAndTitle(100L, "삭제된 제목"))
                .willReturn(Optional.of(TopicCandidateJpaEntity.toEntity(deleted)));
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository, entityManager);

        Optional<TopicCandidate> result = repository.findIncludingDeletedByTeamIdAndTitleForUpdate(100L, "삭제된 제목");

        assertThat(result).isPresent();
        assertThat(result.get().getDeletedAt()).isNotNull();
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
