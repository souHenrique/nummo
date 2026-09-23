import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastContainer } from './toast-container';
import { ToastService } from './toast.service';

describe('ToastContainer', () => {
  let fixture: ComponentFixture<ToastContainer>;
  let service: ToastService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToastContainer],
    }).compileComponents();

    fixture = TestBed.createComponent(ToastContainer);
    service = TestBed.inject(ToastService);
  });

  afterEach(() => {
    service.clear();
  });

  it('should render no toast when the queue is empty', () => {
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelectorAll('.toast')).toHaveLength(0);
  });

  it('should render the toast title and message', () => {
    service.show({
      title: 'Sucesso',
      message: 'Orçamento criado',
      tone: 'success',
    });

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const toast = element.querySelector('.toast');

    expect(toast).not.toBeNull();
    expect(toast?.textContent).toContain('Sucesso');
    expect(toast?.textContent).toContain('Orçamento criado');
    expect(toast?.classList).toContain('toast--success');
  });

  it('should use status role for non-danger toasts', () => {
    service.show({
      message: 'Dados atualizados',
      tone: 'success',
    });

    fixture.detectChanges();

    const toast = fixture.nativeElement.querySelector('.toast') as HTMLElement;

    expect(toast.getAttribute('role')).toBe('status');
  });

  it('should use alert role for danger toasts', () => {
    service.show({
      message: 'Falha ao salvar',
      tone: 'danger',
    });

    fixture.detectChanges();

    const toast = fixture.nativeElement.querySelector('.toast') as HTMLElement;

    expect(toast.getAttribute('role')).toBe('alert');
  });

  it('should render a visible symbol in addition to color', () => {
    service.show({
      message: 'Operação concluída',
      tone: 'success',
    });

    fixture.detectChanges();

    const symbol = fixture.nativeElement.querySelector('.toast__symbol') as HTMLElement;

    expect(symbol.textContent?.trim()).toBe('✓');
    expect(symbol.getAttribute('aria-hidden')).toBe('true');
  });

  it('should remove the toast when close is clicked', () => {
    service.show({
      message: 'Mensagem removível',
    });

    fixture.detectChanges();

    const closeButton = fixture.nativeElement.querySelector(
      'button[aria-label="Fechar notificação"]',
    ) as HTMLButtonElement;

    closeButton.click();
    fixture.detectChanges();

    expect(service.messages()).toEqual([]);
    expect(fixture.nativeElement.querySelector('.toast')).toBeNull();
  });

  it('should render multiple toasts', () => {
    service.show({
      message: 'Primeiro',
    });

    service.show({
      message: 'Segundo',
    });

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.toast')).toHaveLength(2);
  });
});
