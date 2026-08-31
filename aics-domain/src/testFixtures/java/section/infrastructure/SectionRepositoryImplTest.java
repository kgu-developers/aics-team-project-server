package section.infrastructure;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
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
    @DisplayName("교수 정보가 없는 분반을 잠금 조회해도 소유 분반으로 판단하지 않는다")
    void returnsFalseWhenLockedSectionHasNoProfessor() {
        given(entityManager.find(SectionJpaEntity.class, 7L, PESSIMISTIC_WRITE))
                .willReturn(section);
        given(section.getProfessor()).willReturn(null);
        SectionRepositoryImpl repository = repository();

        boolean owned = repository.lockActiveByIdAndProfessorId(7L, "20260001");

        assertThat(owned).isFalse();
        verify(entityManager).find(SectionJpaEntity.class, 7L, PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("일반 조회와 잠금 조회가 같은 교수 소유권 기준을 사용한다")
    void usesSameOwnershipRuleForReadAndLock() {
        given(jpaSectionRepository.findByIdAndDeletedAtIsNull(7L))
                .willReturn(Optional.of(section));
        given(entityManager.find(SectionJpaEntity.class, 7L, PESSIMISTIC_WRITE))
                .willReturn(section);
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
