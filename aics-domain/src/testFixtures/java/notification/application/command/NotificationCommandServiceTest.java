package notification.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kgu.developers.domain.notification.application.command.NotificationCommandService;
import kgu.developers.domain.notification.domain.NotificationType;
import mock.repository.FakeNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationCommandServiceTest {

    private FakeNotificationRepository fakeNotificationRepository;
    private NotificationCommandService commandService;

    @BeforeEach
    void init() {
        fakeNotificationRepository = new FakeNotificationRepository();
        commandService = new NotificationCommandService(fakeNotificationRepository);
    }

    @Test
    @DisplayName("createNotification은 알림 하나를 저장한다")
    void createNotification_Success() {
        // when
        commandService.createNotification("202412345", NotificationType.SECTION_ANNOUNCEMENT, "제목", "내용", null);

        // then
        assertThat(fakeNotificationRepository.findAll()).hasSize(1);
        assertThat(fakeNotificationRepository.findAll().get(0).getUserId()).isEqualTo("202412345");
        assertThat(fakeNotificationRepository.findAll().get(0).isRead()).isFalse();
    }

    @Test
    @DisplayName("broadcast는 전달된 사용자 수만큼 알림을 저장한다")
    void broadcast_SavesOnePerUser() {
        // when
        commandService.broadcast(
            List.of("202412345", "202412346", "202412347"),
            NotificationType.SECTION_ANNOUNCEMENT, "제목", "내용", null
        );

        // then
        assertThat(fakeNotificationRepository.findAll()).hasSize(3);
    }
}
