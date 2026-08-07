package kgu.developers.admin.user.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import kgu.developers.admin.user.application.UserAdminFacade;
import kgu.developers.admin.user.presentation.request.UserAdminRequest;
import kgu.developers.admin.user.presentation.response.UserAdminListResponse;
import kgu.developers.admin.user.presentation.response.UserAdminPersistResponse;
import kgu.developers.admin.user.presentation.response.UserAdminResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/oop/users")
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
  @GetMapping("/{student_number}")
  public ResponseEntity<UserAdminResponse> getUserByStudentNumber(
      @NotBlank @PathVariable String student_number) {
    UserAdminResponse response = userAdminFacade.getUserByStudentNumber(student_number);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping
  public ResponseEntity<UserAdminListResponse> getUsers() {
    UserAdminListResponse response = userAdminFacade.getAllUsers();
    return ResponseEntity.ok(response);
  }

  @Override
  @PutMapping("/{student_number}")
  public ResponseEntity<Void> updateUser(
      @NotBlank @PathVariable String student_number,
      @Valid @RequestBody UserAdminRequest request) {
    userAdminFacade.updateUser(student_number, request);
    return ResponseEntity.noContent().build();
  }

  @Override
  @DeleteMapping("/{student_number}")
  public ResponseEntity<Void> deleteUser(
      @NotBlank @PathVariable String student_number) {
    userAdminFacade.deleteUser(student_number);
    return ResponseEntity.noContent().build();
  }
}
