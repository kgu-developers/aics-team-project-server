package submission.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import kgu.developers.admin.submission.application.SubmissionAdminFacade;
import kgu.developers.admin.submission.application.SubmissionArtifactsZipDownload;
import kgu.developers.admin.submission.presentation.response.SubmissionAdminListResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionAdminResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionVersionAdminDetailResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionVersionAdminListResponse;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.project.domain.ProposalSection;
import kgu.developers.domain.project.domain.ProposalSectionRepository;
import kgu.developers.domain.project.domain.ProposalSectionType;
import kgu.developers.domain.project.application.command.ProjectCommandService;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import kgu.developers.domain.teammessage.domain.DocumentFeedbackRepository;
import kgu.developers.domain.teammessage.domain.DocumentFeedbackSummary;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.submission.application.query.SubmissionQueryService;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionArtifact;
import kgu.developers.domain.submission.domain.SubmissionStatus;
import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.submission.exception.SubmissionVersionNotFoundException;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;

import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportRepository;
import kgu.developers.domain.midreport.domain.MidReportStatus;
import mock.repository.FakeEnrollmentRepository;
import mock.repository.FakeFileObjectRepository;
import mock.repository.FakeFileStorage;
import mock.repository.FakeSubmissionArtifactRepository;
import mock.repository.FakeSubmissionRepository;
import mock.repository.FakeSubmissionVersionRepository;
import mock.repository.FakeTeamRepository;
import mock.repository.FakeUserRepository;

class SubmissionAdminFacadeTest {

    private static final Long SECTION_ID = 1L;
    private static final Long OTHER_TEAM_ID = 20L;
    private static final Long MILESTONE_ID = 5L;
    private static final String PROFESSOR = "professor1";
    private static final String OTHER_PROFESSOR = "professor2";

    private MilestoneRepository milestoneRepository;
    private SectionQueryService sectionQueryService;
    private FakeTeamRepository teamRepository;
    private ProjectRepository projectRepository;
    private FakeSubmissionRepository submissionRepository;
    private FakeSubmissionVersionRepository submissionVersionRepository;
    private FakeSubmissionArtifactRepository submissionArtifactRepository;
    private FakeFileObjectRepository fileObjectRepository;
    private FakeUserRepository userRepository;
    private MeetingRecordQueryService meetingRecordQueryService;
    private MidReportRepository midReportRepository;
    private DocumentFeedbackRepository documentFeedbackRepository;
    private ProposalSectionRepository proposalSectionRepository;
    private SubmissionAdminFacade submissionAdminFacade;
    private Long teamId;

    @BeforeEach
    void setUp() {
        milestoneRepository = mock(MilestoneRepository.class);
        sectionQueryService = mock(SectionQueryService.class);

        teamRepository = new FakeTeamRepository();
        Team team = teamRepository.save(Team.builder()
                .sectionId(SECTION_ID)
                .name("A팀")
                .status(Status.CONFIRMED)
                .build());
        teamId = team.getId();
        projectRepository = mock(ProjectRepository.class);

        submissionRepository = new FakeSubmissionRepository();
        submissionVersionRepository = new FakeSubmissionVersionRepository();
        submissionArtifactRepository = new FakeSubmissionArtifactRepository();
        fileObjectRepository = new FakeFileObjectRepository();
        FakeFileStorage fileStorage = new FakeFileStorage();
        userRepository = new FakeUserRepository();
        userRepository.save(User.create(
                "202412345", "member@kyonggi.ac.kr", "팀원학생", "pw", UserGlobalRole.USER, "010-0000-0001"));
        UserQueryService userQueryService = new UserQueryService(userRepository, new FakeEnrollmentRepository());

        SubmissionQueryService submissionQueryService = new SubmissionQueryService(
                submissionRepository, milestoneRepository,
                mock(org.springframework.transaction.PlatformTransactionManager.class));

        meetingRecordQueryService = mock(MeetingRecordQueryService.class);
        midReportRepository = mock(MidReportRepository.class);
        documentFeedbackRepository = mock(DocumentFeedbackRepository.class);
        proposalSectionRepository = mock(ProposalSectionRepository.class);
        submissionAdminFacade = new SubmissionAdminFacade(
                milestoneRepository,
                sectionQueryService,
                teamRepository,
                projectRepository,
                submissionQueryService,
                submissionVersionRepository,
                submissionArtifactRepository,
                fileObjectRepository,
                fileStorage,
                userQueryService,
                meetingRecordQueryService,
                midReportRepository,
                documentFeedbackRepository,
                proposalSectionRepository
        );
    }

    @Test
    @DisplayName("담당 교수는 마일스톤의 팀별 제출 현황을 조회할 수 있다")
    void getSubmissionsByMilestone_AllowsOwningProfessor() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        SubmissionAdminListResponse response = submissionAdminFacade.getSubmissionsByMilestone(MILESTONE_ID, null, PROFESSOR);

        assertThat(response.contents()).hasSize(1);
        assertThat(response.contents().get(0).teamId()).isEqualTo(teamId);
        assertThat(response.contents().get(0).status()).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
    }

    @Test
    @DisplayName("팀별 제출 현황은 해당 마일스톤과 연결된 회의록 수와 전체 회의록 수를 함께 응답한다")
    void getSubmissionsByMilestone_IncludesMeetingRecordCount() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        given(meetingRecordQueryService.countMeetingRecords(List.of(teamId), MILESTONE_ID))
                .willReturn(Map.of(teamId, 2L));
        given(meetingRecordQueryService.countMeetingRecords(List.of(teamId)))
                .willReturn(Map.of(teamId, 5L));

        SubmissionAdminListResponse response = submissionAdminFacade
                .getSubmissionsByMilestone(MILESTONE_ID, null, PROFESSOR);

        assertThat(response.contents()).singleElement().satisfies(content -> {
            assertThat(content.meetingRecordCount()).isEqualTo(2L);
            assertThat(content.totalMeetingRecordCount()).isEqualTo(5L);
        });
    }

    @Test
    @DisplayName("아직 한 번도 조회되지 않은 팀도 not_submitted 상태로 함께 조회된다")
    void getSubmissionsByMilestone_IncludesTeamsWithoutSubmissionRow() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        SubmissionAdminListResponse response = submissionAdminFacade.getSubmissionsByMilestone(MILESTONE_ID, null, PROFESSOR);

        assertThat(submissionRepository.findByTeamIdAndMilestoneId(teamId, MILESTONE_ID)).isPresent();
        assertThat(response.contents()).extracting(SubmissionAdminResponse::teamId).containsExactly(teamId);
    }

    @Test
    @DisplayName("담당 교수가 아니면 팀별 제출 현황을 조회할 수 없다")
    void getSubmissionsByMilestone_RejectsNonOwningProfessor() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, OTHER_PROFESSOR)).willReturn(false);

        assertThatThrownBy(() -> submissionAdminFacade.getSubmissionsByMilestone(MILESTONE_ID, null, OTHER_PROFESSOR))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("제안서 마일스톤은 팀 프로젝트 주제를 함께 응답한다")
    void getSubmissionsByMilestone_IncludesProjectTitleForProposal() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(proposalMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        given(projectRepository.findAllByTeamIdIn(List.of(teamId))).willReturn(List.of(Project.builder().id(90L).teamId(teamId)
                .title("AI 기반 팀 프로젝트 운영 플랫폼").description("설명").goal("목표")
                .approvalStatus(ApprovalStatus.APPROVED)
                .dataConfiguration(JsonNodeFactory.instance.arrayNode())
                .screenConfiguration(JsonNodeFactory.instance.arrayNode()).build()));

        SubmissionAdminListResponse response = submissionAdminFacade
                .getSubmissionsByMilestone(MILESTONE_ID, null, PROFESSOR);

        assertThat(response.contents()).singleElement()
                .extracting(SubmissionAdminResponse::projectTitle)
                .isEqualTo("AI 기반 팀 프로젝트 운영 플랫폼");
    }

    @Test
    @DisplayName("teamId를 지정하면 해당 팀의 제출 현황만 응답한다")
    void getSubmissionsByMilestone_FiltersByTeamId() {
        Team otherTeam = teamRepository.save(Team.builder()
                .sectionId(SECTION_ID)
                .name("B팀")
                .status(Status.CONFIRMED)
                .build());
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        SubmissionAdminListResponse response = submissionAdminFacade
                .getSubmissionsByMilestone(MILESTONE_ID, otherTeam.getId(), PROFESSOR);

        assertThat(response.contents()).extracting(SubmissionAdminResponse::teamId)
                .containsExactly(otherTeam.getId());
    }

    @Test
    @DisplayName("마일스톤 분반에 속하지 않은 teamId는 거부한다")
    void getSubmissionsByMilestone_RejectsForeignTeamId() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        assertThatThrownBy(() -> submissionAdminFacade
                .getSubmissionsByMilestone(MILESTONE_ID, OTHER_TEAM_ID, PROFESSOR))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("담당 분반의 제출만 조회할 수 있습니다.");
    }

    @Test
    @DisplayName("담당 교수는 제출 상세를 조회할 수 있다")
    void getSubmission_AllowsOwningProfessor() {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        given(meetingRecordQueryService.countMeetingRecords(teamId, MILESTONE_ID)).willReturn(2L);
        given(meetingRecordQueryService.countMeetingRecords(teamId)).willReturn(5L);

        SubmissionAdminResponse response = submissionAdminFacade.getSubmission(submission.getId(), PROFESSOR);

        assertThat(response.id()).isEqualTo(submission.getId());
        assertThat(response.teamName()).isEqualTo("A팀");
        assertThat(response.meetingRecordCount()).isEqualTo(2L);
        assertThat(response.totalMeetingRecordCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("제안서 제출 상세에도 팀 프로젝트 주제를 함께 응답한다")
    void getSubmission_IncludesProjectTitleForProposal() {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(proposalMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        given(projectRepository.findAllByTeamIdIn(List.of(teamId))).willReturn(List.of(Project.builder().id(90L).teamId(teamId)
                .title("AI 기반 팀 프로젝트 운영 플랫폼").description("설명").goal("목표")
                .approvalStatus(ApprovalStatus.APPROVED)
                .dataConfiguration(JsonNodeFactory.instance.arrayNode())
                .screenConfiguration(JsonNodeFactory.instance.arrayNode()).build()));

        SubmissionAdminResponse response = submissionAdminFacade.getSubmission(submission.getId(), PROFESSOR);

        assertThat(response.projectTitle()).isEqualTo("AI 기반 팀 프로젝트 운영 플랫폼");
    }

    @Test
    @DisplayName("다른 분반 담당 교수는 제출 상세를 조회할 수 없다")
    void getSubmission_RejectsNonOwningProfessor() {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, OTHER_PROFESSOR)).willReturn(false);

        assertThatThrownBy(() -> submissionAdminFacade.getSubmission(submission.getId(), OTHER_PROFESSOR))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("담당 교수는 버전 이력을 조회할 수 있다")
    void getVersions_AllowsOwningProfessor() {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        submissionVersionRepository.save(SubmissionVersion.create(
                submission.getId(), 1, "설명", "변경사항", "202412345", false));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        SubmissionVersionAdminListResponse response = submissionAdminFacade.getVersions(submission.getId(), PROFESSOR);

        assertThat(response.contents()).hasSize(1);
        assertThat(response.contents().get(0).submittedBy().userId()).isEqualTo("202412345");
        assertThat(response.contents().get(0).submittedBy().name()).isEqualTo("팀원학생");
    }

    @Test
    @DisplayName("담당 교수는 버전 상세와 아티팩트를 함께 조회할 수 있다")
    void getVersion_AllowsOwningProfessorAndIncludesArtifacts() {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        SubmissionVersion version = submissionVersionRepository.save(SubmissionVersion.create(
                submission.getId(), 1, "설명", "변경사항", "202412345", false));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        SubmissionVersionAdminDetailResponse response = submissionAdminFacade.getVersion(submission.getId(), version.getVersion(), PROFESSOR);

        assertThat(response.version()).isEqualTo(1);
        assertThat(response.teamId()).isEqualTo(teamId);
        assertThat(response.submittedBy().userId()).isEqualTo("202412345");
        assertThat(response.submittedBy().name()).isEqualTo("팀원학생");
    }

    @Test
    @DisplayName("제출자가 그 뒤 탈퇴해도 제출 이력의 이름은 계속 조회된다")
    void getVersion_KeepsSubmitterNameAfterWithdrawal() {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        SubmissionVersion version = submissionVersionRepository.save(SubmissionVersion.create(
                submission.getId(), 1, "설명", "변경사항", "202412345", false));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        userRepository.findByStudentNumber("202412345").orElseThrow().delete();

        SubmissionVersionAdminDetailResponse response = submissionAdminFacade.getVersion(submission.getId(), version.getVersion(), PROFESSOR);

        assertThat(response.submittedBy().name()).isEqualTo("팀원학생");
    }

    @Test
    @DisplayName("존재하지 않는 버전을 조회하면 예외를 던진다")
    void getVersion_NotFound_ThrowsException() {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        assertThatThrownBy(() -> submissionAdminFacade.getVersion(submission.getId(), 99, PROFESSOR))
                .isInstanceOf(SubmissionVersionNotFoundException.class);
    }

    @Test
    @DisplayName("담당 교수는 최신 버전의 파일 아티팩트만 zip으로 일괄 다운로드할 수 있다(링크·텍스트는 제외)")
    void downloadArtifactsZip_IncludesOnlyFileArtifacts() throws Exception {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        SubmissionVersion version = submissionVersionRepository.save(SubmissionVersion.create(
                submission.getId(), 1, "설명", "변경사항", "202412345", false));
        submission.recordNewVersion(version.getVersion());
        submissionRepository.save(submission);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        FileObject fileObject = fileObjectRepository.save(FileObject.create(
                "202412345", "submissions/key-1", "발표자료.pdf", "application/pdf", 100L, false, null));
        submissionArtifactRepository.saveAll(List.of(
                SubmissionArtifact.file(version.getId(), null, fileObject.getId()),
                SubmissionArtifact.link(version.getId(), null, "https://youtu.be/demo")
        ));

        SubmissionArtifactsZipDownload download = submissionAdminFacade
                .downloadArtifactsZip(submission.getId(), PROFESSOR);

        assertThat(download.fileName()).isEqualTo("A팀-submission.zip");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        download.body().writeTo(buffer);
        try (ZipInputStream zipInputStream = new ZipInputStream(
                new ByteArrayInputStream(buffer.toByteArray()))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            assertThat(entry.getName()).isEqualTo("발표자료.pdf");
            assertThat(zipInputStream.getNextEntry()).isNull();
        }
    }

    @Test
    @DisplayName("경로 이탈 형태의 파일명은 zip 엔트리에서 안전한 이름으로 정리된다")
    void downloadArtifactsZip_SanitizesPathTraversalFileNames() throws Exception {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        SubmissionVersion version = submissionVersionRepository.save(SubmissionVersion.create(
                submission.getId(), 1, "설명", "변경사항", "202412345", false));
        submission.recordNewVersion(version.getVersion());
        submissionRepository.save(submission);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        FileObject unixTraversal = fileObjectRepository.save(FileObject.create(
                "202412345", "submissions/key-1", "../../etc/passwd", "text/plain", 10L, false, null));
        FileObject windowsTraversal = fileObjectRepository.save(FileObject.create(
                "202412345", "submissions/key-2", "..\\..\\evil.pdf", "application/pdf", 10L, false, null));
        submissionArtifactRepository.saveAll(List.of(
                SubmissionArtifact.file(version.getId(), null, unixTraversal.getId()),
                SubmissionArtifact.file(version.getId(), null, windowsTraversal.getId())
        ));

        SubmissionArtifactsZipDownload download = submissionAdminFacade
                .downloadArtifactsZip(submission.getId(), PROFESSOR);

        List<String> entryNames = zipEntryNames(download);
        assertThat(entryNames).containsExactlyInAnyOrder("passwd", "evil.pdf");
        assertThat(entryNames).noneMatch(name -> name.contains("..") || name.contains("/") || name.contains("\\"));
    }

    @Test
    @DisplayName("같은 이름의 파일이 여러 개면 zip 엔트리 이름에 접미사가 붙어 겹치지 않는다")
    void downloadArtifactsZip_DeduplicatesSameFileNames() throws Exception {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        SubmissionVersion version = submissionVersionRepository.save(SubmissionVersion.create(
                submission.getId(), 1, "설명", "변경사항", "202412345", false));
        submission.recordNewVersion(version.getVersion());
        submissionRepository.save(submission);
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        FileObject first = fileObjectRepository.save(FileObject.create(
                "202412345", "submissions/key-1", "report.pdf", "application/pdf", 10L, false, null));
        FileObject second = fileObjectRepository.save(FileObject.create(
                "202412345", "submissions/key-2", "report.pdf", "application/pdf", 20L, false, null));
        submissionArtifactRepository.saveAll(List.of(
                SubmissionArtifact.file(version.getId(), null, first.getId()),
                SubmissionArtifact.file(version.getId(), null, second.getId())
        ));

        SubmissionArtifactsZipDownload download = submissionAdminFacade
                .downloadArtifactsZip(submission.getId(), PROFESSOR);

        assertThat(zipEntryNames(download)).containsExactlyInAnyOrder("report.pdf", "report-2.pdf");
    }

    private List<String> zipEntryNames(SubmissionArtifactsZipDownload download) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        download.body().writeTo(buffer);
        List<String> entryNames = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                entryNames.add(entry.getName());
            }
        }
        return entryNames;
    }

    @Test
    @DisplayName("담당 교수가 아니면 일괄 다운로드할 수 없다")
    void downloadArtifactsZip_RejectsNonOwningProfessor() {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, OTHER_PROFESSOR)).willReturn(false);

        assertThatThrownBy(() -> submissionAdminFacade.downloadArtifactsZip(submission.getId(), OTHER_PROFESSOR))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("중간보고서 마일스톤은 MidReport 상태와 midReportId, 버전을 응답한다")
    void getSubmissionsByMilestone_IncludesMidReportDetails() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(midReportMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        given(meetingRecordQueryService.countMeetingRecords(List.of(teamId), MILESTONE_ID))
                .willReturn(Map.of(teamId, 2L));
        given(meetingRecordQueryService.countMeetingRecords(List.of(teamId)))
                .willReturn(Map.of(teamId, 5L));
        MidReport report = MidReport.builder()
                .id(77L)
                .teamId(teamId)
                .milestoneId(MILESTONE_ID)
                .title("A팀 중간보고서")
                .version(2L)
                .status(MidReportStatus.SUBMITTED)
                .dueDate(LocalDateTime.now().plusDays(1))
                .build();
        given(midReportRepository.findAllByTeamIdInAndMilestoneId(List.of(teamId), MILESTONE_ID))
                .willReturn(List.of(report));

        SubmissionAdminListResponse response = submissionAdminFacade
                .getSubmissionsByMilestone(MILESTONE_ID, null, PROFESSOR);

        assertThat(response.contents()).singleElement().satisfies(item -> {
            assertThat(item.teamId()).isEqualTo(teamId);
            assertThat(item.status()).isEqualTo(SubmissionStatus.SUBMITTED);
            assertThat(item.canSubmitNow()).isFalse();
            assertThat(item.midReportId()).isEqualTo(77L);
            assertThat(item.currentVersion()).isEqualTo(2);
            assertThat(item.meetingRecordCount()).isEqualTo(2L);
            assertThat(item.totalMeetingRecordCount()).isEqualTo(5L);
            assertThat(item.id()).isNotNull().isNotEqualTo(77L);
        });
    }

    @Test
    @DisplayName("중간보고서 마일스톤의 제출 상세는 MidReport 상태와 midReportId를 응답한다")
    void getSubmission_IncludesMidReportDetails() {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(midReportMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        given(meetingRecordQueryService.countMeetingRecords(teamId, MILESTONE_ID)).willReturn(2L);
        given(meetingRecordQueryService.countMeetingRecords(teamId)).willReturn(5L);
        MidReport report = MidReport.builder()
                .id(77L)
                .teamId(teamId)
                .milestoneId(MILESTONE_ID)
                .title("A팀 중간보고서")
                .version(1L)
                .status(MidReportStatus.REVISION_REQUESTED)
                .dueDate(LocalDateTime.now().plusDays(1))
                .build();
        given(midReportRepository.findByTeamIdAndMilestoneId(teamId, MILESTONE_ID))
                .willReturn(Optional.of(report));

        SubmissionAdminResponse response = submissionAdminFacade.getSubmission(submission.getId(), PROFESSOR);

        assertThat(response.id()).isEqualTo(submission.getId());
        assertThat(response.midReportId()).isEqualTo(77L);
        assertThat(response.status()).isEqualTo(SubmissionStatus.REVISION_REQUESTED);
        assertThat(response.canSubmitNow()).isTrue();
        assertThat(response.meetingRecordCount()).isEqualTo(2L);
        assertThat(response.totalMeetingRecordCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("회귀 검증: 중간보고서 마일스톤 목록에서 얻은 submission id로 버전 목록 조회가 정상 연계된다")
    void getSubmissionsByMilestone_IdCanBeUsedForVersionsLookup() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(midReportMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        MidReport report = MidReport.builder()
                .id(77L)
                .teamId(teamId)
                .milestoneId(MILESTONE_ID)
                .title("A팀 중간보고서")
                .version(1L)
                .status(MidReportStatus.SUBMITTED)
                .dueDate(LocalDateTime.now().plusDays(1))
                .build();
        given(midReportRepository.findAllByTeamIdInAndMilestoneId(List.of(teamId), MILESTONE_ID))
                .willReturn(List.of(report));

        SubmissionAdminListResponse listResponse = submissionAdminFacade
                .getSubmissionsByMilestone(MILESTONE_ID, null, PROFESSOR);

        Long submissionId = listResponse.contents().get(0).id();
        assertThat(submissionId).isNotNull();
        assertThat(submissionId).isNotEqualTo(77L);

        // 목록 응답에서 얻은 submissionId를 통해 버전 목록 조회 호출 시 404 없이 정상 연계 검증
        SubmissionVersionAdminListResponse versionsResponse = submissionAdminFacade
                .getVersions(submissionId, PROFESSOR);

        assertThat(versionsResponse).isNotNull();
        assertThat(versionsResponse.contents()).isEmpty();
    }

    @Test
    @DisplayName("재제출한 제안서 목록과 상세는 누적 교수 피드백 수와 피드백 요청 여부를 동일하게 응답한다")
    void proposalFeedbackIsConsistentInListAndDetail() {
        LocalDateTime feedbackAt = LocalDateTime.of(2026, 11, 2, 15, 0);
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(proposalMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        given(projectRepository.findAllByTeamIdIn(List.of(teamId))).willReturn(List.of(Project.builder()
                .id(90L).teamId(teamId).title("제안서").approvalStatus(ApprovalStatus.APPROVED)
                .proposalCompletedAt(feedbackAt.plusMinutes(1)).build()));
        given(documentFeedbackRepository.summarizeFeedback(List.of(teamId), TeamMessageRelatedType.PROPOSAL,
                List.of(90L))).willReturn(Map.of(90L, new DocumentFeedbackSummary(2, feedbackAt, null)));

        SubmissionAdminResponse listed = submissionAdminFacade
                .getSubmissionsByMilestone(MILESTONE_ID, null, PROFESSOR).contents().get(0);
        SubmissionAdminResponse detail = submissionAdminFacade.getSubmission(submission.getId(), PROFESSOR);

        assertThat(listed).isEqualTo(detail);
        assertThat(listed.feedbackCount()).isEqualTo(2);
        assertThat(listed.feedbackRequested()).isTrue();
        assertThat(listed.hasPendingReview()).isFalse();
        assertThat(listed.status()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(listed.canSubmitNow()).isFalse();
    }

    @Test
    @DisplayName("중간보고서 수정요청 중 학생이 답글을 남기면 교수 피드백 요청도 함께 표시한다")
    void midReportStudentReplyRequestsFeedback() {
        LocalDateTime feedbackAt = LocalDateTime.of(2026, 11, 2, 15, 0);
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(midReportMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        MidReport report = MidReport.builder().id(77L).teamId(teamId).milestoneId(MILESTONE_ID)
                .status(MidReportStatus.REVISION_REQUESTED).submittedAt(feedbackAt.minusMinutes(1)).build();
        given(midReportRepository.findAllByTeamIdInAndMilestoneId(List.of(teamId), MILESTONE_ID))
                .willReturn(List.of(report));
        given(midReportRepository.findByTeamIdAndMilestoneId(teamId, MILESTONE_ID)).willReturn(Optional.of(report));
        given(documentFeedbackRepository.summarizeFeedback(List.of(teamId), TeamMessageRelatedType.MID_REPORT,
                List.of(77L))).willReturn(Map.of(77L,
                        new DocumentFeedbackSummary(3, feedbackAt, feedbackAt.plusMinutes(1))));

        SubmissionAdminResponse listed = submissionAdminFacade
                .getSubmissionsByMilestone(MILESTONE_ID, null, PROFESSOR).contents().get(0);
        SubmissionAdminResponse detail = submissionAdminFacade.getSubmission(submission.getId(), PROFESSOR);

        assertThat(listed).isEqualTo(detail);
        assertThat(listed.feedbackCount()).isEqualTo(3);
        assertThat(listed.feedbackRequested()).isTrue();
        assertThat(listed.hasPendingReview()).isTrue();
        assertThat(listed.status()).isEqualTo(SubmissionStatus.REVISION_REQUESTED);
    }

    @Test
    @DisplayName("문서나 피드백이 없는 팀도 목록에 남고 피드백 정보는 0과 false다")
    void proposalWithoutDocumentHasNoFeedbackRequest() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(proposalMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        SubmissionAdminResponse row = submissionAdminFacade
                .getSubmissionsByMilestone(MILESTONE_ID, null, PROFESSOR).contents().get(0);

        assertThat(row.feedbackCount()).isZero();
        assertThat(row.feedbackRequested()).isFalse();
        assertThat(row.hasPendingReview()).isFalse();
        assertThat(row.status()).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
        assertThat(row.canSubmitNow()).isFalse();
    }

    private Milestone milestone() {
        return Milestone.restore(
                MILESTONE_ID, SECTION_ID, "마일스톤", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null));
    }

    @Test
    @DisplayName("제안서 목록의 수정요청 상태는 일반 제출 행이 아닌 제안서에서 읽는다")
    void proposalListUsesProjectRevisionStatus() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(proposalMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        given(projectRepository.findAllByTeamIdIn(List.of(teamId))).willReturn(List.of(Project.builder()
                .id(90L).teamId(teamId).title("제안서")
                .approvalStatus(ApprovalStatus.REVISION_REQUESTED).build()));

        SubmissionAdminResponse row = submissionAdminFacade
                .getSubmissionsByMilestone(MILESTONE_ID, null, PROFESSOR).contents().get(0);

        assertThat(row.status()).isEqualTo(SubmissionStatus.REVISION_REQUESTED);
        assertThat(row.hasPendingReview()).isTrue();
        assertThat(row.feedbackRequested()).isFalse();
    }

    private Milestone proposalMilestone() {
        return Milestone.restore(
                MILESTONE_ID, SECTION_ID, "제안서", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null),
                MilestoneType.PROPOSAL);
    }

    @Test
    @DisplayName("마감 후 재개방된 제안서도 실제 제출 조건을 충족하면 목록과 상세에서 제출 가능하다")
    void reopenedProposalCanBeSubmittedAfterDeadline() {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        Milestone closed = Milestone.restore(MILESTONE_ID, SECTION_ID, "제안서", null, 2,
                MilestoneStatus.PUBLISHED, new MilestoneSchedule(null, past, past, past, null, null),
                MilestoneType.PROPOSAL);
        Project project = Project.builder().id(90L).teamId(teamId).title("제안서").goal("목표")
                .approvalStatus(ApprovalStatus.REVISION_REQUESTED).build();
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(closed));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        given(projectRepository.findAllByTeamIdIn(List.of(teamId))).willReturn(List.of(project));
        var completed = java.util.Arrays.stream(ProposalSectionType.values())
                .map(type -> {
                    var section = ProposalSection.create(90L, type);
                    section.updateCompleted(true);
                    return section;
                }).toList();
        given(proposalSectionRepository.findAllByProjectId(90L)).willReturn(completed);
        given(proposalSectionRepository.findAllByProjectIdIn(List.of(90L))).willReturn(completed);
        given(projectRepository.findTeamIdByProjectId(90L)).willReturn(Optional.of(teamId));
        given(projectRepository.findByIdForUpdate(90L)).willReturn(Optional.of(project));

        SubmissionAdminResponse listed = submissionAdminFacade
                .getSubmissionsByMilestone(MILESTONE_ID, null, PROFESSOR).contents().get(0);
        SubmissionAdminResponse detail = submissionAdminFacade.getSubmission(submission.getId(), PROFESSOR);
        new ProjectCommandService(projectRepository, mock(ProjectApprovalRepository.class), proposalSectionRepository,
                mock(TeamMemberRepository.class)).completeProposal(90L);

        assertThat(project.getProposalCompletedAt()).isNotNull();
        assertThat(listed.canSubmitNow()).isTrue();
        assertThat(detail.canSubmitNow()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"목표,4,false,true", "' ',4,false,false", "목표,3,false,false",
            "목표,0,false,false", "목표,4,true,false"})
    @DisplayName("제안서 제출 가능 여부는 목표·모든 섹션 완료·미제출 조건으로 계산한다")
    void proposalReadinessMatchesCompletionRules(String goal, int completedCount, boolean submitted, boolean expected) {
        Project project = Project.builder().id(90L).teamId(teamId).title("제안서").goal(goal)
                .approvalStatus(ApprovalStatus.DRAFT)
                .proposalCompletedAt(submitted ? LocalDateTime.now() : null).build();
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(proposalMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        given(projectRepository.findAllByTeamIdIn(List.of(teamId))).willReturn(List.of(project));
        var sections = java.util.Arrays.stream(ProposalSectionType.values()).map(type -> ProposalSection.create(90L, type))
                .toList();
        sections.stream().limit(completedCount).forEach(section -> section.updateCompleted(true));
        given(proposalSectionRepository.findAllByProjectIdIn(List.of(90L))).willReturn(sections);

        SubmissionAdminResponse listed = submissionAdminFacade
                .getSubmissionsByMilestone(MILESTONE_ID, null, PROFESSOR).contents().get(0);
        SubmissionAdminResponse detail = submissionAdminFacade.getSubmission(submission.getId(), PROFESSOR);

        assertThat(listed.canSubmitNow()).isEqualTo(expected);
        assertThat(detail.canSubmitNow()).isEqualTo(expected);
    }

    @Test
    @DisplayName("목록은 섹션을 일괄 조회하고 각 팀의 완료 상태를 분리해서 판단한다")
    void proposalReadinessIsBatchedAndSeparatedByProject() {
        Team other = teamRepository.save(Team.builder().sectionId(SECTION_ID).name("B팀").status(Status.CONFIRMED).build());
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(proposalMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        given(projectRepository.findAllByTeamIdIn(org.mockito.ArgumentMatchers.anyList())).willReturn(List.of(
                Project.builder().id(90L).teamId(teamId).title("A제안서").goal("목표").build(),
                Project.builder().id(91L).teamId(other.getId()).title("B제안서").goal("목표").build()));
        var sections = java.util.stream.Stream.of(90L, 91L)
                .flatMap(id -> java.util.Arrays.stream(ProposalSectionType.values()).map(type -> {
                    var section = ProposalSection.create(id, type);
                    section.updateCompleted(id == 90L || type != ProposalSectionType.TEAM_OPERATION);
                    return section;
                })).toList();
        given(proposalSectionRepository.findAllByProjectIdIn(List.of(90L, 91L))).willReturn(sections);

        SubmissionAdminListResponse response = submissionAdminFacade
                .getSubmissionsByMilestone(MILESTONE_ID, null, PROFESSOR);

        assertThat(response.contents()).filteredOn(row -> row.teamId().equals(teamId))
                .singleElement().extracting(SubmissionAdminResponse::canSubmitNow).isEqualTo(true);
        assertThat(response.contents()).filteredOn(row -> row.teamId().equals(other.getId()))
                .singleElement().extracting(SubmissionAdminResponse::canSubmitNow).isEqualTo(false);
        org.mockito.Mockito.verify(proposalSectionRepository).findAllByProjectIdIn(List.of(90L, 91L));
    }

    private Milestone midReportMilestone() {
        return Milestone.restore(
                MILESTONE_ID, SECTION_ID, "중간보고서", null, 8, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null),
                MilestoneType.MID_REPORT);
    }

}
