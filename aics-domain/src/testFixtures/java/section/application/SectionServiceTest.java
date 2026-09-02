package section.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.CourseRepository;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.course.exception.CourseNotFoundException;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.section.application.command.SectionCommandService;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.InvalidContactVisiblePeriodException;
import kgu.developers.domain.section.exception.ProfessorRoleRequiredException;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.domain.user.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private SectionCommandService commandService;

    @InjectMocks
    private SectionQueryService queryService;

    private final Course course = Course.builder()
            .id(1L)
            .name("객체지향프로그래밍")
            .year(2026)
            .semester(SemesterType.SPRING)
            .status(StatusType.ACTIVE)
            .build();

    private final User professor = User.create("202012345", "prof@kgu.ac.kr", "김교수",
            "encoded", UserGlobalRole.ADMIN, "010-0000-0000");

    private final User student = User.create("202099999", "student@kgu.ac.kr", "김학생",
            "encoded", UserGlobalRole.USER, "010-1111-1111");

    private Section section() {
        return Section.create("202012345", 1L, "CS101", "01분반", "월3,4", 40, null, null);
    }

    private SectionDetail sectionDetail() {
        return new SectionDetail(section(), course, professor);
    }

    @Test
    @DisplayName("존재하는 강좌와 교수면 분반이 생성된다")
    void createsSectionUnderExistingCourse() {
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));
        given(userRepository.findByStudentNumber("202012345")).willReturn(Optional.of(professor));
        given(sectionRepository.save(any(Section.class))).willReturn(
                Section.builder().id(10L).courseId(1L).build());

        assertThat(commandService.createSection("202012345", 1L, "CS101", "01분반", "월3,4", 40, null, null))
                .isEqualTo(10L);
    }

    @Test
    @DisplayName("없는 강좌에는 분반을 만들 수 없다")
    void rejectsSectionForMissingCourse() {
        given(courseRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                commandService.createSection("202012345", 99L, "CS101", "01분반", "월3,4", 40, null, null))
                .isInstanceOf(CourseNotFoundException.class);

        verify(sectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("없는 교수에게는 분반을 만들 수 없다")
    void rejectsSectionForMissingProfessor() {
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));
        given(userRepository.findByStudentNumber("999999999")).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                commandService.createSection("999999999", 1L, "CS101", "01분반", "월3,4", 40, null, null))
                .isInstanceOf(UserNotFoundException.class);

        verify(sectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("USER 역할의 사용자는 분반 담당 교수로 지정할 수 없다")
    void rejectsSectionForUserRoleProfessor() {
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));
        given(userRepository.findByStudentNumber("202099999")).willReturn(Optional.of(student));

        assertThatThrownBy(() ->
                commandService.createSection("202099999", 1L, "CS101", "01분반", "월3,4", 40, null, null))
                .isInstanceOf(ProfessorRoleRequiredException.class);

        verify(sectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("분반의 모든 수정 가능 필드가 갱신된다")
    void updatesSection() {
        Section section = section();
        given(courseRepository.findById(2L)).willReturn(Optional.of(course));
        given(userRepository.findByStudentNumber("202099999")).willReturn(Optional.of(professor));

        commandService.updateSection(section, "202099999", 2L, "CS102", "02분반", "화5,6", 30,
                LocalDateTime.of(2026, 3, 2, 0, 0), LocalDateTime.of(2026, 6, 20, 18, 0));

        assertThat(section.getProfessorId()).isEqualTo("202099999");
        assertThat(section.getCourseId()).isEqualTo(2L);
        assertThat(section.getCode()).isEqualTo("CS102");
        assertThat(section.getName()).isEqualTo("02분반");
        assertThat(section.getClassTime()).isEqualTo("화5,6");
        assertThat(section.getCapacity()).isEqualTo(30);
        assertThat(section.getContactVisibleFrom()).isEqualTo(LocalDateTime.of(2026, 3, 2, 0, 0));
        assertThat(section.getContactVisibleUntil()).isEqualTo(LocalDateTime.of(2026, 6, 20, 18, 0));
        verify(sectionRepository).save(section);
    }

    @Test
    @DisplayName("null로 보낸 필드는 수정되지 않는다")
    void updatesOnlyGivenFields() {
        Section section = section();

        commandService.updateSection(section, null, null, null, "02분반", null, null, null, null);

        assertThat(section.getName()).isEqualTo("02분반");
        assertThat(section.getProfessorId()).isEqualTo("202012345");
        assertThat(section.getCourseId()).isEqualTo(1L);
        assertThat(section.getCode()).isEqualTo("CS101");
        assertThat(section.getClassTime()).isEqualTo("월3,4");
        assertThat(section.getCapacity()).isEqualTo(40);
        verify(sectionRepository).save(section);
    }

    @Test
    @DisplayName("연락처 공개 기간을 통째로 뒤로 옮기는 수정이 허용된다")
    void shiftsContactVisiblePeriodForward() {
        LocalDateTime from = LocalDateTime.of(2026, 3, 1, 9, 0);
        Section section = Section.create("202012345", 1L, "CS101", "01분반", "월3,4", 40, from, from.plusDays(1));

        commandService.updateSection(section, null, null, null, null, null, null,
                from.plusDays(2), from.plusDays(3));

        assertThat(section.getContactVisibleFrom()).isEqualTo(from.plusDays(2));
        assertThat(section.getContactVisibleUntil()).isEqualTo(from.plusDays(3));
    }

    @Test
    @DisplayName("한쪽만 수정해도 기존 값과의 역전은 거부된다")
    void rejectsReversedContactVisiblePeriodOnPartialUpdate() {
        LocalDateTime from = LocalDateTime.of(2026, 3, 1, 9, 0);
        Section section = Section.create("202012345", 1L, "CS101", "01분반", "월3,4", 40, from, from.plusDays(1));

        assertThatThrownBy(() -> commandService.updateSection(section, null, null, null, null, null, null,
                from.plusDays(5), null))
                .isInstanceOf(InvalidContactVisiblePeriodException.class);

        assertThat(section.getContactVisibleFrom()).isEqualTo(from);
        assertThat(section.getContactVisibleUntil()).isEqualTo(from.plusDays(1));
    }

    @Test
    @DisplayName("없는 교수로 수정하면 어떤 필드도 바뀌지 않는다")
    void rejectsUpdateToMissingProfessor() {
        Section section = section();
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));
        given(userRepository.findByStudentNumber("999999999")).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                commandService.updateSection(section, "999999999", 1L, "CS102", "02분반", "화5,6", 30, null, null))
                .isInstanceOf(UserNotFoundException.class);

        assertThat(section.getProfessorId()).isEqualTo("202012345");
        assertThat(section.getCode()).isEqualTo("CS101");
        verify(sectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("분반 수정으로 USER 역할의 사용자를 담당 교수로 지정할 수 없다")
    void rejectsUpdateToUserRoleProfessor() {
        Section section = section();
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));
        given(userRepository.findByStudentNumber("202099999")).willReturn(Optional.of(student));

        assertThatThrownBy(() ->
                commandService.updateSection(section, "202099999", 1L, "CS102", "02분반", "화5,6", 30, null, null))
                .isInstanceOf(ProfessorRoleRequiredException.class);

        assertThat(section.getProfessorId()).isEqualTo("202012345");
        assertThat(section.getCode()).isEqualTo("CS101");
        verify(sectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("없는 강좌로는 분반을 수정할 수 없다")
    void rejectsUpdateToMissingCourse() {
        Section section = section();
        given(courseRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                commandService.updateSection(section, "202012345", 99L, "CS102", "02분반", "화5,6", 30, null, null))
                .isInstanceOf(CourseNotFoundException.class);

        assertThat(section.getCourseId()).isEqualTo(1L);
        verify(sectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("분반 담당 교수를 없는 사용자로 바꿀 수 없다")
    void rejectsMoveToMissingProfessor() {
        Section section = section();
        given(userRepository.findByStudentNumber("999999999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.changeProfessor(section, "999999999"))
                .isInstanceOf(UserNotFoundException.class);

        assertThat(section.getProfessorId()).isEqualTo("202012345");
    }

    @Test
    @DisplayName("분반 담당 교수를 USER 역할의 사용자로 바꿀 수 없다")
    void rejectsMoveToUserRoleProfessor() {
        Section section = section();
        given(userRepository.findByStudentNumber("202099999")).willReturn(Optional.of(student));

        assertThatThrownBy(() -> commandService.changeProfessor(section, "202099999"))
                .isInstanceOf(ProfessorRoleRequiredException.class);

        assertThat(section.getProfessorId()).isEqualTo("202012345");
        verify(sectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("교수별 분반을 조회한다")
    void findsSectionsByProfessor() {
        given(userRepository.findByStudentNumber("202012345")).willReturn(Optional.of(professor));
        given(sectionRepository.findAllByProfessorId("202012345")).willReturn(List.of(sectionDetail()));

        assertThat(queryService.getSectionsByProfessorId("202012345"))
                .singleElement()
                .satisfies(detail -> {
                    assertThat(detail.professor().getName()).isEqualTo("김교수");
                    assertThat(detail.course().getName()).isEqualTo("객체지향프로그래밍");
                });
    }

    @Test
    @DisplayName("연락처 공개 기간을 역전시키는 변경은 거부되고 저장되지 않는다")
    void rejectsReversedContactVisiblePeriodOnChange() {
        LocalDateTime from = LocalDateTime.of(2026, 3, 1, 9, 0);
        Section section = Section.create("202012345", 1L, "CS101", "01분반", "월3,4", 40, from, from.plusDays(1));

        assertThatThrownBy(() -> commandService.changeContactVisiblePeriod(section, from.plusDays(5), from))
                .isInstanceOf(InvalidContactVisiblePeriodException.class);

        assertThat(section.getContactVisibleFrom()).isEqualTo(from);
        assertThat(section.getContactVisibleUntil()).isEqualTo(from.plusDays(1));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("연락처 공개 기간을 양쪽 null로 지우면 저장된다")
    void clearsContactVisiblePeriod() {
        LocalDateTime from = LocalDateTime.of(2026, 3, 1, 9, 0);
        Section section = Section.create("202012345", 1L, "CS101", "01분반", "월3,4", 40, from, from.plusDays(1));

        commandService.changeContactVisiblePeriod(section, null, null);

        assertThat(section.getContactVisibleFrom()).isNull();
        assertThat(section.getContactVisibleUntil()).isNull();
        verify(sectionRepository).save(section);
    }

    @Test
    @DisplayName("학생별 분반은 교수 소유가 아니라 수강 정보로 조회한다")
    void findsSectionsByStudentNumber() {
        given(userRepository.findByStudentNumber("202099999")).willReturn(Optional.of(professor));
        given(enrollmentRepository.findAllByUserId("202099999")).willReturn(List.of(
                Enrollment.create(1L, "202099999", Role.STUDENT, Status.ACTIVE),
                Enrollment.create(2L, "202099999", Role.STUDENT, Status.WITHDRAWN)));
        // 탈퇴한 수강 정보(2L)는 제외하고 활성 분반만 넘긴다
        given(sectionRepository.findAllByIdIn(List.of(1L))).willReturn(List.of(sectionDetail()));

        assertThat(queryService.getSectionsByStudentNumber("202099999"))
                .singleElement()
                .satisfies(detail -> assertThat(detail.course().getName()).isEqualTo("객체지향프로그래밍"));

        verify(sectionRepository, never()).findAllByProfessorId("202099999");
    }

    @Test
    @DisplayName("없는 학생의 분반은 조회할 수 없다")
    void rejectsSectionsForMissingStudent() {
        given(userRepository.findByStudentNumber("999999999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getSectionsByStudentNumber("999999999"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("분반의 강좌를 없는 강좌로 옮길 수 없다")
    void rejectsMoveToMissingCourse() {
        Section section = section();
        given(courseRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.changeCourse(section, 99L))
                .isInstanceOf(CourseNotFoundException.class);

        assertThat(section.getCourseId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("담당 분반을 강좌 조건으로 거른다")
    void filtersSectionsByCourseCondition() {
        Course archived = Course.builder()
                .id(2L)
                .name("자료구조")
                .year(2025)
                .semester(SemesterType.FALL)
                .status(StatusType.ARCHIVED)
                .build();
        SectionDetail other = new SectionDetail(section(), archived, professor);
        given(userRepository.findByStudentNumber("202012345")).willReturn(Optional.of(professor));
        given(sectionRepository.findAllByProfessorId("202012345"))
                .willReturn(List.of(sectionDetail(), other));

        assertThat(queryService.getSectionsByProfessorId("202012345", StatusType.ACTIVE, null, null))
                .singleElement()
                .satisfies(detail -> assertThat(detail.course().getId()).isEqualTo(1L));
        assertThat(queryService.getSectionsByProfessorId("202012345", null, 2025, SemesterType.FALL))
                .singleElement()
                .satisfies(detail -> assertThat(detail.course().getId()).isEqualTo(2L));
        assertThat(queryService.getSectionsByProfessorId("202012345", null, null, null))
                .hasSize(2);
    }

    @Test
    @DisplayName("강좌별 분반을 조회한다")
    void findsSectionsByCourse() {
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));
        given(sectionRepository.findAllByCourseId(1L)).willReturn(List.of(sectionDetail()));

        assertThat(queryService.getSectionsByCourseId(1L))
                .singleElement()
                .satisfies(detail -> {
                    assertThat(detail.professor().getName()).isEqualTo("김교수");
                    assertThat(detail.course().getName()).isEqualTo("객체지향프로그래밍");
                });
    }

    @Test
    @DisplayName("없는 강좌의 분반 조회는 실패한다")
    void rejectsQueryForMissingCourse() {
        given(courseRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getSectionsByCourseId(99L))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    @DisplayName("담당 교수가 소유한 활성 분반이면 true를 반환한다")
    void confirmsActiveSectionOwnership() {
        given(sectionRepository.existsActiveByIdAndProfessorId(10L, "202012345")).willReturn(true);

        assertThat(queryService.isActiveSectionOwnedByProfessor(10L, "202012345")).isTrue();
    }

    @Test
    @DisplayName("다른 교수 소유이거나 삭제된 분반이면 false를 반환한다")
    void deniesOwnershipForOtherProfessorOrDeletedSection() {
        given(sectionRepository.existsActiveByIdAndProfessorId(10L, "999999999")).willReturn(false);

        assertThat(queryService.isActiveSectionOwnedByProfessor(10L, "999999999")).isFalse();
    }
}
