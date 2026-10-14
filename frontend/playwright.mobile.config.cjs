const { defineConfig } = require('@playwright/test')

module.exports = defineConfig({
  testDir: './tests',
  timeout: 60000,
  reporter: [['line']],
  use: {
    browserName: 'chromium',
    channel: process.env.PLAYWRIGHT_CHANNEL || 'chrome',
    actionTimeout: 15000,
    navigationTimeout: 25000,
    ignoreHTTPSErrors: true
  }
})
