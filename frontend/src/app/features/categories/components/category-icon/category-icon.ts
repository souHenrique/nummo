import { Component, computed, input } from '@angular/core';
import {
  LucideBriefcaseBusiness,
  LucideBaby,
  LucideBanknote,
  LucideCar,
  LucideChartNoAxesCombined,
  LucideCoffee,
  LucideCpu,
  LucideDynamicIcon,
  LucideEllipsis,
  LucideDumbbell,
  LucideFuel,
  LucideGift,
  LucideGraduationCap,
  LucideGamepad2,
  LucideHandCoins,
  LucideHeartHandshake,
  LucideHeartPulse,
  LucideHouse,
  LucideLaptop,
  LucideMusic,
  LucidePawPrint,
  LucidePlane,
  LucideReceiptText,
  LucideShoppingCart,
  LucideShirt,
  LucideSmartphone,
  LucideSun,
  LucideTag,
  LucideUtensils,
  LucideWifi,
  type LucideIcon,
} from '@lucide/angular';

import { CategoryIcon, DEFAULT_CATEGORY_ICON } from '../../models/category.models';

export interface CategoryIconOption {
  value: CategoryIcon;
  label: string;
  icon: LucideIcon;
}

export const CATEGORY_ICON_OPTIONS: readonly CategoryIconOption[] = [
  { value: 'TAG', label: 'Etiqueta', icon: LucideTag },
  { value: 'HOME', label: 'Casa', icon: LucideHouse },
  { value: 'FOOD', label: 'Alimentação', icon: LucideUtensils },
  { value: 'SHOPPING', label: 'Compras', icon: LucideShoppingCart },
  { value: 'TRANSPORT', label: 'Transporte', icon: LucideCar },
  { value: 'HEALTH', label: 'Saúde', icon: LucideHeartPulse },
  { value: 'EDUCATION', label: 'Educação', icon: LucideGraduationCap },
  { value: 'LEISURE', label: 'Lazer', icon: LucideSun },
  { value: 'BILLS', label: 'Contas', icon: LucideReceiptText },
  { value: 'TRAVEL', label: 'Viagem', icon: LucidePlane },
  { value: 'WORK', label: 'Trabalho', icon: LucideBriefcaseBusiness },
  { value: 'GIFT', label: 'Presentes', icon: LucideGift },
  { value: 'PET', label: 'Pet', icon: LucidePawPrint },
  { value: 'INVESTMENT', label: 'Investimentos', icon: LucideChartNoAxesCombined },
  { value: 'RESTAURANT', label: 'Restaurante', icon: LucideCoffee },
  { value: 'CLOTHING', label: 'Roupas', icon: LucideShirt },
  { value: 'FUEL', label: 'Combustível', icon: LucideFuel },
  { value: 'ENTERTAINMENT', label: 'Entretenimento', icon: LucideGamepad2 },
  { value: 'FITNESS', label: 'Academia', icon: LucideDumbbell },
  { value: 'MUSIC', label: 'Música', icon: LucideMusic },
  { value: 'PHONE', label: 'Telefone', icon: LucideSmartphone },
  { value: 'INTERNET', label: 'Internet', icon: LucideWifi },
  { value: 'TECHNOLOGY', label: 'Tecnologia', icon: LucideCpu },
  { value: 'COMPUTER', label: 'Computador', icon: LucideLaptop },
  { value: 'SALARY', label: 'Salário', icon: LucideBanknote },
  { value: 'FREELANCE', label: 'Freelance', icon: LucideHandCoins },
  { value: 'FAMILY', label: 'Família', icon: LucideBaby },
  { value: 'CHARITY', label: 'Doações', icon: LucideHeartHandshake },
  { value: 'OTHER', label: 'Outros', icon: LucideEllipsis },
];

const ICONS_BY_VALUE = new Map(CATEGORY_ICON_OPTIONS.map((option) => [option.value, option]));

@Component({
  selector: 'app-category-icon',
  imports: [LucideDynamicIcon],
  templateUrl: './category-icon.html',
  styleUrl: './category-icon.scss',
})
export class CategoryIconComponent {
  readonly icon = input<CategoryIcon | null | undefined>(DEFAULT_CATEGORY_ICON);

  readonly option = computed(
    () =>
      ICONS_BY_VALUE.get(this.icon() ?? DEFAULT_CATEGORY_ICON) ??
      ICONS_BY_VALUE.get(DEFAULT_CATEGORY_ICON)!,
  );
}
