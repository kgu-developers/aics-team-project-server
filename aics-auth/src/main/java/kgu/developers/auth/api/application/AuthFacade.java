package kgu.developers.auth.api.application;

import kgu.developers.auth.api.presentation.response.LoginResponse;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.exception.InvalidCredentialsException;
import kgu.developers.domain.user.exception.InvalidTokenException;
import kgu.developers.domain.user.exception.UserNotFoundException;
import io.jsonwebtoken.JwtException;
import kgu.developers.auth.api.presentation.request.LoginRequest;
import kgu.developers.globalutils.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthFacade {
    private final UserQueryService userQueryService;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        User user = findForLogin(request.student_number());
        if (!request.password().equals(user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        return LoginResponse.of(
                jwtUtil.createAccessToken(user.getStudent_number(), user.getGlobal_role().name()),
                jwtUtil.createRefreshToken(user.getStudent_number()));
    }

    public LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException();
        }

        String student_number;
        try {
            student_number = jwtUtil.parseRefreshTokenSubject(refreshToken);
        } catch (JwtException e) {
            throw new InvalidTokenException();
        }

        User user = findForRefresh(student_number);
        return LoginResponse.of(
                jwtUtil.createAccessToken(user.getStudent_number(), user.getGlobal_role().name()),
                jwtUtil.createRefreshToken(user.getStudent_number()));
    }

    private User findForRefresh(String student_number) {
        try {
            return userQueryService.getUserByStudentNumber(student_number);
        } catch (UserNotFoundException e) {
            throw new InvalidTokenException();
        }
    }

    private User findForLogin(String student_number) {
        try {
            return userQueryService.getUserByStudentNumber(student_number);
        } catch (UserNotFoundException e) {
            throw new InvalidCredentialsException();
        }
    }
}
