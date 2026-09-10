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
                meetingRecordQueryService
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
    @DisplayName("팀별 제출 현황은 해당 마일스톤과 연결된 회의록 수를 함께 응답한다")
    void getSubmissionsByMilestone_IncludesMeetingRecordCount() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        given(meetingRecordQueryService.countMeetingRecords(List.of(teamId), MILESTONE_ID))
                .willReturn(Map.of(teamId, 2L));

        SubmissionAdminListResponse response = submissionAdminFacade
                .getSubmissionsByMilestone(MILESTONE_ID, null, PROFESSOR);

        assertThat(response.contents()).singleElement()
                .extracting(SubmissionAdminResponse::meetingRecordCount)
                .isEqualTo(2L);
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
        given(projectRepository.findAllByTeamIdIn(List.of(teamId))).willReturn(List.of(Project.create(
                teamId, "AI 기반 팀 프로젝트 운영 플랫폼", "설명", "목표", null, null,
                ApprovalStatus.APPROVED, null, JsonNodeFactory.instance.arrayNode(),
                JsonNodeFactory.instance.arrayNode(), null)));

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

        SubmissionAdminResponse response = submissionAdminFacade.getSubmission(submission.getId(), PROFESSOR);

        assertThat(response.id()).isEqualTo(submission.getId());
        assertThat(response.teamName()).isEqualTo("A팀");
    }

    @Test
    @DisplayName("제안서 제출 상세에도 팀 프로젝트 주제를 함께 응답한다")
    void getSubmission_IncludesProjectTitleForProposal() {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(proposalMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);
        given(projectRepository.findAllByTeamIdIn(List.of(teamId))).willReturn(List.of(Project.create(
                teamId, "AI 기반 팀 프로젝트 운영 플랫폼", "설명", "목표", null, null,
                ApprovalStatus.APPROVED, null, JsonNodeFactory.instance.arrayNode(),
                JsonNodeFactory.instance.arrayNode(), null)));

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

    private Milestone milestone() {
        return Milestone.restore(
                MILESTONE_ID, SECTION_ID, "마일스톤", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null));
    }

    private Milestone proposalMilestone() {
        return Milestone.restore(
                MILESTONE_ID, SECTION_ID, "제안서", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null),
                MilestoneType.PROPOSAL);
    }
}
