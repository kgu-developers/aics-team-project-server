package kgu.developers.admin.user.application;

import kgu.developers.admin.user.presentation.request.UserAdminRequest;
import kgu.developers.admin.user.presentation.response.UserAdminListResponse;
import kgu.developers.admin.user.presentation.response.UserAdminPersistResponse;
import kgu.developers.admin.user.presentation.response.UserAdminResponse;
import kgu.developers.domain.user.application.command.UserCommandService;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAdminFacade {
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    public UserAdminPersistResponse createUser(UserAdminRequest request) {
        String student_number = userCommandService.createUser(request.student_number(), request.email(), request.name(), request.password(), request.global_role(), request.phone());
        return UserAdminPersistResponse.of(student_number);
    }

    public void updateUser(String student_number, UserAdminRequest request) {
        User user = userQueryService.getUserByStudentNumber(student_number);
        userCommandService.updateUser(user, request.name(), request.password(), request.global_role(), request.phone());
    }

    public void deleteUser(String student_number) {
        User user = userQueryService.getUserByStudentNumber(student_number);
        userCommandService.deleteUser(user);
    }

    public UserAdminResponse getUserByStudentNumber(String student_number) {
        return UserAdminResponse.from(userQueryService.getUserByStudentNumber(student_number));
    }

    public UserAdminListResponse getAllUsers() {
        return UserAdminListResponse.from(userQueryService.getAllUsers());
    }
}
