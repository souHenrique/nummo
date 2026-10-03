import { expect, Page, test } from '@playwright/test';

async function mockBills(page: Page): Promise<void> {
  await page.addInitScript(() => {
    sessionStorage.setItem(
      'nummo.session-expiration',
      JSON.stringify({ expiresAt: Date.now() + 3600000 }),
    );
  });
  let bills: Record<string, unknown>[] = [];
  await page.route('**/api/v1/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    const method = request.method();
    let body: unknown = {};
    let status = 200;
    if (path === '/api/v1/auth/csrf') {
      body = { token: 'csrf-e2e', headerName: 'X-XSRF-TOKEN' };
    } else if (path === '/api/v1/accounts') {
      body = [
        {
          id: 'account-jesse',
          name: 'Conta de Jesse',
          status: 'ACTIVE',
          type: 'CHECKING',
          currentBalance: 1000,
        },
      ];
    } else if (path === '/api/v1/categories') {
      body = [
        {
          id: 'transport',
          name: 'Transporte',
          type: 'EXPENSE',
          status: 'ACTIVE',
          parentCategoryId: null,
        },
      ];
    } else if (path === '/api/v1/bills' && method === 'GET') {
      const month = `${url.searchParams.get('year')}-${String(url.searchParams.get('month')).padStart(2, '0')}`;
      const content = bills.filter((bill) => String(bill['dueDate']).startsWith(month));
      body = {
        content,
        number: 0,
        size: 20,
        totalPages: content.length ? 1 : 0,
        totalElements: content.length,
      };
    } else if (path === '/api/v1/bills' && method === 'POST') {
      const data = request.postDataJSON();
      expect(request.headers()['x-xsrf-token']).toBe('csrf-e2e');
      bills = Array.from({ length: data.installmentCount }, (_, index) => {
        const [year, month, day] = data.firstDueDate.split('-').map(Number);
        const date = new Date(year, month - 1 + index, day);
        return {
          id: `bill-${index}`,
          description: data.description,
          amount: data.amount,
          dueDate: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`,
          categoryId: data.categoryId,
          seriesId: 'series-jesse',
          installmentNumber: index + 1,
          installmentCount: data.installmentCount,
          status: 'PENDING',
          version: 0,
          paymentTransactionId: null,
        };
      });
      body = bills;
      status = 201;
    } else if (path.endsWith('/pay') && method === 'POST') {
      const data = request.postDataJSON();
      expect(data.sourceAccountId).toBe('account-jesse');
      expect(data.expectedVersion).toBe(0);
      const bill = bills.find((item) => path.includes(`/${item['id']}/`))!;
      bill['status'] = 'PAID';
      bill['version'] = 1;
      bill['paymentTransactionId'] = 'payment-jesse';
      body = bill;
    } else {
      await route.fulfill({ status: 404, contentType: 'application/json', body: '{}' });
      return;
    }
    await route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
  });
}

for (const width of [390, 1280]) {
  test(`creates monthly bills and confirms payment at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await mockBills(page);
    await page.goto('/bills');
    await expect(page.getByRole('heading', { name: 'Boletos', exact: true })).toBeVisible({
      timeout: 15_000,
    });
    await page.getByRole('button', { name: 'Novo boleto', exact: true }).click();
    await page.getByLabel(/Descrição/).fill('Moto de Jesse');
    await page.getByLabel('Tipo de boleto').selectOption('monthly');
    await page.getByLabel(/Valor de cada boleto/).fill('10025');
    await expect(page.getByLabel(/Valor de cada boleto/)).toHaveValue('R$ 100,25');
    await page.getByLabel(/Primeiro vencimento/).fill('2026-10-10');
    await page.getByLabel(/Quantidade de parcelas/).fill('36');
    await page.getByLabel(/Categoria/).selectOption('transport');
    await page.getByRole('button', { name: 'Cadastrar boleto', exact: true }).click();
    await expect(page.locator('.bills__grid')).toContainText('1 de 36');
    await expect(page.locator('.bills__grid')).toContainText('10/10/2026');
    expect(
      await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
    ).toBe(true);
    await page.getByRole('button', { name: 'Pagar', exact: true }).click();
    await page.getByLabel(/Data do pagamento/).fill('2026-09-29');
    await page.getByRole('button', { name: 'Registrar pagamento', exact: true }).click();
    await expect(page.getByRole('dialog')).toContainText('Conta de Jesse');
    await expect(page.getByRole('dialog')).toContainText('100,25');
    await page.getByRole('button', { name: 'Voltar', exact: true }).last().click();
    await expect(page.locator('.bills__grid')).not.toContainText('Ver pagamento');
    await page.getByRole('button', { name: 'Registrar pagamento', exact: true }).click();
    await page.getByRole('button', { name: 'Confirmar pagamento', exact: true }).click();
    await expect(page.locator('.bills__grid')).toContainText('Pago');
    await expect(page.getByRole('link', { name: 'Ver pagamento' })).toBeVisible();
    await page.getByLabel('Mês de vencimento').fill('2026-11');
    await expect(page.locator('.bills__grid')).toContainText('2 de 36');
    expect(
      await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
    ).toBe(true);
  });
}
