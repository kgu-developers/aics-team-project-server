package kgu.developers.admin.sectionartifact.presentation;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kgu.developers.admin.sectionartifact.application.SectionArtifactAdminFacade;
import kgu.developers.admin.sectionartifact.presentation.response.SectionArtifactSummaryAdminListResponse;
import kgu.developers.common.response.FileDownload;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class SectionArtifactAdminControllerImpl implements SectionArtifactAdminController {

    private final SectionArtifactAdminFacade sectionArtifactAdminFacade;

    @Override
    @GetMapping("/sections/{sectionId}/artifacts/summary")
    public ResponseEntity<SectionArtifactSummaryAdminListResponse> getArtifactSummary(
        @PathVariable Long sectionId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            sectionArtifactAdminFacade.getArtifactSummary(sectionId, asOf, authentication.getName()));
    }

    @Override
    @GetMapping("/sections/{sectionId}/artifacts/download")
    public ResponseEntity<byte[]> downloadArtifactsExcel(
        @PathVariable Long sectionId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
        Authentication authentication
    ) {
        FileDownload download = sectionArtifactAdminFacade
            .downloadArtifactsExcel(sectionId, asOf, authentication.getName());
        ContentDisposition contentDisposition = ContentDisposition.attachment()
            .filename(download.fileName(), StandardCharsets.UTF_8)
            .build();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
            .cacheControl(CacheControl.noStore())  // 다른 분반 제출 현황이 캐시에 남지 않게 한다
            .contentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(download.content());
    }
}
