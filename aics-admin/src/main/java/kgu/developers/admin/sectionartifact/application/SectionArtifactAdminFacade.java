package kgu.developers.admin.sectionartifact.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.admin.sectionartifact.presentation.response.SectionArtifactSummaryAdminListResponse;
import kgu.developers.common.response.FileDownload;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.submission.application.command.SectionArtifactExcelCommandService;
import kgu.developers.domain.submission.application.query.SectionArtifactQueryService;
import kgu.developers.domain.submission.application.query.SectionArtifactTeamRow;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SectionArtifactAdminFacade {
    private final Clock serviceClock;
    private final SectionQueryService sectionQueryService;
    private final SectionArtifactQueryService sectionArtifactQueryService;
    private final SectionArtifactExcelCommandService sectionArtifactExcelCommandService;

    @Transactional(readOnly = true)
    public SectionArtifactSummaryAdminListResponse getArtifactSummary(
            Long sectionId,
            LocalDate asOf,
            String professorId
    ) {
        validateSectionOwnedByProfessor(sectionId, professorId);
        Section section = sectionQueryService.getSectionById(sectionId).section();
        LocalDate baseDate = baseDate(asOf);

        return SectionArtifactSummaryAdminListResponse.of(
                sectionId,
                sectionLabel(section),
                baseDate,
                sectionArtifactQueryService.getSectionArtifactRows(sectionId, until(baseDate)));
    }

    // 엑셀 생성은 CPU·메모리 작업이라 트랜잭션 밖에서 한다 — 집계 조회는 질의 서비스가 자기 트랜잭션에서
    // 끝내고, 여기서는 확정된 행만 받아 통합문서로 만든다(PreSurveyResponseAdminFacade와 같은 이유).
    public FileDownload downloadArtifactsExcel(Long sectionId, LocalDate asOf, String professorId) {
        validateSectionOwnedByProfessor(sectionId, professorId);
        Section section = sectionQueryService.getSectionById(sectionId).section();
        LocalDate baseDate = baseDate(asOf);

        List<SectionArtifactTeamRow> rows = sectionArtifactQueryService
                .getSectionArtifactRows(sectionId, until(baseDate));

        return new FileDownload(
                section.getName().replace('/', '_') + "-산출물-" + baseDate + ".xlsx",
                sectionArtifactExcelCommandService.writeWorkbook(sectionLabel(section), rows));
    }

    private void validateSectionOwnedByProfessor(Long sectionId, String professorId) {
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(sectionId, professorId)) {
            throw new AccessDeniedException("담당 분반의 산출물만 조회할 수 있습니다.");
        }
    }

    // 기준일을 생략하면 "오늘"인데, 저장된 시각이 KST 벽시계 기준이라 JVM 기본 시간대로 구하면
    // UTC 환경에서 하루가 어긋난다 — 심야 다운로드가 그날 제출분을 통째로 빼먹는다(KD3-275와 같은 부류).
    private LocalDate baseDate(LocalDate asOf) {
        return asOf == null ? LocalDate.now(serviceClock) : asOf;
    }

    private LocalDateTime until(LocalDate baseDate) {
        return baseDate.atTime(LocalTime.MAX);
    }

    private String sectionLabel(Section section) {
        return section.getCode() == null ? section.getName() : section.getCode();
    }
}
