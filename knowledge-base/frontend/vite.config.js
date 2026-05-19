import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // [FIX]: 后端未启动时不再刷屏 ECONNREFUSED，静默跳过
        configure: (proxy) => {
          proxy.on('error', (err) => {
            // 只在首次连接失败时提示一次，后续静默
            if (!globalThis.__backendWarned) {
              console.log('\x1b[33m%s\x1b[0m', '[vite] 后端服务(8080)未启动，API请求将失败。请先启动后端：cd backend && mvn spring-boot:run')
              globalThis.__backendWarned = true
            }
          })
        }
      }
    }
  }
})
