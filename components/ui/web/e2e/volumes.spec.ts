import { expect, test } from './support/fixtures';

test.beforeEach(async ({ page }) => {
  await page.goto('/web/files');
  await expect(page.getByRole('row', { name: /persistent/ })).toBeVisible();
});

test('creates a user volume in a root volume that allows it', async ({ page, backend }) => {
  await page.getByRole('button', { name: /Create user volume/ }).click();
  const dialog = page.getByRole('dialog');

  await dialog.getByLabel('Name').fill('bad/name');
  await expect(dialog).toContainText('can’t contain /');
  await dialog.getByLabel('Name').fill('survey');
  await dialog.getByLabel('Description').fill('Survey data');
  await dialog.getByLabel('Root volume').click();
  await page.getByRole('option', { name: /Storage/ }).click();
  await dialog.getByRole('button', { name: 'Create volume' }).click();

  await expect(page.getByRole('row', { name: /survey/ })).toBeVisible();
  expect(backend.called('createUserVolume')[0].variables).toEqual({ rootVolumeName: 'Storage', owner: 'ana', name: 'survey', description: 'Survey data' });
});

test('edits and deletes a volume, only where the server allows', async ({ page, backend }) => {
  // persistent has no delete action
  await page.getByRole('button', { name: 'More actions for persistent' }).click();
  await expect(page.getByRole('menuitem', { name: /Edit/ })).toBeVisible();
  await expect(page.getByRole('menuitem', { name: /Delete/ })).toHaveCount(0);
  await page.keyboard.press('Escape');

  await page.getByRole('button', { name: 'More actions for FESS' }).click();
  await page.getByRole('menuitem', { name: /Edit/ }).click();
  await page.getByRole('dialog').getByLabel('Name').fill('FESS-2');
  await page.getByRole('dialog').getByRole('button', { name: 'Save changes' }).click();
  await expect(page.getByRole('row', { name: /FESS-2/ })).toBeVisible();
  expect(backend.called('updateUserVolume')[0].variables).toMatchObject({ name: 'FESS', newName: 'FESS-2' });

  await page.getByRole('button', { name: 'More actions for FESS-2' }).click();
  await page.getByRole('menuitem', { name: /Delete/ }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Delete volume' }).click();
  await expect(page.getByRole('row', { name: /FESS-2/ })).toHaveCount(0);
});

test('read-only shared volumes have no management menu', async ({ page }) => {
  await expect(page.getByRole('button', { name: 'More actions for NotebookExamples' })).toHaveCount(0);
});

test('shares a volume with a group, telling it apart from a user with the same id', async ({ page, backend }) => {
  await page.getByRole('button', { name: 'More actions for FESS' }).click();
  await page.getByRole('menuitem', { name: /Sharing/ }).click();
  const dialog = page.getByRole('dialog');

  await expect(dialog.getByText('bob')).toBeVisible();
  await expect(dialog.getByText('astro-group')).toBeVisible();

  await dialog.getByRole('button', { name: 'Groups' }).click();
  await expect(dialog.getByText('carol')).toHaveCount(0);
  await dialog.getByRole('button', { name: /astro-group/ }).click();
  await dialog.getByRole('checkbox', { name: 'Write' }).last().check();
  // remove bob
  await dialog.getByRole('button', { name: 'Remove bob' }).click();
  await dialog.getByRole('button', { name: 'Save changes' }).click();

  await expect(page.getByText('Updated sharing for “FESS”')).toBeVisible();
  const payload = backend.called('shareUserVolume')[0].variables.sharedWith;
  expect(payload).toEqual(expect.arrayContaining([
    expect.objectContaining({ name: 'astro-group', type: 'GROUP', allowedActions: ['read', 'write'] }),
    expect.objectContaining({ name: 'bob', type: 'USER', allowedActions: [] })
  ]));
});

test('loads the sharing directory only when the dialog opens', async ({ page, backend }) => {
  expect(backend.called('publicUsersAndGroups')).toHaveLength(0);
  expect(backend.called('sharingDetails')).toHaveLength(0);
  await page.getByRole('button', { name: 'More actions for FESS' }).click();
  await page.getByRole('menuitem', { name: /Sharing/ }).click();
  await expect(page.getByRole('dialog').getByText('astro-group')).toBeVisible();
  expect(backend.called('sharingDetails')).toHaveLength(1);
  expect(backend.called('publicUsersAndGroups')).toHaveLength(1);
});

test('shows quotas, red when full', async ({ page }) => {
  await page.getByRole('button', { name: 'View quotas' }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog).toContainText('ana’s user volumes');
  await expect(dialog).toContainText('500.0 MB used out of 1.0 GB');
  await expect(dialog).toContainText('scratch');
  await expect(dialog).toContainText('2.0 GB used out of 2.0 GB');
});

test('a failed create keeps the dialog open with what was typed', async ({ page, backend }) => {
  backend.failNext('createUserVolume', 'A volume with that name exists');
  await page.getByRole('button', { name: /Create user volume/ }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Name').fill('survey');
  await dialog.getByLabel('Description').fill('Survey data');
  await dialog.getByLabel('Root volume').click();
  await page.getByRole('option', { name: /Storage/ }).click();
  await dialog.getByRole('button', { name: 'Create volume' }).click();

  await expect(dialog).toContainText('Could not create the volume: A volume with that name exists');
  await expect(dialog.getByLabel('Name')).toHaveValue('survey');
  await expect(dialog.getByLabel('Description')).toHaveValue('Survey data');

  await dialog.getByRole('button', { name: 'Create volume' }).click();
  await expect(page.getByRole('row', { name: /survey/ })).toBeVisible();
});

test('a failed sharing save keeps the dialog and the edits', async ({ page, backend }) => {
  backend.failNext('shareUserVolume', 'Not allowed');
  await page.getByRole('button', { name: 'More actions for FESS' }).click();
  await page.getByRole('menuitem', { name: /Sharing/ }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByText('bob')).toBeVisible();
  await dialog.getByRole('checkbox', { name: 'Write' }).first().check();
  await dialog.getByRole('button', { name: 'Save changes' }).click();

  await expect(dialog).toContainText('Could not update sharing for “FESS”: Not allowed');
  await expect(dialog.getByRole('checkbox', { name: 'Write' }).first()).toBeChecked();
  await dialog.getByRole('button', { name: 'Save changes' }).click();
  await expect(page.getByText('Updated sharing for “FESS”')).toBeVisible();
});
