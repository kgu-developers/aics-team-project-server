package section.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.api.section.application.SectionFacade;
import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;

@ExtendWith(MockitoExtension.class)
class SectionFacadeTest {

    private static final String STUDENT_NUMBER = "202012345";

    @Mock
    private SectionQueryService sectionQueryService;

    @InjectMocks
    private SectionFacade sectionFacade;

    private final Course course = Course.builder()
            .id(1L)
            .name("객체지향프로그래밍")
            .year(2026)
            .semester(SemesterType.SPRING)
            .status(StatusType.ACTIVE)
            .build();

    private final User professor = User.create(STUDENT_NUMBER, "prof@kgu.ac.kr", "김교수", "encoded",
            UserGlobalRole.USER, "010-0000-0000");

    private SectionDetail detail() {
        Section section = Section.builder()
                .id(1L)
                .professorId(STUDENT_NUMBER)
                .courseId(1L)
                .code("CS101")
                .name("01분반")
                .classTime("월3,4")
                .capacity(40)
                .contactVisibleFrom(LocalDateTime.of(2026, 3, 2, 0, 0))
                .contactVisibleUntil(LocalDateTime.of(2026, 6, 20, 18, 0))
                .build();
        return new SectionDetail(section, course, professor);
    }

    @Test
    @DisplayName("getMySections는 토큰의 학번과 조건을 그대로 쿼리 서비스에 넘긴다")
    void getMySections() {
        given(sectionQueryService.getSectionsByStudentNumber(STUDENT_NUMBER, StatusType.ACTIVE, 2026,
                SemesterType.SPRING)).willReturn(List.of(detail()));

        assertThat(sectionFacade.getMySections(STUDENT_NUMBER, StatusType.ACTIVE, 2026, SemesterType.SPRING)
                .contents())
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.id()).isEqualTo(1L);
                    assertThat(response.courseName()).isEqualTo("객체지향프로그래밍");
                    assertThat(response.semester()).isEqualTo(SemesterType.SPRING);
                });
    }

    @Test
    @DisplayName("getSectionById는 조회 결과를 상세 응답으로 변환한다")
    void getSectionById() {
        given(sectionQueryService.getSectionById(1L)).willReturn(detail());

        assertThat(sectionFacade.getSectionById(1L).code()).isEqualTo("CS101");
    }
}
