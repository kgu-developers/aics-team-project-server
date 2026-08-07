package kgu.developers.api.user.application;

import kgu.developers.api.user.presentation.request.UserUpdateRequest;
import kgu.developers.domain.user.application.command.UserCommandService;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFacade {
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    public void updateUserPassword(String studentNumber, UserUpdateRequest request) {
        User user = userQueryService.getUserByStudentNumber(studentNumber);
        userCommandService.updatePassword(user, request.password());
    }
}
