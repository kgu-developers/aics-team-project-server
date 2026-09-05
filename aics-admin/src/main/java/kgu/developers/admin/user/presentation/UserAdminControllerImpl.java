package kgu.developers.admin.user.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import kgu.developers.admin.user.application.UserAdminFacade;
import kgu.developers.admin.user.presentation.request.UserAdminRequest;
import kgu.developers.admin.user.presentation.request.UserAdminUpdateRequest;
import kgu.developers.admin.user.presentation.response.UserAdminListResponse;
import kgu.developers.admin.user.presentation.response.UserAdminPersistResponse;
import kgu.developers.admin.user.presentation.response.UserAdminResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class UserAdminControllerImpl implements UserAdminController {

  private final UserAdminFacade userAdminFacade;

  @Override
  @PostMapping
  public ResponseEntity<UserAdminPersistResponse> createUser(
      @Valid @RequestBody UserAdminRequest request) {
    UserAdminPersistResponse response = userAdminFacade.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  @GetMapping("/{studentNumber}")
  public ResponseEntity<UserAdminResponse> getUserByStudentNumber(
      @NotBlank @PathVariable String studentNumber) {
    UserAdminResponse response = userAdminFacade.getUserByStudentNumber(studentNumber);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping
  public ResponseEntity<UserAdminListResponse> getUsers() {
    UserAdminListResponse response = userAdminFacade.getAllUsers();
    return ResponseEntity.ok(response);
  }

  @Override
  @PutMapping("/{studentNumber}")
  public ResponseEntity<Void> updateUser(
      @NotBlank @PathVariable String studentNumber,
      @Valid @RequestBody UserAdminUpdateRequest request) {
    userAdminFacade.updateUser(studentNumber, request);
    return ResponseEntity.noContent().build();
  }

  @Override
  @DeleteMapping("/{studentNumber}")
  public ResponseEntity<Void> deleteUser(
      @NotBlank @PathVariable String studentNumber) {
    userAdminFacade.deleteUser(studentNumber);
    return ResponseEntity.noContent().build();
  }
}
