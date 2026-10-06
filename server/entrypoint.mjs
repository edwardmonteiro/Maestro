// Render supplies the public origin; local Compose may set APP_ORIGIN explicitly.
if (!process.env.APP_ORIGIN && process.env.RENDER_EXTERNAL_URL) {
  process.env.APP_ORIGIN = new URL(process.env.RENDER_EXTERNAL_URL).origin;
}
if (!process.env.OWNER_TOKEN || process.env.OWNER_TOKEN.length < 24) {
  throw new Error('Configure OWNER_TOKEN with at least 24 random characters.');
}
await import('./dist/server/server/index.js');
