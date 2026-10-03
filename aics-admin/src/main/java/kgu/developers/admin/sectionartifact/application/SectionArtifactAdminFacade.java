package kgu.developers.admin.sectionartifact.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import kgu.developers.admin.sectionartifact.presentation.response.SectionArtifactSummaryAdminListResponse;
import kgu.developers.common.response.FileDownload;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.submission.application.query.SectionArtifactQueryService;
import kgu.developers.domain.submission.application.query.SectionArtifactTeamRow;
import lombok.RequiredArgsConstructor;

// 조회는 각 질의 서비스가 자기 트랜잭션에서 끝내므로 파사드에는 트랜잭션을 두지 않는다. 엑셀 생성은
// CPU·메모리 작업이라, 통합문서를 만드는 동안 DB 커넥션을 붙잡지 않아야 한다(PreSurveyResponseAdminFacade와 같은 이유).
@Component
@RequiredArgsConstructor
public class SectionArtifactAdminFacade {
    private final Clock serviceClock;
    private final SectionQueryService sectionQueryService;
    private final SectionArtifactQueryService sectionArtifactQueryService;

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

    public FileDownload downloadArtifactsExcel(Long sectionId, LocalDate asOf, String professorId) {
        validateSectionOwnedByProfessor(sectionId, professorId);
        Section section = sectionQueryService.getSectionById(sectionId).section();
        LocalDate baseDate = baseDate(asOf);

        List<SectionArtifactTeamRow> rows = sectionArtifactQueryService
                .getSectionArtifactRows(sectionId, until(baseDate));

        return new FileDownload(
                section.getName().replace('/', '_') + "-산출물-" + baseDate + ".xlsx",
                SectionArtifactExcelWriter.write(sectionLabel(section), rows));
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
