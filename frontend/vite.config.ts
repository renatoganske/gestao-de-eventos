/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // GDE-35: alvo do proxy configurável (VITE_API_TARGET) para poder rodar o
  // frontend local contra a API já deployada em produção, sem mexer no backend
  // -- lido de .env.local (gitignored) ou da env var do shell, com fallback
  // para o backend local de sempre.
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8080'

  return {
    plugins: [react()],
    server: {
      proxy: {
        // ADR-0012: proxy do Vite pro backend em dev, sem precisar configurar CORS no Spring
        '/api': {
          target: apiTarget,
          changeOrigin: true,
        },
      },
    },
    test: {
      // ADR-0019: Vitest reaproveita esta mesma config (jsdom para simular DOM em componente React)
      environment: 'jsdom',
      globals: true,
      setupFiles: './src/test/setup.ts',
      // Por padrao o Vitest esvazia CSS; este arquivo precisa ser lido de verdade
      // por src/styles/panelOverflow.test.ts (regressao GDE-40).
      css: { include: [/components\.css/] },
    },
  }
})
