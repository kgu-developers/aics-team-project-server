package kgu.developers.api.user.application;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import kgu.developers.api.user.presentation.request.UserUpdateRequest;
import kgu.developers.api.user.presentation.response.UserResponse;
import kgu.developers.api.section.presentation.response.SectionResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
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
    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public UserResponse getMe(String studentNumber) {
        User user = userQueryService.getUserByStudentNumber(studentNumber);

        List<Enrollment> enrollments = enrollmentRepository.findAllByUserId(studentNumber)
                .stream()
                .filter(e -> e.getStatus() == Status.ACTIVE)
                .toList();

        List<SectionDetail> professorSections = List.of();
        if (!enrollments.isEmpty()) {
            professorSections = sectionRepository.findAllByProfessorId(studentNumber);
        }

        if (enrollments.isEmpty() && professorSections.isEmpty()) {
            return UserResponse.from(user);
        }

        List<Long> enrollmentSectionIds = enrollments.stream()
                .map(Enrollment::getSectionId)
                .toList();

        List<SectionDetail> enrollmentSectionDetails = sectionRepository.findAllByIdIn(enrollmentSectionIds);

        List<SectionResponse> sections = mergeAndDeduplicateSections(
                enrollments, enrollmentSectionDetails, professorSections);

        return UserResponse.from(user, sections);
    }

    public void updateUserPassword(String studentNumber, UserUpdateRequest request) {
        User user = userQueryService.getUserByStudentNumber(studentNumber);
        userCommandService.updatePassword(user, request.currentPassword(), request.password());
    }

    private List<SectionResponse> mergeAndDeduplicateSections(
            List<Enrollment> enrollments,
            List<SectionDetail> enrollmentSectionDetails,
            List<SectionDetail> professorSections) {
        Map<Long, SectionDetail> enrollmentSectionMap = enrollmentSectionDetails.stream()
                .collect(Collectors.toMap(sd -> sd.section().getId(), Function.identity()));

        List<SectionResponse> enrollmentSections = enrollments.stream()
                .filter(e -> enrollmentSectionMap.containsKey(e.getSectionId()))
                .map(e -> SectionResponse.from(enrollmentSectionMap.get(e.getSectionId())))
                .toList();

        List<SectionResponse> professorSectionResponses = professorSections.stream()
                .map(SectionResponse::from)
                .toList();

        return Stream.concat(enrollmentSections.stream(), professorSectionResponses.stream())
                .distinct()
                .toList();
    }
}

