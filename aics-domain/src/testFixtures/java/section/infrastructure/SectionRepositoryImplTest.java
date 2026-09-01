package section.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.section.infrastructure.JpaSectionRepository;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.section.infrastructure.SectionRepositoryImpl;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SectionRepositoryImplTest {

    @Mock
    private JpaSectionRepository jpaSectionRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private SectionJpaEntity section;

    @Mock
    private UserJpaEntity professor;

    @Test
    @DisplayName("담당 교수가 아닌 분반은 잠금 대상에서 제외한다")
    void excludesAnotherProfessorsSectionFromLockQuery() {
        given(jpaSectionRepository
                .findByIdAndProfessorStudentNumberAndDeletedAtIsNull(7L, "20260001"))
                .willReturn(Optional.empty());
        SectionRepositoryImpl repository = repository();

        boolean owned = repository.lockActiveByIdAndProfessorId(7L, "20260001");

        assertThat(owned).isFalse();
        verify(jpaSectionRepository)
                .findByIdAndProfessorStudentNumberAndDeletedAtIsNull(7L, "20260001");
    }

    @Test
    @DisplayName("일반 조회와 잠금 조회가 같은 교수 소유권 기준을 사용한다")
    void usesSameOwnershipRuleForReadAndLock() {
        given(jpaSectionRepository.findByIdAndDeletedAtIsNull(7L))
                .willReturn(Optional.of(section));
        given(jpaSectionRepository
                .findByIdAndProfessorStudentNumberAndDeletedAtIsNull(7L, "20260001"))
                .willReturn(Optional.of(section));
        given(section.getProfessor()).willReturn(professor);
        given(professor.getStudentNumber()).willReturn("20260001");
        SectionRepositoryImpl repository = repository();

        boolean readable = repository.existsActiveByIdAndProfessorId(7L, "20260001");
        boolean lockable = repository.lockActiveByIdAndProfessorId(7L, "20260001");

        assertThat(readable).isTrue();
        assertThat(lockable).isTrue();
    }

    private SectionRepositoryImpl repository() {
        return new SectionRepositoryImpl(jpaSectionRepository, entityManager);
    }
}
