package submission.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.databind.ObjectMapper;

import kgu.developers.api.submission.application.SubmissionFacade;
import kgu.developers.api.submission.presentation.request.PresentationOrderRequest;
import kgu.developers.api.submission.presentation.request.SubmissionArtifactRequest;
import kgu.developers.api.submission.presentation.request.SubmissionReopenRequest;
import kgu.developers.api.submission.presentation.response.MilestonePresentationsResponse;
import kgu.developers.api.submission.presentation.response.SubmissionMemberConsentResponse;
import kgu.developers.api.submission.presentation.response.SubmissionResponse;
import kgu.developers.api.submission.presentation.response.SubmissionVersionDetailResponse;
import kgu.developers.api.submission.presentation.response.SubmissionVersionListResponse;
import kgu.developers.api.submission.presentation.response.SubmissionVersionSummaryResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.submission.application.command.SubmissionCommandService;
import kgu.developers.domain.submission.application.query.SubmissionQueryService;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionArtifact;
import kgu.developers.domain.submission.domain.SubmissionStatus;
import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.submission.exception.SubmissionLeaderOnlyException;
import kgu.developers.domain.submission.exception.SubmissionMemberConfirmationNotApplicableException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;

import mock.repository.FakeEnrollmentRepository;
import mock.repository.FakeFileObjectRepository;
import mock.repository.FakeFileStorage;
import mock.repository.FakeProjectRepository;
import mock.repository.FakeRequiredArtifactRepository;
import mock.repository.FakeSubmissionArtifactRepository;
import mock.repository.FakeSubmissionMemberConfirmationRepository;
import mock.repository.FakeSubmissionRepository;
import mock.repository.FakeSubmissionVersionRepository;
import mock.repository.FakeTeamMemberRepository;
import mock.repository.FakeTeamRepository;
import mock.repository.FakeUserRepository;

class SubmissionFacadeTest {

    private static final Long SECTION_ID = 1L;
    private static final Long TEAM_ID = 10L;
    private static final Long MILESTONE_ID = 5L;
    private static final String LEADER = "202688888";
    private static final String MEMBER = "202699999";
    private static final String NON_MEMBER = "202600000";
    private static final String PROFESSOR = "professor1";

    private MilestoneRepository milestoneRepository;
    private SectionQueryService sectionQueryService;
    private FakeTeamMemberRepository teamMemberRepository;
    private FakeSubmissionRepository submissionRepository;
    private FakeTeamRepository teamRepository;
    private FakeProjectRepository projectRepository;
    private FakeFileObjectRepository fileObjectRepository;
    private FakeFileStorage fileStorage;
    private FakeSubmissionVersionRepository submissionVersionRepository;
    private FakeSubmissionArtifactRepository submissionArtifactRepository;
    private FakeUserRepository userRepository;
    private SubmissionFacade submissionFacade;

    @BeforeEach
    void setUp() {
        milestoneRepository = mock(MilestoneRepository.class);
        sectionQueryService = mock(SectionQueryService.class);

        teamMemberRepository = new FakeTeamMemberRepository();
        teamMemberRepository.save(TeamMember.create(TEAM_ID, LEADER, true, "팀장"));
        teamMemberRepository.save(TeamMember.create(TEAM_ID, MEMBER, false, "백엔드"));
        teamMemberRepository.assignTeamToSection(TEAM_ID, SECTION_ID);

        submissionRepository = new FakeSubmissionRepository();
        submissionVersionRepository = new FakeSubmissionVersionRepository();
        submissionArtifactRepository = new FakeSubmissionArtifactRepository();
        FakeSubmissionMemberConfirmationRepository submissionMemberConfirmationRepository =
                new FakeSubmissionMemberConfirmationRepository();
        fileObjectRepository = new FakeFileObjectRepository();
        fileStorage = new FakeFileStorage();
        FakeEnrollmentRepository enrollmentRepository = new FakeEnrollmentRepository();
        enrollmentRepository.save(Enrollment.create(SECTION_ID, LEADER, Role.STUDENT, Status.ACTIVE));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, MEMBER, Role.STUDENT, Status.ACTIVE));
        userRepository = new FakeUserRepository();
        userRepository.save(User.create(LEADER, "leader@kyonggi.ac.kr", "팀장학생", "pw", UserGlobalRole.USER, "010-0000-0001"));
        userRepository.save(User.create(MEMBER, "member@kyonggi.ac.kr", "팀원학생", "pw", UserGlobalRole.USER, "010-0000-0002"));
        UserQueryService userQueryService = new UserQueryService(userRepository, enrollmentRepository);
        FakeRequiredArtifactRepository requiredArtifactRepository = new FakeRequiredArtifactRepository();
        teamRepository = new FakeTeamRepository();
        projectRepository = new FakeProjectRepository();

        SubmissionQueryService submissionQueryService =
                new SubmissionQueryService(submissionRepository, milestoneRepository, mock(org.springframework.transaction.PlatformTransactionManager.class));
        SubmissionCommandService submissionCommandService = new SubmissionCommandService(
                submissionRepository,
                submissionVersionRepository,
                submissionArtifactRepository,
                submissionMemberConfirmationRepository,
                fileObjectRepository,
                fileStorage,
                milestoneRepository,
                teamMemberRepository,
                enrollmentRepository,
                requiredArtifactRepository,
                teamRepository,
                submissionQueryService
        );

        submissionFacade = new SubmissionFacade(
                submissionCommandService,
                submissionQueryService,
                submissionVersionRepository,
                submissionArtifactRepository,
                submissionMemberConfirmationRepository,
                teamRepository,
                projectRepository,
                fileObjectRepository,
                fileStorage,
                milestoneRepository,
                teamMemberRepository,
                enrollmentRepository,
                sectionQueryService,
                userQueryService
        );
    }

    @Test
    @DisplayName("처음 조회하는 팀원은 not_submitted 상태의 제출을 자동으로 받는다")
    void getMyTeamSubmission_CreatesLazilyForFirstView() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        SubmissionResponse response = submissionFacade.getMyTeamSubmission(MILESTONE_ID, MEMBER);

        assertThat(response.status()).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
        assertThat(response.teamId()).isEqualTo(TEAM_ID);
    }

    @Test
    @DisplayName("그 분반에 팀이 없는 사용자는 우리팀 제출 조회를 할 수 없다")
    void getMyTeamSubmission_RejectsNonSectionMember() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.getMyTeamSubmission(MILESTONE_ID, NON_MEMBER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("다른 팀 소속은 제출 상세를 조회할 수 없다")
    void getSubmission_RejectsNonTeamMember() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.getSubmission(submission.getId(), NON_MEMBER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("팀원은 제출 상세를 조회할 수 있다")
    void getSubmission_AllowsTeamMember() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        SubmissionResponse response = submissionFacade.getSubmission(submission.getId(), MEMBER);

        assertThat(response.id()).isEqualTo(submission.getId());
    }

    @Test
    @DisplayName("탈퇴한 사용자는 팀원 행이 남아있어도 비공개 제출 상세를 조회할 수 없다")
    void getSubmission_RejectsWithdrawnEnrollment() {
        teamMemberRepository.save(TeamMember.create(TEAM_ID, "202677776", false, "탈퇴예정"));
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.getSubmission(submission.getId(), "202677776"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("제출하면 currentVersion이 1로 올라간 응답을 받는다")
    void submitVersion_ReturnsUpdatedSubmission() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        SubmissionResponse response = submissionFacade.submitVersion(
                submission.getId(), MEMBER, "1차 제출", null, List.of(), List.of(), List.of());

        assertThat(response.currentVersion()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(SubmissionStatus.SUBMITTED);
    }

    @Test
    @DisplayName("최종보고서는 팀장이 아니면 제출할 수 없다")
    void submitVersion_RejectsNonLeaderForFinalReport() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));

        assertThatThrownBy(() -> submissionFacade.submitVersion(
                submission.getId(), MEMBER, "1차 제출", null, List.of(), List.of(), List.of()))
                .isInstanceOf(SubmissionLeaderOnlyException.class);
    }

    @Test
    @DisplayName("최종보고서는 팀장이면 제출할 수 있다")
    void submitVersion_AllowsLeaderForFinalReport() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));

        SubmissionResponse response = submissionFacade.submitVersion(
                submission.getId(), LEADER, "1차 제출", null, List.of(), List.of(), List.of());

        assertThat(response.currentVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("최종보고서를 팀장이 제출하면 그 즉시 팀장 본인 확인이 1건 자동 등록된다")
    void submitVersion_FinalReport_AutoConfirmsLeader() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));

        SubmissionResponse response = submissionFacade.submitVersion(
                submission.getId(), LEADER, "1차 제출", null, List.of(), List.of(), List.of());

        assertThat(response.memberConsent()).isNotNull();
        assertThat(response.memberConsent().confirmedCount()).isEqualTo(1);
        assertThat(response.memberConsent().totalCount()).isEqualTo(2);
        assertThat(response.memberConsent().isConfirmedByMe()).isTrue();
    }

    @Test
    @DisplayName("일반 마일스톤 제출물 응답의 memberConsent는 null이다")
    void submitVersion_GeneralMilestone_MemberConsentIsNull() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        SubmissionResponse response = submissionFacade.submitVersion(
                submission.getId(), MEMBER, "1차 제출", null, List.of(), List.of(), List.of());

        assertThat(response.memberConsent()).isNull();
    }

    @Test
    @DisplayName("아티팩트 종류(type)가 없으면 NPE 대신 400으로 거부된다")
    void submitVersion_RejectsMissingArtifactType() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        SubmissionArtifactRequest missingType = new SubmissionArtifactRequest(null, null, null, null);

        assertThatThrownBy(() -> submissionFacade.submitVersion(
                submission.getId(), MEMBER, "1차 제출", null, List.of(missingType), List.of(), List.of()))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionArtifactTypeRequiredException.class);
    }

    @Test
    @DisplayName("아티팩트 배열 원소 자체가 null이면 NPE 대신 400으로 거부된다")
    void submitVersion_RejectsNullArtifactElement() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        List<SubmissionArtifactRequest> artifactsWithNullElement = new java.util.ArrayList<>();
        artifactsWithNullElement.add(null);

        assertThatThrownBy(() -> submissionFacade.submitVersion(
                submission.getId(), MEMBER, "1차 제출", null, artifactsWithNullElement, List.of(), List.of()))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionArtifactTypeRequiredException.class);
    }

    @Test
    @DisplayName("탈퇴한 사용자는 팀원 행이 남아있어도 제출할 수 없다")
    void submitVersion_RejectsWithdrawnEnrollment() {
        teamMemberRepository.save(TeamMember.create(TEAM_ID, "202677777", false, "탈퇴예정"));
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.submitVersion(
                submission.getId(), "202677777", "탈퇴 후 제출 시도", null, List.of(), List.of(), List.of()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("팀장이 아니면 완료 처리를 할 수 없다")
    void completeSubmission_RejectsNonLeader() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.completeSubmission(submission.getId(), MEMBER))
                .isInstanceOf(SubmissionLeaderOnlyException.class);
    }

    @Test
    @DisplayName("일반 마일스톤은 이미 제출한 팀장이면 팀원 확인 없이도 완료 게이트를 통과한다")
    void completeSubmission_AllowsLeaderForGeneralMilestone() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        submissionFacade.submitVersion(submission.getId(), LEADER, "제출", null, List.of(), List.of(), List.of());

        SubmissionResponse response = submissionFacade.completeSubmission(submission.getId(), LEADER);

        assertThat(response.status()).isEqualTo(SubmissionStatus.COMPLETED);
    }

    @Test
    @DisplayName("탈퇴했거나 조교로 전환된 기존 팀장은 완료 처리를 할 수 없다")
    void completeSubmission_RejectsLeaderWithoutActiveStudentEnrollment() {
        String withdrawnLeader = "202677778";
        teamMemberRepository.save(TeamMember.create(TEAM_ID, withdrawnLeader, true, "탈퇴한 팀장"));
        // 활성 STUDENT enrollment를 일부러 안 심어둠(탈퇴했거나 조교로 전환된 상황을 흉내).
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.completeSubmission(submission.getId(), withdrawnLeader))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("아직 제출한 적 없으면 팀장이어도 완료 처리할 수 없다")
    void completeSubmission_RejectsWhenNotYetSubmitted() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.completeSubmission(submission.getId(), LEADER))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionNotYetSubmittedException.class);
    }

    @Test
    @DisplayName("담당 교수가 아니면 재오픈할 수 없다")
    void reopenSubmission_RejectsNonOwningProfessor() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(false);

        assertThatThrownBy(() -> submissionFacade.reopenSubmission(
                submission.getId(), PROFESSOR, new SubmissionReopenRequest(LocalDateTime.now().plusDays(1))))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionAccessDeniedException.class);
    }

    @Test
    @DisplayName("완료된 제출은 담당 교수가 재오픈할 수 있다")
    void reopenSubmission_AllowsOwningProfessor() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        submission.recordNewVersion(1);
        submission.complete(LEADER);
        submissionRepository.save(submission);
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        SubmissionResponse response = submissionFacade.reopenSubmission(
                submission.getId(), PROFESSOR, new SubmissionReopenRequest(LocalDateTime.now().plusDays(1)));

        assertThat(response.status()).isEqualTo(SubmissionStatus.REVISION_REQUESTED);
        // 재오픈 후엔 "미완료면 null"이라는 응답 계약대로 완료 이력이 지워져야 한다.
        assertThat(response.completedAt()).isNull();
        assertThat(response.completedBy()).isNull();
    }

    @Test
    @DisplayName("완료되지 않은 제출은 담당 교수여도 재오픈할 수 없다")
    void reopenSubmission_RejectsWhenNotCompletedEvenForOwningProfessor() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        assertThatThrownBy(() -> submissionFacade.reopenSubmission(
                submission.getId(), PROFESSOR, new SubmissionReopenRequest(LocalDateTime.now().plusDays(1))))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionNotCompletedException.class);
    }

    @Test
    @DisplayName("담당 교수는 발표순서를 일괄 지정할 수 있고, 지정된 순서대로 목록이 조회된다")
    void assignPresentationOrder_ThenListedInOrder() {
        teamRepository.save(Team.builder().id(TEAM_ID).sectionId(SECTION_ID).name("우리팀").build());
        teamRepository.save(Team.builder().id(20L).sectionId(SECTION_ID).name("다른팀").build());
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        submissionFacade.assignPresentationOrder(MILESTONE_ID, PROFESSOR, new PresentationOrderRequest(List.of(
                new PresentationOrderRequest.TeamOrder(TEAM_ID, 2),
                new PresentationOrderRequest.TeamOrder(20L, 1)
        )));

        MilestonePresentationsResponse response = submissionFacade.getMilestonePresentations(MILESTONE_ID, MEMBER);
        assertThat(response.contents()).hasSize(2);
        assertThat(response.contents().get(0).teamId()).isEqualTo(20L);
        assertThat(response.contents().get(0).teamName()).isEqualTo("다른팀");
        assertThat(response.contents().get(1).teamId()).isEqualTo(TEAM_ID);
        assertThat(response.contents().get(1).teamName()).isEqualTo("우리팀");
    }

    @Test
    @DisplayName("발표자료 조회 시 해당 팀의 제안서 정보와 제출된 산출물(PDF, 영상 링크)이 함께 반환된다")
    void getMilestonePresentations_IncludesProjectAndArtifacts() throws Exception {
        teamRepository.save(Team.builder().id(TEAM_ID).sectionId(SECTION_ID).name("캡스톤1조").build());
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));

        // 팀원이 업로드한 화면 캡처 이미지
        FileObject screenImage = fileObjectRepository.save(
                FileObject.create(MEMBER, "screen-key-1", "screen1.png", "image/png", 2048L, false, null));

        ObjectMapper om = new ObjectMapper();
        var screensNode = om.readTree("""
                [{"title":"메인 화면","description":"로그인 후 첫 화면","imageFileId":%d}]
                """.formatted(screenImage.getId()));
        var featuresNode = om.readTree("""
                [{"title":"AI 요약","description":"회의록 요약 기능"}]
                """);
        var flowNode = om.readTree("""
                [{"number":1,"title":"로그인"}]
                """);

        projectRepository.save(Project.create(
                TEAM_ID,
                "AI 협업 플랫폼",
                "팀 프로젝트 관리 및 AI 보조 도구",
                "개발 생산성 30% 향상",
                "https://github.com/test/repo",
                null,
                ApprovalStatus.APPROVED,
                "주 1회 대면 회의",
                null,
                "로그 데이터",
                screensNode,
                featuresNode,
                flowNode
        ));

        // 발표 마일스톤 제출물 및 산출물 (PDF 파일 + YouTube 시연영상 링크)
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        FileObject pdfFile = fileObjectRepository.save(
                FileObject.create(LEADER, "pdf-key", "발표자료.pdf", "application/pdf", 1048576L, false, null));
        SubmissionVersion version = submissionVersionRepository.save(
                SubmissionVersion.create(submission.getId(), 1, "최종 발표자료", null, LEADER, false));
        submission.recordNewVersion(1);
        submissionRepository.save(submission);

        submissionArtifactRepository.saveAll(List.of(
                SubmissionArtifact.file(version.getId(), null, pdfFile.getId()),
                SubmissionArtifact.link(version.getId(), null, "https://youtube.com/watch?v=demo123")
        ));

        MilestonePresentationsResponse response = submissionFacade.getMilestonePresentations(MILESTONE_ID, MEMBER);

        assertThat(response.contents()).hasSize(1);
        var presentation = response.contents().get(0);
        assertThat(presentation.teamId()).isEqualTo(TEAM_ID);
        assertThat(presentation.teamName()).isEqualTo("캡스톤1조");
        assertThat(presentation.submissionId()).isEqualTo(submission.getId());

        // 제안서 검증
        assertThat(presentation.project()).isNotNull();
        assertThat(presentation.project().title()).isEqualTo("AI 협업 플랫폼");
        assertThat(presentation.project().description()).isEqualTo("팀 프로젝트 관리 및 AI 보조 도구");
        assertThat(presentation.project().goal()).isEqualTo("개발 생산성 30% 향상");
        // 화면 이미지 presigned url 보강 확인
        var resolvedScreens = presentation.project().screenConfiguration();
        assertThat(resolvedScreens.get(0).get("imageUrl").asText()).isEqualTo("https://fake-storage.local/screen-key-1");

        // 산출물 검증
        assertThat(presentation.artifacts()).hasSize(2);
        var fileArtifact = presentation.artifacts().stream()
                .filter(a -> a.type() == kgu.developers.domain.submission.domain.ArtifactType.FILE).findFirst().orElseThrow();
        assertThat(fileArtifact.fileName()).isEqualTo("발표자료.pdf");
        assertThat(fileArtifact.downloadUrl()).isEqualTo("https://fake-storage.local/pdf-key");

        var linkArtifact = presentation.artifacts().stream()
                .filter(a -> a.type() == kgu.developers.domain.submission.domain.ArtifactType.LINK).findFirst().orElseThrow();
        assertThat(linkArtifact.url()).isEqualTo("https://youtube.com/watch?v=demo123");
    }

    @Test
    @DisplayName("제안서가 없거나 제출 이력이 없어도 오류 없이 안전하게 조회된다")
    void getMilestonePresentations_WhenProjectOrVersionAbsent() {
        teamRepository.save(Team.builder().id(TEAM_ID).sectionId(SECTION_ID).name("캡스톤1조").build());
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));

        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));

        MilestonePresentationsResponse response = submissionFacade.getMilestonePresentations(MILESTONE_ID, MEMBER);

        assertThat(response.contents()).hasSize(1);
        var presentation = response.contents().get(0);
        assertThat(presentation.teamId()).isEqualTo(TEAM_ID);
        assertThat(presentation.teamName()).isEqualTo("캡스톤1조");
        assertThat(presentation.project()).isNull();
        assertThat(presentation.artifacts()).isEmpty();
    }

    @Test
    @DisplayName("분반의 일부 팀이 빠지면 발표순서 지정이 거부된다")
    void assignPresentationOrder_RejectsWhenTeamMissing() {
        teamRepository.save(Team.builder().id(TEAM_ID).sectionId(SECTION_ID).name("우리팀").build());
        teamRepository.save(Team.builder().id(20L).sectionId(SECTION_ID).name("다른팀").build());
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        assertThatThrownBy(() -> submissionFacade.assignPresentationOrder(
                MILESTONE_ID, PROFESSOR, new PresentationOrderRequest(List.of(
                        new PresentationOrderRequest.TeamOrder(TEAM_ID, 1)))))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionInvalidPresentationOrderException.class);
    }

    @Test
    @DisplayName("발표순서 지정 요청에 팀이 중복되면 거부된다")
    void assignPresentationOrder_RejectsDuplicateTeam() {
        teamRepository.save(Team.builder().id(TEAM_ID).sectionId(SECTION_ID).name("우리팀").build());
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        assertThatThrownBy(() -> submissionFacade.assignPresentationOrder(
                MILESTONE_ID, PROFESSOR, new PresentationOrderRequest(List.of(
                        new PresentationOrderRequest.TeamOrder(TEAM_ID, 1),
                        new PresentationOrderRequest.TeamOrder(TEAM_ID, 2)))))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionInvalidPresentationOrderException.class);
    }

    @Test
    @DisplayName("완료된 제출은 공식 기간이 남아있어도 재오픈 전에는 재제출할 수 없다")
    void submitVersion_RejectsWhenAlreadyCompleted() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        submissionFacade.submitVersion(submission.getId(), LEADER, "1차 제출", null, List.of(), List.of(), List.of());
        submissionFacade.completeSubmission(submission.getId(), LEADER);

        assertThatThrownBy(() -> submissionFacade.submitVersion(
                submission.getId(), MEMBER, "완료 후 재제출 시도", null, List.of(), List.of(), List.of()))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionNotAllowedNowException.class);
    }

    @Test
    @DisplayName("탈퇴한 사용자는 팀원 행이 남아있어도 우리팀 제출 조회·생성을 할 수 없다")
    void getMyTeamSubmission_RejectsWithdrawnEnrollment() {
        teamMemberRepository.save(TeamMember.create(TEAM_ID, "202677775", false, "탈퇴예정"));
        teamMemberRepository.assignTeamToSection(TEAM_ID, SECTION_ID);
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.getMyTeamSubmission(MILESTONE_ID, "202677775"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("아무도 확인하지 않았으면 확인 인원 0명, 본인 확인 여부는 false로 조회된다")
    void getMemberConsent_ReportsZeroBeforeAnyoneConfirms() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));

        SubmissionMemberConsentResponse response = submissionFacade.getMemberConsent(submission.getId(), MEMBER);

        assertThat(response.confirmedCount()).isZero();
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.isConfirmedByMe()).isFalse();
    }

    @Test
    @DisplayName("확인을 등록하면 확인 인원과 본인 확인 여부가 바로 반영된다")
    void confirmAsMember_ReflectsInConsentImmediately() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));

        SubmissionMemberConsentResponse response = submissionFacade.confirmAsMember(submission.getId(), MEMBER);

        assertThat(response.confirmedCount()).isEqualTo(1);
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.isConfirmedByMe()).isTrue();
    }

    @Test
    @DisplayName("확인을 취소하면 확인 인원과 본인 확인 여부가 다시 줄어든다")
    void cancelConfirmation_ReflectsInConsentImmediately() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));
        submissionFacade.confirmAsMember(submission.getId(), MEMBER);

        SubmissionMemberConsentResponse response = submissionFacade.cancelConfirmation(submission.getId(), MEMBER);

        assertThat(response.confirmedCount()).isZero();
        assertThat(response.isConfirmedByMe()).isFalse();
    }

    @Test
    @DisplayName("최종보고서가 아닌 마일스톤에서는 확인 조회·등록·취소가 전부 거부된다")
    void memberConfirmationApis_RejectNonFinalReportMilestone() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.getMemberConsent(submission.getId(), MEMBER))
                .isInstanceOf(SubmissionMemberConfirmationNotApplicableException.class);
        assertThatThrownBy(() -> submissionFacade.confirmAsMember(submission.getId(), MEMBER))
                .isInstanceOf(SubmissionMemberConfirmationNotApplicableException.class);
        assertThatThrownBy(() -> submissionFacade.cancelConfirmation(submission.getId(), MEMBER))
                .isInstanceOf(SubmissionMemberConfirmationNotApplicableException.class);
    }

    @Test
    @DisplayName("getVersion은 제출자 이름과 파일 아티팩트의 크기·MIME 타입·다운로드 URL을 함께 내려준다")
    void getVersion_IncludesSubmitterNameAndFileMetadata() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        FileObject file = fileObjectRepository.save(
                FileObject.create(MEMBER, "report-key", "report.pdf", "application/pdf", 2048L, false, null));
        SubmissionVersion version = submissionVersionRepository.save(
                SubmissionVersion.create(submission.getId(), 1, "1차 제출", null, MEMBER, false));
        submissionArtifactRepository.saveAll(
                List.of(SubmissionArtifact.file(version.getId(), null, file.getId())));

        SubmissionVersionDetailResponse response = submissionFacade.getVersion(submission.getId(), 1, MEMBER);

        assertThat(response.id()).isEqualTo(version.getId());
        assertThat(response.submittedBy().userId()).isEqualTo(MEMBER);
        assertThat(response.submittedBy().name()).isEqualTo("팀원학생");
        assertThat(response.updatedAt()).isNotNull();
        assertThat(response.artifacts()).hasSize(1);
        assertThat(response.artifacts().get(0).size()).isEqualTo(2048L);
        assertThat(response.artifacts().get(0).mimeType()).isEqualTo("application/pdf");
        assertThat(response.artifacts().get(0).downloadUrl()).isEqualTo("https://fake-storage.local/report-key");
    }

    @Test
    @DisplayName("getVersions는 이력 각 버전마다 아티팩트와 제출자 이름을 함께 내려준다")
    void getVersions_IncludesArtifactsAndSubmitterForEachVersion() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        FileObject file = fileObjectRepository.save(
                FileObject.create(LEADER, "v1-key", "v1.zip", "application/zip", 4096L, false, null));
        SubmissionVersion version1 = submissionVersionRepository.save(
                SubmissionVersion.create(submission.getId(), 1, "1차 제출", null, LEADER, false));
        submissionArtifactRepository.saveAll(
                List.of(SubmissionArtifact.file(version1.getId(), null, file.getId())));
        submissionVersionRepository.save(
                SubmissionVersion.create(submission.getId(), 2, "2차 제출", "버그 수정", MEMBER, false));

        SubmissionVersionListResponse response = submissionFacade.getVersions(submission.getId(), MEMBER);

        assertThat(response.contents()).hasSize(2);
        SubmissionVersionSummaryResponse first = response.contents().stream()
                .filter(v -> v.version() == 1).findFirst().orElseThrow();
        assertThat(first.submittedBy().name()).isEqualTo("팀장학생");
        assertThat(first.artifacts()).hasSize(1);
        SubmissionVersionSummaryResponse second = response.contents().stream()
                .filter(v -> v.version() == 2).findFirst().orElseThrow();
        assertThat(second.submittedBy().name()).isEqualTo("팀원학생");
        assertThat(second.artifacts()).isEmpty();
    }

    @Test
    @DisplayName("제출자가 그 뒤 탈퇴해도 제출 이력의 이름은 계속 조회된다")
    void getVersion_KeepsSubmitterNameAfterWithdrawal() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        submissionVersionRepository.save(
                SubmissionVersion.create(submission.getId(), 1, "1차 제출", null, MEMBER, false));
        userRepository.findByStudentNumber(MEMBER).orElseThrow().delete();

        SubmissionVersionDetailResponse response = submissionFacade.getVersion(submission.getId(), 1, LEADER);

        assertThat(response.submittedBy().name()).isEqualTo("팀원학생");
    }

    private Milestone milestone() {
        return Milestone.restore(
                MILESTONE_ID, SECTION_ID, "마일스톤", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null));
    }

    private Milestone presentationMilestone() {
        return Milestone.restore(
                MILESTONE_ID, SECTION_ID, "발표", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null),
                MilestoneType.PRESENTATION);
    }

    private Milestone finalReportMilestone() {
        return Milestone.restore(
                MILESTONE_ID, SECTION_ID, "최종보고서", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null),
                MilestoneType.FINAL_REPORT);
    }
}
