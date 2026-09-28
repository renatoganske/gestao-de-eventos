/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // ADR-0012: proxy do Vite pro backend em dev, sem precisar configurar CORS no Spring
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    // ADR-0019: Vitest reaproveita esta mesma config (jsdom para simular DOM em componente React)
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
  },
})
