import { test as base } from '@playwright/test';
import { FakeBackend } from './api';

export const test = base.extend<{ backend: FakeBackend }>({
  // auto: the stubs and the login cookie must be in place before any beforeEach hook navigates.
  backend: [async ({ context, page }, use) => {
    const backend = new FakeBackend();
    await backend.install(context, page);
    await use(backend);
  }, { auto: true }]
});

export { expect } from '@playwright/test';
