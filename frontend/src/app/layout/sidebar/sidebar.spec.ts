import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { APP_NAVIGATION } from '../navigation/app-navigation';
import { Sidebar } from './sidebar';

describe('Sidebar', () => {
  let fixture: ComponentFixture<Sidebar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Sidebar],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Sidebar);
    fixture.detectChanges();
  });

  it('should render every navigation item', () => {
    const element = fixture.nativeElement as HTMLElement;
    const links = Array.from(element.querySelectorAll<HTMLAnchorElement>('.sidebar__link'));

    expect(links).toHaveLength(APP_NAVIGATION.length);

    expect(links.map((link) => link.textContent?.trim())).toEqual(
      APP_NAVIGATION.map((item) => item.label),
    );
  });

  it('should expose an accessible navigation landmark', () => {
    const navigation = fixture.nativeElement.querySelector('nav') as HTMLElement;

    expect(navigation.getAttribute('aria-label')).toBe('Navegação principal');
  });

  it('should emit navigated when a link is selected', () => {
    const emitSpy = vi.spyOn(fixture.componentInstance.navigated, 'emit');

    const link = fixture.nativeElement.querySelector('.sidebar__link') as HTMLAnchorElement;

    link.dispatchEvent(
      new MouseEvent('click', {
        bubbles: true,
        button: 1,
      }),
    );

    expect(emitSpy).toHaveBeenCalledOnce();
  });
});
