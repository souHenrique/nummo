import { ChangeDetectionStrategy, Component, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { LucideDynamicIcon } from '@lucide/angular';
import { APP_NAVIGATION } from '../navigation/app-navigation';

@Component({
  selector: 'app-sidebar',
  imports: [LucideDynamicIcon, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Sidebar {
  protected readonly items = APP_NAVIGATION;

  readonly navigated = output<void>();
}
