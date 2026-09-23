package com.amorim.finance_manager.user.service;

import com.amorim.finance_manager.shared.exception.DuplicateEmailException;
import com.amorim.finance_manager.shared.exception.InvalidPasswordChangeException;
import com.amorim.finance_manager.shared.exception.InvalidCurrentPasswordException;
import com.amorim.finance_manager.shared.exception.InvalidProfileUpdateException;
import com.amorim.finance_manager.user.dto.ChangePasswordRequest;
import com.amorim.finance_manager.user.dto.ConfirmCurrentPasswordRequest;
import com.amorim.finance_manager.user.dto.UpdateProfileRequest;
import com.amorim.finance_manager.user.dto.UserResponse;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.entity.UserStatus;
import com.amorim.finance_manager.user.mapper.UserMapper;
import com.amorim.finance_manager.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@AllArgsConstructor
public class UserProfileService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getCurrentProfile() {
        User user = currentUserService.getCurrentUser();

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateCurrentProfile(UpdateProfileRequest request) {
        validate(request);

        User user = currentUserService.getCurrentUser();

        boolean isEmailChange = request.email() != null && !request.email().equals(user.getEmail());

        if (isEmailChange) {
            requireCurrentPassword(user, request.currentPassword());
        }

        if (isEmailChange
                && userRepository.existsByEmailAndIdNot(request.email(), user.getId())) {
            throw new DuplicateEmailException();
        }

        userMapper.updateEntity(request, user);

        try {
            User updateUser = userRepository.saveAndFlush(user);
            return userMapper.toResponse(updateUser);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }

    @Transactional
    public void changeCurrentPassword(ChangePasswordRequest request) {
        User user = currentUserService.getCurrentUser();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordChangeException("A senha atual está incorreta.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordChangeException("A nova senha deve ser diferente da senha atual");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setAuthenticationVersion(user.getAuthenticationVersion() + 1);
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public void deleteCurrentUser(ConfirmCurrentPasswordRequest request) {
        User user = currentUserService.getCurrentUser();

        requireCurrentPassword(user, request.currentPassword());

        user.setStatus(UserStatus.DELETED);
        user.setAuthenticationVersion(user.getAuthenticationVersion() + 1);
        userRepository.saveAndFlush(user);
    }

    private void validate(UpdateProfileRequest request) {
        if (request.name() == null && request.email() == null) {
            throw new InvalidProfileUpdateException("Informe ao menos um campo para atualização");
        }

        if (request.name() != null && request.name().isBlank()) {
            throw new InvalidProfileUpdateException("Nome não pode ser vazio");
        }

        if (request.email() != null && request.email().isBlank()) {
            throw new InvalidProfileUpdateException("E-mail não pode ser vazio");
        }
    }

    private void requireCurrentPassword(User user, String currentPassword) {
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }
    }
}
