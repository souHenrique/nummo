import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { EMPTY, finalize, switchMap, take } from 'rxjs';

import { AppDialogService } from '../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../core/http/api-request-error';
import { User } from '../../../shared/models/user.models';
import {
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  passwordComplexityValidator,
} from '../../../shared/validators/password-complexity.validator';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Card } from '../../../shared/ui/card/card';
import { ErrorState } from '../../../shared/ui/error-state/error-state';
import { InputDirective } from '../../../shared/ui/form-control/input';
import { FormField } from '../../../shared/ui/form-field/form-field';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import { AuthService } from '../../auth/services/auth.service';
import { ProfileApiService } from '../data-access/profile-api.service';
import {
  ChangePasswordRequest,
  ConfirmCurrentPasswordRequest,
  UpdateProfileRequest,
} from '../models/profile.models';

const nonBlankValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;

  return typeof value === 'string' && value.trim().length > 0 ? null : { required: true };
};

const passwordsMatchValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const newPassword = control.get('newPassword')?.value;
  const confirmation = control.get('confirmation')?.value;

  return newPassword === confirmation ? null : { passwordMismatch: true };
};

type ProfileField = 'name' | 'email' | 'currentPassword';
type PasswordField = 'currentPassword' | 'newPassword' | 'confirmation';

@Component({
  selector: 'app-profile-page',
  imports: [
    ReactiveFormsModule,
    Alert,
    Button,
    Card,
    ErrorState,
    FormField,
    InputDirective,
    Skeleton,
  ],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePage implements OnInit {
  private readonly profileApi = inject(ProfileApiService);
  private readonly dialog = inject(AppDialogService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);

  protected readonly profile = signal<User | null>(null);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly passwordSaving = signal(false);
  protected readonly deletingAccount = signal(false);
  protected readonly submitted = signal(false);
  protected readonly passwordSubmitted = signal(false);
  protected readonly loadError = signal<string | undefined>(undefined);
  protected readonly submissionError = signal<string | undefined>(undefined);
  protected readonly passwordSubmissionError = signal<string | undefined>(undefined);
  protected readonly deletionError = signal<string | undefined>(undefined);

  protected readonly form = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [nonBlankValidator, Validators.maxLength(120)],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [nonBlankValidator, Validators.email, Validators.maxLength(320)],
    }),
    currentPassword: new FormControl('', { nonNullable: true }),
  });

  protected readonly deletionForm = new FormGroup({
    currentPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  protected readonly passwordForm = new FormGroup(
    {
      currentPassword: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required],
      }),
      newPassword: new FormControl('', {
        nonNullable: true,
        validators: [
          Validators.required,
          Validators.minLength(PASSWORD_MIN_LENGTH),
          Validators.maxLength(PASSWORD_MAX_LENGTH),
          passwordComplexityValidator,
        ],
      }),
      confirmation: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required],
      }),
    },
    { validators: passwordsMatchValidator },
  );

  ngOnInit(): void {
    this.loadProfile();
  }

  protected loadProfile(): void {
    this.loading.set(true);
    this.loadError.set(undefined);

    this.profileApi
      .getCurrentUser()
      .pipe(
        finalize(() => {
          this.loading.set(false);
        }),
      )
      .subscribe({
        next: (profile) => {
          this.setProfile(profile);
        },
        error: (error: unknown) => {
          this.loadError.set(this.errorMessage(error));
        },
      });
  }

  protected submit(): void {
    this.submitted.set(true);
    this.submissionError.set(undefined);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.emailWillChange() && !this.form.controls.currentPassword.value.trim()) {
      this.form.controls.currentPassword.setErrors({ required: true });
      this.form.controls.currentPassword.markAsTouched();
      return;
    }

    const request = this.buildUpdateRequest();

    if (!request) {
      this.toast.show({
        tone: 'info',
        title: 'Nenhuma alteração',
        message: 'Altere o nome ou o e-mail antes de salvar.',
      });
      return;
    }

    this.saving.set(true);

    this.profileApi
      .updateCurrentUser(request)
      .pipe(
        finalize(() => {
          this.saving.set(false);
        }),
      )
      .subscribe({
        next: (profile) => {
          this.setProfile(profile);
          this.submitted.set(false);

          this.toast.show({
            tone: 'success',
            title: 'Perfil atualizado',
            message: 'Suas informações foram salvas.',
          });
        },
        error: (error: unknown) => {
          this.handleUpdateError(error);
        },
      });
  }

  protected fieldError(fieldName: ProfileField): string | undefined {
    const control = this.form.controls[fieldName];

    if (!this.submitted() && !control.touched) {
      return undefined;
    }

    if (fieldName === 'currentPassword' && this.emailWillChange() && control.hasError('required')) {
      return 'Informe sua senha atual para alterar o e-mail.';
    }

    if (control.hasError('required')) {
      return 'Campo obrigatório.';
    }

    if (fieldName === 'name' && control.hasError('maxlength')) {
      return 'Nome deve possuir no máximo 120 caracteres.';
    }

    if (fieldName === 'email' && control.hasError('email')) {
      return 'Informe um e-mail válido.';
    }

    if (fieldName === 'email' && control.hasError('maxlength')) {
      return 'E-mail deve possuir no máximo 320 caracteres.';
    }

    const serverError = control.getError('server');

    return typeof serverError === 'string' ? serverError : undefined;
  }

  protected changePassword(): void {
    this.passwordSubmitted.set(true);
    this.passwordSubmissionError.set(undefined);

    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const { currentPassword, newPassword } = this.passwordForm.getRawValue();
    const request: ChangePasswordRequest = { currentPassword, newPassword };

    this.passwordSaving.set(true);

    this.profileApi
      .changePassword(request)
      .pipe(
        finalize(() => {
          this.passwordSaving.set(false);
        }),
      )
      .subscribe({
        next: () => {
          this.passwordForm.reset();
          this.passwordSubmitted.set(false);

          this.toast.show({
            tone: 'success',
            title: 'Senha alterada',
            message: 'Entre novamente usando sua nova senha.',
          });
          this.auth.logout();
        },
        error: (error: unknown) => {
          this.handlePasswordError(error);
        },
      });
  }

  protected confirmAccountDeletion(): void {
    if (this.deletingAccount()) {
      return;
    }

    this.deletionError.set(undefined);

    if (this.deletionForm.invalid) {
      this.deletionForm.markAllAsTouched();
      return;
    }

    this.deletingAccount.set(true);
    const request: ConfirmCurrentPasswordRequest = this.deletionForm.getRawValue();

    this.dialog
      .confirm({
        title: 'Excluir sua conta?',
        message: 'Sua conta será desativada e você perderá o acesso imediatamente.',
        confirmLabel: 'Excluir minha conta',
        cancelLabel: 'Manter minha conta',
        danger: true,
      })
      .pipe(
        take(1),
        switchMap((confirmed) => (confirmed ? this.profileApi.deleteCurrentUser(request) : EMPTY)),
        finalize(() => this.deletingAccount.set(false)),
      )
      .subscribe({
        next: () => {
          this.toast.show({
            tone: 'success',
            title: 'Conta excluída',
            message: 'Sua conta foi desativada e seus dados foram preservados.',
          });
          this.auth.logout();
        },
        error: (error: unknown) => {
          this.deletionForm.reset();
          this.deletionError.set(
            error instanceof ApiRequestError
              ? error.message
              : 'Não foi possível excluir sua conta. Tente novamente.',
          );
        },
      });
  }

  protected passwordFieldError(fieldName: PasswordField): string | undefined {
    const control = this.passwordForm.controls[fieldName];

    if (!this.passwordSubmitted() && !control.touched) {
      return undefined;
    }

    if (control.hasError('required')) {
      return 'Campo obrigatório.';
    }

    if (fieldName === 'newPassword' && control.hasError('minlength')) {
      return 'A nova senha deve possuir ao menos 8 caracteres.';
    }

    if (fieldName === 'newPassword' && control.hasError('maxlength')) {
      return 'A nova senha deve possuir no máximo 72 caracteres.';
    }

    if (fieldName === 'newPassword' && control.hasError('passwordComplexity')) {
      return 'Use maiúscula, minúscula, número e caractere especial.';
    }

    if (fieldName === 'confirmation' && this.passwordForm.hasError('passwordMismatch')) {
      return 'As senhas não coincidem.';
    }

    const serverError = control.getError('server');

    return typeof serverError === 'string' ? serverError : undefined;
  }

  private setProfile(profile: User): void {
    this.profile.set(profile);

    this.form.reset({
      name: profile.name,
      email: profile.email,
      currentPassword: '',
    });

    this.form.markAsPristine();
    this.form.markAsUntouched();
  }

  private buildUpdateRequest(): UpdateProfileRequest | null {
    const currentProfile = this.profile();

    if (!currentProfile) {
      return null;
    }

    const values = this.form.getRawValue();
    const name = values.name.trim();
    const email = values.email.trim().toLowerCase();

    const request: UpdateProfileRequest = {};

    if (name !== currentProfile.name) {
      request.name = name;
    }

    if (email !== currentProfile.email.toLowerCase()) {
      request.email = email;
      request.currentPassword = values.currentPassword;
    }

    return Object.keys(request).length > 0 ? request : null;
  }

  private handleUpdateError(error: unknown): void {
    if (error instanceof ApiRequestError && error.code === 'INVALID_CURRENT_PASSWORD') {
      this.form.controls.currentPassword.reset();
    }

    this.submissionError.set(this.errorMessage(error));

    if (!(error instanceof ApiRequestError)) {
      return;
    }

    for (const fieldError of error.fieldErrors) {
      const control = this.form.get(fieldError.field);

      if (control) {
        control.setErrors({
          ...control.errors,
          server: fieldError.message,
        });
      }
    }
  }

  private handlePasswordError(error: unknown): void {
    this.passwordForm.reset();
    this.passwordSubmitted.set(false);

    this.passwordSubmissionError.set(
      error instanceof ApiRequestError
        ? error.message
        : 'Não foi possível alterar a senha. Tente novamente.',
    );
  }

  private errorMessage(error: unknown): string {
    if (error instanceof ApiRequestError) {
      return error.message;
    }

    return 'Não foi possível carregar ou atualizar o perfil.';
  }

  protected emailWillChange(): boolean {
    const currentProfile = this.profile();

    return (
      currentProfile !== null &&
      this.form.controls.email.value.trim().toLowerCase() !== currentProfile.email.toLowerCase()
    );
  }

  protected deletionPasswordError(): string | undefined {
    const control = this.deletionForm.controls.currentPassword;

    if (!control.touched || !control.hasError('required')) {
      return undefined;
    }

    return 'Informe sua senha atual para excluir a conta.';
  }
}
