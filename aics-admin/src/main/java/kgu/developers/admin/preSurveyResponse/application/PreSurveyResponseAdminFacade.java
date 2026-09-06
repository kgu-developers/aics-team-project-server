package kgu.developers.admin.preSurveyResponse.application;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import kgu.developers.admin.preSurveyResponse.presentation.response.PreSurveyResponseAdminListResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponseRepository;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.user.application.query.UserQueryService;
import lombok.RequiredArgsConstructor;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PreSurveyResponseAdminFacade {

    private final SectionQueryService sectionQueryService;
    private final PreSurveyResponseRepository preSurveyResponseRepository;
    private final UserQueryService userQueryService;

    public PreSurveyResponseAdminListResponse getResponsesBySection(Long sectionId, String professorId) {
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(sectionId, professorId)) {
            throw new AccessDeniedException("담당 분반의 사전조사 응답만 조회할 수 있습니다.");
        }
        List<PreSurveyResponse> responses = preSurveyResponseRepository.findAllBySectionId(sectionId);
        // 지목당한 학생은 사전조사를 제출하지 않았을 수 있어, 응답자 학번만으로는 이름을 채울 수 없다.
        List<String> userIds = Stream.concat(
                        responses.stream().map(PreSurveyResponse::getUserId),
                        responses.stream().map(PreSurveyResponse::getPreferredPeerUserId).filter(Objects::nonNull))
                .distinct()
                .toList();
        return PreSurveyResponseAdminListResponse.from(responses, userQueryService.getUsersByStudentNumbers(userIds));
    }
}
