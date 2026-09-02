package kgu.developers.api.user.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kgu.developers.api.section.presentation.response.SectionResponse;
import kgu.developers.api.user.presentation.request.UserUpdateRequest;
import kgu.developers.api.user.presentation.response.UserResponse;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.application.command.UserCommandService;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserFacade {
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final SectionQueryService sectionQueryService;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional(readOnly = true)
    public UserResponse getMe(String studentNumber) {
        User user = userQueryService.getUserByStudentNumber(studentNumber);

        Long teamId = teamMemberRepository.findAllByUserId(studentNumber).stream()
                .map(TeamMember::getTeamId)
                .max(Long::compareTo)
                .orElse(null);

        List<SectionDetail> enrollmentSections = sectionQueryService.getSectionsByStudentNumber(studentNumber);
        List<SectionDetail> professorSections = sectionQueryService.getSectionsByProfessorId(studentNumber);
        List<SectionResponse> sections = mergeAndDeduplicateSections(enrollmentSections, professorSections);

        return UserResponse.from(user, sections, teamId);
    }

    public void updateUserPassword(String studentNumber, UserUpdateRequest request) {
        User user = userQueryService.getUserByStudentNumber(studentNumber);
        userCommandService.updatePassword(user, request.currentPassword(), request.password());
    }

    private List<SectionResponse> mergeAndDeduplicateSections(
            List<SectionDetail> enrollmentSections,
            List<SectionDetail> professorSections) {
        Map<Long, SectionResponse> sectionsById = new LinkedHashMap<>();
        enrollmentSections.stream()
                .map(SectionResponse::from)
                .forEach(section -> sectionsById.putIfAbsent(section.id(), section));
        professorSections.stream()
                .map(SectionResponse::from)
                .forEach(section -> sectionsById.putIfAbsent(section.id(), section));
        return List.copyOf(sectionsById.values());
    }
}
