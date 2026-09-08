package project.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.project.application.command.ProjectCommandService;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.project.domain.ProposalSection;
import kgu.developers.domain.project.domain.ProposalSectionRepository;
import kgu.developers.domain.project.domain.ProposalSectionType;
import kgu.developers.domain.project.exception.ProjectProposalCompletedException;
import kgu.developers.domain.project.exception.ProposalSectionAssigneeNotMemberException;
import kgu.developers.domain.project.exception.ProposalSectionIncompleteException;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.common.json.JsonConverter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProjectCommandServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectApprovalRepository projectApprovalRepository;
    @Mock private ProposalSectionRepository proposalSectionRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @InjectMocks private ProjectCommandService projectCommandService;

    @Test
    @DisplayName("saveProject는 기존 제안서가 없으면 DRAFT 상태로 생성한다")
    void saveProject_createsProject() throws Exception {
        given(projectRepository.findIncludingDeletedByTeamId(1L)).willReturn(Optional.empty());
        given(projectRepository.save(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> invocation.getArgument(0));

        Project result = saveProject();

        assertThat(result.getApprovalStatus()).isEqualTo(ApprovalStatus.DRAFT);
        assertThat(result.getTitle()).isEqualTo("새 제목");
    }

    @Test
    @DisplayName("saveProject는 기존 제안서를 수정하면서 승인 이력을 삭제한다")
    void saveProject_updatesAndClearsApprovals() throws Exception {
        Project existing = Project.builder().id(10L).teamId(1L).title("기존 제목").description("기존 설명")
            .goal("기존 목표").approvalStatus(ApprovalStatus.APPROVED).build();
        given(projectRepository.findIncludingDeletedByTeamId(1L)).willReturn(Optional.of(existing));
        given(projectRepository.save(existing)).willReturn(existing);

        Project result = saveProject();

        assertThat(result.getTitle()).isEqualTo("새 제목");
        assertThat(result.getApprovalStatus()).isEqualTo(ApprovalStatus.DRAFT);
        assertThat(result.getProposalRevision()).isEqualTo(1L);
        then(projectApprovalRepository).should().deleteAllByProjectId(10L);
    }

    @Test
    @DisplayName("saveProject는 내용이 같으면 승인 이력을 초기화하지 않는다")
    void saveProject_keepsApprovalsWhenContentIsUnchanged() throws Exception {
        Project existing = Project.builder().id(10L).teamId(1L).title("새 제목").description("새 설명")
            .goal("새 목표").repositoryUrl("https://github.com/kgu/project")
            .externalLinks(new ObjectMapper().readTree("[]")).approvalStatus(ApprovalStatus.DRAFT)
            .dataConfiguration(JsonConverter.parse("[{\"name\":\"학습 로그\",\"description\":\"문제 풀이 기록\",\"expectedCount\":\"약 1만 건\"}]"))
            .screenConfiguration(new ObjectMapper().readTree("[{\"title\":\"홈\",\"description\":\"요약\",\"imageFileId\":1}]"))
            .projectSchedule("4월: 설계, 5월: 개발, 6월: 통합 테스트")
            .build();
        given(projectRepository.findIncludingDeletedByTeamId(1L)).willReturn(Optional.of(existing));

        saveProject();

        then(projectApprovalRepository).shouldHaveNoInteractions();
        then(projectRepository).should(org.mockito.Mockito.never()).save(existing);
    }

    @Test
    @DisplayName("saveProject는 화면 구성만 바뀌어도 리비전을 올리고 승인 이력을 지운다")
    void saveProject_bumpsRevisionWhenScreenConfigurationChanges() throws Exception {
        Project existing = Project.builder().id(10L).teamId(1L).title("새 제목").description("새 설명")
            .goal("새 목표").repositoryUrl("https://github.com/kgu/project")
            .externalLinks(new ObjectMapper().readTree("[]")).approvalStatus(ApprovalStatus.DRAFT)
            .dataConfiguration(JsonConverter.parse("[{\"name\":\"학습 로그\",\"description\":\"문제 풀이 기록\",\"expectedCount\":\"약 1만 건\"}]"))
            .screenConfiguration(new ObjectMapper().readTree("[]"))
            .projectSchedule("4월: 설계, 5월: 개발, 6월: 통합 테스트")
            .build();
        given(projectRepository.findIncludingDeletedByTeamId(1L)).willReturn(Optional.of(existing));
        given(projectRepository.save(existing)).willReturn(existing);

        Project result = saveProject();

        assertThat(result.getProposalRevision()).isEqualTo(1L);
        assertThat(result.getScreenConfiguration().get(0).get("title").asText()).isEqualTo("홈");
        then(projectApprovalRepository).should().deleteAllByProjectId(10L);
    }

    @Test
    @DisplayName("saveProject는 완료된 제안서를 수정할 수 없다")
    void saveProject_rejectsCompletedProject() throws Exception {
        Project completed = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).proposalCompletedAt(LocalDateTime.now()).build();
        given(projectRepository.findIncludingDeletedByTeamId(1L)).willReturn(Optional.of(completed));

        assertThatThrownBy(this::saveProject).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("completeProposal은 모든 섹션이 완료면 팀원 동의가 없어도 제출한다")
    void completeProposal() {
        Project project = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).build();
        given(projectRepository.findByIdForUpdate(10L)).willReturn(Optional.of(project));
        given(proposalSectionRepository.findAllByProjectId(10L)).willReturn(completedSections(ProposalSectionType.values()));

        projectCommandService.completeProposal(10L);

        assertThat(project.getProposalCompletedAt()).isNotNull();
        assertThat(project.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        then(projectRepository).should().save(project);
        then(projectApprovalRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("completeProposal은 완료되지 않은 섹션이 있으면 예외를 던진다")
    void completeProposal_rejectsIncompleteSections() {
        Project project = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).build();
        given(projectRepository.findByIdForUpdate(10L)).willReturn(Optional.of(project));
        given(proposalSectionRepository.findAllByProjectId(10L))
            .willReturn(completedSections(ProposalSectionType.TOPIC, ProposalSectionType.SCREEN, ProposalSectionType.DATA));

        assertThatThrownBy(() -> projectCommandService.completeProposal(10L))
            .isInstanceOf(ProposalSectionIncompleteException.class);
        then(projectRepository).should(org.mockito.Mockito.never()).save(project);
    }

    @Test
    @DisplayName("updateProposalSection은 없던 섹션을 만들고 담당자·완료 상태를 저장한다")
    void updateProposalSection_createsSection() {
        Project project = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).build();
        given(projectRepository.findById(10L)).willReturn(Optional.of(project));
        given(teamMemberRepository.findByTeamIdAndUserId(1L, "202412345"))
            .willReturn(Optional.of(TeamMember.create(1L, "202412345", false, "개발자")));
        given(proposalSectionRepository.findByProjectIdAndType(10L, ProposalSectionType.SCREEN)).willReturn(Optional.empty());
        given(proposalSectionRepository.save(org.mockito.ArgumentMatchers.any()))
            .willAnswer(invocation -> invocation.getArgument(0));

        ProposalSection result = projectCommandService.updateProposalSection(10L, ProposalSectionType.SCREEN, "202412345", true);

        assertThat(result.getType()).isEqualTo(ProposalSectionType.SCREEN);
        assertThat(result.getAssigneeUserId()).isEqualTo("202412345");
        assertThat(result.isCompleted()).isTrue();
        then(projectRepository).should().lockTeam(1L);
    }

    @Test
    @DisplayName("updateProposalSection은 팀원이 아닌 담당자를 거부한다")
    void updateProposalSection_rejectsNonMemberAssignee() {
        Project project = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).build();
        given(projectRepository.findById(10L)).willReturn(Optional.of(project));
        given(teamMemberRepository.findByTeamIdAndUserId(1L, "202499999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectCommandService.updateProposalSection(10L, ProposalSectionType.SCREEN, "202499999", true))
            .isInstanceOf(ProposalSectionAssigneeNotMemberException.class);
        then(proposalSectionRepository).should(org.mockito.Mockito.never())
            .save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("updateProposalSection은 이미 제출된 제안서면 예외를 던진다")
    void updateProposalSection_rejectsCompletedProposal() {
        Project project = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.APPROVED).proposalCompletedAt(LocalDateTime.now()).build();
        given(projectRepository.findById(10L)).willReturn(Optional.of(project));

        assertThatThrownBy(() -> projectCommandService.updateProposalSection(10L, ProposalSectionType.SCREEN, null, true))
            .isInstanceOf(ProjectProposalCompletedException.class);
    }

    private List<ProposalSection> completedSections(ProposalSectionType... types) {
        return java.util.Arrays.stream(types)
            .map(type -> {
                ProposalSection section = ProposalSection.create(10L, type);
                section.updateCompleted(true);
                return section;
            })
            .toList();
    }

    @Test
    @DisplayName("completeProposal은 이미 완료된 제안서면 예외를 던진다")
    void completeProposal_rejectsCompletedProject() {
        Project project = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).proposalCompletedAt(LocalDateTime.now()).build();
        given(projectRepository.findByIdForUpdate(10L)).willReturn(Optional.of(project));

        assertThatThrownBy(() -> projectCommandService.completeProposal(10L)).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("saveProject는 삭제된 프로젝트를 복원해 동일 팀의 새 제안서를 저장한다")
    void saveProject_restoresSoftDeletedProject() throws Exception {
        Project deleted = Project.builder().id(10L).teamId(1L).title("기존 제목").description("기존 설명")
            .goal("기존 목표").approvalStatus(ApprovalStatus.DRAFT).deletedAt(LocalDateTime.now()).build();
        given(projectRepository.findIncludingDeletedByTeamId(1L)).willReturn(Optional.of(deleted));
        given(projectRepository.reactivate(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
            .willAnswer(invocation -> {
                Project newProject = invocation.getArgument(1);
                deleted.reactivate(newProject.getTitle(), newProject.getDescription(), newProject.getGoal(),
                    newProject.getRepositoryUrl(), newProject.getExternalLinks(), newProject.getApprovalStatus(),
                    newProject.getTopicCandidateId(),
                    newProject.getDataConfiguration(), newProject.getScreenConfiguration(), newProject.getProjectSchedule());
                return deleted;
            });

        Project result = saveProject();

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getDeletedAt()).isNull();
        assertThat(result.getTitle()).isEqualTo("새 제목");
        assertThat(result.getApprovalStatus()).isEqualTo(ApprovalStatus.DRAFT);
        assertThat(result.getProposalRevision()).isEqualTo(1L);
        then(projectApprovalRepository).should().deleteAllByProjectId(10L);
        then(projectRepository).should(org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("saveProject는 조회 전에 팀 행을 잠가 최초 등록 경합을 막는다")
    void saveProject_locksTeamBeforeLookup() throws Exception {
        given(projectRepository.findIncludingDeletedByTeamId(1L)).willReturn(Optional.empty());
        given(projectRepository.save(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> invocation.getArgument(0));

        saveProject();

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(projectRepository);
        inOrder.verify(projectRepository).lockTeam(1L);
        inOrder.verify(projectRepository).findIncludingDeletedByTeamId(1L);
    }

    @Test
    @DisplayName("deleteProject는 제안서와 동의 이력을 함께 삭제한다")
    void deleteProject_deletesProjectAndApprovals() {
        projectCommandService.deleteProject(10L);

        then(projectRepository).should().deleteById(10L);
        then(projectApprovalRepository).should().deleteAllByProjectId(10L);
    }

    @Test
    @DisplayName("finalizeTopic은 주제를 바꾸면 리비전을 올리고 기존 동의를 무효화한다")
    void finalizeTopic_bumpsRevisionAndClearsApprovals() {
        Project existing = Project.builder().id(10L).teamId(1L).title("기존 제목").description("기존 설명")
            .goal("기존 목표").approvalStatus(ApprovalStatus.APPROVED)
            .dataConfiguration(JsonConverter.parse("[{\"name\":\"학습 로그\",\"description\":\"문제 풀이 기록\",\"expectedCount\":\"약 1만 건\"}]"))
            .screenConfiguration(new ObjectMapper().createArrayNode())
            .build();
        given(projectRepository.findIncludingDeletedByTeamId(1L)).willReturn(Optional.of(existing));
        given(projectRepository.save(existing)).willReturn(existing);

        Project result = projectCommandService.finalizeTopic(1L, 7L, "확정 제목", "확정 설명", "확정 목표");

        assertThat(result.getTitle()).isEqualTo("확정 제목");
        assertThat(result.getTopicCandidateId()).isEqualTo(7L);
        assertThat(result.getApprovalStatus()).isEqualTo(ApprovalStatus.DRAFT);
        assertThat(result.getProposalRevision()).isEqualTo(1L);
        then(projectApprovalRepository).should().deleteAllByProjectId(10L);
    }

    @Test
    @DisplayName("finalizeTopic은 완료된 제안서의 주제를 바꿀 수 없다")
    void finalizeTopic_rejectsCompletedProject() {
        Project completed = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).proposalCompletedAt(LocalDateTime.now()).build();
        given(projectRepository.findIncludingDeletedByTeamId(1L)).willReturn(Optional.of(completed));

        assertThatThrownBy(() -> projectCommandService.finalizeTopic(1L, 7L, "확정 제목", "확정 설명", "확정 목표"))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("invalidateProposalForKickoffChange는 리비전을 올리고 이전 동의를 지운다")
    void invalidateProposalForKickoffChange() {
        Project active = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).build();
        given(projectRepository.findIncludingDeletedByTeamId(1L)).willReturn(Optional.of(active));

        projectCommandService.invalidateProposalForKickoffChange(1L);

        assertThat(active.getProposalRevision()).isEqualTo(1L);
        then(projectApprovalRepository).should().deleteAllByProjectId(10L);
    }

    @Test
    @DisplayName("invalidateProposalForKickoffChange는 이미 제출된 제안서는 건드리지 않는다")
    void invalidateProposalForKickoffChange_skipsCompletedProposal() {
        Project completed = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).proposalCompletedAt(LocalDateTime.now()).build();
        given(projectRepository.findIncludingDeletedByTeamId(1L)).willReturn(Optional.of(completed));

        projectCommandService.invalidateProposalForKickoffChange(1L);

        assertThat(completed.getProposalRevision()).isZero();
        then(projectApprovalRepository).shouldHaveNoInteractions();
    }

    private Project saveProject() throws Exception {
        return projectCommandService.saveProject(1L, "새 제목", "새 설명", "새 목표",
            "https://github.com/kgu/project", new ObjectMapper().readTree("[]"),
            JsonConverter.parse("[{\"name\":\"학습 로그\",\"description\":\"문제 풀이 기록\",\"expectedCount\":\"약 1만 건\"}]"),
            new ObjectMapper().readTree("[{\"title\":\"홈\",\"description\":\"요약\",\"imageFileId\":1}]"), "4월: 설계, 5월: 개발, 6월: 통합 테스트");
    }
}
