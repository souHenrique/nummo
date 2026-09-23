import { Injectable, signal } from '@angular/core';
import { ShowToastOptions, ToastMessage } from './toast.model';

@Injectable({ providedIn: 'root' })
export class ToastService {
  private static readonly DURATION_MS = 5_000;

  private nextId = 0;
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();
  private readonly messagesState = signal<ToastMessage[]>([]);

  readonly messages = this.messagesState.asReadonly();

  show(options: ShowToastOptions): number {
    const id = ++this.nextId;
    const tone = options.tone ?? 'info';
    const durationMs = ToastService.DURATION_MS;

    const toast: ToastMessage = {
      id,
      title: options.title,
      message: options.message,
      tone,
      durationMs,
    };

    this.messagesState.update((messages) => [...messages, toast]);

    if (durationMs > 0) {
      const timer = setTimeout(() => this.remove(id), durationMs);
      this.timers.set(id, timer);
    }

    return id;
  }

  remove(id: number): void {
    const timer = this.timers.get(id);

    if (timer) {
      clearTimeout(timer);
      this.timers.delete(id);
    }

    this.messagesState.update((messages) => messages.filter((message) => message.id !== id));
  }

  clear(): void {
    for (const timer of this.timers.values()) {
      clearTimeout(timer);
    }

    this.timers.clear();
    this.messagesState.set([]);
  }
}
