package com.amorim.finance_manager.user.controller;

import com.amorim.finance_manager.config.openapi.OpenApiConfig;
import com.amorim.finance_manager.user.api.UserApiDocs;
import com.amorim.finance_manager.user.dto.ChangePasswordRequest;
import com.amorim.finance_manager.user.dto.ConfirmCurrentPasswordRequest;
import com.amorim.finance_manager.user.dto.UpdateProfileRequest;
import com.amorim.finance_manager.user.dto.UserResponse;
import com.amorim.finance_manager.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "Usuários", description = "Perfil do usuário autenticado")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class UserController implements UserApiDocs {

    private final UserProfileService userProfileService;

    @Override
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userProfileService.getCurrentProfile());
    }

    @Override
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateCurrentProfile(request));
    }

    @Override
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changeCurrentUserPassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userProfileService.changeCurrentPassword(request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser(
            @Valid @RequestBody ConfirmCurrentPasswordRequest request
    ) {
        userProfileService.deleteCurrentUser(request);
        return ResponseEntity.noContent().build();
    }
}
