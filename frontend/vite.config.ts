import path from "path"
import react from "@vitejs/plugin-react"
import { defineConfig } from "vite"
import { inspectAttr } from 'kimi-plugin-inspect-react'

// https://vite.dev/config/
export default defineConfig(({ command }) => ({
  // Root-absolute base so nested deep-link routes (e.g. /jobs/:id) resolve
  // their assets correctly instead of relative to the current path.
  base: '/',
  // inspectAttr is a dev-only inspection helper; keep it out of production builds.
  plugins: [...(command === 'serve' ? [inspectAttr()] : []), react()],
  define: {
    '__APP_VERSION__': JSON.stringify(process.env.VITE_APP_VERSION || 'dev'),
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
}));
