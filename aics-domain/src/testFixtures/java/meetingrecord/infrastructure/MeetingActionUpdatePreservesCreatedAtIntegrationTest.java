package meetingrecord.infrastructure;

import java.time.LocalDateTime;
import kgu.developers.domain.meetingrecord.application.command.MeetingActionCommandService;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.meetingrecord.infrastructure.MeetingActionRepositoryImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// 수정 API가 수정 직후 같은 트랜잭션 안에서 재조회해 응답을 만드는데(MeetingActionFacade),
// MeetingActionJpaEntity.toEntity()가 createdAt을 안 채워서 그 재조회 결과의 createdAt이
// null로 보였고, 응답 조립(MeetingActionResponse.from())이 그 값을 null 체크 없이 format()해서
// NPE(500)로 이어졌다(KD3-233). 이 흐름을 실제 H2 + 진짜 리포지토리로 재현·검증한다.
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:meeting-action-update;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({MeetingActionRepositoryImpl.class, MeetingActionCommandService.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MeetingActionUpdatePreservesCreatedAtIntegrationTest {
    @SpringBootConfiguration
    @EntityScan("kgu.developers.domain.meetingrecord.infrastructure")
    @EnableJpaRepositories("kgu.developers.domain.meetingrecord.infrastructure")
    static class TestConfig {
    }

    @Autowired
    private MeetingActionCommandService commandService;

    @Autowired
    private MeetingActionRepositoryImpl repository;

    @Test
    @DisplayName("수정 직후 같은 트랜잭션에서 재조회해도 createdAt이 유지되어 응답 조립 시 NPE가 나지 않는다")
    void updateThenReadWithinSameTransactionKeepsCreatedAt() {
        Long id = commandService.createMeetingAction(1L, "202611111", "초안 내용", LocalDateTime.now().plusDays(3));

        commandService.updateMeetingAction(
            id, null, "수정된 내용", MeetingActionStatus.IN_PROGRESS, null, false, false
        );

        // MeetingActionFacade.updateMeetingAction()과 동일하게 같은 트랜잭션 안에서 곧바로 재조회
        MeetingAction updated = repository.findById(id).orElseThrow();

        Assertions.assertThat(updated.getCreatedAt()).isNotNull();
        Assertions.assertThatCode(() -> updated.getCreatedAt().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        )).doesNotThrowAnyException();
        Assertions.assertThat(updated.getContent()).isEqualTo("수정된 내용");
        Assertions.assertThat(updated.getStatus()).isEqualTo(MeetingActionStatus.IN_PROGRESS);
    }
}
