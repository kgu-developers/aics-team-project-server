package project.application;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import kgu.developers.api.project.application.ProjectFacade;
import kgu.developers.api.project.presentation.request.ProjectRequest;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.team.application.TeamFacade;
import kgu.developers.domain.editlock.application.command.EditLockCommandService;
import kgu.developers.domain.editlock.application.query.EditLockQueryService;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.editlock.exception.EditLockConflictException;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import mock.repository.FakeEnrollmentRepository;
import mock.repository.FakeTeamMemberRepository;
import mock.repository.FakeTeamRepository;
import mock.repository.FakeSectionRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.project.application.command.ProjectCommandService;
import kgu.developers.domain.project.application.query.ProjectQueryService;
import kgu.developers.domain.project.domain.*;
import kgu.developers.domain.project.exception.ProjectVersionConflictException;
import kgu.developers.domain.projectApproval.application.command.ProjectApprovalCommandService;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.domain.UserRepository;
import mock.repository.FakeEditLockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProposalSectionContentTest {
    private final JsonNodeFactory json = JsonNodeFactory.instance;
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final ProposalSectionRepository sections = mock(ProposalSectionRepository.class);
    private final EnumMap<ProposalSectionType, ProposalSection> state = new EnumMap<>(ProposalSectionType.class);
    private final Project project = Project.builder().id(10L).teamId(1L).version(0L)
        .title("title").description("description").goal("goal").approvalStatus(ApprovalStatus.DRAFT)
        .dataConfiguration(json.arrayNode()).screenConfiguration(json.arrayNode()).build();
    private final FakeEditLockRepository locks = new FakeEditLockRepository();
    private final FakeEnrollmentRepository enrollments = new FakeEnrollmentRepository();
    private final EditLockCommandService lockCommands = new EditLockCommandService(locks);
    private ValidatorFactory validation;
    private ProjectFacade facade;

    @BeforeEach
    void setUp() {
        enrollments.save(Enrollment.create(100L, "A", Role.STUDENT, Status.ACTIVE));
        enrollments.save(Enrollment.create(100L, "B", Role.STUDENT, Status.ACTIVE));
        when(projects.findAllByTeamId(1L)).thenAnswer(call -> List.of(project));
        when(projects.findIncludingDeletedByTeamId(1L)).thenAnswer(call -> Optional.of(project));
        when(projects.save(any())).thenAnswer(call -> {
            ReflectionTestUtils.setField(project, "version", project.getVersion() + 1);
            return project;
        });
        when(sections.findByProjectIdAndType(eq(10L), any())).thenAnswer(call -> Optional.ofNullable(state.get(call.getArgument(1))));
        when(sections.findAllByProjectId(10L)).thenAnswer(call -> List.copyOf(state.values()));
        when(sections.save(any())).thenAnswer(call -> {
            ProposalSection section = call.getArgument(0);
            state.put(section.getType(), section);
            return section;
        });
        var members = new FakeTeamMemberRepository();
        members.save(TeamMember.create(1L, "A", true, "개발"));
        members.save(TeamMember.create(1L, "B", false, "개발"));
        var teams = new FakeTeamRepository();
        teams.save(Team.builder().id(1L).sectionId(100L).build());
        var approvals = mock(ProjectApprovalRepository.class);
        validation = Validation.buildDefaultValidatorFactory();
        facade = new ProjectFacade(new ProjectCommandService(projects, approvals, sections, members), projects,
            new EditLockQueryService(locks), new ProjectQueryService(projects),
            new TeamAccessValidator(teams, members, new FakeSectionRepository()),
            approvals, mock(ProjectApprovalCommandService.class), members, mock(FileObjectRepository.class),
            mock(FileStorage.class), sections, mock(UserRepository.class), mock(TeamFacade.class), validation.getValidator(),
            teams, enrollments, mock(org.springframework.transaction.PlatformTransactionManager.class));
        lockCommands.acquire(EditLockTargetType.PROJECT, 10L, "DATA", "A");
        lockCommands.acquire(EditLockTargetType.PROJECT, 10L, "SCREEN", "B");
    }

    @AfterEach
    void close() {
        validation.close();
    }

    @Test
    void assistantCannotSaveWithAnExistingStudentLock() {
        var snapshot = facade.getProject(1L, "A");
        enrollments.findBySectionIdAndUserId(100L, "A").orElseThrow().updateRole(Role.ASSISTANT);

        assertThatThrownBy(() -> facade.saveProject(1L, "A", ProjectRequest.builder()
            .sectionKey(ProposalSectionType.DATA).sectionVersion(snapshot.sectionVersions().get(ProposalSectionType.DATA))
            .dataConfiguration(json.arrayNode().add("unauthorized data")).build()))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        assertThat(project.getDataConfiguration()).isEmpty();
        assertThat(state.get(ProposalSectionType.DATA).getContentVersion())
            .isEqualTo(snapshot.sectionVersions().get(ProposalSectionType.DATA));
        verify(projects, never()).save(any());
    }

    @Test
    void differentSectionsCanSaveFromTheSameInitialProjectSnapshot() {
        var data = facade.getProject(1L, "A");
        var screen = facade.getProject(1L, "B");
        var dataBody = json.arrayNode().add(json.objectNode().put("name", "A data"));
        var screenBody = json.arrayNode().add(json.objectNode().put("title", "B screen"));
        var a = facade.saveProject(1L, "A", ProjectRequest.builder().sectionKey(ProposalSectionType.DATA)
            .sectionVersion(data.sectionVersions().get(ProposalSectionType.DATA)).dataConfiguration(dataBody).build());
        var b = facade.saveProject(1L, "B", ProjectRequest.builder().sectionKey(ProposalSectionType.SCREEN)
            .sectionVersion(screen.sectionVersions().get(ProposalSectionType.SCREEN)).screenConfiguration(screenBody).build());
        assertThat(a.sectionVersions().get(ProposalSectionType.DATA)).isEqualTo(1);
        assertThat(b.sectionVersions().get(ProposalSectionType.SCREEN)).isEqualTo(1);
        assertThat(project.getDataConfiguration()).isEqualTo(dataBody);
        assertThat(project.getScreenConfiguration()).isEqualTo(screenBody);
    }

    @Test
    void oldVersionOfTheSameSectionIsRejectedWithoutOverwriting() {
        var snapshot = facade.getProject(1L, "A");
        var first = json.arrayNode().add("first");
        facade.saveProject(1L, "A", ProjectRequest.builder().sectionKey(ProposalSectionType.DATA)
            .sectionVersion(snapshot.sectionVersions().get(ProposalSectionType.DATA)).dataConfiguration(first).build());
        assertThatThrownBy(() -> facade.saveProject(1L, "A", ProjectRequest.builder().sectionKey(ProposalSectionType.DATA)
            .sectionVersion(snapshot.sectionVersions().get(ProposalSectionType.DATA)).dataConfiguration(json.arrayNode().add("stale")).build()))
            .isInstanceOf(ProjectVersionConflictException.class);
        assertThat(project.getDataConfiguration()).isEqualTo(first);
    }

    @Test
    void cannotSaveASectionOwnedByAnotherUser() {
        var snapshot = facade.getProject(1L, "B");
        assertThatThrownBy(() -> facade.saveProject(1L, "B", ProjectRequest.builder().sectionKey(ProposalSectionType.DATA)
            .sectionVersion(snapshot.sectionVersions().get(ProposalSectionType.DATA)).dataConfiguration(json.arrayNode()).build()))
            .isInstanceOf(EditLockConflictException.class);
    }

    @Test
    void fieldsOutsideSelectedSectionAreNeverApplied() {
        var snapshot = facade.getProject(1L, "B");
        facade.saveProject(1L, "B", ProjectRequest.builder().sectionKey(ProposalSectionType.SCREEN)
            .sectionVersion(snapshot.sectionVersions().get(ProposalSectionType.SCREEN)).screenConfiguration(json.arrayNode())
            .dataConfiguration(json.arrayNode().add("unauthorized data")).title("unauthorized title").build());
        assertThat(project.getTitle()).isEqualTo("title");
        assertThat(project.getDataConfiguration()).isEmpty();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"null", "{}", "\"text\""})
    void invalidDataShapeIsRejectedBeforeAnyWrite(String body) throws Exception {
        var invalid = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        assertThatThrownBy(() -> facade.saveProject(1L, "A", ProjectRequest.builder().sectionKey(ProposalSectionType.DATA).sectionVersion(0L).dataConfiguration(invalid).build()))
            .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        verify(projects, never()).save(any());
        assertThat(state).isEmpty();
    }

    @Test
    void sharedCommandSaveAlsoInvalidatesTheAffectedSectionVersion() {
        var old = facade.getProject(1L, "A");
        var commands = new ProjectCommandService(projects, mock(ProjectApprovalRepository.class), sections, mock(TeamMemberRepository.class));
        commands.saveProject(1L, project.getTitle(), project.getDescription(), project.getGoal(), null, null,
            json.arrayNode().add("shared command save"), project.getScreenConfiguration(), null);
        assertThatThrownBy(() -> facade.saveProject(1L, "A", ProjectRequest.builder().sectionKey(ProposalSectionType.DATA).sectionVersion(old.sectionVersions().get(ProposalSectionType.DATA)).dataConfiguration(json.arrayNode()).build()))
            .isInstanceOf(ProjectVersionConflictException.class);
        assertThat(project.getDataConfiguration().get(0).asText()).isEqualTo("shared command save");
    }

    @Test
    void expiredLockIsRejectedAndDraftOfOtherSectionsMayBeBlank() {
        ReflectionTestUtils.setField(project, "title", "");
        ReflectionTestUtils.setField(project, "description", "");
        ReflectionTestUtils.setField(project, "goal", "");
        var snapshot = facade.getProject(1L, "A");
        var saved = facade.saveProject(1L, "A", ProjectRequest.builder().sectionKey(ProposalSectionType.DATA).sectionVersion(snapshot.sectionVersions().get(ProposalSectionType.DATA)).dataConfiguration(json.arrayNode().add("data")).build());
        var lock = locks.findByTargetTypeAndTargetIdAndSectionKey(EditLockTargetType.PROJECT, 10L, "DATA").orElseThrow();
        locks.forceLockedAt(lock.getId(), java.time.LocalDateTime.now().minusMinutes(3));
        assertThatThrownBy(() -> facade.saveProject(1L, "A", ProjectRequest.builder().sectionKey(ProposalSectionType.DATA).sectionVersion(saved.sectionVersions().get(ProposalSectionType.DATA)).dataConfiguration(json.arrayNode()).build()))
            .isInstanceOf(EditLockConflictException.class);
    }

    @Test
    void recreatedSectionStartsAfterThePreviousProjectLifetime() {
        var old = facade.getProject(1L, "A");
        state.clear();
        ReflectionTestUtils.setField(project, "version", 5L);
        assertThat(facade.getProject(1L, "A").sectionVersions().get(ProposalSectionType.DATA)).isEqualTo(5L);
        assertThatThrownBy(() -> facade.saveProject(1L, "A", ProjectRequest.builder().sectionKey(ProposalSectionType.DATA).sectionVersion(old.sectionVersions().get(ProposalSectionType.DATA)).dataConfiguration(json.arrayNode()).build()))
            .isInstanceOf(ProjectVersionConflictException.class);
    }

    @Test
    void completedProjectCannotBeChanged() {
        project.completeProposal();
        assertThatThrownBy(() -> facade.saveProject(1L, "A", ProjectRequest.builder().sectionKey(ProposalSectionType.DATA).sectionVersion(0L).dataConfiguration(json.arrayNode()).build()))
            .isInstanceOf(kgu.developers.domain.project.exception.ProjectProposalCompletedException.class);
        verify(projects, never()).save(any());
    }

    @Test
    void teamOperationAndTopicUseTheirOwnVersionsAndFields() {
        lockCommands.acquire(EditLockTargetType.PROJECT, 10L, "TEAM_OPERATION", "A");
        lockCommands.acquire(EditLockTargetType.PROJECT, 10L, "TOPIC", "B");
        var operation = facade.getProject(1L, "A");
        var topic = facade.getProject(1L, "B");
        facade.saveProject(1L, "A", ProjectRequest.builder().sectionKey(ProposalSectionType.TEAM_OPERATION)
            .sectionVersion(operation.sectionVersions().get(ProposalSectionType.TEAM_OPERATION)).projectSchedule("new schedule").build());
        var saved = facade.saveProject(1L, "B", ProjectRequest.builder().sectionKey(ProposalSectionType.TOPIC)
            .sectionVersion(topic.sectionVersions().get(ProposalSectionType.TOPIC)).title("new title").description("new description").goal("new goal").build());
        assertThat(saved.sectionVersions().get(ProposalSectionType.TOPIC)).isEqualTo(1L);
        assertThat(project.getProjectSchedule()).isEqualTo("new schedule");
        assertThat(project.getTitle()).isEqualTo("new title");
    }

    @Test
    void topicRequiresItsRequiredTextEvenWhenOtherSectionsAreOptional() {
        assertThatThrownBy(() -> facade.saveProject(1L, "A", ProjectRequest.builder().sectionKey(ProposalSectionType.TOPIC).sectionVersion(0L).title(" ").description("description").goal("goal").build()))
            .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        verify(projects, never()).save(any());
    }

    @Test
    void screenImageOwnershipIsStillRequired() {
        var snapshot = facade.getProject(1L, "B");
        assertThatThrownBy(() -> facade.saveProject(1L, "B", ProjectRequest.builder().sectionKey(ProposalSectionType.SCREEN).sectionVersion(snapshot.sectionVersions().get(ProposalSectionType.SCREEN))
                .screenConfiguration(json.arrayNode().add(json.objectNode().put("imageFileId", 999))).build()))
            .isInstanceOf(kgu.developers.domain.project.exception.ProjectScreenImageOwnershipException.class);
        verify(projects, never()).save(any());
    }

    @Test
    void existingGetProvidesAllSectionVersionsInOneSnapshot() {
        var response = facade.getProject(1L, "A");
        var body = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(response);
        assertThat(body.path("sectionVersions").has("DATA")).isTrue();
        assertThat(body.path("sectionVersions").has("SCREEN")).isTrue();
    }

}
