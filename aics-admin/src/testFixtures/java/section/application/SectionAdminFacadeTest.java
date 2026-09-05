package section.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.admin.section.application.SectionAdminFacade;
import kgu.developers.admin.enrollment.presentation.request.EnrollmentAdminRequest;
import kgu.developers.admin.enrollment.presentation.request.EnrollmentAdminUpdateRequest;
import kgu.developers.admin.section.presentation.request.SectionAdminRequest;
import kgu.developers.admin.section.presentation.request.SectionAdminUpdateRequest;
import kgu.developers.admin.section.presentation.request.SectionContactVisibilityUpdateRequest;
import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.enrollment.application.command.EnrollmentCommandService;
import kgu.developers.domain.enrollment.application.query.EnrollmentQueryService;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentDetail;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.section.application.command.SectionCommandService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.application.command.TeamMemberCommandService;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;

@ExtendWith(MockitoExtension.class)
class SectionAdminFacadeTest {

    private static final String PROFESSOR_ID = "202012345";
    private static final LocalDateTime FROM = LocalDateTime.of(2026, 3, 2, 0, 0);
    private static final LocalDateTime UNTIL = LocalDateTime.of(2026, 6, 20, 18, 0);

    @Mock
    private SectionCommandService sectionCommandService;

    @Mock
    private SectionQueryService sectionQueryService;

    @Mock
    private EnrollmentQueryService enrollmentQueryService;

    @Mock
    private EnrollmentCommandService enrollmentCommandService;

    @Mock
    private TeamQueryService teamQueryService;

    @Mock
    private TeamMemberCommandService teamMemberCommandService;

    @InjectMocks
    private SectionAdminFacade sectionAdminFacade;

    private final Course course = Course.builder()
            .id(1L)
            .name("객체지향프로그래밍")
            .year(2026)
            .semester(SemesterType.SPRING)
            .status(StatusType.ACTIVE)
            .build();

    private final User professor = User.create(PROFESSOR_ID, "prof@kgu.ac.kr", "김교수", "encoded",
            UserGlobalRole.USER, "010-0000-0000");

    private Section section() {
        return Section.builder()
                .id(1L)
                .professorId(PROFESSOR_ID)
                .courseId(1L)
                .code("1154")
                .name("월3,4/1154")
                .classTime("월3,4")
                .capacity(40)
                .contactVisibleFrom(FROM)
                .contactVisibleUntil(UNTIL)
                .build();
    }

    private SectionDetail detail(Section section) {
        return new SectionDetail(section, course, professor);
    }

    @Test
    @DisplayName("createSection은 요청 값을 커맨드 서비스에 넘기고 id를 응답한다")
    void createSection() {
        SectionAdminRequest request =
                new SectionAdminRequest(PROFESSOR_ID, 1L, "1154", "월3,4/1154", "월3,4", 40, FROM, UNTIL);
        given(sectionCommandService.createSection(PROFESSOR_ID, 1L, "1154", "월3,4/1154", "월3,4", 40, FROM, UNTIL))
                .willReturn(1L);

        assertThat(sectionAdminFacade.createSection(request).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getSectionsById는 조회 결과를 응답으로 변환한다")
    void getSectionsById() {
        given(sectionQueryService.getSectionById(1L)).willReturn(detail(section()));

        assertThat(sectionAdminFacade.getSectionsById(1L).code()).isEqualTo("1154");
    }

    @Test
    @DisplayName("getSectionsByProfessorId는 조건을 그대로 쿼리 서비스에 넘긴다")
    void getSectionsByProfessorId() {
        given(sectionQueryService.getSectionsByProfessorId(PROFESSOR_ID, StatusType.ACTIVE, 2026,
                SemesterType.SPRING)).willReturn(List.of(detail(section())));

        assertThat(sectionAdminFacade
                .getSectionsByProfessorId(PROFESSOR_ID, StatusType.ACTIVE, 2026, SemesterType.SPRING)
                .contents()).hasSize(1);
    }

    @Test
    @DisplayName("createEnrollment는 학번과 역할을 커맨드 서비스에 넘기고 id를 응답한다")
    void createEnrollment() {
        given(enrollmentCommandService.createEnrollment(1L, "202699999", Role.ASSISTANT)).willReturn(10L);

        assertThat(sectionAdminFacade
                .createEnrollment(1L, new EnrollmentAdminRequest("202699999", Role.ASSISTANT)).id())
                .isEqualTo(10L);
    }

    @Test
    @DisplayName("updateEnrollment는 변경 후 다시 조회한 값으로 응답한다")
    void updateEnrollment() {
        EnrollmentAdminUpdateRequest request = new EnrollmentAdminUpdateRequest(Role.ASSISTANT, Status.WITHDRAWN);
        Enrollment updated = Enrollment.builder()
                .id(1L).sectionId(1L).userId("202699999").role(Role.ASSISTANT).status(Status.WITHDRAWN).build();
        User student = User.create("202699999", "kgu@kyonggi.ac.kr", "김철수", "encoded",
                UserGlobalRole.USER, "010-1234-6789");
        given(enrollmentQueryService.getEnrollment(1L, "202699999"))
                .willReturn(new EnrollmentDetail(updated, student));

        assertThat(sectionAdminFacade.updateEnrollment(1L, "202699999", request))
                .satisfies(response -> {
                    assertThat(response.role()).isEqualTo(Role.ASSISTANT);
                    assertThat(response.status()).isEqualTo(Status.WITHDRAWN);
                });
        verify(enrollmentCommandService).updateEnrollment(1L, "202699999", Role.ASSISTANT, Status.WITHDRAWN);
        verify(teamMemberCommandService).withdrawFromTeam(1L, "202699999");
    }

    @Test
    @DisplayName("수강 상태를 ACTIVE로 복구해도 이전 팀 소속은 자동 복구하지 않는다")
    void reactivateEnrollmentDoesNotRestoreTeamMembership() {
        EnrollmentAdminUpdateRequest request = new EnrollmentAdminUpdateRequest(null, Status.ACTIVE);
        Enrollment updated = Enrollment.builder()
                .id(1L).sectionId(1L).userId("202699999").role(Role.STUDENT).status(Status.ACTIVE).build();
        User student = User.create("202699999", "kgu@kyonggi.ac.kr", "김철수", "encoded",
                UserGlobalRole.USER, "010-1234-6789");
        given(enrollmentQueryService.getEnrollment(1L, "202699999"))
                .willReturn(new EnrollmentDetail(updated, student));

        assertThat(sectionAdminFacade.updateEnrollment(1L, "202699999", request).status())
                .isEqualTo(Status.ACTIVE);

        verify(enrollmentCommandService).updateEnrollment(1L, "202699999", null, Status.ACTIVE);
        verify(teamMemberCommandService, never()).withdrawFromTeam(1L, "202699999");
    }

    @Test
    @DisplayName("getEnrollmentsBySectionId는 수강생 명단을 응답으로 변환한다")
    void getEnrollmentsBySectionId() {
        Enrollment enrollment = Enrollment.builder()
                .id(1L)
                .sectionId(1L)
                .userId("202699999")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .build();
        User student = User.create("202699999", "kgu@kyonggi.ac.kr", "김철수", "encoded",
                UserGlobalRole.USER, "010-1234-6789");
        given(enrollmentQueryService.getEnrollmentsBySectionId(1L))
                .willReturn(List.of(new EnrollmentDetail(enrollment, student)));

        assertThat(sectionAdminFacade.getEnrollmentsBySectionId(1L).contents())
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.studentNumber()).isEqualTo("202699999");
                    assertThat(response.name()).isEqualTo("김철수");
                    assertThat(response.status()).isEqualTo(Status.ACTIVE);
                });
    }

    @Test
    @DisplayName("getTeamsBySectionId는 팀 목록을 응답으로 변환한다")
    void getTeamsBySectionId() {
        Team team = Team.builder()
                .id(1L).sectionId(1L).name("1팀").kickoffRule("규칙").meetingSchedule("매주 목 19:00")
                .status(kgu.developers.domain.team.domain.Status.CONFIRMED).build();
        given(teamQueryService.getTeamsBySectionId(1L)).willReturn(List.of(team));

        assertThat(sectionAdminFacade.getTeamsBySectionId(1L).contents())
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.name()).isEqualTo("1팀");
                    assertThat(response.meetingSchedule()).isEqualTo("매주 목 19:00");
                });
    }

    @Test
    @DisplayName("updateSection은 연락처 공개기간을 건드리지 않는다")
    void updateSectionLeavesContactVisiblePeriodUntouched() {
        Section section = section();
        given(sectionQueryService.getSectionById(1L)).willReturn(detail(section));

        sectionAdminFacade.updateSection(1L,
                new SectionAdminUpdateRequest(null, null, null, "02분반", null, null));

        verify(sectionCommandService).updateSection(section, null, null, null, "02분반", null, null, null, null);
    }

    @Test
    @DisplayName("updateSection은 수정 후 다시 조회한 값으로 응답한다")
    void updateSectionRespondsWithRefetchedSection() {
        Section before = section();
        Section after = section();
        after.updateCode("1155");
        given(sectionQueryService.getSectionById(1L))
                .willReturn(detail(before), detail(after));

        assertThat(sectionAdminFacade.updateSection(1L,
                new SectionAdminUpdateRequest(null, null, "1155", null, null, null)).name())
                .isEqualTo("월3,4/1155");
    }

    @Test
    @DisplayName("updateSectionContactVisibility는 요청한 공개기간을 커맨드 서비스에 넘긴다")
    void updateSectionContactVisibility() {
        Section section = section();
        given(sectionQueryService.getSectionById(1L)).willReturn(detail(section));

        sectionAdminFacade.updateSectionContactVisibility(1L,
                new SectionContactVisibilityUpdateRequest(FROM.plusDays(1), UNTIL.plusDays(1)));

        verify(sectionCommandService).changeContactVisiblePeriod(section, FROM.plusDays(1), UNTIL.plusDays(1));
    }

    @Test
    @DisplayName("deleteSection은 조회한 분반을 커맨드 서비스에 넘긴다")
    void deleteSection() {
        Section section = section();
        given(sectionQueryService.getSectionById(1L)).willReturn(detail(section));

        sectionAdminFacade.deleteSection(1L);

        verify(sectionCommandService).deleteSection(section);
    }
}
