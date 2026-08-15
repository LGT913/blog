import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  define: {
    // sockjs-client 依赖 Node.js global 对象，在浏览器中 polyfill
    global: 'globalThis'
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true
      }
    }
  }
})
