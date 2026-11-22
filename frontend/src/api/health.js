// All backend calls live in this folder. The path starts with /api, which the
// Vite dev server forwards to Spring Boot (see vite.config.js).
export async function fetchHealth() {
  const response = await fetch('/api/v1/health');
  if (!response.ok) {
    throw new Error(`The backend answered with status ${response.status}`);
  }
  return response.json();
}
