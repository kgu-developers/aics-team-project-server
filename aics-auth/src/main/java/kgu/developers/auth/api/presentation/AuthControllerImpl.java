package kgu.developers.auth.api.presentation;

import java.time.Duration;

import jakarta.validation.Valid;
import kgu.developers.auth.api.application.AuthFacade;
import kgu.developers.auth.api.presentation.request.LoginRequest;
import kgu.developers.auth.api.presentation.response.LoginResponse;
import kgu.developers.auth.api.presentation.response.MessageResponse;
import kgu.developers.globalutils.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/oop/auth")
public class AuthControllerImpl implements AuthController {

    private static final String ACCESS_TOKEN = "accessToken";
    private static final String REFRESH_TOKEN = "refreshToken";

    private final AuthFacade userFacade;
    private final JwtUtil jwtUtil;
    private final boolean cookieSecure;

    public AuthControllerImpl(AuthFacade userFacade, JwtUtil jwtUtil,
                              @Value("${jwt.cookie-secure:true}") boolean cookieSecure) {
        this.userFacade = userFacade;
        this.jwtUtil = jwtUtil;
        this.cookieSecure = cookieSecure;
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<MessageResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return withTokenCookies(userFacade.login(request),
                MessageResponse.of("Login successfully", userFacade.getUserRole(request.studentNumber())));
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<MessageResponse> refresh(
            @CookieValue(name = REFRESH_TOKEN, required = false) String refreshToken) {
        return withTokenCookies(userFacade.refresh(refreshToken), MessageResponse.of("Refresh successfully"));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @CookieValue(name = REFRESH_TOKEN, required = false) String refreshToken) {
        userFacade.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie(ACCESS_TOKEN).toString())
                .header(HttpHeaders.SET_COOKIE, expiredCookie(REFRESH_TOKEN).toString())
                .body(MessageResponse.of("Logout successfully"));
    }

    private ResponseCookie expiredCookie(String name) {
        return tokenCookie(name, "", Duration.ZERO);
    }

    private ResponseEntity<MessageResponse> withTokenCookies(LoginResponse tokens, MessageResponse body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        tokenCookie(ACCESS_TOKEN, tokens.accessToken(), jwtUtil.getAccessTokenValidity()).toString())
                .header(HttpHeaders.SET_COOKIE,
                        tokenCookie(REFRESH_TOKEN, tokens.refreshToken(), jwtUtil.getRefreshTokenValidity()).toString())
                .body(body);
    }

    private ResponseCookie tokenCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
