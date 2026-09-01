package kgu.developers.domain.auditLog.application.query;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.domain.AuditLogRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;

    public Page<AuditLog> getTeamHistories(Long sectionId, Long teamId, Pageable pageable) {
        return auditLogRepository.findAllByTeam(sectionId, teamId, pageable);
    }

    public List<AuditLog> getMemberActivities(Long sectionId, List<String> memberIds) {
        return auditLogRepository.findAllBySectionIdAndActorIdIn(sectionId, memberIds);
    }
}
