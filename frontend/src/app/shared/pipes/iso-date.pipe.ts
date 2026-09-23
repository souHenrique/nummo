import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'isoDate',
})
export class IsoDatePipe implements PipeTransform {
  transform(value: string | null | undefined, fallback = '—'): string {
    if (!value) {
      return fallback;
    }

    const datePart = value.slice(0, 10);
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(datePart);

    return match ? `${match[3]}/${match[2]}/${match[1]}` : value;
  }
}
