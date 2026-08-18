package enrollment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.enrollment.application.command.EnrollmentCommandService;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.enrollment.exception.DuplicateEnrollmentException;
import kgu.developers.domain.enrollment.exception.EnrollmentNotFoundException;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.domain.user.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class EnrollmentCommandServiceTest {

    private static final String STUDENT_NUMBER = "202699999";

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EnrollmentCommandService enrollmentCommandService;

    private final User student = User.create(STUDENT_NUMBER, "kgu@kyonggi.ac.kr", "김철수", "encoded",
            UserGlobalRole.USER, "010-1234-6789");

    private SectionDetail sectionDetail() {
        Section section = Section.builder().id(1L).professorId("202012345").courseId(1L).build();
        Course course = Course.builder().id(1L).name("객체지향프로그래밍").year(2026)
                .semester(SemesterType.SPRING).status(StatusType.ACTIVE).build();
        User professor = User.create("202012345", "prof@kgu.ac.kr", "김교수", "encoded",
                UserGlobalRole.USER, "010-0000-0000");
        return new SectionDetail(section, course, professor);
    }

    @Test
    @DisplayName("분반에 수강생을 ACTIVE 상태로 등록한다")
    void createEnrollment() {
        given(sectionRepository.findById(1L)).willReturn(Optional.of(sectionDetail()));
        given(userRepository.findByStudentNumber(STUDENT_NUMBER)).willReturn(Optional.of(student));
        given(enrollmentRepository.existsBySectionIdAndUserId(1L, STUDENT_NUMBER)).willReturn(false);
        given(enrollmentRepository.save(any(Enrollment.class)))
                .willReturn(Enrollment.builder().id(10L).build());

        assertThat(enrollmentCommandService.createEnrollment(1L, STUDENT_NUMBER, Role.STUDENT)).isEqualTo(10L);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        assertThat(captor.getValue().getSectionId()).isEqualTo(1L);
        assertThat(captor.getValue().getUserId()).isEqualTo(STUDENT_NUMBER);
        assertThat(captor.getValue().getRole()).isEqualTo(Role.STUDENT);
        assertThat(captor.getValue().getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    @DisplayName("없는 분반에는 등록할 수 없다")
    void rejectsMissingSection() {
        given(sectionRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentCommandService.createEnrollment(99L, STUDENT_NUMBER, Role.STUDENT))
                .isInstanceOf(SectionNotFoundException.class);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("없는 사용자는 등록할 수 없다")
    void rejectsMissingUser() {
        given(sectionRepository.findById(1L)).willReturn(Optional.of(sectionDetail()));
        given(userRepository.findByStudentNumber("209999999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentCommandService.createEnrollment(1L, "209999999", Role.STUDENT))
                .isInstanceOf(UserNotFoundException.class);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 분반에 이미 등록된 사용자는 중복 등록할 수 없다")
    void rejectsDuplicateEnrollment() {
        given(sectionRepository.findById(1L)).willReturn(Optional.of(sectionDetail()));
        given(userRepository.findByStudentNumber(STUDENT_NUMBER)).willReturn(Optional.of(student));
        given(enrollmentRepository.existsBySectionIdAndUserId(1L, STUDENT_NUMBER)).willReturn(true);

        assertThatThrownBy(() -> enrollmentCommandService.createEnrollment(1L, STUDENT_NUMBER, Role.ASSISTANT))
                .isInstanceOf(DuplicateEnrollmentException.class);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("역할과 상태를 함께 바꾼다")
    void updateEnrollment() {
        Enrollment enrollment = Enrollment.create(1L, STUDENT_NUMBER, Role.STUDENT, Status.ACTIVE);
        given(enrollmentRepository.findBySectionIdAndUserId(1L, STUDENT_NUMBER))
                .willReturn(Optional.of(enrollment));

        enrollmentCommandService.updateEnrollment(1L, STUDENT_NUMBER, Role.ASSISTANT, Status.WITHDRAWN);

        assertThat(enrollment.getRole()).isEqualTo(Role.ASSISTANT);
        assertThat(enrollment.getStatus()).isEqualTo(Status.WITHDRAWN);
        verify(enrollmentRepository).save(enrollment);
    }

    @Test
    @DisplayName("null로 보낸 항목은 그대로 둔다")
    void updateEnrollmentKeepsOmittedFields() {
        Enrollment enrollment = Enrollment.create(1L, STUDENT_NUMBER, Role.STUDENT, Status.ACTIVE);
        given(enrollmentRepository.findBySectionIdAndUserId(1L, STUDENT_NUMBER))
                .willReturn(Optional.of(enrollment));

        enrollmentCommandService.updateEnrollment(1L, STUDENT_NUMBER, null, Status.WITHDRAWN);

        assertThat(enrollment.getRole()).isEqualTo(Role.STUDENT);
        assertThat(enrollment.getStatus()).isEqualTo(Status.WITHDRAWN);
    }

    @Test
    @DisplayName("WITHDRAWN에서 ACTIVE로 되돌릴 수 있다")
    void reactivatesWithdrawnEnrollment() {
        Enrollment enrollment = Enrollment.create(1L, STUDENT_NUMBER, Role.STUDENT, Status.WITHDRAWN);
        given(enrollmentRepository.findBySectionIdAndUserId(1L, STUDENT_NUMBER))
                .willReturn(Optional.of(enrollment));

        enrollmentCommandService.updateEnrollment(1L, STUDENT_NUMBER, null, Status.ACTIVE);

        assertThat(enrollment.getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    @DisplayName("등록되지 않은 사용자는 변경할 수 없다")
    void rejectsUpdateForMissingEnrollment() {
        given(enrollmentRepository.findBySectionIdAndUserId(1L, "209999999")).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                enrollmentCommandService.updateEnrollment(1L, "209999999", Role.STUDENT, Status.ACTIVE))
                .isInstanceOf(EnrollmentNotFoundException.class);

        verify(enrollmentRepository, never()).save(any());
    }
}
