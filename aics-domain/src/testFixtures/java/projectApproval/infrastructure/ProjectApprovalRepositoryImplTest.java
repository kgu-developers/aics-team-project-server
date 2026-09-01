package projectApproval.infrastructure;

import kgu.developers.domain.projectApproval.domain.ProjectApproval;
import kgu.developers.domain.projectApproval.exception.DuplicateProjectApprovalException;
import kgu.developers.domain.projectApproval.exception.ProjectApprovalNotFoundException;
import kgu.developers.domain.projectApproval.infrastructure.JpaProjectApprovalRepository;
import kgu.developers.domain.projectApproval.infrastructure.ProjectApprovalJpaEntity;
import kgu.developers.domain.projectApproval.infrastructure.ProjectApprovalRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectApprovalRepositoryImplTest {

    @Mock
    private JpaProjectApprovalRepository jpaProjectApprovalRepository;

    @InjectMocks
    private ProjectApprovalRepositoryImpl projectApprovalRepository;

    @Test
    @DisplayName("프로젝트 동의 저장소 어댑터는 저장 결과를 도메인으로 반환한다")
    void save() {
        ProjectApproval approval = ProjectApproval.create(1L, "20260001", LocalDateTime.now());
        given(jpaProjectApprovalRepository.findByProjectIdAndUserId(1L, "20260001"))
                .willReturn(Optional.empty());
        given(jpaProjectApprovalRepository.saveAndFlush(any(ProjectApprovalJpaEntity.class)))
                .willReturn(ProjectApprovalJpaEntity.builder()
                        .id(1L)
                        .projectId(1L)
                        .userId("20260001")
                        .approvedAt(LocalDateTime.now())
                        .build());

        ProjectApproval saved = projectApprovalRepository.save(approval);

        assertThat(saved.getId()).isEqualTo(1L);
        ArgumentCaptor<ProjectApprovalJpaEntity> captor = ArgumentCaptor.forClass(ProjectApprovalJpaEntity.class);
        verify(jpaProjectApprovalRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getProjectId()).isEqualTo(1L);
        assertThat(captor.getValue().getUserId()).isEqualTo("20260001");
    }

    @Test
    @DisplayName("findById는 삭제되지 않은 프로젝트 동의를 조회한다")
    void findById() {
        given(jpaProjectApprovalRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(ProjectApprovalJpaEntity.builder()
                        .id(1L)
                        .projectId(1L)
                        .userId("20260001")
                        .approvedAt(LocalDateTime.now())
                        .build()));

        Optional<ProjectApproval> found = projectApprovalRepository.findById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getProjectId()).isEqualTo(1L);
        assertThat(found.get().getUserId()).isEqualTo("20260001");
    }

    @Test
    @DisplayName("findById는 존재하지 않는 ID면 빈 Optional을 반환한다")
    void findById_NotFound() {
        given(jpaProjectApprovalRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.empty());

        Optional<ProjectApproval> found = projectApprovalRepository.findById(1L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByProjectIdAndUserId는 삭제되지 않은 동의 존재 여부를 확인한다")
    void existsByProjectIdAndUserId() {
        given(jpaProjectApprovalRepository.existsByProjectIdAndUserIdAndDeletedAtIsNull(1L, "20260001"))
                .willReturn(true);

        boolean exists = projectApprovalRepository.existsByProjectIdAndUserId(1L, "20260001");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("findByProjectIdAndUserId는 삭제되지 않은 동의를 조회한다")
    void findByProjectIdAndUserId() {
        given(jpaProjectApprovalRepository.findByProjectIdAndUserIdAndDeletedAtIsNull(1L, "20260001"))
                .willReturn(Optional.of(ProjectApprovalJpaEntity.builder()
                        .id(1L)
                        .projectId(1L)
                        .userId("20260001")
                        .approvedAt(LocalDateTime.now())
                        .build()));

        Optional<ProjectApproval> found = projectApprovalRepository.findByProjectIdAndUserId(1L, "20260001");

        assertThat(found).isPresent();
        assertThat(found.get().getProjectId()).isEqualTo(1L);
        assertThat(found.get().getUserId()).isEqualTo("20260001");
    }

    @Test
    @DisplayName("findAllByProjectId는 삭제되지 않은 동의 목록을 userId 오름차순으로 조회한다")
    void findAllByProjectId() {
        given(jpaProjectApprovalRepository.findAllByProjectIdAndDeletedAtIsNullOrderByUserIdAsc(1L))
                .willReturn(List.of(
                        ProjectApprovalJpaEntity.builder()
                                .id(1L)
                                .projectId(1L)
                                .userId("20260001")
                                .approvedAt(LocalDateTime.now())
                                .build(),
                        ProjectApprovalJpaEntity.builder()
                                .id(2L)
                                .projectId(1L)
                                .userId("20260002")
                                .approvedAt(LocalDateTime.now())
                                .build()
                ));

        List<ProjectApproval> approvals = projectApprovalRepository.findAllByProjectId(1L);

        assertThat(approvals).hasSize(2);
        assertThat(approvals).extracting(ProjectApproval::getUserId)
                .containsExactly("20260001", "20260002");
    }

    @Test
    @DisplayName("findAllByUserId는 삭제되지 않은 동의 목록을 projectId 오름차순으로 조회한다")
    void findAllByUserId() {
        given(jpaProjectApprovalRepository.findAllByUserIdAndDeletedAtIsNullOrderByProjectIdAsc("20260001"))
                .willReturn(List.of(
                        ProjectApprovalJpaEntity.builder()
                                .id(1L)
                                .projectId(1L)
                                .userId("20260001")
                                .approvedAt(LocalDateTime.now())
                                .build(),
                        ProjectApprovalJpaEntity.builder()
                                .id(2L)
                                .projectId(2L)
                                .userId("20260001")
                                .approvedAt(LocalDateTime.now())
                                .build()
                ));

        List<ProjectApproval> approvals = projectApprovalRepository.findAllByUserId("20260001");

        assertThat(approvals).hasSize(2);
        assertThat(approvals).extracting(ProjectApproval::getProjectId)
                .containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("deleteById는 삭제되지 않은 동의를 soft delete한다")
    void deleteById() {
        ProjectApprovalJpaEntity entity = ProjectApprovalJpaEntity.builder()
                .id(1L)
                .projectId(1L)
                .userId("20260001")
                .approvedAt(LocalDateTime.now())
                .build();
        given(jpaProjectApprovalRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(entity));

        projectApprovalRepository.deleteById(1L);

        assertThat(entity.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("deleteById는 존재하지 않는 ID면 예외를 발생시킨다")
    void deleteById_NotFound() {
        given(jpaProjectApprovalRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> projectApprovalRepository.deleteById(1L))
                .isInstanceOf(ProjectApprovalNotFoundException.class);
    }

    @Test
    @DisplayName("deleteById 후에는 삭제된 행이 조회에서 제외된다")
    void deleteById_ExcludesFromQueries() {
        ProjectApprovalJpaEntity entity = ProjectApprovalJpaEntity.builder()
                .id(1L)
                .projectId(1L)
                .userId("20260001")
                .approvedAt(LocalDateTime.now())
                .build();
        given(jpaProjectApprovalRepository.findByIdAndDeletedAtIsNull(1L))
                .willReturn(Optional.of(entity));

        projectApprovalRepository.deleteById(1L);

        assertThat(entity.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("재승인 시 기존 삭제된 행을 재활용한다")
    void reapproval_RecyclesDeletedRow() {
        LocalDateTime now = LocalDateTime.now();
        ProjectApproval approval = ProjectApproval.create(1L, "20260001", now);
        ProjectApprovalJpaEntity existingEntity = ProjectApprovalJpaEntity.builder()
                .id(1L)
                .projectId(1L)
                .userId("20260001")
                .approvedAt(now.minusDays(1))
                .build();
        existingEntity.setDeletedAt(now.minusDays(1));

        given(jpaProjectApprovalRepository.findByProjectIdAndUserId(1L, "20260001"))
                .willReturn(Optional.of(existingEntity));
        given(jpaProjectApprovalRepository.saveAndFlush(any(ProjectApprovalJpaEntity.class)))
                .willReturn(existingEntity);

        ProjectApproval saved = projectApprovalRepository.save(approval);

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(existingEntity.getDeletedAt()).isNull();
        assertThat(existingEntity.getApprovedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("save는 조회와 INSERT 사이에 끼어든 동시 동의를 DuplicateProjectApprovalException 으로 바꾼다")
    void saveMapsConstraintViolationToDuplicate() {
        ProjectApproval approval = ProjectApproval.create(1L, "20260001", LocalDateTime.now());
        given(jpaProjectApprovalRepository.findByProjectIdAndUserId(1L, "20260001"))
                .willReturn(Optional.empty());
        given(jpaProjectApprovalRepository.saveAndFlush(any(ProjectApprovalJpaEntity.class)))
                .willThrow(new DataIntegrityViolationException("uk_project_approval_project_user"));

        assertThatThrownBy(() -> projectApprovalRepository.save(approval))
                .isInstanceOf(DuplicateProjectApprovalException.class);
    }
}
