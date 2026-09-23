import { Directive, ElementRef, forwardRef, inject, input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Directive({
  selector: 'input[appCurrencyInput]',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CurrencyInputDirective),
      multi: true,
    },
  ],
  host: {
    type: 'text',
    inputmode: 'decimal',
    autocomplete: 'off',
    '(input)': 'onInput()',
    '(blur)': 'onBlur()',
  },
})
export class CurrencyInputDirective implements ControlValueAccessor {
  readonly allowNegative = input(false);

  private onChange: (value: number | null) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  private readonly elementRef = inject<ElementRef<HTMLInputElement>>(ElementRef);

  writeValue(value: number | null | undefined): void {
    this.elementRef.nativeElement.value =
      value === null || value === undefined ? '' : this.format(value);
  }

  registerOnChange(onChange: (value: number | null) => void): void {
    this.onChange = onChange;
  }

  registerOnTouched(onTouched: () => void): void {
    this.onTouched = onTouched;
  }

  setDisabledState(disabled: boolean): void {
    this.elementRef.nativeElement.disabled = disabled;
  }

  onInput(): void {
    const input = this.elementRef.nativeElement;
    const value = this.parse(input.value);

    input.value = value === null ? '' : this.format(value);
    input.setSelectionRange(input.value.length, input.value.length);
    this.onChange(value);
  }

  onBlur(): void {
    this.onTouched();
  }

  private parse(rawValue: string): number | null {
    const digits = rawValue.replace(/\D/g, '');

    if (!digits) {
      return null;
    }

    const amount = Number(digits) / 100;

    if (!Number.isFinite(amount)) {
      return null;
    }

    return this.allowNegative() && rawValue.includes('-') ? -amount : amount;
  }

  private format(value: number): string {
    const absoluteValue = Math.abs(value);
    const [integerPart, decimalPart] = absoluteValue.toFixed(2).split('.');
    const groupedInteger = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    const sign = value < 0 ? '-' : '';

    return `${sign}R$ ${groupedInteger},${decimalPart}`;
  }
}
