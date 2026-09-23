import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';

import { AppDialogService } from '../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../core/http/api-request-error';
import { User } from '../../../shared/models/user.models';
import { AuthService } from '../../auth/services/auth.service';
import { ProfileApiService } from '../data-access/profile-api.service';
import { ProfilePage } from './profile-page';

describe('ProfilePage', () => {
  let fixture: ComponentFixture<ProfilePage>;
  let profileApi: {
    getCurrentUser: ReturnType<typeof vi.fn>;
    updateCurrentUser: ReturnType<typeof vi.fn>;
    changePassword: ReturnType<typeof vi.fn>;
    deleteCurrentUser: ReturnType<typeof vi.fn>;
  };
  let dialog: {
    confirm: ReturnType<typeof vi.fn>;
  };
  let toast: {
    show: ReturnType<typeof vi.fn>;
  };
  let auth: {
    logout: ReturnType<typeof vi.fn>;
  };

  const user: User = {
    id: '2a1fbc5b-cbb9-4879-b0c5-42f034d64261',
    name: 'Jesse Pinkman',
    email: 'jesse.pinkman@example.com',
    createdAt: '2026-09-02T12:00:00Z',
    updatedAt: '2026-09-02T12:30:00Z',
  };

  beforeEach(async () => {
    profileApi = {
      getCurrentUser: vi.fn().mockReturnValue(of(user)),
      updateCurrentUser: vi.fn(),
      changePassword: vi.fn(),
      deleteCurrentUser: vi.fn(),
    };

    dialog = {
      confirm: vi.fn().mockReturnValue(of(false)),
    };

    toast = {
      show: vi.fn(),
    };

    auth = {
      logout: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [ProfilePage],
      providers: [
        {
          provide: ProfileApiService,
          useValue: profileApi,
        },
        {
          provide: ToastService,
          useValue: toast,
        },
        {
          provide: AppDialogService,
          useValue: dialog,
        },
        {
          provide: AuthService,
          useValue: auth,
        },
      ],
    }).compileComponents();
  });

  function createPage(): void {
    fixture = TestBed.createComponent(ProfilePage);
    fixture.detectChanges();
  }

  function getInput(id: string): HTMLInputElement {
    return fixture.nativeElement.querySelector(`#${id}`) as HTMLInputElement;
  }

  function fillInput(id: string, value: string): void {
    const input = getInput(id);

    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function submitForm(): void {
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;

    form.dispatchEvent(
      new Event('submit', {
        bubbles: true,
        cancelable: true,
      }),
    );

    fixture.detectChanges();
  }

  function submitPasswordForm(): void {
    const form = fixture.nativeElement.querySelector('.password-form') as HTMLFormElement;

    form.dispatchEvent(
      new Event('submit', {
        bubbles: true,
        cancelable: true,
      }),
    );

    fixture.detectChanges();
  }

  it('should load the current profile when the page opens', () => {
    createPage();

    expect(profileApi.getCurrentUser).toHaveBeenCalledOnce();
    expect(getInput('profile-name').value).toBe(user.name);
    expect(getInput('profile-email').value).toBe(user.email);
  });

  it('should display skeletons while the profile is loading', () => {
    const response = new Subject<User>();

    profileApi.getCurrentUser.mockReturnValue(response.asObservable());

    createPage();

    expect(fixture.nativeElement.querySelectorAll('app-skeleton')).toHaveLength(3);

    response.next(user);
    response.complete();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-skeleton')).toBeNull();
  });

  it('should display an error state and retry loading the profile', () => {
    const apiError = new ApiRequestError({
      timestamp: '2026-09-15T12:00:00Z',
      status: 500,
      code: 'INTERNAL_SERVER_ERROR',
      message: 'Não foi possível carregar o perfil.',
      path: '/api/v1/users/me',
      fieldErrors: [],
    });

    profileApi.getCurrentUser
      .mockReturnValueOnce(throwError(() => apiError))
      .mockReturnValueOnce(of(user));

    createPage();

    expect(fixture.nativeElement.querySelector('app-error-state')).not.toBeNull();

    const retryButton = fixture.nativeElement.querySelector(
      '.error-state button',
    ) as HTMLButtonElement;

    retryButton.click();
    fixture.detectChanges();

    expect(profileApi.getCurrentUser).toHaveBeenCalledTimes(2);
    expect(getInput('profile-name').value).toBe(user.name);
  });

  it('should not submit an invalid form', () => {
    createPage();

    fillInput('profile-name', '');
    submitForm();

    expect(profileApi.updateCurrentUser).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Campo obrigatório.');
  });

  it('should not send a PATCH when no field changed', () => {
    createPage();

    submitForm();

    expect(profileApi.updateCurrentUser).not.toHaveBeenCalled();
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'info',
      title: 'Nenhuma alteração',
      message: 'Altere o nome ou o e-mail antes de salvar.',
    });
  });

  it('should send only the changed name and show a success toast', () => {
    const updatedUser: User = {
      ...user,
      name: 'Skyler White',
      updatedAt: '2026-09-15T12:00:00Z',
    };

    profileApi.updateCurrentUser.mockReturnValue(of(updatedUser));

    createPage();

    fillInput('profile-name', 'Skyler White');
    submitForm();

    expect(profileApi.updateCurrentUser).toHaveBeenCalledWith({
      name: 'Skyler White',
    });
    expect(getInput('profile-name').value).toBe('Skyler White');
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Perfil atualizado',
      message: 'Suas informações foram salvas.',
    });
  });

  it('should send only the changed email', () => {
    const updatedUser: User = {
      ...user,
      email: 'skyler.white@example.com',
      updatedAt: '2026-09-15T12:00:00Z',
    };

    profileApi.updateCurrentUser.mockReturnValue(of(updatedUser));

    createPage();

    fillInput('profile-email', 'skyler.white@example.com');
    fillInput('profile-email-current-password', 'SenhaSegura123!');
    submitForm();

    expect(profileApi.updateCurrentUser).toHaveBeenCalledWith({
      email: 'skyler.white@example.com',
      currentPassword: 'SenhaSegura123!',
    });
  });

  it('should require the current password before changing the email', () => {
    createPage();

    fillInput('profile-email', 'skyler.white@example.com');
    submitForm();

    expect(profileApi.updateCurrentUser).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'Informe sua senha atual para alterar o e-mail.',
    );
  });

  it('should show field errors returned by the API', () => {
    const apiError = new ApiRequestError({
      timestamp: '2026-09-15T12:00:00Z',
      status: 409,
      code: 'EMAIL_ALREADY_EXISTS',
      message: 'Já existe uma conta com este e-mail.',
      path: '/api/v1/users/me',
      fieldErrors: [
        {
          field: 'email',
          message: 'E-mail já está em uso.',
        },
      ],
    });

    profileApi.updateCurrentUser.mockReturnValue(throwError(() => apiError));

    createPage();

    fillInput('profile-email', 'outro.usuario@example.com');
    fillInput('profile-email-current-password', 'SenhaSegura123!');
    submitForm();

    expect(fixture.nativeElement.textContent).toContain('E-mail já está em uso.');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
  });

  it('should render a separate password form', () => {
    createPage();

    expect(fixture.nativeElement.querySelectorAll('input[type="password"]')).toHaveLength(4);
    expect(fixture.nativeElement.textContent).toContain('Senha atual');
    expect(fixture.nativeElement.textContent).toContain('Confirmar nova senha');
  });

  it('should prevent changing the password when confirmation differs', () => {
    createPage();

    fillInput('profile-current-password', 'SenhaSegura123!');
    fillInput('profile-new-password', 'NovaSenhaSegura456!');
    fillInput('profile-confirm-password', 'OutraSenhaSegura789');
    submitPasswordForm();

    expect(profileApi.changePassword).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('As senhas não coincidem.');
  });

  it('should prevent changing the password when a required character group is missing', () => {
    createPage();

    fillInput('profile-current-password', 'SenhaSegura123!');
    fillInput('profile-new-password', 'NovaSenhaSegura456');
    fillInput('profile-confirm-password', 'NovaSenhaSegura456');
    submitPasswordForm();

    expect(profileApi.changePassword).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'Use maiúscula, minúscula, número e caractere especial.',
    );
  });

  it('should change the password, show feedback and logout', () => {
    profileApi.changePassword.mockReturnValue(of(void 0));

    createPage();

    fillInput('profile-current-password', 'SenhaSegura123!');
    fillInput('profile-new-password', 'NovaSenhaSegura456!');
    fillInput('profile-confirm-password', 'NovaSenhaSegura456!');
    submitPasswordForm();

    expect(profileApi.changePassword).toHaveBeenCalledWith({
      currentPassword: 'SenhaSegura123!',
      newPassword: 'NovaSenhaSegura456!',
    });
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Senha alterada',
      message: 'Entre novamente usando sua nova senha.',
    });
    expect(auth.logout).toHaveBeenCalledOnce();
    expect(getInput('profile-current-password').value).toBe('');
    expect(getInput('profile-new-password').value).toBe('');
  });

  it('should show an error and clear password inputs when the current password is invalid', () => {
    const apiError = new ApiRequestError({
      timestamp: '2026-09-15T12:00:00Z',
      status: 400,
      code: 'INVALID_PASSWORD_CHANGE',
      message: 'A senha atual está incorreta.',
      path: '/api/v1/users/me/password',
      fieldErrors: [],
    });
    profileApi.changePassword.mockReturnValue(throwError(() => apiError));

    createPage();

    fillInput('profile-current-password', 'SenhaIncorreta999');
    fillInput('profile-new-password', 'NovaSenhaSegura456!');
    fillInput('profile-confirm-password', 'NovaSenhaSegura456!');
    submitPasswordForm();

    expect(fixture.nativeElement.textContent).toContain('A senha atual está incorreta.');
    expect(getInput('profile-current-password').value).toBe('');
    expect(auth.logout).not.toHaveBeenCalled();
  });

  it('should render an explicit account deletion action', () => {
    createPage();

    expect(fixture.nativeElement.textContent).toContain('Excluir conta');
    expect(fixture.nativeElement.textContent).toContain(
      'Esta ação encerra sua sessão e impede novos acessos.',
    );
    expect(
      fixture.nativeElement.querySelector('.profile-page__danger-actions .button'),
    ).not.toBeNull();
  });

  it('should not delete the account when the user declines the confirmation', () => {
    dialog.confirm.mockReturnValue(of(false));
    createPage();

    const deleteButton = fixture.nativeElement.querySelector(
      '.profile-page__danger-actions .button',
    ) as HTMLButtonElement;
    fillInput('profile-delete-current-password', 'SenhaSegura123!');
    deleteButton.click();

    expect(dialog.confirm).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'Excluir sua conta?',
        confirmLabel: 'Excluir minha conta',
        danger: true,
      }),
    );
    expect(profileApi.deleteCurrentUser).not.toHaveBeenCalled();
    expect(auth.logout).not.toHaveBeenCalled();
  });

  it('should soft-delete the account, show feedback and end the session after confirmation', () => {
    dialog.confirm.mockReturnValue(of(true));
    profileApi.deleteCurrentUser.mockReturnValue(of(void 0));
    createPage();

    const deleteButton = fixture.nativeElement.querySelector(
      '.profile-page__danger-actions .button',
    ) as HTMLButtonElement;
    fillInput('profile-delete-current-password', 'SenhaSegura123!');
    deleteButton.click();

    expect(profileApi.deleteCurrentUser).toHaveBeenCalledWith({
      currentPassword: 'SenhaSegura123!',
    });
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Conta excluída',
      message: 'Sua conta foi desativada e seus dados foram preservados.',
    });
    expect(auth.logout).toHaveBeenCalledOnce();
  });

  it('should prevent duplicate account deletion requests while confirmation is open', () => {
    const confirmation = new Subject<boolean>();
    dialog.confirm.mockReturnValue(confirmation.asObservable());
    createPage();

    const deleteButton = fixture.nativeElement.querySelector(
      '.profile-page__danger-actions .button',
    ) as HTMLButtonElement;
    fillInput('profile-delete-current-password', 'SenhaSegura123!');
    deleteButton.click();
    deleteButton.click();

    expect(dialog.confirm).toHaveBeenCalledOnce();
    expect(profileApi.deleteCurrentUser).not.toHaveBeenCalled();

    confirmation.next(false);
    confirmation.complete();
  });

  it('should keep the session and show an error when account deletion fails', () => {
    const apiError = new ApiRequestError({
      timestamp: '2026-09-18T12:00:00Z',
      status: 500,
      code: 'INTERNAL_SERVER_ERROR',
      message: 'Não foi possível excluir a conta.',
      path: '/api/v1/users/me',
      fieldErrors: [],
    });
    dialog.confirm.mockReturnValue(of(true));
    profileApi.deleteCurrentUser.mockReturnValue(throwError(() => apiError));
    createPage();

    const deleteButton = fixture.nativeElement.querySelector(
      '.profile-page__danger-actions .button',
    ) as HTMLButtonElement;
    fillInput('profile-delete-current-password', 'SenhaSegura123!');
    deleteButton.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível excluir a conta.');
    expect(auth.logout).not.toHaveBeenCalled();
  });

  it('should require the current password before opening the account deletion confirmation', () => {
    createPage();

    const deleteButton = fixture.nativeElement.querySelector(
      '.profile-page__danger-actions .button',
    ) as HTMLButtonElement;
    deleteButton.click();
    fixture.detectChanges();

    expect(dialog.confirm).not.toHaveBeenCalled();
    expect(profileApi.deleteCurrentUser).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'Informe sua senha atual para excluir a conta.',
    );
  });
});
