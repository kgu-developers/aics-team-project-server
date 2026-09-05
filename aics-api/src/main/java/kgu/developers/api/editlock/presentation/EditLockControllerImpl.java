package kgu.developers.api.editlock.presentation;

import jakarta.validation.Valid;
import kgu.developers.api.editlock.application.EditLockFacade;
import kgu.developers.api.editlock.presentation.request.EditLockAcquireRequest;
import kgu.developers.api.editlock.presentation.response.EditLockStatusResponse;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class EditLockControllerImpl implements EditLockController {

    private final EditLockFacade editLockFacade;

    @Override
    @GetMapping("/edit-locks")
    public ResponseEntity<EditLockStatusResponse> getStatus(
        @RequestParam EditLockTargetType targetType,
        @RequestParam Long targetId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(editLockFacade.getStatus(targetType, targetId, authentication.getName()));
    }

    @Override
    @PostMapping("/edit-locks")
    public ResponseEntity<EditLockStatusResponse> acquire(
        @Valid @RequestBody EditLockAcquireRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(editLockFacade.acquire(authentication.getName(), request));
    }

    @Override
    @DeleteMapping("/edit-locks")
    public ResponseEntity<Void> release(
        @RequestParam EditLockTargetType targetType,
        @RequestParam Long targetId,
        Authentication authentication
    ) {
        editLockFacade.release(targetType, targetId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
