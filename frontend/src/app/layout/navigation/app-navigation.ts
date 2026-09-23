import {
  LucideArrowLeftRight,
  LucideChartNoAxesCombined,
  LucideCircleDollarSign,
  LucideCreditCard,
  LucideFileText,
  LucideLandmark,
  LucideLayoutDashboard,
  LucideReceiptText,
  LucideTags,
  LucideUserRound,
  type LucideIcon,
} from '@lucide/angular';

export interface NavigationItem {
  readonly label: string;
  readonly route: string;
  readonly icon: LucideIcon;
  readonly exact?: boolean;
}

export const APP_NAVIGATION: readonly NavigationItem[] = [
  {
    label: 'Dashboard',
    route: '/dashboard',
    icon: LucideLayoutDashboard,
    exact: true,
  },
  {
    label: 'Transações',
    route: '/transactions',
    icon: LucideReceiptText,
  },
  {
    label: 'Nova transferência',
    route: '/transfers/new',
    icon: LucideArrowLeftRight,
    exact: true,
  },
  {
    label: 'Contas',
    route: '/accounts',
    icon: LucideLandmark,
  },
  {
    label: 'Categorias',
    route: '/categories',
    icon: LucideTags,
  },
  {
    label: 'Cartões',
    route: '/credit-cards',
    icon: LucideCreditCard,
  },
  {
    label: 'Faturas',
    route: '/invoices',
    icon: LucideFileText,
  },
  {
    label: 'Orçamentos',
    route: '/budgets',
    icon: LucideCircleDollarSign,
  },
  {
    label: 'Relatórios',
    route: '/reports',
    icon: LucideChartNoAxesCombined,
  },
  {
    label: 'Perfil',
    route: '/profile',
    icon: LucideUserRound,
  },
];
