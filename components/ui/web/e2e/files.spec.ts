import { expect, test } from './support/fixtures';

const open = async (page: import('@playwright/test').Page, query = '') => {
  await page.goto(`/web/files${query}`);
  await expect(page.getByRole('heading', { name: 'Files' })).toBeVisible();
};

const PERSISTENT = '?type=user&volume=persistent&root=Storage&owner=ana';

test.describe('volume list', () => {
  test('lists user volumes with their tags and counts, then data volumes', async ({ page, backend }) => {
    await open(page);
    await expect(page.getByRole('tab', { name: /User volumes\s*4/ })).toBeVisible();
    await expect(page.getByRole('tab', { name: /Data volumes\s*2/ })).toBeVisible();

    await expect(page.getByRole('row', { name: /FESS/ })).toContainText('Shared');
    await expect(page.getByRole('row', { name: /NotebookExamples/ })).toContainText('Read-only');
    await expect(page.getByRole('row', { name: /persistent/ })).not.toContainText('Read-only');

    await page.getByRole('tab', { name: /Data volumes/ }).click();
    await expect(page.getByRole('row', { name: /SDSS DAS/ })).toContainText('Sloan Digital Sky Survey data');
    await expect(page.getByRole('button', { name: /Create user volume/ })).toHaveCount(0);
    expect(backend.called('fileVolumes')).toHaveLength(1);
  });

  test('filters and sorts the volume list', async ({ page }) => {
    await open(page);
    await page.getByLabel('Filter volumes').fill('fe');
    await expect(page.getByRole('row', { name: /FESS/ })).toBeVisible();
    await expect(page.getByRole('row', { name: /persistent/ })).toHaveCount(0);

    await page.getByLabel('Filter volumes').fill('nothing here');
    await expect(page.getByText('No volumes found')).toBeVisible();
  });
});

test.describe('browsing', () => {
  test('opens a volume and folders, and the breadcrumb goes back up', async ({ page, backend }) => {
    await open(page);
    await page.getByRole('row', { name: /persistent/ }).click();
    await expect(page).toHaveURL(/volume=persistent/);

    const rows = page.getByRole('row');
    await expect(rows.nth(1)).toContainText('data');
    await expect(page.getByRole('row', { name: /results\.csv/ })).toContainText('2.0 KB');

    await page.getByRole('row', { name: /^.*data\b(?!.*results)/ }).first().click();
    await expect(page).toHaveURL(/path=%2Fdata/);
    await expect(page.getByRole('row', { name: /cutout\.fits/ })).toContainText('8.1 MB');

    await page.getByRole('navigation', { name: 'Breadcrumb' }).getByRole('button', { name: 'persistent' }).click();
    await expect(page.getByRole('row', { name: /results\.csv/ })).toBeVisible();
    expect(backend.called('jsonTree').length).toBeGreaterThanOrEqual(2);
  });

  test('renders the README and filters the folder', async ({ page }) => {
    await open(page, PERSISTENT);
    await expect(page.getByText('Hello from the README.')).toBeVisible();

    await page.getByLabel('Filter this folder').fill('csv');
    await expect(page.getByRole('row', { name: /results\.csv/ })).toBeVisible();
    await expect(page.getByRole('row', { name: /a\.txt/ })).toHaveCount(0);
  });

  test('only asks for the README once the auth token is known', async ({ page, backend }) => {
    await open(page, PERSISTENT);
    await expect(page.getByText('Hello from the README.')).toBeVisible();
    expect(backend.readmeTokens.length).toBeGreaterThan(0);
    expect(backend.readmeTokens.every((token) => token === 'e2e-token')).toBe(true);
  });

  test('shows an error with a retry when a folder fails to load', async ({ page, backend }) => {
    backend.failNext('jsonTree', 'Missing required permissions on volume');
    await open(page, PERSISTENT);
    await expect(page.getByText(/Missing required permissions/)).toBeVisible();
    await page.getByRole('button', { name: 'Retry' }).click();
    await expect(page.getByRole('row', { name: /results\.csv/ })).toBeVisible();
  });

  test('selecting rows shows the bulk bar', async ({ page }) => {
    await open(page, PERSISTENT);
    await page.getByRole('button', { name: 'Select a.txt' }).click();
    await page.getByRole('button', { name: 'Select results.csv' }).click();
    await expect(page.getByText('2 selected')).toBeVisible();
    await page.getByRole('button', { name: 'Clear selection' }).click();
    await expect(page.getByText('2 selected')).toHaveCount(0);
  });
});

test.describe('permissions', () => {
  test('writable volumes show the write actions', async ({ page }) => {
    await open(page, PERSISTENT);
    await expect(page.getByRole('button', { name: /New folder/ })).toBeVisible();
    await expect(page.getByRole('button', { name: /Upload/ })).toBeVisible();
  });

  test('read-only user volumes and data volumes hide them', async ({ page }) => {
    await open(page, '?type=user&volume=NotebookExamples&root=Storage&owner=arik');
    await expect(page.getByText('Read-only: shared with you')).toBeVisible();
    await expect(page.getByRole('button', { name: /New folder/ })).toHaveCount(0);
    await expect(page.getByRole('button', { name: /Upload/ })).toHaveCount(0);

    await open(page, '?type=data&volume=sdss_das');
    await expect(page.getByText('Data volumes are read-only')).toBeVisible();
    await expect(page.getByRole('button', { name: /New folder/ })).toHaveCount(0);
  });
});
