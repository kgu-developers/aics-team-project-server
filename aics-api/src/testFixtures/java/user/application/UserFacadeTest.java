package user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.api.section.presentation.response.SectionResponse;
import kgu.developers.api.user.application.UserFacade;
import kgu.developers.api.user.presentation.response.UserResponse;
import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.user.application.command.UserCommandService;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;

@ExtendWith(MockitoExtension.class)
class UserFacadeTest {

    private static final String STUDENT_NUMBER = "202699999";

    @Mock
    private UserCommandService userCommandService;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private UserFacade userFacade;

    private final User student = User.create(STUDENT_NUMBER, "kgu@kyonggi.ac.kr", "김철수", "encoded",
            UserGlobalRole.USER, "010-1234-6789");

    private final User professor = User.create(STUDENT_NUMBER, "professor@kyonggi.ac.kr", "김교수", "encoded",
            UserGlobalRole.ADMIN, "010-9876-5432");

    private final Course course = Course.builder()
            .id(1L)
            .name("객체지향프로그래밍")
            .year(2026)
            .semester(SemesterType.SPRING)
            .status(StatusType.ACTIVE)
            .build();

    private SectionDetail sectionDetail(Long sectionId) {
        Section section = Section.builder()
                .id(sectionId)
                .professorId(STUDENT_NUMBER)
                .courseId(1L)
                .code("CS101")
                .name("0" + sectionId + "분반")
                .classTime("월3,4")
                .capacity(40)
                .contactVisibleFrom(LocalDateTime.of(2026, 3, 2, 0, 0))
                .contactVisibleUntil(LocalDateTime.of(2026, 6, 20, 18, 0))
                .build();
        return new SectionDetail(section, course, professor);
    }

    private Enrollment activeEnrollment(Long sectionId) {
        return Enrollment.create(sectionId, STUDENT_NUMBER, Role.STUDENT, Status.ACTIVE);
    }

    @Test
    @DisplayName("수강 분반과 담당 분반이 겹치면 한 번만 내려간다")
    void getMeDeduplicatesSectionBelongingToBothSources() {
        given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(professor);
        given(enrollmentRepository.findAllByUserId(STUDENT_NUMBER)).willReturn(List.of(activeEnrollment(1L)));
        given(sectionRepository.findAllByProfessorId(STUDENT_NUMBER)).willReturn(List.of(sectionDetail(1L)));
        given(sectionRepository.findAllByIdIn(List.of(1L))).willReturn(List.of(sectionDetail(1L)));

        UserResponse response = userFacade.getMe(STUDENT_NUMBER);

        assertThat(response.sections())
                .singleElement()
                .extracting(SectionResponse::id)
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("수강 분반과 담당 분반이 다르면 둘 다 내려간다")
    void getMeKeepsDistinctSections() {
        given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(professor);
        given(enrollmentRepository.findAllByUserId(STUDENT_NUMBER)).willReturn(List.of(activeEnrollment(1L)));
        given(sectionRepository.findAllByProfessorId(STUDENT_NUMBER)).willReturn(List.of(sectionDetail(2L)));
        given(sectionRepository.findAllByIdIn(List.of(1L))).willReturn(List.of(sectionDetail(1L)));

        UserResponse response = userFacade.getMe(STUDENT_NUMBER);

        assertThat(response.sections())
                .extracting(SectionResponse::id)
                .containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("탈퇴한 수강 내역만 있으면 소속 분반은 비어 있다")
    void getMeIgnoresWithdrawnEnrollments() {
        given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(student);
        given(enrollmentRepository.findAllByUserId(STUDENT_NUMBER))
                .willReturn(List.of(Enrollment.create(1L, STUDENT_NUMBER, Role.STUDENT, Status.WITHDRAWN)));

        UserResponse response = userFacade.getMe(STUDENT_NUMBER);

        assertThat(response.sections()).isEmpty();
        verifyNoInteractions(sectionRepository);
    }

    @Test
    @DisplayName("활성 수강 내역이 없는 교수도 담당 분반을 조회한다")
    void getMeIncludesProfessorSectionsWithoutEnrollments() {
        given(userQueryService.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(professor);
        given(enrollmentRepository.findAllByUserId(STUDENT_NUMBER)).willReturn(List.of());
        given(sectionRepository.findAllByProfessorId(STUDENT_NUMBER)).willReturn(List.of(sectionDetail(1L)));

        UserResponse response = userFacade.getMe(STUDENT_NUMBER);

        assertThat(response.sections())
                .singleElement()
                .extracting(SectionResponse::id)
                .isEqualTo(1L);
    }
}
