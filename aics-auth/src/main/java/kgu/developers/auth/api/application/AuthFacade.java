package kgu.developers.auth.api.application;

import kgu.developers.auth.api.presentation.response.LoginResponse;
import kgu.developers.domain.auth.domain.LoginRole;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.exception.InvalidCredentialsException;
import kgu.developers.domain.user.exception.InvalidTokenException;
import kgu.developers.domain.user.exception.UserNotFoundException;
import io.jsonwebtoken.JwtException;
import kgu.developers.auth.api.presentation.request.LoginRequest;
import kgu.developers.globalutils.jwt.JwtUtil;
import kgu.developers.globalutils.jwt.TokenRevocationStore;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthFacade {
    private final UserQueryService userQueryService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenRevocationStore tokenRevocationStore;

    public LoginResponse login(LoginRequest request) {
        User user = findForLogin(request.studentNumber());
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        LoginRole role = userQueryService.getUserRoleByStudentNumber(user.getStudentNumber());
        return issue(user, role);
    }

    public LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException();
        }

        String studentNumber;
        try {
            studentNumber = jwtUtil.parseRefreshTokenSubject(refreshToken);
        } catch (JwtException e) {
            throw new InvalidTokenException();
        }

        User user = findForRefresh(studentNumber);
        LoginRole role = userQueryService.getUserRoleByStudentNumber(studentNumber);
        String newRefreshToken = jwtUtil.createRefreshToken(studentNumber);

        if (!rotate(studentNumber, refreshToken, newRefreshToken)) {
            throw new InvalidTokenException();
        }

        return tokens(user, newRefreshToken, role);
    }

    // 쿠키가 없거나 깨졌으면 지울 것도 없다. 로그아웃 자체는 성공시킨다.
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            String studentNumber = jwtUtil.parseRefreshTokenSubject(refreshToken);
            if (refreshTokenStore.deleteIfMatches(studentNumber, refreshToken)) {
                tokenRevocationStore.revokeTokensIssuedBefore(studentNumber);
            }
        } catch (JwtException | OptimisticLockingFailureException e) {
            // 무시. 동시 logout/refresh로 버전이 어긋나도 어차피 이 토큰은 폐기된 상태다.
        }
    }

    private boolean rotate(String studentNumber, String refreshToken, String newRefreshToken) {
        try {
            return refreshTokenStore.replace(studentNumber, refreshToken, newRefreshToken);
        } catch (OptimisticLockingFailureException e) {
            return false;
        }
    }

    private LoginResponse issue(User user, LoginRole role) {
        String refreshToken = jwtUtil.createRefreshToken(user.getStudentNumber());
        refreshTokenStore.save(user.getStudentNumber(), refreshToken);
        return tokens(user, refreshToken, role);
    }

    private LoginResponse tokens(User user, String refreshToken, LoginRole role) {
        return LoginResponse.of(
                jwtUtil.createAccessToken(user.getStudentNumber(), user.getGlobalRole().name()),
                refreshToken,
                role);
    }

    private User findForRefresh(String studentNumber) {
        try {
            return userQueryService.getUserByStudentNumber(studentNumber);
        } catch (UserNotFoundException e) {
            throw new InvalidTokenException();
        }
    }

    private User findForLogin(String studentNumber) {
        try {
            return userQueryService.getUserByStudentNumber(studentNumber);
        } catch (UserNotFoundException e) {
            throw new InvalidCredentialsException();
        }
    }
}
