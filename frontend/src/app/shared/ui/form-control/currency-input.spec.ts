import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { CurrencyInputDirective } from './currency-input';

@Component({
  imports: [ReactiveFormsModule, CurrencyInputDirective],
  template: '<input appCurrencyInput [allowNegative]="allowNegative" [formControl]="control" />',
})
class CurrencyInputTestHost {
  readonly control = new FormControl<number | null>(null);
  allowNegative = true;
}

describe('CurrencyInputDirective', () => {
  let fixture: ComponentFixture<CurrencyInputTestHost>;
  let component: CurrencyInputTestHost;
  let input: HTMLInputElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [CurrencyInputTestHost] }).compileComponents();

    fixture = TestBed.createComponent(CurrencyInputTestHost);
    component = fixture.componentInstance;
    fixture.detectChanges();
    input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
  });

  it('formats digits as Brazilian currency while updating the numeric control value', () => {
    input.value = '00123456';
    input.dispatchEvent(new Event('input'));

    expect(input.value).toBe('R$ 1.234,56');
    expect(component.control.value).toBe(1234.56);
  });

  it('removes leading zeros and keeps cents while typing', () => {
    input.value = '000025';
    input.dispatchEvent(new Event('input'));

    expect(input.value).toBe('R$ 0,25');
    expect(component.control.value).toBe(0.25);
  });

  it('supports negative balances when enabled', () => {
    input.value = '-001250';
    input.dispatchEvent(new Event('input'));

    expect(input.value).toBe('-R$ 12,50');
    expect(component.control.value).toBe(-12.5);
  });

  it('formats numeric values written by the form control', () => {
    component.control.setValue(1500.5);
    fixture.detectChanges();

    expect(input.value).toBe('R$ 1.500,50');
  });
});
