package kgu.developers.admin.user.application;

import kgu.developers.admin.user.presentation.request.UserAdminRequest;
import kgu.developers.admin.user.presentation.request.UserAdminUpdateRequest;
import kgu.developers.admin.user.presentation.response.UserAdminListResponse;
import kgu.developers.admin.user.presentation.response.UserAdminPersistResponse;
import kgu.developers.admin.user.presentation.response.UserAdminResponse;
import kgu.developers.domain.user.application.command.UserCommandService;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAdminFacade {
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;

    public UserAdminPersistResponse createUser(UserAdminRequest request, String callerStudentNumber) {
        validateSuperAdmin(request.globalRole(), callerStudentNumber);
        String studentNumber = userCommandService.createUser(request.studentNumber(), request.email(), request.name(), request.password(), request.globalRole(), request.phone(), request.reactivate());
        return UserAdminPersistResponse.of(studentNumber);
    }

    public void updateUser(String studentNumber, UserAdminUpdateRequest request, String callerStudentNumber) {
        User user = userQueryService.getUserByStudentNumber(studentNumber);
        validateSuperAdmin(user.getGlobalRole() == UserGlobalRole.SUPER_ADMIN
            ? user.getGlobalRole() : request.globalRole(), callerStudentNumber);
        userCommandService.updateUser(user, request.email(), request.name(), request.password(), request.globalRole(),
                request.phone());
    }

    public void deleteUser(String studentNumber, String callerStudentNumber) {
        User user = userQueryService.getUserByStudentNumber(studentNumber);
        validateSuperAdmin(user.getGlobalRole(), callerStudentNumber);
        userCommandService.deleteUser(user);
    }

    private void validateSuperAdmin(UserGlobalRole role, String callerStudentNumber) {
        if (role == UserGlobalRole.SUPER_ADMIN
            && userQueryService.getUserByStudentNumber(callerStudentNumber).getGlobalRole() != UserGlobalRole.SUPER_ADMIN) {
            throw new AccessDeniedException("슈퍼 관리자 계정은 슈퍼 관리자만 관리할 수 있습니다.");
        }
    }

    public void resetPassword(String studentNumber, String callerStudentNumber) {
        User user = userQueryService.getUserByStudentNumber(studentNumber);
        if (user.getGlobalRole() != UserGlobalRole.USER || !enrollmentRepository.findAllByUserId(studentNumber).stream()
            .filter(enrollment -> enrollment.isActiveStudent())
            .anyMatch(enrollment -> sectionRepository.existsActiveByIdAndProfessorId(enrollment.getSectionId(), callerStudentNumber))) {
            throw new AccessDeniedException("담당 분반의 학생 비밀번호만 초기화할 수 있습니다.");
        }
        userCommandService.resetPassword(user, user.getPhone());
    }

    public UserAdminResponse getUserByStudentNumber(String studentNumber) {
        return UserAdminResponse.from(userQueryService.getUserByStudentNumber(studentNumber));
    }

    public UserAdminListResponse getAllUsers() {
        return UserAdminListResponse.from(userQueryService.getAllUsers());
    }
}
