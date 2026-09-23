import { TestBed } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    vi.useFakeTimers();

    TestBed.configureTestingModule({});

    service = TestBed.inject(ToastService);
  });

  afterEach(() => {
    service.clear();
    vi.clearAllTimers();
    vi.useRealTimers();
  });

  it('should add an informational toast by default', () => {
    const id = service.show({
      message: 'Dados carregados',
    });

    expect(id).toBe(1);
    expect(service.messages()).toEqual([
      {
        id: 1,
        title: undefined,
        message: 'Dados carregados',
        tone: 'info',
        durationMs: 5000,
      },
    ]);
  });

  it('should generate sequential identifiers', () => {
    const firstId = service.show({
      message: 'Primeiro',
    });

    const secondId = service.show({
      message: 'Segundo',
    });

    expect(firstId).toBe(1);
    expect(secondId).toBe(2);
  });

  it('should remove a toast after its duration', () => {
    service.show({
      message: 'Operação concluída',
      tone: 'success',
    });

    expect(service.messages()).toHaveLength(1);

    vi.advanceTimersByTime(4999);

    expect(service.messages()).toHaveLength(1);

    vi.advanceTimersByTime(1);

    expect(service.messages()).toHaveLength(0);
  });

  it('should remove danger toasts after five seconds', () => {
    service.show({
      message: 'Não foi possível salvar',
      tone: 'danger',
    });

    vi.advanceTimersByTime(5000);

    expect(service.messages()).toEqual([]);
  });

  it('should remove a toast manually', () => {
    const id = service.show({
      message: 'Mensagem removível',
    });

    service.remove(id);

    expect(service.messages()).toEqual([]);
  });

  it('should remove every toast when clear is called', () => {
    service.show({
      message: 'Primeiro',
    });

    service.show({
      message: 'Segundo',
    });

    service.clear();

    expect(service.messages()).toEqual([]);
  });

  it('should cancel the five-second timer when a toast is removed manually', () => {
    const id = service.show({
      message: 'Mensagem temporária',
    });

    service.remove(id);
    vi.advanceTimersByTime(5000);

    expect(service.messages()).toEqual([]);
  });
});
