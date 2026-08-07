package kgu.developers.admin.user.application;

import kgu.developers.admin.user.presentation.request.UserAdminRequest;
import kgu.developers.admin.user.presentation.request.UserAdminUpdateRequest;
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
        String studentNumber = userCommandService.createUser(request.studentNumber(), request.email(), request.name(), request.password(), request.globalRole(), request.phone());
        return UserAdminPersistResponse.of(studentNumber);
    }

    public void updateUser(String studentNumber, UserAdminUpdateRequest request) {
        User user = userQueryService.getUserByStudentNumber(studentNumber);
        userCommandService.updateUser(user, request.email(), request.name(), request.password(), request.globalRole(),
                request.phone());
    }

    public void deleteUser(String studentNumber) {
        User user = userQueryService.getUserByStudentNumber(studentNumber);
        userCommandService.deleteUser(user);
    }

    public UserAdminResponse getUserByStudentNumber(String studentNumber) {
        return UserAdminResponse.from(userQueryService.getUserByStudentNumber(studentNumber));
    }

    public UserAdminListResponse getAllUsers() {
        return UserAdminListResponse.from(userQueryService.getAllUsers());
    }
}
