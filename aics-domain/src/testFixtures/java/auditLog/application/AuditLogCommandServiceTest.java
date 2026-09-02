package auditLog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.auditLog.application.command.AuditLogCommandService;
import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.domain.AuditLogEventType;
import kgu.developers.domain.auditLog.domain.AuditLogRepository;
import kgu.developers.domain.auditLog.domain.TargetType;

@ExtendWith(MockitoExtension.class)
class AuditLogCommandServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogCommandService auditLogCommandService;

    @Test
    @DisplayName("팀 변경 감사 로그에 행위자, 분반, 팀, 이벤트와 metadata를 저장한다")
    void recordsTeamChange() {
        given(auditLogRepository.save(any(AuditLog.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        AuditLog saved = auditLogCommandService.recordTeamChange(
                "202699999",
                10L,
                1L,
                AuditLogEventType.TEAM_NAME_UPDATED,
                JsonConverter.parse("{\"before\":{\"name\":\"1팀\"},\"after\":{\"name\":\"새 팀\"}}")
        );

        assertThat(saved.getActorId()).isEqualTo("202699999");
        assertThat(saved.getSectionId()).isEqualTo(10L);
        assertThat(saved.getTargetType()).isEqualTo(TargetType.TEAM);
        assertThat(saved.getTargetId()).isEqualTo(1L);
        assertThat(saved.getEventType()).isEqualTo("TEAM_NAME_UPDATED");
        assertThat(saved.getMetadata().at("/after/name").asText()).isEqualTo("새 팀");
        verify(auditLogRepository).save(saved);
    }
}
