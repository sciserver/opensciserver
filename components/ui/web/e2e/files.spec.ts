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
    await expect(page.getByRole('tab', { name: /Data volumes\s*4/ })).toBeVisible();

    await expect(page.getByRole('row', { name: /FESS/ })).toContainText('Shared');
    await expect(page.getByRole('row', { name: /NotebookExamples/ })).toContainText('Read-only');
    await expect(page.getByRole('row', { name: /persistent/ })).not.toContainText('Read-only');

    await page.getByRole('tab', { name: /Data volumes/ }).click();
    await expect(page.getByRole('row', { name: /SDSS DAS/ })).toContainText('Sloan Digital Sky Survey data');
    await expect(page.getByRole('button', { name: /Create user volume/ })).toHaveCount(0);
    expect(backend.called('fileVolumes')).toHaveLength(1);
  });

  test('switching tabs after filtering does not carry data volumes into the user list', async ({ page }) => {
    await open(page);
    await page.getByRole('tab', { name: /Data volumes/ }).click();
    await page.getByLabel('Filter volumes').fill('droid');
    await expect(page.getByRole('row', { name: /droid-workspace/ })).toHaveCount(2);

    await page.getByRole('tab', { name: /User volumes/ }).click();
    await expect(page.getByRole('row', { name: /persistent/ })).toBeVisible();
    await expect(page.getByRole('row', { name: /droid-workspace/ })).toHaveCount(0);
    // header row + the four user volumes, nothing left over
    await expect(page.getByRole('row')).toHaveCount(5);
    await expect(page.getByLabel('Filter volumes')).toHaveValue('');
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

  test('a deep link opens the folder directly, without flashing the volume list', async ({ page, backend }) => {
    await open(page, PERSISTENT);
    await expect(page.getByRole('row', { name: /results\.csv/ })).toBeVisible();
    // Only the one volumes request, and no volume row was ever listed for another volume.
    expect(backend.called('fileVolumes')).toHaveLength(1);
    await expect(page.getByRole('row', { name: /NotebookExamples/ })).toHaveCount(0);
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

  test('shows a pointer to the README and jumps to it', async ({ page }) => {
    await open(page, PERSISTENT);
    await expect(page.getByText('It is shown at the bottom of the list.')).toBeVisible();
    await page.getByRole('button', { name: /Jump to README/ }).click();
    await expect(page.getByText('Hello from the README.')).toBeInViewport();

    // a folder without a README has no pointer
    await open(page, '?type=user&volume=FESS&root=Storage&owner=ana');
    await expect(page.getByRole('row', { name: /persistent/ })).toHaveCount(0);
    await expect(page.getByText('It is shown at the bottom of the list.')).toHaveCount(0);
  });

  test('offers Download only when every selected item is a file', async ({ page }) => {
    await open(page, PERSISTENT);
    await page.getByRole('button', { name: 'Select a.txt' }).click();
    await page.getByRole('button', { name: 'Select results.csv' }).click();
    await expect(page.getByRole('button', { name: /^download\s*Download$/ })).toBeVisible();

    await page.getByRole('button', { name: 'Select data' }).click();
    await expect(page.getByText('3 selected')).toBeVisible();
    await expect(page.getByRole('button', { name: /^download\s*Download$/ })).toHaveCount(0);
    // the other actions are still there
    await expect(page.getByRole('button', { name: /Copy/ })).toBeVisible();
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
    await expect(page.getByText('Read-only: you can only read this data volume')).toBeVisible();
    await expect(page.getByRole('button', { name: /New folder/ })).toHaveCount(0);
  });

  test('data volumes follow their own allowedActions', async ({ page }) => {
    await open(page, '?type=data');
    await expect(page.getByRole('row', { name: /SDSS DAS/ })).toContainText('Read-only');
    await expect(page.getByRole('row', { name: /Gaia DR3/ })).toContainText('Read/write');

    // a data volume with write access offers the write actions
    await open(page, '?type=data&volume=gaia_dr3');
    await expect(page.getByRole('button', { name: /New folder/ })).toBeVisible();
    await expect(page.getByRole('button', { name: /Upload/ })).toBeVisible();
  });
});
