import path from 'node:path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const staticOutDir = path.resolve(
  __dirname,
  '../ms-spring-boot-autoconfigure/src/main/resources/static/ms-console',
)

export default defineConfig({
  plugins: [react()],
  base: '/ms-console/',
  build: {
    outDir: staticOutDir,
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/ms-console/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
