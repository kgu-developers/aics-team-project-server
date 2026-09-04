package auditLog.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import kgu.developers.domain.auditLog.application.query.AuditLogQueryService;
import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.domain.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditLogQueryServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogQueryService auditLogQueryService;

    @Test
    @DisplayName("getTeamHistories는 분반과 팀 조건으로 감사 로그를 조회한다")
    void getTeamHistories() {
        Pageable pageable = PageRequest.of(0, 20);
        AuditLog auditLog = AuditLog.builder().id(1L).build();
        Page<AuditLog> histories = new PageImpl<>(List.of(auditLog), pageable, 1);
        given(auditLogRepository.findAllByTeam(10L, 20L, pageable)).willReturn(histories);

        Page<AuditLog> result = auditLogQueryService.getTeamHistories(10L, 20L, pageable);

        assertThat(result).isSameAs(histories);
        verify(auditLogRepository).findAllByTeam(10L, 20L, pageable);
    }

    @Test
    @DisplayName("getMemberActivities는 분반과 팀원 학번 조건으로 감사 로그를 조회한다")
    void getMemberActivities() {
        List<String> memberIds = List.of("202600001", "202600002");
        List<AuditLog> activities = List.of(AuditLog.builder().id(1L).build());
        given(auditLogRepository.findAllByTeamAndActorIdIn(10L, 20L, memberIds))
                .willReturn(activities);

        List<AuditLog> result = auditLogQueryService.getMemberActivities(10L, 20L, memberIds);

        assertThat(result).isSameAs(activities);
        verify(auditLogRepository).findAllByTeamAndActorIdIn(10L, 20L, memberIds);
    }
}
