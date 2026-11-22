import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server forwards every /api request to Spring Boot, so the browser
// only ever talks to one origin (localhost:5173). This is what lets the
// refresh-token cookie work later without any CORS setup.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
