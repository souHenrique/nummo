export interface UpdateProfileRequest {
  name?: string;
  email?: string;
  currentPassword?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ConfirmCurrentPasswordRequest {
  currentPassword: string;
}
