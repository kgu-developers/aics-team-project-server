package milestone.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.milestone.application.command.MilestoneCommandService;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.section.domain.SectionRepository;

@ExtendWith(MockitoExtension.class)
class MilestoneCommandConcurrencyTest {

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private MilestoneCommandService milestoneCommandService;

    @Test
    @DisplayName("수정은 잠근 최신 상태를 기준으로 적용해 다른 요청의 상태 변경을 보존한다")
    void updateDetailsFromLatestLockedState() {
        Milestone latestMilestone = Milestone.restore(
                1L,
                7L,
                "제안서",
                null,
                2,
                MilestoneStatus.PUBLISHED,
                schedule(LocalDateTime.of(2026, 9, 10, 23, 59))
        );
        MilestoneSchedule updatedSchedule = schedule(LocalDateTime.of(2026, 9, 20, 23, 59));
        given(milestoneRepository.findByIdAndSectionIdForUpdate(1L, 7L))
                .willReturn(Optional.of(latestMilestone));
        given(sectionRepository.lockActiveByIdAndProfessorId(7L, "20260001"))
                .willReturn(true);
        given(milestoneRepository.save(any(Milestone.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        milestoneCommandService.updateMilestone(
                7L,
                "20260001",
                1L,
                "수정된 제안서",
                "수정 내용",
                updatedSchedule
        );

        ArgumentCaptor<Milestone> captor = ArgumentCaptor.forClass(Milestone.class);
        InOrder lockOrder = inOrder(sectionRepository, milestoneRepository);
        lockOrder.verify(sectionRepository).lockActiveByIdAndProfessorId(7L, "20260001");
        lockOrder.verify(milestoneRepository).findByIdAndSectionIdForUpdate(1L, 7L);
        verify(milestoneRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("수정된 제안서");
        assertThat(captor.getValue().getStatus()).isEqualTo(MilestoneStatus.PUBLISHED);
        assertThat(captor.getValue().getSchedule()).isEqualTo(updatedSchedule);
        verify(milestoneRepository, never()).findById(1L);
    }

    private MilestoneSchedule schedule(LocalDateTime dueAt) {
        return new MilestoneSchedule(null, dueAt, null, null, null, null);
    }
}
