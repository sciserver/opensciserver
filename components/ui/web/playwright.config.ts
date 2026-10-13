import { defineConfig, devices } from '@playwright/test';

const PORT = 3200;

// The app is pointed at made-up service URLs. Every request to them is answered by the stubs in e2e/support/api.ts.
export const SERVICES = {
  graphql: 'http://localhost:3100/graphql',
  fileService: 'http://localhost:3100/fileservice/api/'
};

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: `http://localhost:${PORT}`,
    trace: 'retain-on-failure'
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: `npx next dev -p ${PORT}`,
    url: `http://localhost:${PORT}/web/login`,
    reuseExistingServer: false,
    timeout: 180_000,
    env: {
      NEXT_DIST_DIR: '.next-e2e',
      NEXT_PUBLIC_BASE_PATH: '/web',
      NEXT_PUBLIC_GRAPHQL_URL: SERVICES.graphql,
      NEXT_PUBLIC_FILE_SERVICE_URL: SERVICES.fileService,
      NEXT_PUBLIC_DASHBOARD_URL: 'http://localhost:3100/dashboard',
      NEXT_PUBLIC_FILES_URL: 'http://localhost:3100/dashboard/files',
      NEXT_PUBLIC_NOTEBOOKS_URL: 'http://localhost:3100/compute/',
      NEXT_PUBLIC_HELPDESK_EMAIL: 'helpdesk@example.org',
      NEXT_PUBLIC_JOB_WORKSPACE_PATH: '/home/idies/workspace/',
      NEXT_PUBLIC_NEW_JOB_DOMAIN_NAME_DEFAULT: 'Small Jobs Domain',
      NEXT_PUBLIC_NEW_JOB_IMAGE_NAME_DEFAULT: 'SciServer Essentials 4.0'
    }
  }
});
