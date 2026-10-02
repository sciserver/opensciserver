import { expect, test } from './support/fixtures';

const PERSISTENT = '/web/files?type=user&volume=persistent&root=Storage&owner=ana';

test.beforeEach(async ({ page }) => {
  await page.goto(PERSISTENT);
  await expect(page.getByRole('row', { name: /results\.csv/ })).toBeVisible();
});

test('creates a folder with a suggested name and rejects duplicates', async ({ page, backend }) => {
  await page.getByRole('button', { name: /New folder/ }).click();
  const name = page.getByLabel('Name');
  await expect(name).toHaveValue('New folder');

  await name.fill('data');
  await expect(page.getByText('A file or folder with this name exists')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Confirm' })).toBeDisabled();

  await name.fill('results-2026');
  await name.press('Enter');
  await expect(page.getByRole('row', { name: /results-2026/ })).toBeVisible();
  expect(backend.called('createFolder')[0].variables).toMatchObject({ path: '', name: 'results-2026' });
});

test('renames a file from its menu', async ({ page, backend }) => {
  await page.getByRole('button', { name: 'More actions for a.txt' }).click();
  await page.getByRole('menuitem', { name: /Rename/ }).click();
  await page.getByLabel('Name').fill('b.txt');
  await page.getByLabel('Name').press('Enter');

  await expect(page.getByRole('row', { name: /b\.txt/ })).toBeVisible();
  await expect(page.getByRole('row', { name: /a\.txt/ })).toHaveCount(0);
  expect(backend.called('renameFile')[0].variables).toMatchObject({ name: 'a.txt', newName: 'b.txt' });
});

test('asks before deleting, and cancel keeps the file', async ({ page, backend }) => {
  await page.getByRole('button', { name: 'More actions for a.txt' }).click();
  await page.getByRole('menuitem', { name: /Delete/ }).click();
  await expect(page.getByRole('dialog')).toContainText('permanently deleted');

  await page.getByRole('button', { name: 'Cancel' }).click();
  await expect(page.getByRole('row', { name: /a\.txt/ })).toBeVisible();
  expect(backend.called('deleteFile')).toHaveLength(0);

  await page.getByRole('button', { name: 'More actions for a.txt' }).click();
  await page.getByRole('menuitem', { name: /Delete/ }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Delete' }).click();
  await expect(page.getByRole('row', { name: /a\.txt/ })).toHaveCount(0);
  expect(backend.called('deleteFile')).toHaveLength(1);
});

test('deletes several selected items in one go', async ({ page, backend }) => {
  await page.getByRole('button', { name: 'Select a.txt' }).click();
  await page.getByRole('button', { name: 'Select results.csv' }).click();
  await page.getByRole('button', { name: /^delete\s*Delete$/ }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Delete' }).click();

  await expect(page.getByText('Deleted 2 items')).toBeVisible();
  expect(backend.called('deleteFile').map((call) => call.variables.name).sort()).toEqual(['a.txt', 'results.csv']);
});

test('copies into a folder and numbers a name that is already taken', async ({ page, backend }) => {
  // data/ already holds cutout.fits; copy a file with the same name from the volume root first.
  await page.getByRole('row', { name: /^.*data\b(?!.*results)/ }).first().click();
  await expect(page.getByRole('row', { name: /cutout\.fits/ })).toBeVisible();
  await page.getByRole('button', { name: 'More actions for cutout.fits' }).click();
  await page.getByRole('menuitem', { name: /Copy to/ }).click();

  const dialog = page.getByRole('dialog');
  await expect(dialog).toContainText('Copy “cutout.fits” to…');
  await dialog.getByRole('button', { name: 'Copy here' }).click();

  await expect(page.getByText('Copied “cutout.fits”')).toBeVisible();
  const copy = backend.called('copyFile')[0].variables;
  expect(copy).toMatchObject({ name: 'cutout.fits', newName: 'cutout.fits (1)' });
  expect(copy.destination.path).toBe('/data');
});

test('moves a file to another folder', async ({ page, backend }) => {
  await page.getByRole('button', { name: 'More actions for a.txt' }).click();
  await page.getByRole('menuitem', { name: /Move to/ }).click();

  const dialog = page.getByRole('dialog');
  await dialog.getByRole('row', { name: /notebooks/ }).dblclick();
  await dialog.getByRole('button', { name: 'Move here' }).click();

  await expect(page.getByText('Moved “a.txt”')).toBeVisible();
  await expect(page.getByRole('row', { name: /a\.txt/ })).toHaveCount(0);
  expect(backend.called('moveFile')[0].variables.destination.path).toBe('/notebooks');
});

test('uploads files and reloads once', async ({ page, backend }) => {
  const before = backend.called('jsonTree').length;
  await page.getByTestId('upload-input').setInputFiles([
    { name: 'one.txt', mimeType: 'text/plain', buffer: Buffer.from('1') },
    { name: 'two.txt', mimeType: 'text/plain', buffer: Buffer.from('22') }
  ]);

  await expect(page.getByRole('row', { name: /one\.txt/ })).toBeVisible();
  await expect(page.getByRole('row', { name: /two\.txt/ })).toBeVisible();
  expect(backend.uploads.sort()).toEqual(['Storage/ana/persistent/one.txt', 'Storage/ana/persistent/two.txt']);
  expect(backend.called('jsonTree').length - before).toBe(1);
  // finished uploads clear themselves
  await expect(page.getByRole('status', { name: 'Uploads' })).toHaveCount(0, { timeout: 8000 });
});

test('says why a delete failed and keeps the item selected', async ({ page, backend }) => {
  backend.failNext('deleteFile', 'Missing required permissions');
  await page.getByRole('button', { name: 'Select a.txt' }).click();
  await page.getByRole('button', { name: /^delete\s*Delete$/ }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Delete' }).click();

  await expect(page.getByText(/Could not delete “a\.txt”: Missing required permissions/)).toBeVisible();
  await expect(page.getByText('1 selected')).toBeVisible();
});

test.describe('copy and move dialog', () => {
  test('moving into the same folder keeps the dialog open with a message', async ({ page, backend }) => {
    await page.getByRole('button', { name: 'More actions for a.txt' }).click();
    await page.getByRole('menuitem', { name: /Move to/ }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('button', { name: 'Move here' }).click();

    await expect(dialog).toContainText('already in this folder');
    expect(backend.called('moveFile')).toHaveLength(0);

    // pick another folder in the same dialog and carry on
    await dialog.getByRole('row', { name: /notebooks/ }).dblclick();
    await dialog.getByRole('button', { name: 'Move here' }).click();
    await expect(page.getByText('Moved “a.txt”')).toBeVisible();
  });

  test('a folder cannot be copied or moved into itself', async ({ page, backend }) => {
    await page.getByRole('button', { name: 'More actions for data' }).click();
    await page.getByRole('menuitem', { name: /Copy to/ }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('row', { name: /data/ }).dblclick();
    await dialog.getByRole('button', { name: 'Copy here' }).click();

    await expect(dialog).toContainText('can’t be copied into itself');
    expect(backend.called('copyFile')).toHaveLength(0);
  });

  test('shows the server reason and stays open when nothing could be copied', async ({ page, backend }) => {
    backend.failNext('copyFile', 'Missing required permissions on volume');
    await page.getByRole('button', { name: 'More actions for a.txt' }).click();
    await page.getByRole('menuitem', { name: /Copy to/ }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('row', { name: /notebooks/ }).dblclick();
    await dialog.getByRole('button', { name: 'Copy here' }).click();

    await expect(dialog).toContainText('Could not copy “a.txt”: Missing required permissions on volume');
    // the dialog is still usable: trying again works
    await dialog.getByRole('button', { name: 'Copy here' }).click();
    await expect(page.getByText('Copied “a.txt”')).toBeVisible();
  });
});
