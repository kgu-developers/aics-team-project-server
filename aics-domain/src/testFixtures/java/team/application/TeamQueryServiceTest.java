package team.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import kgu.developers.domain.course.domain.SemesterType;
import kgu.developers.domain.course.domain.StatusType;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.ContactNotVisibleException;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;

@ExtendWith(MockitoExtension.class)
class TeamQueryServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private TeamQueryService teamQueryService;

    private SectionDetail sectionDetail() {
        return sectionDetail(null, null);
    }

    private SectionDetail sectionDetail(LocalDateTime from, LocalDateTime until) {
        Section section = Section.builder().id(1L).professorId("202012345").courseId(1L)
                .contactVisibleFrom(from).contactVisibleUntil(until).build();
        Course course = Course.builder().id(1L).name("객체지향프로그래밍").year(2026)
                .semester(SemesterType.SPRING).status(StatusType.ACTIVE).build();
        User professor = User.create("202012345", "prof@kgu.ac.kr", "김교수", "encoded",
                UserGlobalRole.USER, "010-0000-0000");
        return new SectionDetail(section, course, professor);
    }

    private Team team(String name) {
        return Team.create(1L, name, "규칙", "매주 목 19:00", Status.FORMING);
    }

    @Test
    @DisplayName("분반의 팀 목록을 팀명 순으로 조회한다")
    void getTeamsBySectionId() {
        given(sectionRepository.findById(1L)).willReturn(Optional.of(sectionDetail()));
        given(teamRepository.findAllBySectionId(1L))
                .willReturn(List.of(team("3팀"), team("1팀"), team("2팀")));

        assertThat(teamQueryService.getTeamsBySectionId(1L))
                .extracting(Team::getName)
                .containsExactly("1팀", "2팀", "3팀");
    }

    @Test
    @DisplayName("없는 분반의 팀 목록은 조회할 수 없다")
    void rejectsMissingSection() {
        given(sectionRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamQueryService.getTeamsBySectionId(99L))
                .isInstanceOf(SectionNotFoundException.class);

        verify(teamRepository, never()).findAllBySectionId(99L);
    }

    @Test
    @DisplayName("공개기간 안이면 연락처 조회를 허용한다")
    void validateContactVisible() {
        LocalDateTime now = LocalDateTime.now();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team("1팀")));
        given(sectionRepository.findById(1L))
                .willReturn(Optional.of(sectionDetail(now.minusDays(1), now.plusDays(1))));

        teamQueryService.validateContactVisible(1L);
    }

    @Test
    @DisplayName("공개기간 밖이면 연락처를 조회할 수 없다")
    void rejectsContactOutsidePeriod() {
        LocalDateTime now = LocalDateTime.now();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team("1팀")));
        given(sectionRepository.findById(1L))
                .willReturn(Optional.of(sectionDetail(now.minusDays(10), now.minusDays(1))));

        assertThatThrownBy(() -> teamQueryService.validateContactVisible(1L))
                .isInstanceOf(ContactNotVisibleException.class);
    }
}
