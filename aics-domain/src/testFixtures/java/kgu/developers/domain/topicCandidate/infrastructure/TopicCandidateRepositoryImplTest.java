package kgu.developers.domain.topicCandidate.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.exception.TopicCandidateNotFoundException;

@ExtendWith(MockitoExtension.class)
class TopicCandidateRepositoryImplTest {

    @Mock
    private JpaTopicCandidateRepository jpaTopicCandidateRepository;

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
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository);

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
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository);

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
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository);

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
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository);

        List<TopicCandidate> result = repository.findByProposerUserId("20230001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProposerUserId()).isEqualTo("20230001");
    }

    @Test
    @DisplayName("새 주제 후보 저장 시 같은 팀의 활성 주제 제목이 있으면 예외를 발생한다")
    void saveThrowsExceptionWhenActiveTitleExists() {
        TopicCandidate existing = TopicCandidate.builder()
                .id(1L)
                .teamId(100L)
                .proposerUserId("20230001")
                .title("중복 제목")
                .description("설명")
                .createdAt(LocalDateTime.now())
                .deletedAt(null)
                .build();
        given(jpaTopicCandidateRepository.findByTeamIdAndTitleAndDeletedAtIsNull(100L, "중복 제목"))
                .willReturn(Optional.of(TopicCandidateJpaEntity.toEntity(existing)));
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository);
        TopicCandidate newCandidate = TopicCandidate.create(100L, "20230002", "중복 제목", "새 설명");

        assertThatThrownBy(() -> repository.save(newCandidate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 존재하는 주제 제목입니다");
    }

    @Test
    @DisplayName("새 주제 후보 저장 시 같은 팀의 삭제된 주제 제목이 있으면 저장을 허용한다")
    void saveAllowsReusingDeletedTitle() {
        LocalDateTime deletedAt = LocalDateTime.now();
        TopicCandidate deleted = TopicCandidate.builder()
                .id(1L)
                .teamId(100L)
                .proposerUserId("20230001")
                .title("삭제된 제목")
                .description("설명")
                .createdAt(LocalDateTime.now())
                .deletedAt(deletedAt)
                .build();
        given(jpaTopicCandidateRepository.findByTeamIdAndTitleAndDeletedAtIsNull(100L, "삭제된 제목"))
                .willReturn(Optional.empty());
        TopicCandidate saved = TopicCandidate.builder()
                .id(2L)
                .teamId(100L)
                .proposerUserId("20230002")
                .title("삭제된 제목")
                .description("새 설명")
                .createdAt(LocalDateTime.now())
                .deletedAt(null)
                .build();
        given(jpaTopicCandidateRepository.save(any(TopicCandidateJpaEntity.class)))
                .willReturn(TopicCandidateJpaEntity.toEntity(saved));
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository);
        TopicCandidate newCandidate = TopicCandidate.create(100L, "20230002", "삭제된 제목", "새 설명");

        TopicCandidate result = repository.save(newCandidate);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getTitle()).isEqualTo("삭제된 제목");
        ArgumentCaptor<TopicCandidateJpaEntity> captor = ArgumentCaptor.forClass(TopicCandidateJpaEntity.class);
        verify(jpaTopicCandidateRepository).save(captor.capture());
        assertThat(captor.getValue().getTeamId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("새 주제 후보 저장 시 같은 팀에 중복 제목이 없으면 저장한다")
    void saveWhenNoDuplicateTitle() {
        TopicCandidate newCandidate = TopicCandidate.create(100L, "20230001", "새 제목", "설명");
        TopicCandidate saved = TopicCandidate.builder()
                .id(1L)
                .teamId(100L)
                .proposerUserId("20230001")
                .title("새 제목")
                .description("설명")
                .createdAt(LocalDateTime.now())
                .deletedAt(null)
                .build();
        given(jpaTopicCandidateRepository.findByTeamIdAndTitleAndDeletedAtIsNull(100L, "새 제목"))
                .willReturn(Optional.empty());
        given(jpaTopicCandidateRepository.save(any(TopicCandidateJpaEntity.class)))
                .willReturn(TopicCandidateJpaEntity.toEntity(saved));
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository);

        TopicCandidate result = repository.save(newCandidate);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("새 제목");
        ArgumentCaptor<TopicCandidateJpaEntity> captor = ArgumentCaptor.forClass(TopicCandidateJpaEntity.class);
        verify(jpaTopicCandidateRepository).save(captor.capture());
        assertThat(captor.getValue().getTeamId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("기존 주제 후보 수정은 제목 중복 검사를 하지 않는다")
    void saveUpdateSkipsDuplicateCheck() {
        TopicCandidate existing = TopicCandidate.builder()
                .id(1L)
                .teamId(100L)
                .proposerUserId("20230001")
                .title("기존 제목")
                .description("설명")
                .createdAt(LocalDateTime.now())
                .deletedAt(null)
                .build();
        TopicCandidate updated = TopicCandidate.builder()
                .id(1L)
                .teamId(100L)
                .proposerUserId("20230001")
                .title("수정된 제목")
                .description("수정된 설명")
                .createdAt(LocalDateTime.now())
                .deletedAt(null)
                .build();
        given(jpaTopicCandidateRepository.save(any(TopicCandidateJpaEntity.class)))
                .willReturn(TopicCandidateJpaEntity.toEntity(updated));
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository);

        TopicCandidate result = repository.save(updated);

        assertThat(result.getTitle()).isEqualTo("수정된 제목");
        verify(jpaTopicCandidateRepository).save(any(TopicCandidateJpaEntity.class));
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
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository);

        repository.deleteById(1L);

        assertThat(entity.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 주제 후보 삭제 시 예외를 발생한다")
    void deleteByIdThrowsExceptionWhenNotFound() {
        given(jpaTopicCandidateRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.empty());
        TopicCandidateRepositoryImpl repository = new TopicCandidateRepositoryImpl(jpaTopicCandidateRepository);

        assertThatThrownBy(() -> repository.deleteById(1L))
                .isInstanceOf(TopicCandidateNotFoundException.class);
    }
}
