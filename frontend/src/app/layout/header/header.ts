import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../../features/auth/services/auth.service';
import { ThemeService } from '../../core/theme/theme.service';
import { Button } from '../../shared/ui/button/button';

@Component({
  selector: 'app-header',
  imports: [RouterLink, Button],
  templateUrl: './header.html',
  styleUrl: './header.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Header {
  readonly menuOpen = input(false);
  readonly menuToggle = output<void>();
  readonly skipToContent = output<void>();

  private readonly auth = inject(AuthService);
  protected readonly themeService = inject(ThemeService);

  protected logout(): void {
    this.auth.logout();
  }

  protected toggleTheme(): void {
    this.themeService.toggle();
  }

  protected focusMainContent(event: MouseEvent): void {
    event.preventDefault();
    this.skipToContent.emit();
  }
}
