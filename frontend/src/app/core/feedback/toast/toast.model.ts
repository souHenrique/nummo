export type ToastTone = 'info' | 'success' | 'warning' | 'danger';

export interface ToastMessage {
  id: number;
  title?: string;
  message: string;
  tone: ToastTone;
  durationMs: number;
}

export interface ShowToastOptions {
  title?: string;
  message: string;
  tone?: ToastTone;
}
