import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Category } from '../../models/category.models';
import { CategoryFormComponent, CategoryFormMode } from './category-form';

describe('CategoryFormComponent', () => {
  let fixture: ComponentFixture<CategoryFormComponent>;
  let component: CategoryFormComponent;

  const parentCategory: Category = {
    id: '8c11df5a-9683-407d-a25b-3ed88e1e5e27',
    name: 'Gus Fring',
    icon: 'HOME',
    type: 'EXPENSE',
    parentCategoryId: null,
    status: 'ACTIVE',
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };

  const category: Category = {
    id: 'fac77ea6-a8ea-4e2e-a6e7-9f9bc4457cd9',
    name: 'Jesse Pinkman',
    icon: 'WORK',
    type: 'INCOME',
    parentCategoryId: null,
    status: 'INACTIVE',
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoryFormComponent],
    }).compileComponents();
  });

  function createComponent(
    mode: CategoryFormMode = 'create',
    currentCategory: Category | null = null,
    parent: Category | null = null,
    defaultType: 'INCOME' | 'EXPENSE' = 'EXPENSE',
    submitting = false,
  ): void {
    fixture = TestBed.createComponent(CategoryFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('mode', mode);
    fixture.componentRef.setInput('category', currentCategory);
    fixture.componentRef.setInput('parentCategory', parent);
    fixture.componentRef.setInput('defaultType', defaultType);
    fixture.componentRef.setInput('submitting', submitting);
    fixture.detectChanges();
  }

  function submitForm(): void {
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;

    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
  }

  it('should create a root expense category', () => {
    createComponent();
    const created = vi.fn();
    component.created.subscribe(created);

    component.form.controls.name.setValue('Walter White');
    submitForm();

    expect(created).toHaveBeenCalledWith({
      name: 'Walter White',
      icon: 'TAG',
      type: 'EXPENSE',
      parentCategoryId: null,
    });
  });

  it('should create a root income category', () => {
    createComponent('create', null, null, 'INCOME');
    const created = vi.fn();
    component.created.subscribe(created);

    component.form.controls.name.setValue('Saul Goodman');
    submitForm();

    expect(created).toHaveBeenCalledWith({
      name: 'Saul Goodman',
      icon: 'TAG',
      type: 'INCOME',
      parentCategoryId: null,
    });
  });

  it('should create a child category with the parent id', () => {
    createComponent('create', null, parentCategory);
    const created = vi.fn();
    component.created.subscribe(created);

    component.form.controls.name.setValue('Mike Ehrmantraut');
    submitForm();

    expect(created).toHaveBeenCalledWith({
      name: 'Mike Ehrmantraut',
      icon: 'TAG',
      type: 'EXPENSE',
      parentCategoryId: parentCategory.id,
    });
  });

  it('should lock the type and use the parent type for a child category', () => {
    createComponent('create', null, parentCategory, 'INCOME');

    expect(component.form.controls.type.disabled).toBe(true);
    expect(component.form.controls.type.value).toBe(parentCategory.type);
    expect(fixture.nativeElement.textContent).toContain(parentCategory.name);
  });

  it('should not submit an empty category name', () => {
    createComponent();
    const created = vi.fn();
    component.created.subscribe(created);

    submitForm();

    expect(created).not.toHaveBeenCalled();
    expect(component.form.controls.name.touched).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Informe um nome de até 120 caracteres.');
  });

  it('should load the name and status in edit mode', () => {
    createComponent('edit', category);

    expect(component.form.controls.name.value).toBe(category.name);
    expect(component.form.controls.status.value).toBe(category.status);
    expect(component.form.controls.icon.value).toBe(category.icon);
  });

  it('should not display type or parent category in edit mode', () => {
    createComponent('edit', category, parentCategory);

    expect(fixture.nativeElement.querySelector('#category-type')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Categoria pai');
  });

  it('should emit only name and status in edit mode', () => {
    createComponent('edit', category);
    const updated = vi.fn();
    component.updated.subscribe(updated);

    component.form.controls.name.setValue('Skyler White');
    component.form.controls.status.setValue('ACTIVE');
    submitForm();

    expect(updated).toHaveBeenCalledWith({
      name: 'Skyler White',
      status: 'ACTIVE',
    });
  });

  it('should emit the selected icon when editing a category', () => {
    createComponent('edit', category);
    const updated = vi.fn();
    component.updated.subscribe(updated);

    component.selectIcon('SHOPPING');
    submitForm();

    expect(updated).toHaveBeenCalledWith({
      name: category.name,
      status: category.status,
      icon: 'SHOPPING',
    });
  });

  it('should render a selectable generic icon palette', () => {
    createComponent();

    expect(fixture.nativeElement.querySelectorAll('[role="radio"]')).toHaveLength(
      fixture.componentInstance.iconOptions.length,
    );
    expect(fixture.nativeElement.querySelector('[aria-label="Alimentação"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="Salário"]')).not.toBeNull();
  });

  it('should disable both actions while submitting', () => {
    createComponent('create', null, null, 'EXPENSE', true);

    const buttons = fixture.nativeElement.querySelectorAll(
      'app-button button',
    ) as NodeListOf<HTMLButtonElement>;

    expect(buttons).toHaveLength(2);
    expect([...buttons].every((button) => button.disabled)).toBe(true);
  });

  it('should emit cancelled when the user clicks cancel', () => {
    createComponent();
    const cancelled = vi.fn();
    component.cancelled.subscribe(cancelled);

    const cancelButton = fixture.nativeElement.querySelectorAll(
      'app-button button',
    )[0] as HTMLButtonElement;
    cancelButton.click();

    expect(cancelled).toHaveBeenCalledOnce();
  });
});
