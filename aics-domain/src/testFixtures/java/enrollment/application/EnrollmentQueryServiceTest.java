package enrollment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.enrollment.application.query.EnrollmentQueryService;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentDetail;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;

@ExtendWith(MockitoExtension.class)
class EnrollmentQueryServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EnrollmentQueryService enrollmentQueryService;

    private final User student = User.create("202699999", "kgu@kyonggi.ac.kr", "김철수", "encoded",
            UserGlobalRole.USER, "010-1234-6789");

    private SectionDetail sectionDetail() {
        Section section = Section.builder().id(1L).professorId("202012345").courseId(1L).build();
        Course course = Course.builder().id(1L).name("객체지향프로그래밍").year(2026)
                .semester(SemesterType.SPRING).status(StatusType.ACTIVE).build();
        User professor = User.create("202012345", "prof@kgu.ac.kr", "김교수", "encoded",
                UserGlobalRole.USER, "010-0000-0000");
        return new SectionDetail(section, course, professor);
    }

    private Enrollment enrollment(String userId) {
        return Enrollment.builder()
                .id(1L)
                .sectionId(1L)
                .userId(userId)
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("수강 정보에 사용자를 붙여 명단을 만든다")
    void buildsRosterWithUsers() {
        given(sectionRepository.findById(1L)).willReturn(Optional.of(sectionDetail()));
        given(enrollmentRepository.findAllBySectionId(1L)).willReturn(List.of(enrollment("202699999")));
        given(userRepository.findAllByStudentNumberIn(List.of("202699999"))).willReturn(List.of(student));

        List<EnrollmentDetail> details = enrollmentQueryService.getEnrollmentsBySectionId(1L);

        assertThat(details).singleElement().satisfies(detail -> {
            assertThat(detail.user().getName()).isEqualTo("김철수");
            assertThat(detail.enrollment().getRole()).isEqualTo(Role.STUDENT);
        });
    }

    @Test
    @DisplayName("없는 분반의 명단은 조회할 수 없다")
    void rejectsMissingSection() {
        given(sectionRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentQueryService.getEnrollmentsBySectionId(99L))
                .isInstanceOf(SectionNotFoundException.class);

        verify(enrollmentRepository, never()).findAllBySectionId(99L);
    }

    @Test
    @DisplayName("사용자가 삭제된 수강 정보는 명단에서 빠진다")
    void skipsEnrollmentsWithoutUser() {
        given(sectionRepository.findById(1L)).willReturn(Optional.of(sectionDetail()));
        given(enrollmentRepository.findAllBySectionId(1L))
                .willReturn(List.of(enrollment("202699999"), enrollment("202600000")));
        given(userRepository.findAllByStudentNumberIn(List.of("202699999", "202600000")))
                .willReturn(List.of(student));

        assertThat(enrollmentQueryService.getEnrollmentsBySectionId(1L))
                .singleElement()
                .satisfies(detail -> assertThat(detail.user().getStudentNumber()).isEqualTo("202699999"));
    }
}
