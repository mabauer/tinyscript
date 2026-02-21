import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'happy-dom',
    globals: true,
  },
  build: {
    outDir: '../src/main/resources/public',
    emptyOutDir: true,
  },
} as any)
