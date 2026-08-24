package sectionannouncement.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import kgu.developers.api.sectionannouncement.application.SectionAnnouncementFacade;
import kgu.developers.api.sectionannouncement.presentation.request.SectionAnnouncementCreateRequest;
import kgu.developers.api.sectionannouncement.presentation.request.SectionAnnouncementUpdateRequest;
import kgu.developers.api.sectionannouncement.presentation.response.SectionAnnouncementListResponse;
import kgu.developers.api.sectionannouncement.presentation.response.SectionAnnouncementResponse;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.notification.application.command.NotificationCommandService;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.sectionannouncement.application.command.SectionAnnouncementCommandService;
import kgu.developers.domain.sectionannouncement.application.query.SectionAnnouncementQueryService;
import mock.repository.FakeEnrollmentRepository;
import mock.repository.FakeNotificationRepository;
import mock.repository.FakeSectionAnnouncementRepository;
import mock.repository.FakeSectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

public class SectionAnnouncementFacadeTest {

    private static final String PROFESSOR = "202400001";
    private static final String STUDENT = "202412345";
    private static final String OUTSIDER = "202400000";

    private SectionAnnouncementFacade facade;
    private FakeNotificationRepository fakeNotificationRepository;
    private Long sectionId;

    @BeforeEach
    public void init() {
        FakeSectionRepository fakeSectionRepository = new FakeSectionRepository();
        FakeEnrollmentRepository fakeEnrollmentRepository = new FakeEnrollmentRepository();
        FakeSectionAnnouncementRepository fakeSectionAnnouncementRepository = new FakeSectionAnnouncementRepository();
        fakeNotificationRepository = new FakeNotificationRepository();

        Section section = fakeSectionRepository.save(
            Section.create(PROFESSOR, 1L, "DD015_1206", "1분반", "월 1교시", 40, null, null)
        );
        sectionId = section.getId();

        fakeEnrollmentRepository.save(Enrollment.create(sectionId, STUDENT, Role.STUDENT, Status.ACTIVE));

        facade = new SectionAnnouncementFacade(
            new SectionAnnouncementCommandService(fakeSectionAnnouncementRepository),
            new SectionAnnouncementQueryService(fakeSectionAnnouncementRepository),
            new NotificationCommandService(fakeNotificationRepository),
            fakeSectionRepository,
            fakeEnrollmentRepository
        );
    }

    private SectionAnnouncementCreateRequest buildCreateRequest() {
        return SectionAnnouncementCreateRequest.builder()
            .title("중간고사 안내")
            .content("다음 주 진행합니다.")
            .build();
    }

    @Test
    @DisplayName("createAnnouncement는 담당 교수가 공지사항을 생성하면 분반 전원에게 알림을 발송한다")
    public void createAnnouncement_BroadcastsToActiveEnrollments() {
        // when
        SectionAnnouncementResponse result = facade.createAnnouncement(sectionId, PROFESSOR, buildCreateRequest());

        // then
        assertNotNull(result.id());
        assertEquals(1, fakeNotificationRepository.findAll().size());
        assertEquals(STUDENT, fakeNotificationRepository.findAll().get(0).getUserId());
        assertEquals(result.id(), fakeNotificationRepository.findAll().get(0).getSourceId());
    }

    @Test
    @DisplayName("게시일시가 미래인 공지사항은 목록 조회에서 보이지 않는다")
    public void getAnnouncements_ExcludesScheduledAnnouncement() {
        // given
        SectionAnnouncementCreateRequest scheduledRequest = SectionAnnouncementCreateRequest.builder()
            .title("예약 공지")
            .content("내일 공개됩니다.")
            .publishedAt(LocalDateTime.now().plusDays(1))
            .build();
        facade.createAnnouncement(sectionId, PROFESSOR, scheduledRequest);

        // when
        SectionAnnouncementListResponse result = facade.getAnnouncements(sectionId, STUDENT);

        // then
        assertEquals(0, result.contents().size());
    }

    @Test
    @DisplayName("담당 교수가 아니면 공지사항을 생성할 수 없다")
    public void createAnnouncement_NonProfessor_ThrowsAccessDenied() {
        // when & then
        assertThatThrownBy(() -> facade.createAnnouncement(sectionId, STUDENT, buildCreateRequest()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getAnnouncements는 분반 소속 학생도 조회할 수 있다")
    public void getAnnouncements_StudentCanView() {
        // given
        facade.createAnnouncement(sectionId, PROFESSOR, buildCreateRequest());

        // when
        SectionAnnouncementListResponse result = facade.getAnnouncements(sectionId, STUDENT);

        // then
        assertEquals(1, result.contents().size());
    }

    @Test
    @DisplayName("분반에 소속되지 않은 사용자는 공지사항을 조회할 수 없다")
    public void getAnnouncements_Outsider_ThrowsAccessDenied() {
        // when & then
        assertThatThrownBy(() -> facade.getAnnouncements(sectionId, OUTSIDER))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("updateAnnouncement는 전달된 필드만 갱신한다")
    public void updateAnnouncement_UpdatesOnlyProvidedFields() {
        // given
        SectionAnnouncementResponse persisted = facade.createAnnouncement(sectionId, PROFESSOR, buildCreateRequest());
        SectionAnnouncementUpdateRequest updateRequest = SectionAnnouncementUpdateRequest.builder()
            .title("수정된 제목")
            .build();

        // when
        SectionAnnouncementResponse updated = facade.updateAnnouncement(persisted.id(), PROFESSOR, updateRequest);

        // then
        assertEquals("수정된 제목", updated.title());
        assertEquals("다음 주 진행합니다.", updated.content());
    }

    @Test
    @DisplayName("담당 교수가 아니면 공지사항을 수정할 수 없다")
    public void updateAnnouncement_NonProfessor_ThrowsAccessDenied() {
        // given
        SectionAnnouncementResponse persisted = facade.createAnnouncement(sectionId, PROFESSOR, buildCreateRequest());
        SectionAnnouncementUpdateRequest updateRequest = SectionAnnouncementUpdateRequest.builder()
            .title("수정 시도")
            .build();

        // when & then
        assertThatThrownBy(() -> facade.updateAnnouncement(persisted.id(), STUDENT, updateRequest))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("updateAnnouncement는 존재하지 않는 공지사항이면 예외를 던진다")
    public void updateAnnouncement_NotFound_ThrowsException() {
        // given
        SectionAnnouncementUpdateRequest updateRequest = SectionAnnouncementUpdateRequest.builder()
            .title("제목")
            .build();

        // when & then
        assertThatThrownBy(() -> facade.updateAnnouncement(999L, PROFESSOR, updateRequest))
            .isInstanceOf(CustomException.class);
    }
}
