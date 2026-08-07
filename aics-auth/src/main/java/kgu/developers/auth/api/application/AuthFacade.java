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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthFacade {
    private final UserQueryService userQueryService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;

    public LoginResponse login(LoginRequest request) {
        User user = findForLogin(request.studentNumber());
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        return issue(user);
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

        // 서명이 유효해도 서버에 저장된 토큰과 다르면(이미 썼거나 탈취본이면) 거부한다.
        if (!refreshToken.equals(refreshTokenStore.consume(student_number))) {
            throw new InvalidTokenException();
        }

        return issue(findForRefresh(student_number));
    }

    // 쿠키가 없거나 깨졌으면 지울 것도 없다. 로그아웃 자체는 성공시킨다.
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            refreshTokenStore.delete(jwtUtil.parseRefreshTokenSubject(refreshToken));
        } catch (JwtException e) {
            // 무시
        }
    }

    private LoginResponse issue(User user) {
        String refreshToken = jwtUtil.createRefreshToken(user.getStudentNumber());
        refreshTokenStore.save(user.getStudentNumber(), refreshToken);
        return LoginResponse.of(
                jwtUtil.createAccessToken(user.getStudentNumber(), user.getGlobalRole().name()),
                refreshToken);
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
