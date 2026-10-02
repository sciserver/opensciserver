import { expect, test } from './support/fixtures';

test.beforeEach(async ({ page }) => {
  await page.goto('/web/jobs/new');
  await page.getByRole('tab', { name: 'Working Directory' }).click();
});

test('uses an automatic jobs folder until the box is unchecked', async ({ page, backend }) => {
  await expect(page.getByText('A new folder will be created in jobs/')).toBeVisible();
  await expect(page.getByText('/home/idies/workspace/Temporary/ana/jobs/', { exact: false }).first()).toBeVisible();
  // the picker is not even loaded while it cannot be used
  expect(backend.called('fileVolumes')).toHaveLength(0);

  await page.getByRole('checkbox').first().uncheck();
  await expect(page.getByRole('tab', { name: /User volumes/ })).toBeVisible();
  expect(backend.called('fileVolumes')).toHaveLength(1);
});

test('picks a subfolder of a volume as the working directory', async ({ page }) => {
  await page.getByRole('checkbox').first().uncheck();
  // starts in the user's persistent volume
  await expect(page.getByRole('row', { name: /notebooks/ })).toBeVisible();
  await page.getByRole('row', { name: /notebooks/ }).click();
  await page.getByRole('button', { name: 'Use as working directory' }).click();

  await expect(page.getByText('/home/idies/workspace/Storage/ana/persistent/notebooks/', { exact: false })).toBeVisible();
});

test('warns when the chosen volume is not mounted in the job', async ({ page }) => {
  await page.getByRole('checkbox').first().uncheck();
  await page.getByRole('navigation', { name: 'Breadcrumb' }).getByRole('button', { name: /User volumes/ }).click();

  // read-only volumes cannot be picked
  await expect(page.getByRole('row', { name: /NotebookExamples/ })).toHaveAttribute('title', /Read-only/);

  await page.getByRole('row', { name: /FESS/ }).click();
  await page.getByRole('button', { name: 'Use as working directory' }).click();
  await expect(page.getByText('/home/idies/workspace/Storage/ana/FESS/', { exact: false })).toBeVisible();
  await expect(page.getByText(/not selected for the job/)).toBeVisible();

  // persistent is mounted automatically, so no warning there (the picker is still showing the volume list)
  await page.getByRole('row', { name: /persistent/ }).click();
  await page.getByRole('button', { name: 'Use as working directory' }).click();
  await expect(page.getByText(/not selected for the job/)).toHaveCount(0);
});
