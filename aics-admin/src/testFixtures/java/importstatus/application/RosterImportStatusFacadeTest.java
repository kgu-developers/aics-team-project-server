package importstatus.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.admin.importcommon.SectionStaffValidator;
import kgu.developers.admin.importstatus.application.RosterImportStatusFacade;
import kgu.developers.domain.importBatch.domain.ImportBatch;
import kgu.developers.domain.importBatch.domain.ImportBatchRepository;
import kgu.developers.domain.importBatch.domain.Status;
import kgu.developers.domain.importBatch.domain.Type;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;

@ExtendWith(MockitoExtension.class)
class RosterImportStatusFacadeTest {

    private static final Long SECTION_ID = 1L;
    private static final String USER_ID = "202699999";

    @Mock
    private ImportBatchRepository importBatchRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SectionStaffValidator sectionStaffValidator;

    @InjectMocks
    private RosterImportStatusFacade rosterImportStatusFacade;

    @Test
    @DisplayName("학생·팀 명단의 마지막 성공 반영 파일명과 시각을 응답한다")
    void getStatus() {
        LocalDateTime studentAppliedAt = LocalDateTime.of(2026, 9, 9, 14, 30);
        LocalDateTime teamAppliedAt = LocalDateTime.of(2026, 9, 9, 15, 0);
        ImportBatch studentBatch = appliedBatch(Type.ENROLLMENT, "학생명단.xlsx", studentAppliedAt);
        ImportBatch teamBatch = appliedBatch(Type.TEAM, "팀명단.xlsx", teamAppliedAt);
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(mock(SectionDetail.class)));
        given(importBatchRepository.findLatestApplied(SECTION_ID, Type.ENROLLMENT))
            .willReturn(Optional.of(studentBatch));
        given(importBatchRepository.findLatestApplied(SECTION_ID, Type.TEAM))
            .willReturn(Optional.of(teamBatch));

        var response = rosterImportStatusFacade.getStatus(SECTION_ID, USER_ID);

        assertThat(response.studentRoster().fileName()).isEqualTo("학생명단.xlsx");
        assertThat(response.studentRoster().appliedAt()).isEqualTo(studentAppliedAt);
        assertThat(response.teamRoster().fileName()).isEqualTo("팀명단.xlsx");
        assertThat(response.teamRoster().appliedAt()).isEqualTo(teamAppliedAt);
        verify(sectionStaffValidator).validate(SECTION_ID, USER_ID);
    }

    @Test
    @DisplayName("성공 반영 이력이 없는 명단 유형은 null로 응답한다")
    void getStatus_WithoutHistory() {
        given(sectionRepository.findById(SECTION_ID)).willReturn(Optional.of(mock(SectionDetail.class)));
        given(importBatchRepository.findLatestApplied(SECTION_ID, Type.ENROLLMENT))
            .willReturn(Optional.empty());
        given(importBatchRepository.findLatestApplied(SECTION_ID, Type.TEAM))
            .willReturn(Optional.empty());

        var response = rosterImportStatusFacade.getStatus(SECTION_ID, USER_ID);

        assertThat(response.studentRoster()).isNull();
        assertThat(response.teamRoster()).isNull();
    }

    private ImportBatch appliedBatch(Type type, String fileName, LocalDateTime appliedAt) {
        return ImportBatch.builder()
            .id(1L)
            .sectionId(SECTION_ID)
            .uploadedBy(USER_ID)
            .type(type)
            .status(Status.APPLIED)
            .fileName(fileName)
            .updatedAt(appliedAt)
            .build();
    }
}
