package kgu.developers.domain.auditLog.application.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.domain.AuditLogEventType;
import kgu.developers.domain.auditLog.domain.AuditLogRepository;
import kgu.developers.domain.auditLog.domain.TargetType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogCommandService {
    private final AuditLogRepository auditLogRepository;

    public AuditLog recordTeamChange(
            String actorId,
            Long sectionId,
            Long teamId,
            AuditLogEventType eventType,
            JsonNode metadata
    ) {
        return auditLogRepository.save(AuditLog.create(
                actorId,
                sectionId,
                eventType.name(),
                TargetType.TEAM,
                teamId,
                metadata
        ));
    }
}
