package kgu.developers.admin.preSurveyResponse.application;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.admin.preSurveyResponse.presentation.response.PreSurveyResponseAdminListResponse;
import kgu.developers.domain.preSurveyResponse.application.query.PreSurveyResponseQueryService;
import kgu.developers.domain.preSurveyResponse.application.query.PreSurveyResponseRow;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponseRepository;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.user.application.query.UserQueryService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PreSurveyResponseAdminFacade {

    private final SectionQueryService sectionQueryService;
    private final PreSurveyResponseRepository preSurveyResponseRepository;
    private final UserQueryService userQueryService;
    private final PreSurveyResponseQueryService preSurveyResponseQueryService;

    @Transactional(readOnly = true)
    public PreSurveyResponseAdminListResponse getResponsesBySection(Long sectionId, String professorId) {
        validateSectionOwnedByProfessor(sectionId, professorId);
        List<PreSurveyResponse> responses = preSurveyResponseRepository.findAllBySectionId(sectionId);
        // 지목당한 학생은 사전조사를 제출하지 않았을 수 있어, 응답자 학번만으로는 이름을 채울 수 없다.
        List<String> userIds = Stream.concat(
                        responses.stream().map(PreSurveyResponse::getUserId),
                        responses.stream().map(PreSurveyResponse::getPreferredPeerUserId).filter(Objects::nonNull))
                .distinct()
                .toList();
        return PreSurveyResponseAdminListResponse.from(responses, userQueryService.getUsersByStudentNumbers(userIds));
    }

    // 엑셀 생성은 CPU·메모리 작업이라 트랜잭션 밖에서 한다. 조회는 각 도메인 서비스가 자기 트랜잭션에서
    // 끝내므로, 통합문서를 만드는 동안 DB 커넥션을 붙잡고 있지 않는다.
    public PreSurveyResponseExcelDownload downloadResponsesExcel(Long sectionId, String professorId) {
        Section section = sectionQueryService.getSectionById(sectionId).section();
        if (!professorId.equals(section.getProfessorId())) {
            throw new AccessDeniedException("담당 분반의 사전조사 응답만 조회할 수 있습니다.");
        }
        List<PreSurveyResponseRow> rows = preSurveyResponseQueryService.getSectionResponseRows(sectionId);

        // 분반명은 "수업시간/과목번호" 형태라 경로 구분자를 파일명에 그대로 쓸 수 없다.
        return new PreSurveyResponseExcelDownload(section.getName().replace('/', '_') + "-사전조사.xlsx",
                PreSurveyResponseExcelWriter.write(rows));
    }

    private void validateSectionOwnedByProfessor(Long sectionId, String professorId) {
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(sectionId, professorId)) {
            throw new AccessDeniedException("담당 분반의 사전조사 응답만 조회할 수 있습니다.");
        }
    }
}
