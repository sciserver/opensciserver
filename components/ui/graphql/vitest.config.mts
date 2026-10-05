import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
    // Never talk to real services: environment.ts loads .env via dotenv, which does not
    // override variables that are already set, so these win over a developer's local .env.
    env: {
      FILES_BASE_URL: 'https://files.test/api/',
      COMPUTE_BASE_URL: 'https://compute.test/api/',
      RACM_BASE_URL: 'https://racm.test/',
      LOGIN_PORTAL_BASE_URL: 'https://login.test/api/'
    }
  }
});
