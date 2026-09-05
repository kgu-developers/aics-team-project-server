package kgu.developers.api.user.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import kgu.developers.api.user.application.UserFacade;
import kgu.developers.api.user.presentation.request.UserUpdateRequest;
import kgu.developers.api.user.presentation.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserControllerImpl implements UserController {

  private final UserFacade userFacade;

  @Override
  @GetMapping("/me")
  public ResponseEntity<UserResponse> getMe() {
    String studentNumber = SecurityContextHolder.getContext().getAuthentication().getName();
    UserResponse response = userFacade.getMe(studentNumber);
    return ResponseEntity.ok(response);
  }

  @Override
  @PutMapping("/{studentNumber}/password")
  public ResponseEntity<String> updateUserPassword(
      @NotBlank @PathVariable String studentNumber,
      @Valid @RequestBody UserUpdateRequest request) {
    String loginStudentNumber = SecurityContextHolder.getContext().getAuthentication().getName();
    if (!studentNumber.equals(loginStudentNumber)) {
      throw new AccessDeniedException("본인의 비밀번호만 변경할 수 있습니다.");
    }

    userFacade.updateUserPassword(studentNumber, request);
    return ResponseEntity.ok("Password changed successfully");
  }
}
