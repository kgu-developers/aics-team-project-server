package projectApproval.infrastructure;

import kgu.developers.domain.projectApproval.domain.ApprovalCount;
import kgu.developers.domain.projectApproval.domain.ProjectApproval;
import kgu.developers.domain.projectApproval.exception.DuplicateProjectApprovalException;
import kgu.developers.domain.projectApproval.exception.ProjectApprovalNotFoundException;
import kgu.developers.domain.projectApproval.infrastructure.JpaProjectApprovalRepository;
import kgu.developers.domain.projectApproval.infrastructure.ProjectApprovalJpaEntity;
import kgu.developers.domain.projectApproval.infrastructure.ProjectApprovalRepositoryImpl;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.DisplayName;
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
        ProjectApproval approval = ProjectApproval.create(1L, "20260001", 0L, LocalDateTime.now());
        given(jpaProjectApprovalRepository.saveAndFlush(any(ProjectApprovalJpaEntity.class)))
                .willReturn(ProjectApprovalJpaEntity.builder()
                        .id(1L)
                        .projectId(1L)
                        .userId("20260001")
                        .proposalRevision(0L)
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
    @DisplayName("save는 동의한 제안서 리비전을 그대로 저장한다")
    void saveKeepsProposalRevision() {
        ProjectApproval approval = ProjectApproval.create(1L, "20260001", 3L, LocalDateTime.now());
        given(jpaProjectApprovalRepository.saveAndFlush(any(ProjectApprovalJpaEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ProjectApproval saved = projectApprovalRepository.save(approval);

        assertThat(saved.getProposalRevision()).isEqualTo(3L);
    }

    @Test
    @DisplayName("save는 도메인의 deletedAt을 그대로 반영해 재활성화를 저장한다")
    void saveAppliesReactivatedDomain() {
        LocalDateTime now = LocalDateTime.now();
        ProjectApproval approval = ProjectApproval.builder()
                .id(1L)
                .projectId(1L)
                .userId("20260001")
                .proposalRevision(2L)
                .approvedAt(now.minusDays(1))
                .deletedAt(now.minusDays(1))
                .build();
        approval.reactivate(now);
        given(jpaProjectApprovalRepository.saveAndFlush(any(ProjectApprovalJpaEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        projectApprovalRepository.save(approval);

        ArgumentCaptor<ProjectApprovalJpaEntity> captor = ArgumentCaptor.forClass(ProjectApprovalJpaEntity.class);
        verify(jpaProjectApprovalRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getDeletedAt()).isNull();
        assertThat(captor.getValue().getApprovedAt()).isEqualTo(now);
        assertThat(captor.getValue().getProposalRevision()).isEqualTo(2L);
    }

    @Test
    @DisplayName("save는 유니크 제약 위반을 DuplicateProjectApprovalException으로 변환하고 원인을 보존한다")
    void saveTranslatesUniqueViolation() {
        ProjectApproval approval = ProjectApproval.create(1L, "20260001", 0L, LocalDateTime.now());
        DataIntegrityViolationException violation =
                new DataIntegrityViolationException("uk_project_approval_project_user");
        given(jpaProjectApprovalRepository.saveAndFlush(any(ProjectApprovalJpaEntity.class)))
                .willThrow(violation);

        assertThatThrownBy(() -> projectApprovalRepository.save(approval))
                .isInstanceOf(DuplicateProjectApprovalException.class)
                .hasCause(violation);
    }

    @Test
    @DisplayName("save는 다른 무결성 위반은 원인을 유지한 채 다시 던진다")
    void saveRethrowsOtherIntegrityViolation() {
        ProjectApproval approval = ProjectApproval.create(1L, "20260001", 0L, LocalDateTime.now());
        DataIntegrityViolationException cause =
                new DataIntegrityViolationException("fk_project_approval_project violation");
        given(jpaProjectApprovalRepository.saveAndFlush(any(ProjectApprovalJpaEntity.class)))
                .willThrow(cause);

        assertThatThrownBy(() -> projectApprovalRepository.save(approval))
                .isSameAs(cause);
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
    @DisplayName("findAllByProjectIdAndProposalRevision은 해당 리비전의 동의만 조회한다")
    void findAllByProjectIdAndProposalRevision() {
        given(jpaProjectApprovalRepository
                .findAllByProjectIdAndProposalRevisionAndDeletedAtIsNullOrderByUserIdAsc(1L, 2L))
                .willReturn(List.of(ProjectApprovalJpaEntity.builder()
                        .id(1L)
                        .projectId(1L)
                        .userId("20260001")
                        .proposalRevision(2L)
                        .approvedAt(LocalDateTime.now())
                        .build()));

        List<ProjectApproval> approvals = projectApprovalRepository.findAllByProjectIdAndProposalRevision(1L, 2L);

        assertThat(approvals).extracting(ProjectApproval::getProposalRevision).containsExactly(2L);
    }

    @Test
    @DisplayName("countApprovalsByTeamMembers는 팀원 수와 해당 리비전 동의 수를 함께 센다")
    void countApprovalsByTeamMembers() {
        given(jpaProjectApprovalRepository.countApprovalsByTeamMembers(1L, 7L, 2L))
                .willReturn(new ApprovalCount(3L, 1L));

        ApprovalCount count = projectApprovalRepository.countApprovalsByTeamMembers(1L, 7L, 2L);

        assertThat(count.totalMembers()).isEqualTo(3L);
        assertThat(count.approvedMembers()).isEqualTo(1L);
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

        given(jpaProjectApprovalRepository.findAllByProjectIdAndDeletedAtIsNullOrderByUserIdAsc(1L))
                .willReturn(List.of());
        List<ProjectApproval> approvals = projectApprovalRepository.findAllByProjectId(1L);
        assertThat(approvals).isEmpty();
    }

    @Test
    @DisplayName("deleteAllByProjectId는 프로젝트의 살아있는 동의를 한 번에 무효화한다")
    void deleteAllByProjectId() {
        projectApprovalRepository.deleteAllByProjectId(1L);

        verify(jpaProjectApprovalRepository).softDeleteAllByProjectId(1L);
    }

    @Test
    @DisplayName("findIncludingDeleted는 같은 리비전의 삭제된 동의도 함께 조회한다")
    void findIncludingDeleted() {
        LocalDateTime deletedAt = LocalDateTime.now().minusDays(1);
        ProjectApprovalJpaEntity entity = ProjectApprovalJpaEntity.builder()
                .id(1L)
                .projectId(1L)
                .userId("20260001")
                .proposalRevision(2L)
                .approvedAt(deletedAt)
                .build();
        entity.setDeletedAt(deletedAt);
        given(jpaProjectApprovalRepository.findByProjectIdAndUserIdAndProposalRevision(1L, "20260001", 2L))
                .willReturn(Optional.of(entity));

        Optional<ProjectApproval> found = projectApprovalRepository.findIncludingDeleted(1L, "20260001", 2L);

        assertThat(found).isPresent();
        assertThat(found.get().getDeletedAt()).isEqualTo(deletedAt);
        assertThat(found.get().getProposalRevision()).isEqualTo(2L);
    }
}
