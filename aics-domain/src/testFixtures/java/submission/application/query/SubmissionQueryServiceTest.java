package submission.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.submission.application.query.SubmissionQueryService;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionStatus;

import mock.repository.FakeSubmissionRepository;

@ExtendWith(MockitoExtension.class)
class SubmissionQueryServiceTest {

    private static final Long TEAM_ID = 10L;
    private static final Long SECTION_ID = 1L;

    @Mock
    private MilestoneRepository milestoneRepository;

    private FakeSubmissionRepository submissionRepository;
    private SubmissionQueryService submissionQueryService;

    @BeforeEach
    void setUp() {
        submissionRepository = new FakeSubmissionRepository();
        submissionQueryService = new SubmissionQueryService(submissionRepository, milestoneRepository);
    }

    @Test
    @DisplayName("처음 조회하는 팀은 not_submitted 상태의 제출이 자동으로 만들어진다")
    void getOrCreateSubmission_CreatesWhenAbsent() {
        Submission submission = submissionQueryService.getOrCreateSubmission(TEAM_ID, 5L);

        assertThat(submission.getId()).isNotNull();
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
        assertThat(submission.getCurrentVersion()).isZero();
    }

    @Test
    @DisplayName("이미 만들어진 제출이 있으면 그대로 반환하고 새로 만들지 않는다")
    void getOrCreateSubmission_ReturnsExistingOne() {
        Submission first = submissionQueryService.getOrCreateSubmission(TEAM_ID, 5L);
        Submission second = submissionQueryService.getOrCreateSubmission(TEAM_ID, 5L);

        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @Test
    @DisplayName("마감 전이면 제출할 수 있다")
    void canSubmitNow_TrueBeforeDueDate() {
        Milestone milestone = milestone(5L, 2, schedule(LocalDateTime.now().plusDays(1), null, null));
        given(milestoneRepository.findById(5L)).willReturn(Optional.of(milestone));
        Submission submission = submission(5L);

        assertThat(submissionQueryService.canSubmitNow(submission)).isTrue();
    }

    @Test
    @DisplayName("마감·지각제출기간·수정기간을 전부 지나면 제출할 수 없다")
    void canSubmitNow_FalseAfterAllWindowsClosed() {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        Milestone milestone = milestone(5L, 2, schedule(past, past, past));
        given(milestoneRepository.findById(5L)).willReturn(Optional.of(milestone));
        Submission submission = submission(5L);

        assertThat(submissionQueryService.canSubmitNow(submission)).isFalse();
    }

    @Test
    @DisplayName("opensAt 전이면 공개된 마일스톤이라도 제출할 수 없다")
    void canSubmitNow_FalseBeforeOpensAt() {
        LocalDateTime opensAt = LocalDateTime.now().plusDays(1);
        LocalDateTime dueAt = LocalDateTime.now().plusDays(7);
        Milestone milestone = milestone(5L, 2, new MilestoneSchedule(opensAt, dueAt, null, null, null, null));
        given(milestoneRepository.findById(5L)).willReturn(Optional.of(milestone));
        Submission submission = submission(5L);

        assertThat(submissionQueryService.canSubmitNow(submission)).isFalse();
    }

    @Test
    @DisplayName("DRAFT 상태 마일스톤은 기간이 열려 있어도 제출할 수 없다")
    void canSubmitNow_FalseWhenMilestoneIsDraft() {
        Milestone draftMilestone = Milestone.restore(
                5L, SECTION_ID, "마일스톤", null, 2, MilestoneStatus.DRAFT,
                schedule(LocalDateTime.now().plusDays(1), null, null));
        given(milestoneRepository.findById(5L)).willReturn(Optional.of(draftMilestone));
        Submission submission = submission(5L);

        assertThat(submissionQueryService.canSubmitNow(submission)).isFalse();
    }

    @Test
    @DisplayName("지각제출기간이 남아있으면 마감이 지나도 제출할 수 있다")
    void canSubmitNow_TrueWithinLateSubmissionWindow() {
        LocalDateTime due = LocalDateTime.now().minusHours(1);
        LocalDateTime lateUntil = LocalDateTime.now().plusDays(1);
        Milestone milestone = milestone(5L, 2, schedule(due, lateUntil, null));
        given(milestoneRepository.findById(5L)).willReturn(Optional.of(milestone));
        Submission submission = submission(5L);

        assertThat(submissionQueryService.canSubmitNow(submission)).isTrue();
    }

    @Test
    @DisplayName("아직 공식 오픈 전이어도, 직전 마일스톤을 이미 제출했으면 다음 마일스톤은 조기 오픈된다")
    void canSubmitNow_TrueWhenPreviousMilestoneAlreadySubmitted() {
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        LocalDateTime opensAt = LocalDateTime.now().plusHours(1);
        LocalDateTime dueAt = LocalDateTime.now().plusDays(7);

        Milestone previousMilestone = milestone(4L, 1, schedule(future, null, null));
        Milestone currentMilestone = milestone(5L, 2, new MilestoneSchedule(opensAt, dueAt, null, null, null, null));
        given(milestoneRepository.findById(5L)).willReturn(Optional.of(currentMilestone));
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID))
                .willReturn(List.of(previousMilestone, currentMilestone));

        // 직전 마일스톤(4L)은 이미 제출을 끝낸 상태로 만들어둔다
        Submission previousSubmission = submissionRepository.save(Submission.create(TEAM_ID, 4L));
        previousSubmission.recordNewVersion(1);
        submissionRepository.save(previousSubmission);

        Submission currentSubmission = submission(5L);

        assertThat(submissionQueryService.canSubmitNow(currentSubmission)).isTrue();
    }

    @Test
    @DisplayName("공식 오픈 시각이 이미 지났으면, 직전 마일스톤을 제출했어도 더 이상 '조기'오픈은 적용되지 않는다")
    void canSubmitNow_FalseAfterOpensAtEvenIfPreviousSubmitted() {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        Milestone currentMilestone = milestone(5L, 2, schedule(past, past, past));
        given(milestoneRepository.findById(5L)).willReturn(Optional.of(currentMilestone));

        Submission previousSubmission = submissionRepository.save(Submission.create(TEAM_ID, 4L));
        previousSubmission.recordNewVersion(1);
        submissionRepository.save(previousSubmission);

        Submission currentSubmission = submission(5L);

        assertThat(submissionQueryService.canSubmitNow(currentSubmission)).isFalse();
    }

    @Test
    @DisplayName("직전 마일스톤을 아직 제출 안 했으면 조기 오픈되지 않는다")
    void canSubmitNow_FalseWhenPreviousMilestoneNotSubmittedYet() {
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        LocalDateTime opensAt = LocalDateTime.now().plusHours(1);
        LocalDateTime dueAt = LocalDateTime.now().plusDays(7);

        Milestone previousMilestone = milestone(4L, 1, schedule(future, null, null));
        Milestone currentMilestone = milestone(5L, 2, new MilestoneSchedule(opensAt, dueAt, null, null, null, null));
        given(milestoneRepository.findById(5L)).willReturn(Optional.of(currentMilestone));
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID))
                .willReturn(List.of(previousMilestone, currentMilestone));

        Submission currentSubmission = submission(5L);

        assertThat(submissionQueryService.canSubmitNow(currentSubmission)).isFalse();
    }

    private Submission submission(Long milestoneId) {
        return submissionRepository.save(Submission.create(TEAM_ID, milestoneId));
    }

    private Milestone milestone(Long id, int weekNumber, MilestoneSchedule schedule) {
        return Milestone.restore(id, SECTION_ID, "마일스톤", null, weekNumber, MilestoneStatus.PUBLISHED, schedule);
    }

    private MilestoneSchedule schedule(LocalDateTime dueAt, LocalDateTime lateSubmissionUntil, LocalDateTime revisionUntil) {
        return new MilestoneSchedule(null, dueAt, lateSubmissionUntil, revisionUntil, null, null);
    }
}
