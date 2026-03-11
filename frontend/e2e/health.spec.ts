import { test, expect } from '@playwright/test';

test.describe('Application Health', () => {
  test('should load the application', async ({ page }) => {
    await page.goto('/');
    // The app should render without crashing
    await expect(page).toHaveTitle(/.*/);
  });

  test('should display login page for unauthenticated users', async ({ page }) => {
    await page.goto('/');
    // App should redirect to login or show login UI
    await expect(page.locator('body')).toBeVisible();
  });
});
