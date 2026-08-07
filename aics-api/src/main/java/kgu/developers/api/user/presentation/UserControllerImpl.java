package kgu.developers.api.user.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import kgu.developers.api.user.application.UserFacade;
import kgu.developers.api.user.presentation.request.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oop/users")
public class UserControllerImpl implements UserController {

  private final UserFacade userFacade;

  @Override
  @PutMapping("/{student_number}/password")
  public ResponseEntity<String> updateUserPassword(
      @NotBlank @PathVariable String student_number,
      @Valid @RequestBody UserUpdateRequest request) {
    userFacade.updateUserPassword(student_number, request);
    return ResponseEntity.ok("Password changed successfully");
  }
}
